package snow.player;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ServiceInfo;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.media.MediaBrowserCompat;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.view.View;
import android.widget.RemoteViews;

import androidx.annotation.DrawableRes;
import androidx.annotation.IdRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.media.MediaBrowserServiceCompat;
import androidx.media.session.MediaButtonReceiver;

import com.bumptech.glide.Glide;
import com.bumptech.glide.RequestBuilder;
import com.bumptech.glide.load.resource.bitmap.GranularRoundedCorners;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;
import com.google.common.base.Objects;
import com.google.common.base.Preconditions;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import channel.helper.ChannelHelper;
import channel.helper.Dispatcher;
import channel.helper.DispatcherUtil;
import channel.helper.pipe.CustomActionPipe;

import channel.helper.pipe.MessengerPipe;
import media.helper.HeadsetHookHelper;

import snow.player.effect.AudioEffectManager;
import snow.player.media.MediaMusicPlayer;
import snow.player.media.MusicItem;
import snow.player.media.MusicPlayer;
import snow.player.playlist.PlaylistManager;


public class PlayerService extends MediaBrowserServiceCompat implements PlayerManager {
    public static final String DEFAULT_MEDIA_ROOT_ID = "root";

    private static final String NAME_COMPONENT_FACTORY = "component-factory";

    private String mPersistentId;
    private int mNotificationId;

    private PlayerConfig mPlayerConfig;
    private PlayerState mPlayerState;

    private PlaylistManager mPlaylistManager;
    private PlayerImp mPlayer;
    private CustomActionPipe mControllerPipe;

    private HashMap<String, OnCommandCallback> mCommandCallbackMap;

    private boolean mForeground;

    private NotificationManager mNotificationManager;

    private Map<String, Runnable> mStartCommandActionMap;

    private MediaSessionCompat mMediaSession;
    private PlaybackStateCompat.Builder mPlaybackStateBuilder;
    private MediaMetadataCompat.Builder mMediaMetadataBuilder;

    private HeadsetHookHelper mHeadsetHookHelper;

    private RemoteView mRemoteView;

    @Nullable
    private AudioEffectManager mAudioEffectManager;

    private ComponentFactory mComponentFactory;

    @Nullable
    private HistoryRecorder mHistoryRecorder;

    @Override
    public void onCreate() {
        super.onCreate();
        startService(new Intent(this, this.getClass()));

        mPersistentId = getPersistentId();
        mNotificationId = getNotificationId();
        mCommandCallbackMap = new HashMap<>();
        mStartCommandActionMap = new HashMap<>();

        mNotificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

        initComponentFactory();
        initPlayerConfig();
        initAudioEffectManager();
        initPlayerState();
        initPlaylistManager();
        initPlayer();
        initControllerPipe();
        initRemoteViewManager();
        initHeadsetHookHelper();
        initMediaSession();
        initHistoryRecorder();

        updateNotificationView();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        MediaButtonReceiver.handleIntent(mMediaSession, intent);

        Runnable task = mStartCommandActionMap.get(intent.getAction());
        if (task != null) {
            task.run();
        }
        return super.onStartCommand(intent, flags, startId);
    }

    @Nullable
    @Override
    public BrowserRoot onGetRoot(@NonNull String clientPackageName, int clientUid, @Nullable Bundle rootHints) {
        return new BrowserRoot(DEFAULT_MEDIA_ROOT_ID, null);
    }

    @Override
    public void onLoadChildren(@NonNull String parentId, @NonNull Result<List<MediaBrowserCompat.MediaItem>> result) {
        result.sendResult(null);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        stopForegroundEx(true);

        mRemoteView.onRelease();

        mMediaSession.release();
        mPlayer.release();

        mPlayer = null;

        if (mAudioEffectManager != null) {
            mAudioEffectManager.release();
        }
    }

    private void initComponentFactory() {
        try {
            ComponentName service = new ComponentName(this, this.getClass());
            ServiceInfo serviceInfo = getPackageManager().getServiceInfo(service, PackageManager.GET_META_DATA);
            if (serviceInfo.metaData == null) {
                return;
            }

            String factoryName = serviceInfo.metaData.getString(NAME_COMPONENT_FACTORY);
            if (factoryName == null) {
                return;
            }

            Class<?> clazz = Class.forName(factoryName);
            mComponentFactory = (ComponentFactory) clazz.newInstance();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
    }

    private void initPlayerConfig() {
        mPlayerConfig = new PlayerConfig(this, mPersistentId);
    }

    private void initPlayerState() {
        mPlayerState = new PersistentPlayerState(this, mPersistentId);
    }

    private void initPlaylistManager() {
        mPlaylistManager = new PlaylistManager(this, mPersistentId) {
        };
    }

    private void initPlayer() {
        mPlayer = new PlayerImp(this,
                mPlayerConfig,
                mPlayerState,
                mPlaylistManager);
    }

    private void initControllerPipe() {
        final Dispatcher playerManagerDispatcher
                = ChannelHelper.newDispatcher(PlayerManager.class, this);

        final Dispatcher playerDispatcher =
                ChannelHelper.newDispatcher(Player.class, mPlayer);

        mControllerPipe = new CustomActionPipe(
                DispatcherUtil.merge(playerManagerDispatcher, playerDispatcher)
        );
    }

    private void initRemoteViewManager() {
        RemoteView remoteView = createRemoteView();

        if (remoteView == null) {
            return;
        }

        remoteView.init(this);
        MusicItem musicItem = getPlayingMusicItem();

        if (musicItem != null) {
            remoteView.setPlayingMusicItem(musicItem);
        }

        mRemoteView = remoteView;
    }

    private void initHeadsetHookHelper() {
        mHeadsetHookHelper = new HeadsetHookHelper(new HeadsetHookHelper.OnHeadsetHookClickListener() {
            @Override
            public void onHeadsetHookClicked(int clickCount) {
                switch (clickCount) {
                    case 1:
                        playOrPause();
                        break;
                    case 2:
                        skipToNext();
                        break;
                    case 3:
                        skipToPrevious();
                        break;
                }
            }
        });
    }

    private void initMediaSession() {
        mPlaybackStateBuilder = new PlaybackStateCompat.Builder();
        mMediaMetadataBuilder = new MediaMetadataCompat.Builder();

        mMediaSession = new MediaSessionCompat(this, this.getClass().getName());

        mMediaSession.setPlaybackState(
                buildPlaybackState(
                        PlaybackStateCompat.STATE_NONE,
                        getPlayerState().getPlayProgress(),
                        System.currentTimeMillis(),
                        PlaybackStateCompat.ACTION_PLAY |
                                PlaybackStateCompat.ACTION_PLAY_PAUSE |
                                PlaybackStateCompat.ACTION_SKIP_TO_QUEUE_ITEM |
                                PlaybackStateCompat.ACTION_SKIP_TO_NEXT |
                                PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS |
                                PlaybackStateCompat.ACTION_STOP)
        );

        mMediaSession.setMetadata(getFreshMediaMetadata());
        mMediaSession.setCallback(new MediaSessionCallbackImp());

        setSessionToken(mMediaSession.getSessionToken());
    }

    private void initAudioEffectManager() {
        mAudioEffectManager = createAudioEffectManager();

        if (mAudioEffectManager == null) {
            return;
        }

        Bundle config = mPlayerConfig.getAudioEffectConfig();
        if (config.isEmpty()) {
            config = mAudioEffectManager.getDefaultConfig();
            mPlayerConfig.setAudioEffectConfig(config);
        }
        mAudioEffectManager.init(config);
    }

    private void initHistoryRecorder() {
        mHistoryRecorder = createHistoryRecorder();
    }

    private PlaybackStateCompat buildPlaybackState(int state,
                                                   int position,
                                                   long updateTime,
                                                   long actions) {
        return buildPlaybackState(state, position, updateTime, actions, 0, "");
    }

    private PlaybackStateCompat buildPlaybackState(int state,
                                                   int position,
                                                   long updateTime,
                                                   long actions,
                                                   int errorCode,
                                                   String errorMessage) {
        return mPlaybackStateBuilder.setState(state, position, 1.0F, updateTime)
                .setErrorMessage(errorCode, errorMessage)
                .setActions(actions)
                .build();
    }

    private MediaMetadataCompat getFreshMediaMetadata() {
        MusicItem musicItem = getPlayerState().getMusicItem();

        if (musicItem != null) {
            return mMediaMetadataBuilder.putString(MediaMetadataCompat.METADATA_KEY_TITLE, musicItem.getTitle())
                    .putString(MediaMetadataCompat.METADATA_KEY_ARTIST, musicItem.getArtist())
                    .putString(MediaMetadataCompat.METADATA_KEY_ALBUM, musicItem.getAlbum())
                    .putString(MediaMetadataCompat.METADATA_KEY_DISPLAY_ICON_URI, musicItem.getIconUri())
                    .putLong(MediaMetadataCompat.METADATA_KEY_DURATION, musicItem.getDuration())
                    .build();
        }

        return null;
    }

    @Nullable
    protected final RemoteView createRemoteView() {
        if (injectRemoteView()) {
            return mComponentFactory.createRemoteView();
        }

        return onCreateRemoteView();
    }

    /***
     * 创建一个通知栏控制器，你可以通过覆盖该方法来提供自定义的通知栏控制器。
     *
     * 该方法默认返回 {@link SimpleRemoteView}，如果你不需要在通知栏中显示控制器，可以覆盖该方法并返回 null。
     *
     * @return {@link RemoteView} 对象，返回 null 时将隐藏通知栏控制器
     * @see SimpleRemoteView
     * @see MediaRemoteView
     */
    @Nullable
    protected RemoteView onCreateRemoteView() {
        return new SimpleRemoteView();
    }

    @Nullable
    protected final AudioEffectManager createAudioEffectManager() {
        if (injectAudioEffectManager()) {
            return mComponentFactory.createAudioEffectManager();
        }

        return onCreateAudioEffectManager();
    }

    /**
     * 创建音频特效引擎。
     *
     * @return 如果返回 null，将会关闭音频特效引擎
     */
    @Nullable
    protected AudioEffectManager onCreateAudioEffectManager() {
        return null;
    }

    @Nullable
    protected final HistoryRecorder createHistoryRecorder() {
        if (injectHistoryRecorder()) {
            return mComponentFactory.createHistoryRecorder();
        }

        return onCreateHistoryRecorder();
    }

    /**
     * 创建历史记录器，用于记录播放器的播放历史。
     *
     * @return 如果返回 null，则不会记录播放历史（默认返回 null）
     */
    @Nullable
    protected final HistoryRecorder onCreateHistoryRecorder() {
        return null;
    }

    @Override
    public void setSoundQuality(Player.SoundQuality soundQuality) {
        if (soundQuality == mPlayerConfig.getSoundQuality()) {
            return;
        }

        mPlayerConfig.setSoundQuality(soundQuality);
        notifySoundQualityChanged();
    }

    private void notifySoundQualityChanged() {
        mPlayer.notifySoundQualityChanged();
    }

    @Override
    public void setAudioEffectEnabled(boolean enabled) {
        if (mPlayerConfig.isAudioEffectEnabled() == enabled) {
            return;
        }

        mPlayerConfig.setAudioEffectEnabled(enabled);
        notifyAudioEffectEnableChanged();
    }

    @Override
    public void setAudioEffectConfig(Bundle config) {
        if (noAudioEffectManager() || !mPlayerConfig.isAudioEffectEnabled()) {
            return;
        }

        mAudioEffectManager.updateConfig(config);
        mPlayerConfig.setAudioEffectConfig(config);
    }

    private boolean noAudioEffectManager() {
        return mAudioEffectManager == null;
    }

    /**
     * 对象指定的 audio session id 应用音频特效。
     *
     * @param audioSessionId 当前正在播放的音乐的 audio session id。如果为 0，则可以忽略。
     */
    protected void attachAudioEffect(int audioSessionId) {
        if (noAudioEffectManager()) {
            return;
        }

        mAudioEffectManager.attachAudioEffect(audioSessionId);
    }

    /**
     * 取消当前的音频特效。
     */
    protected void detachAudioEffect() {
        if (noAudioEffectManager()) {
            return;
        }

        mAudioEffectManager.detachAudioEffect();
    }

    private void notifyAudioEffectEnableChanged() {
        mPlayer.notifyAudioEffectEnableChanged();
    }

    @Override
    public void setOnlyWifiNetwork(boolean onlyWifiNetwork) {
        if (mPlayerConfig.isOnlyWifiNetwork() == onlyWifiNetwork) {
            return;
        }

        mPlayerConfig.setOnlyWifiNetwork(onlyWifiNetwork);
        notifyOnlyWifiNetworkChanged();
    }

    private void notifyOnlyWifiNetworkChanged() {
        mPlayer.notifyOnlyWifiNetworkChanged();
    }

    /**
     * 关闭播放器。
     * <p>
     * 调用该方法后 Service 会要求所有已绑定的客户端断开连接，然后终止自己。
     */
    @Override
    public final void shutdown() {
        if (isPlaying()) {
            pause();
        }

        notifyOnShutdown();
        stopSelf();
    }

    @Override
    public void registerPlayerStateListener(String token, IBinder listener) {
        MessengerPipe pipe = new MessengerPipe(listener);

        addOnCommandCallback(token, ChannelHelper.newEmitter(OnCommandCallback.class, pipe));
        mPlayer.addStateListener(token, ChannelHelper.newEmitter(PlayerStateListener.class, pipe));
    }

    @Override
    public void unregisterPlayerStateListener(String token) {
        removeOnConfigChangeListener(token);
        mPlayer.removeStateListener(token);
    }

    /**
     * 获取当前 Service 中的 {@link MediaSessionCompat} 对象。
     *
     * @return {@link MediaSessionCompat} 对象
     */
    protected final MediaSessionCompat getMediaSession() {
        return mMediaSession;
    }

    /**
     * 添加一个自定义动作。
     * <p>
     * 该方法会返回一个 PendingIntent 对象，该 PendingIntent 对象会使用指定的 action 启动当前 Service。当
     * Service 在 {@link #onStartCommand(Intent, int, int)} 方法中检测到该 action 时，会执行其对应的 task。
     */
    protected final PendingIntent addOnStartCommandAction(@NonNull String action, @NonNull Runnable task) {
        Preconditions.checkNotNull(action);
        Preconditions.checkNotNull(task);

        mStartCommandActionMap.put(action, task);

        Context context = getApplicationContext();
        Intent intent = new Intent(context, this.getClass());
        intent.setAction(action);
        return PendingIntent.getService(context, 0, intent, 0);
    }

    /**
     * 移除一个已添加的自定义动作。
     * <p>
     * 需要指出的时，当一个自定义动作被移除后，其对应的 PendingIntent 仍然可以用来启动当前 Service，只不过
     * {@link #onStartCommand(Intent, int, int)} 不会在响应对应的 action。建议在调用该方法后同时取消注册
     * 时返回的那个 PendingIntent 对象（调用 PendingIntent 的 cancel() 方法进行取消）。
     */
    protected final void removeOnStartCommandAction(@NonNull String action) {
        Preconditions.checkNotNull(action);
        mStartCommandActionMap.remove(action);
    }

    private void syncPlayerState(OnCommandCallback listener) {
        listener.syncPlayerState(new PlayerState(mPlayerState));
    }

    private void addOnCommandCallback(@NonNull String token, @NonNull OnCommandCallback listener) {
        Preconditions.checkNotNull(token);
        Preconditions.checkNotNull(listener);

        mCommandCallbackMap.put(token, listener);
        syncPlayerState(listener);
    }

    private void removeOnConfigChangeListener(@NonNull String token) {
        Preconditions.checkNotNull(token);

        mCommandCallbackMap.remove(token);
    }

    private void notifyOnShutdown() {
        for (String key : mCommandCallbackMap.keySet()) {
            OnCommandCallback callback = mCommandCallbackMap.get(key);
            if (callback != null) {
                callback.onShutdown();
            }
        }

        mCommandCallbackMap.clear();
    }

    /**
     * 返回一个字符串 ID。该 ID 将用于对播放器的状态进行持久化。请确保该 ID 的唯一性。
     * <p>
     * 默认的 ID 值为：this.getClass().getName()
     */
    @NonNull
    protected String getPersistentId() {
        return this.getClass().getName();
    }

    /**
     * 获取 Notification 的 ID（默认返回 1）。
     */
    @SuppressWarnings("SameReturnValue")
    protected int getNotificationId() {
        return 1;
    }

    /**
     * 获取播放器的播放模式。
     */
    protected final Player.PlayMode getPlaylistPlayMode() {
        return mPlayerState.getPlayMode();
    }

    /**
     * 获取播放队列携带的额外参数（可为 null）。
     */
    @Nullable
    protected final Bundle getPlaylistExtra() {
        return mPlayer.getPlaylistExtra();
    }

    /**
     * 当前是否正在播放音乐。
     */
    protected final boolean isPlaying() {
        return mPlayer.isPlaying();
    }

    /**
     * 当前是否处于 {@link snow.player.Player.PlaybackState#PLAYING} 状态。
     * <p>
     * 该方法与 {@link #isPlaying()} 方法的区别是，{@link #isPlaying()} 方法判断是当前播放器是否正在
     * 播放音乐，而当前方法判断是当前是否处于 {@link snow.player.Player.PlaybackState#PLAYING} 状态。在
     * 切换歌曲时，当前会处于 {@link snow.player.Player.PlaybackState#PLAYING} 状态，但此时播放器却没有正
     * 在播放，因为旧的播放器被释放掉了，而新的播放器还没有准备好播放。
     */
    private boolean isPlayingState() {
        return getPlayerState().getPlaybackState() == Player.PlaybackState.PLAYING;
    }

    /**
     * 获取播放器的状态。
     */
    protected final PlayerState getPlayerState() {
        return mPlayerState;
    }

    public final boolean isPreparingState() {
        return getPlayerState().getPlaybackState() == Player.PlaybackState.PREPARING;
    }

    public final boolean isStalled() {
        return mPlayer.isStalled();
    }

    /**
     * 获取当前正在播放的音乐的 MusicItem 对象。
     */
    protected final MusicItem getPlayingMusicItem() {
        return mPlayerState.getMusicItem();
    }

    /**
     * 播放器是否发生了错误。
     */
    protected final boolean isError() {
        return getErrorCode() != Player.Error.NO_ERROR;
    }

    /**
     * 获取错误码。
     * <p>
     * 该方法的返回值仅在发生错误（{@link #isError()} 方法返回 true）时才有意义。
     */
    protected final int getErrorCode() {
        return mPlayerState.getErrorCode();
    }

    /**
     * 获取错误信息。
     * <p>
     * 该方法的返回值仅在发生错误（{@link #isError()} 方法返回 true）时才有意义。
     */
    protected final String getErrorMessage() {
        return Player.Error.getErrorMessage(this, getErrorCode());
    }

    /**
     * 要求 Service 更新 NotificationView，如果没有设置 NotificationView，则忽略本次操作。
     */
    protected final void updateNotificationView() {
        if (noRemoteView()) {
            return;
        }

        MusicItem musicItem = getPlayingMusicItem();
        if (musicItem == null) {
            stopForegroundEx(true);
            return;
        }

        mRemoteView.setPlayingMusicItem(musicItem);

        if (isPreparingOrPlayingState() && !isForeground()) {
            startForeground();
            return;
        }

        if (!isPreparingOrPlayingState() && isForeground()) {
            stopForegroundEx(false);
        }

        updateNotification();
    }

    private boolean noRemoteView() {
        return mRemoteView == null;
    }

    private boolean isPreparingOrPlayingState() {
        return isPreparingState() | isPlayingState();
    }

    /**
     * 当前 Service 是否处于前台。
     */
    protected final boolean isForeground() {
        return mForeground;
    }

    /**
     * 启动前台 Service。
     */
    protected final void startForeground() {
        if (noRemoteView()) {
            return;
        }

        if (getPlayingMusicItem() == null) {
            stopForegroundEx(true);
            return;
        }

        mForeground = true;
        startForeground(mNotificationId, createNotification());
    }

    /**
     * 停止前台 Service。
     *
     * @param removeNotification 是否清除 NotificationView
     */
    protected final void stopForegroundEx(boolean removeNotification) {
        mForeground = false;
        stopForeground(removeNotification);
    }

    private void updateNotification() {
        if (noRemoteView()) {
            return;
        }

        if (getPlayingMusicItem() == null) {
            stopForegroundEx(true);
            return;
        }

        mNotificationManager.notify(mNotificationId, createNotification());
    }

    @NonNull
    private Notification createNotification() {
        return mRemoteView.createNotification();
    }

    /**
     * 查询具有 soundQuality 音质的 MusicItem 表示的的音乐是否已被缓存。
     * <p>
     * 该方法会在异步线程中被调用。
     *
     * @param musicItem    要查询的 MusicItem 对象
     * @param soundQuality 音乐的音质
     * @return 如果已被缓存，则返回 true，否则返回 false
     */
    @SuppressWarnings("SameReturnValue")
    protected boolean isCached(MusicItem musicItem, Player.SoundQuality soundQuality) {
        if (mComponentFactory != null) {
            return mComponentFactory.isCached(musicItem, soundQuality);
        }

        return false;
    }

    @NonNull
    protected final MusicPlayer createMusicPlayer(Context context) {
        if (injectMusicPlayer()) {
            return mComponentFactory.createMusicPlayer(context);
        }

        return onCreateMusicPlayer(this);
    }

    /**
     * 该方法会在创建 MusicPlayer 对象时调用。
     * <p>
     * 你可以重写该方法来返回你自己的 MusicPlayer 实现。
     *
     * @param context Application Context
     * @return 音乐播放器（不能为 null）
     */
    @NonNull
    protected MusicPlayer onCreateMusicPlayer(Context context) {
        return new MediaMusicPlayer();
    }

    /**
     * 获取音乐的播放链接。
     * <p>
     * 该方法会在异步线程中执行，因此可以执行各种耗时操作，例如访问网络。
     *
     * @param musicItem    要播放的音乐
     * @param soundQuality 要播放的音乐的音质
     * @return 音乐的播放链接
     * @throws Exception 获取音乐播放链接的过程中发生的任何异常
     */
    protected final Uri retrieveMusicItemUri(@NonNull MusicItem musicItem, @NonNull Player.SoundQuality soundQuality) throws Exception {
        if (injectMusicItemUri()) {
            return mComponentFactory.retrieveMusicItemUri(musicItem, soundQuality);
        }

        return onRetrieveMusicItemUri(musicItem, soundQuality);
    }

    /**
     * 获取音乐的播放链接。
     * <p>
     * 该方法会在异步线程中执行，因此可以执行各种耗时操作，例如访问网络。
     *
     * @param musicItem    要播放的音乐
     * @param soundQuality 要播放的音乐的音质
     * @return 音乐的播放链接
     * @throws Exception 获取音乐播放链接的过程中发生的任何异常
     */
    @SuppressWarnings("RedundantThrows")
    protected Uri onRetrieveMusicItemUri(@NonNull MusicItem musicItem, @NonNull Player.SoundQuality soundQuality) throws Exception {
        return Uri.parse(musicItem.getUri());
    }

    /**
     * 获取播放器的 Player 对象。可用于对播放器进行控制。
     */
    protected final Player getPlayer() {
        return mPlayer;
    }

    /**
     * 播放。
     */
    protected final void play() {
        getPlayer().play();
    }

    /**
     * 暂停。
     */
    protected final void pause() {
        getPlayer().pause();
    }

    /**
     * 播放/暂停。
     */
    protected final void playOrPause() {
        getPlayer().playPause();
    }

    /**
     * 停止。
     */
    protected final void stop() {
        getPlayer().stop();
    }

    /**
     * 快进。
     */
    protected final void fastForward() {
        getPlayer().fastForward();
    }

    /**
     * 快退。
     */
    protected final void rewind() {
        getPlayer().rewind();
    }

    /**
     * 调整音乐播放进度。
     *
     * @param progress 要调整到的播放进度
     */
    protected final void seekTo(int progress) {
        getPlayer().seekTo(progress);
    }

    /**
     * 下一曲。
     */
    protected final void skipToNext() {
        getPlayer().skipToNext();
    }

    /**
     * 上一曲。
     */
    protected final void skipToPrevious() {
        getPlayer().skipToPrevious();
    }

    /**
     * 正在准备播放器。
     */
    protected void onPreparing() {
        updateNotificationView();
    }

    /**
     * 播放器准备完毕。
     *
     * @param audioSessionId 播放器的 audio session id
     */
    protected void onPrepared(int audioSessionId) {
    }

    /**
     * 播放器开始播放。
     *
     * @param progress   播放进度
     * @param updateTime 播放进度的更新时间
     */
    protected void onPlaying(int progress, long updateTime) {
        mMediaSession.setActive(true);

        PlaybackStateCompat playbackState = buildPlaybackState(
                PlaybackStateCompat.STATE_PLAYING,
                progress,
                updateTime,
                PlaybackStateCompat.ACTION_PAUSE |
                        PlaybackStateCompat.ACTION_PLAY_PAUSE |
                        PlaybackStateCompat.ACTION_SKIP_TO_NEXT |
                        PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS |
                        PlaybackStateCompat.ACTION_SKIP_TO_QUEUE_ITEM |
                        PlaybackStateCompat.ACTION_STOP |
                        PlaybackStateCompat.ACTION_SEEK_TO |
                        PlaybackStateCompat.ACTION_FAST_FORWARD |
                        PlaybackStateCompat.ACTION_REWIND);

        mMediaSession.setPlaybackState(playbackState);

        updateNotificationView();
    }

    /**
     * 已暂停播放。
     */
    protected void onPaused() {
        PlaybackStateCompat playbackState = buildPlaybackState(
                PlaybackStateCompat.STATE_PAUSED,
                getPlayerState().getPlayProgress(),
                getPlayerState().getPlayProgressUpdateTime(),
                PlaybackStateCompat.ACTION_PLAY |
                        PlaybackStateCompat.ACTION_PLAY_PAUSE |
                        PlaybackStateCompat.ACTION_STOP |
                        PlaybackStateCompat.ACTION_SEEK_TO |
                        PlaybackStateCompat.ACTION_SKIP_TO_NEXT |
                        PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS |
                        PlaybackStateCompat.ACTION_SKIP_TO_QUEUE_ITEM |
                        PlaybackStateCompat.ACTION_FAST_FORWARD |
                        PlaybackStateCompat.ACTION_REWIND);

        mMediaSession.setPlaybackState(playbackState);

        updateNotificationView();
    }

    /**
     * 播放器的 {@code stalled} 状态发生了改变。
     *
     * @param stalled 播放器的 {@code stalled} 状态。当缓冲区中没有足够的数据支持继续播放时，该参数为 true，
     *                否则为 false
     */
    protected void onStalledChanged(boolean stalled) {
        updateNotificationView();
    }

    /**
     * 播放器已停止播放。
     */
    protected void onStopped() {
        mMediaSession.setActive(false);

        PlaybackStateCompat playbackState = buildPlaybackState(
                PlaybackStateCompat.STATE_STOPPED,
                0,
                getPlayerState().getPlayProgressUpdateTime(),
                PlaybackStateCompat.ACTION_PLAY |
                        PlaybackStateCompat.ACTION_SKIP_TO_NEXT |
                        PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS |
                        PlaybackStateCompat.ACTION_SKIP_TO_QUEUE_ITEM);

        mMediaSession.setPlaybackState(playbackState);

        updateNotificationView();
    }

    /**
     * 播放器发生了错误。
     *
     * @param errorCode    错误码
     * @param errorMessage 错误信息
     */
    protected void onError(int errorCode, String errorMessage) {
        PlaybackStateCompat playbackState = buildPlaybackState(
                PlaybackStateCompat.STATE_ERROR,
                0,
                getPlayerState().getPlayProgressUpdateTime(),
                PlaybackStateCompat.ACTION_PLAY |
                        PlaybackStateCompat.ACTION_PLAY_PAUSE |
                        PlaybackStateCompat.ACTION_SKIP_TO_NEXT |
                        PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS |
                        PlaybackStateCompat.ACTION_SKIP_TO_QUEUE_ITEM,
                PlaybackStateCompat.ERROR_CODE_APP_ERROR,
                errorMessage);

        mMediaSession.setPlaybackState(playbackState);

        updateNotificationView();
    }

    protected void onPlayingMusicItemChanged(@Nullable MusicItem musicItem) {
        mMediaSession.setMetadata(getFreshMediaMetadata());
        updateNotificationView();

        if (mHistoryRecorder != null && musicItem != null) {
            mHistoryRecorder.onPlayingMusicItemChanged(musicItem);
        }
    }

    /**
     * 该方法会在媒体按钮被触发时调用。
     *
     * @param mediaButtonEvent 被触发的每天按钮
     * @return 是否已处理该媒体按钮事件，如果已处理，则应该返回 true，否则返回 false
     */
    protected boolean onMediaButtonEvent(Intent mediaButtonEvent) {
        return mHeadsetHookHelper.handleMediaButton(mediaButtonEvent);
    }

    /**
     * 该方法会在 MediaSession 接收到 custom action 时调用。
     *
     * @param action 自定义动作的名称
     * @param extras 自定义动作携带的额外数据
     */
    protected void onCustomAction(String action, Bundle extras) {
        mControllerPipe.dispatch(action, extras);
    }

    private class PlayerImp extends AbstractPlayer {

        public PlayerImp(@NonNull Context context,
                         @NonNull PlayerConfig playerConfig,
                         @NonNull PlayerState playlistState,
                         @NonNull PlaylistManager playlistManager) {
            super(context, playerConfig, playlistState, playlistManager);
        }

        @Override
        protected boolean isCached(MusicItem musicItem, SoundQuality soundQuality) {
            return PlayerService.this.isCached(musicItem, soundQuality);
        }

        @NonNull
        @Override
        protected MusicPlayer onCreateMusicPlayer(Context context) {
            return PlayerService.this.createMusicPlayer(context);
        }

        @Nullable
        @Override
        protected Uri retrieveMusicItemUri(@NonNull MusicItem musicItem, @NonNull SoundQuality soundQuality) throws Exception {
            return PlayerService.this.retrieveMusicItemUri(musicItem, soundQuality);
        }

        @Override
        protected void onPreparing() {
            super.onPreparing();
            PlayerService.this.onPreparing();
        }

        @Override
        protected void onPrepared(int audioSessionId) {
            super.onPrepared(audioSessionId);
            PlayerService.this.onPrepared(audioSessionId);
        }

        @Override
        protected void onPlaying(int progress, long updateTime) {
            super.onPlaying(progress, updateTime);
            PlayerService.this.onPlaying(progress, updateTime);
        }

        @Override
        protected void onPaused() {
            super.onPaused();
            PlayerService.this.onPaused();
        }

        @Override
        protected void onStalledChanged(boolean stalled) {
            super.onStalledChanged(stalled);
            PlayerService.this.onStalledChanged(stalled);
        }

        @Override
        protected void onStopped() {
            super.onStopped();
            PlayerService.this.onStopped();
        }

        @Override
        protected void onError(int errorCode, String errorMessage) {
            super.onError(errorCode, errorMessage);
            PlayerService.this.onError(errorCode, errorMessage);
        }

        @Override
        protected void onPlayingMusicItemChanged(@Nullable MusicItem musicItem) {
            super.onPlayingMusicItemChanged(musicItem);
            PlayerService.this.onPlayingMusicItemChanged(musicItem);
        }

        @Override
        protected void attachAudioEffect(int audioSessionId) {
            super.attachAudioEffect(audioSessionId);
            PlayerService.this.attachAudioEffect(audioSessionId);
        }

        @Override
        protected void detachAudioEffect() {
            super.detachAudioEffect();
            PlayerService.this.detachAudioEffect();
        }
    }

    private class MediaSessionCallbackImp extends MediaSessionCompat.Callback {
        @Override
        public boolean onMediaButtonEvent(Intent mediaButtonEvent) {
            if (PlayerService.this.onMediaButtonEvent(mediaButtonEvent)) {
                return true;
            }

            return super.onMediaButtonEvent(mediaButtonEvent);
        }

        @Override
        public void onCustomAction(String action, Bundle extras) {
            PlayerService.this.onCustomAction(action, extras);
        }

        @Override
        public void onPlay() {
            PlayerService.this.play();
        }

        @Override
        public void onSkipToQueueItem(long id) {
            mPlayer.playPause((int) id);
        }

        @Override
        public void onPause() {
            PlayerService.this.pause();
        }

        @Override
        public void onSkipToNext() {
            PlayerService.this.skipToNext();
        }

        @Override
        public void onSkipToPrevious() {
            PlayerService.this.skipToPrevious();
        }

        @Override
        public void onFastForward() {
            PlayerService.this.fastForward();
        }

        @Override
        public void onRewind() {
            PlayerService.this.rewind();
        }

        @Override
        public void onStop() {
            PlayerService.this.stop();
        }

        @Override
        public void onSeekTo(long pos) {
            PlayerService.this.seekTo((int) pos);
        }

        @Override
        public void onSetRepeatMode(int repeatMode) {
            if (repeatMode == PlaybackStateCompat.REPEAT_MODE_ONE) {
                mPlayer.setPlayMode(Player.PlayMode.LOOP);
                return;
            }

            mPlayer.setPlayMode(Player.PlayMode.SEQUENTIAL);
        }

        @Override
        public void onSetShuffleMode(int shuffleMode) {
            if (shuffleMode == PlaybackStateCompat.SHUFFLE_MODE_NONE ||
                    shuffleMode == PlaybackStateCompat.SHUFFLE_MODE_INVALID) {
                mPlayer.setPlayMode(Player.PlayMode.SEQUENTIAL);
                return;
            }

            mPlayer.setPlayMode(Player.PlayMode.SHUFFLE);
        }
    }

    /**
     * 用于显示通知栏控制器。
     */
    public static abstract class RemoteView {
        /**
         * 默认的 channelId 值，值为：{@code "player"}
         */
        public static final String DEFAULT_CHANNEL_ID = "player";

        private PlayerService mPlayerService;
        private MusicItem mMusicItem;

        private int[] mIconSize;            // [width, height]
        private int[] mIconCornerRadius;    // [topLeft, topRight, bottomRight, bottomLeft]
        private boolean mNeedReloadIcon;
        private Bitmap mIcon;
        private CustomTarget<Bitmap> mTarget;

        void init(PlayerService playerService) {
            mPlayerService = playerService;
            mMusicItem = new MusicItem();
            mIconSize = new int[2];
            mIconCornerRadius = new int[4];

            setNeedReloadIcon(true);
            onInit(mPlayerService);

            mTarget = new CustomTarget<Bitmap>(mIconSize[0], mIconSize[1]) {
                @Override
                public void onResourceReady(@NonNull Bitmap resource, @Nullable Transition<? super Bitmap> transition) {
                    setIcon(resource);
                }

                @Override
                public void onLoadCleared(@Nullable Drawable placeholder) {
                    // ignore
                }
            };
        }

        /**
         * 获取默认图标的资源 ID
         */
        @DrawableRes
        protected int getDefaultIconId() {
            return R.mipmap.snow_notif_default_icon;
        }

        /**
         * 该方法会在初次创建 NotificationView 对象时调用，你可以重写该方法来进行一些初始化操作。
         */
        protected void onInit(Context context) {
        }

        /**
         * 获取 Notification 的 {@code channelId}，不能为 null。
         * <p>
         * 可以覆盖该方法来提供你自己的 {@code channelId}。
         *
         * @return channelId，不能为 null。
         */
        @NonNull
        protected String getChannelId() {
            return DEFAULT_CHANNEL_ID;
        }

        /**
         * 该方法会在 Service 销毁时调用，可以在该方法中释放占用的资源。
         */
        protected void onRelease() {
            Glide.with(getContext())
                    .clear(mTarget);
        }

        /**
         * 加载当前正在播放的歌曲的图标。
         * <p>
         * 你可以重写该方法实现自己的图标加载逻辑。
         */
        protected void reloadIcon() {
            setNeedReloadIcon(false);

            Glide.with(getContext())
                    .clear(mTarget);

            Glide.with(getContext())
                    .asBitmap()
                    .load(getPlayingMusicItem().getIconUri())
                    .error(loadEmbeddedIcon())
                    .transform(new GranularRoundedCorners(mIconCornerRadius[0],
                            mIconCornerRadius[1],
                            mIconCornerRadius[2],
                            mIconCornerRadius[3]))
                    .into(mTarget);
        }

        private boolean notLocaleMusic() {
            String stringUri = getPlayingMusicItem().getUri();
            String scheme = Uri.parse(stringUri).getScheme();

            return "http".equalsIgnoreCase(scheme) | "https".equalsIgnoreCase(scheme);
        }

        private RequestBuilder<Bitmap> loadEmbeddedIcon() {
            return Glide.with(getContext())
                    .asBitmap()
                    .load(getEmbeddedIcon())
                    .error(loadDefaultIcon())
                    .transform(new GranularRoundedCorners(mIconCornerRadius[0],
                            mIconCornerRadius[1],
                            mIconCornerRadius[2],
                            mIconCornerRadius[3]));
        }

        private RequestBuilder<Bitmap> loadDefaultIcon() {
            return Glide.with(getContext())
                    .asBitmap()
                    .load(getDefaultIconId())
                    .transform(new GranularRoundedCorners(mIconCornerRadius[0],
                            mIconCornerRadius[1],
                            mIconCornerRadius[2],
                            mIconCornerRadius[3]));
        }

        private byte[] getEmbeddedIcon() {
            if (notLocaleMusic()) {
                return null;
            }

            MediaMetadataRetriever retriever = new MediaMetadataRetriever();

            try {
                retriever.setDataSource(getPlayingMusicItem().getUri());
                return retriever.getEmbeddedPicture();
            } catch (IllegalArgumentException e) {
                e.printStackTrace();
                return null;
            } finally {
                retriever.release();
            }
        }

        /**
         * 重写该方法创建一个新的 Notification 对象。
         *
         * @return Notification 对象，不能为 null。
         */
        @NonNull
        protected abstract Notification onCreateNotification();

        /**
         * 上一曲
         */
        protected final void skipToPrevious() {
            mPlayerService.skipToPrevious();
        }

        /**
         * 下一曲
         */
        protected final void skipToNext() {
            mPlayerService.skipToNext();
        }

        /**
         * 播放/暂停
         */
        protected final void playOrPause() {
            mPlayerService.playOrPause();
        }

        /**
         * 关闭播放器
         */
        protected final void shutdown() {
            mPlayerService.shutdown();
        }

        /**
         * 获取当前通知栏控制器的图标。
         */
        protected final Bitmap getIcon() {
            return mIcon;
        }

        /**
         * 设置当前通知栏控制器的图标。
         * <p>
         * 调用该方法后会自动更新通知栏控制器，以应用最新设置的图标。
         */
        protected final void setIcon(@NonNull Bitmap icon) {
            mIcon = icon;
            invalidate();
        }

        /**
         * 设置图标的尺寸（默认尺寸为 0）。
         * <p>
         * 建议子类覆盖 {@link #onInit(Context)} 方法，并在该方法中完成图标尺寸的设置。
         */
        protected final void setIconSize(int size) {
            setIconSize(size, size);
        }

        /**
         * 分别设置图标的宽高尺寸（默认宽高为 0）。
         * <p>
         * 建议子类覆盖 {@link #onInit(Context)} 方法，并在该方法中完成图标宽高尺寸的设置。
         */
        protected final void setIconSize(int width, int height) {
            mIconSize[0] = width;
            mIconSize[1] = height;
        }

        /**
         * 设置图标圆角的半径。
         * <p>
         * 建议子类覆盖 {@link #onInit(Context)} 方法，并在该方法中完成圆角半径的设置。
         *
         * @param radius 图标圆角的半径
         */
        protected final void setIconCornerRadius(int radius) {
            setIconCornerRadius(radius, radius, radius, radius);
        }

        /**
         * 分别设置图标 4 个圆角的半径。
         * <p>
         * 建议子类覆盖 {@link #onInit(Context)} 方法，并在该方法中完成圆角半径的设置。
         *
         * @param topLeft     左上角圆角半径
         * @param topRight    右上角圆角半径
         * @param bottomLeft  左下角圆角半径
         * @param bottomRight 右下角圆角半径
         */
        protected final void setIconCornerRadius(int topLeft, int topRight, int bottomRight, int bottomLeft) {
            mIconCornerRadius[0] = topLeft;
            mIconCornerRadius[1] = topRight;
            mIconCornerRadius[2] = bottomRight;
            mIconCornerRadius[3] = bottomLeft;
        }

        /**
         * 获取通知栏控制器的 content title
         */
        protected final CharSequence getContentTitle() {
            return getPlayingMusicItem().getTitle();
        }

        /**
         * 这是一个帮助方法，获取通知栏控制器用于显示的 content text，该方法会根据播放器状态的不同而返回不同
         * 的值。
         * <p>
         * 例如，在 {@link snow.player.Player.PlaybackState#ERROR} 状态时，会返回一个
         * {@code android.R.color.holo_red_dark} 颜色的描述错误信息的 CharSequence 对象；而在
         * {@link snow.player.Player.PlaybackState#PREPARING} 状态时，会返回一个
         * {@code android.R.color.holo_green_dark} 颜色的值为 “准备中…” 的 CharSequence 对象；而在
         * {@link #isStalled()} 返回 true 时，会返回一个 {@code android.R.color.holo_orange_dark} 颜色
         * 的值为 “缓冲中…” 的 CharSequence 对象。其它状态下会将 {@code defaultValue} 原值返回。
         *
         * @param contentText context text 的值，如果播放器处于正常播放器状态，该方法会将这个值原样返回，
         *                    如果播放器正在缓冲，或发生了错误，则将返回一个提示字符串
         */
        protected final CharSequence getContentText(String contentText) {
            String value = contentText;
            int textColor = 0;

            Resources res = getContext().getResources();

            if (isError()) {
                value = getErrorMessage();
                textColor = res.getColor(android.R.color.holo_red_dark);
            }

            if (isPreparingState()) {
                value = getContext().getString(R.string.snow_preparing);
                textColor = res.getColor(android.R.color.holo_green_dark);
            }

            if (isStalled()) {
                value = res.getString(R.string.snow_buffering);
                textColor = res.getColor(android.R.color.holo_orange_dark);
            }

            CharSequence text = value;

            if (textColor != 0) {
                SpannableString colorText = new SpannableString(value);
                colorText.setSpan(new ForegroundColorSpan(textColor), 0, value.length(), Spanned.SPAN_INCLUSIVE_EXCLUSIVE);

                text = colorText;
            }

            return text;
        }

        protected final Context getContext() {
            return mPlayerService;
        }

        protected final String getPackageName() {
            return getContext().getPackageName();
        }

        /**
         * 获取播放器的播放模式。
         */
        protected final Player.PlayMode getPlaylistPlayMode() {
            return mPlayerService.getPlaylistPlayMode();
        }

        /**
         * 获取播放队列携带的额外参数（可为 null）。
         */
        @Nullable
        protected final Bundle getPlaylistExtra() {
            return mPlayerService.getPlaylistExtra();
        }

        protected final PendingIntent addOnStartCommandAction(@NonNull String action, @NonNull Runnable task) {
            return mPlayerService.addOnStartCommandAction(action, task);
        }

        protected final void removeOnStartCommandAction(@NonNull String action) {
            mPlayerService.removeOnStartCommandAction(action);
        }

        /**
         * 当前是否正在播放音乐。
         */
        public final boolean isPreparingOrPlayingState() {
            return mPlayerService.isPreparingOrPlayingState();
        }

        public final boolean isPreparingState() {
            return mPlayerService.isPreparingState();
        }

        public final boolean isStalled() {
            return mPlayerService.isStalled();
        }

        /**
         * 获取当前正在播放的音乐的 MusicItem 对象。
         */
        @NonNull
        public final MusicItem getPlayingMusicItem() {
            return mMusicItem;
        }

        /**
         * 获取当前播放器的 {@link MediaSessionCompat} 对象。
         *
         * @return {@link MediaSessionCompat} 对象
         */
        public final MediaSessionCompat getMediaSession() {
            return mPlayerService.getMediaSession();
        }

        /**
         * 播放器是否发生了错误。
         */
        public final boolean isError() {
            return mPlayerService.isError();
        }

        /**
         * 获取错误信息。
         * <p>
         * 该方法的返回值仅在发生错误（{@link #isError()} 方法返回 true）时才有意义。
         */
        @NonNull
        public final String getErrorMessage() {
            return mPlayerService.getErrorMessage();
        }

        /**
         * 要求 Service 更新 NotificationView，如果没有设置 NotificationView，则忽略本次操作。
         */
        public final void invalidate() {
            mPlayerService.updateNotificationView();
        }

        @NonNull
        Notification createNotification() {
            if (isNeedReloadIcon()) {
                reloadIcon();
            }
            return onCreateNotification();
        }

        void setPlayingMusicItem(@NonNull MusicItem musicItem) {
            Preconditions.checkNotNull(musicItem);
            if (mMusicItem.equals(musicItem)) {
                return;
            }

            mNeedReloadIcon = true;
            mMusicItem = musicItem;
        }

        boolean isNeedReloadIcon() {
            return mNeedReloadIcon;
        }

        void setNeedReloadIcon(boolean needReloadIcon) {
            mNeedReloadIcon = needReloadIcon;
        }
    }

    /**
     * 通知栏控制器，使用 Android 系统提供的样式。通知的样式为：<a href="https://developer.android.google.cn/reference/androidx/media/app/NotificationCompat.MediaStyle?hl=en">NotificationCompat.MediaStyle</a>
     * <p>
     * 可以通过实现 {@link #onBuildMediaStyle(androidx.media.app.NotificationCompat.MediaStyle)} 方法
     * 和实现 {@link #onBuildNotification(NotificationCompat.Builder)} 来对当前 RemoteView 的外观进行定
     * 制。
     * <p>
     * 更多信息，请参考官方文档： <a href="https://developer.android.google.cn/training/notify-user/expanded?hl=zh-cn#media-style">https://developer.android.google.cn/training/notify-user/expanded?hl=zh-cn#media-style</a>
     */
    public static abstract class MediaRemoteView extends RemoteView {
        @Override
        protected void onInit(Context context) {
            super.onInit(context);

            Resources res = context.getResources();

            setIconSize(res.getDimensionPixelSize(R.dimen.snow_notif_icon_size_big));
            setIconCornerRadius(res.getDimensionPixelSize(R.dimen.snow_notif_icon_corner_radius));
        }

        @NonNull
        @Override
        protected Notification onCreateNotification() {
            androidx.media.app.NotificationCompat.MediaStyle mediaStyle =
                    new androidx.media.app.NotificationCompat.MediaStyle()
                            .setMediaSession(getMediaSession().getSessionToken());

            onBuildMediaStyle(mediaStyle);

            NotificationCompat.Builder builder = new NotificationCompat.Builder(getContext(), getChannelId())
                    .setSmallIcon(getSmallIconId())
                    .setLargeIcon(getIcon())
                    .setContentTitle(getContentTitle())
                    .setContentText(getContentText(getPlayingMusicItem().getArtist()))
                    .setStyle(mediaStyle);

            onBuildNotification(builder);

            return builder.build();
        }

        /**
         * @return drawable 资源的 ID
         */
        @DrawableRes
        protected abstract int getSmallIconId();

        /**
         * 该方法会在创建 {@code NotificationCompat.MediaStyle} 对象期间调用。
         * <p>
         * 可以在该方法中对 {@code NotificationCompat.MediaStyle} 对象进行配置。例如，调用
         * {@code setShowActionsInCompactView (int... actions)} 方法设置要在紧凑的通知视图中显示的操作。
         */
        protected abstract void onBuildMediaStyle(androidx.media.app.NotificationCompat.MediaStyle mediaStyle);

        /**
         * 该方法会在创建 {@code NotificationCompat.Builder} 期间调用。
         * <p>
         * 可以在该方法中对 {@code NotificationCompat.Builder} 对象进行配置。例如，调用
         * {@code addAction (int icon, CharSequence title, PendingIntent intent)} 向通知添加操作。
         */
        protected abstract void onBuildNotification(NotificationCompat.Builder builder);
    }

    /**
     * {@link RemoteView} 的默认实现，提供了一个自定义的外观。
     */
    public static class SimpleRemoteView extends RemoteView {
        private static final String ACTION_SKIP_TO_PREVIOUS = "snow.player.action.skip_to_previous";
        private static final String ACTION_SKIP_TO_NEXT = "snow.player.action.skip_to_next";
        private static final String ACTION_PLAY_PAUSE = "snow.player.action.play_pause";
        private static final String CUSTOM_ACTION_1 = "snow.player.action.custom_action_1";
        private static final String CUSTOM_ACTION_2 = "snow.player.action.custom_action_2";

        private PendingIntent mContentIntent;

        private List<CustomAction> mAllCustomAction;
        private Map<String, PendingIntent> mPendingIntentMap;

        @Override
        protected void onInit(Context context) {
            super.onInit(context);

            mAllCustomAction = new ArrayList<>();
            mPendingIntentMap = new HashMap<>();

            Resources res = context.getResources();

            setIconSize(res.getDimensionPixelSize(R.dimen.snow_notif_icon_size_big));
            setIconCornerRadius(res.getDimensionPixelSize(R.dimen.snow_notif_icon_corner_radius));

            initCustomAction();
        }

        public int getContentViewLayoutId() {
            return R.layout.snow_simple_remote_view;
        }

        public int getBigContentViewLayoutId() {
            return R.layout.snow_simple_remote_view_big;
        }

        /**
         * 是否支持 “上一曲” 功能（默认为 true）。
         *
         * @return 如果播放器不支持 “上一曲” 功能，可以返回 false，这样的话会禁用通知栏控制器的 “上一曲” 按钮
         */
        protected boolean supportSkipToPrevious() {
            return true;
        }

        private void initCustomAction() {
            if (supportSkipToPrevious()) {
                addCustomAction(new CustomAction(ACTION_SKIP_TO_PREVIOUS, R.id.snow_notif_skip_to_previous, new Runnable() {
                    @Override
                    public void run() {
                        skipToPrevious();
                    }
                }));
            }

            CustomAction skipToNext = new CustomAction(ACTION_SKIP_TO_NEXT, R.id.snow_notif_skip_to_next, new Runnable() {
                @Override
                public void run() {
                    skipToNext();
                }
            });
            skipToNext.setShowOnContentView(true);
            addCustomAction(skipToNext);

            CustomAction playPause = new CustomAction(ACTION_PLAY_PAUSE, R.id.snow_notif_play_pause, new Runnable() {
                @Override
                public void run() {
                    playOrPause();
                }
            });
            playPause.setShowOnContentView(true);
            addCustomAction(playPause);
        }

        @NonNull
        @Override
        protected Notification onCreateNotification() {
            RemoteViews contentView = onCreateContentView();
            RemoteViews bigContentView = onCreateBigContentView();

            initAllCustomAction(contentView, true);
            initAllCustomAction(bigContentView, false);

            return new NotificationCompat.Builder(getContext(), getChannelId())
                    .setSmallIcon(R.drawable.snow_ic_music)
                    .setStyle(new androidx.media.app.NotificationCompat.DecoratedMediaCustomViewStyle())
                    .setCustomContentView(contentView)
                    .setCustomBigContentView(bigContentView)
                    .setContentIntent(getContentIntent())
                    .build();
        }

        private void initAllCustomAction(RemoteViews remoteViews, boolean isContentView) {
            for (CustomAction customAction : mAllCustomAction) {
                if (isContentView && !customAction.isShowOnContentView()) {
                    continue;
                }

                initCustomAction(customAction, remoteViews);
            }
        }

        private void initCustomAction(CustomAction customAction, RemoteViews remoteViews) {
            int viewId = customAction.getViewId();
            int iconId = customAction.getIconId();

            remoteViews.setViewVisibility(viewId, View.VISIBLE);

            if (iconId != CustomAction.IGNORE_ICON_ID) {
                remoteViews.setImageViewResource(viewId, iconId);
            }

            remoteViews.setOnClickPendingIntent(viewId, mPendingIntentMap.get(customAction.getActionName()));
        }

        @NonNull
        public RemoteViews onCreateContentView() {
            RemoteViews contentView = new RemoteViews(getPackageName(), getContentViewLayoutId());

            contentView.setImageViewBitmap(R.id.snow_notif_icon, getIcon());
            contentView.setTextViewText(R.id.snow_notif_title, getContentTitle());
            contentView.setTextViewText(R.id.snow_notif_text, getContentText(getPlayingMusicItem().getArtist()));

            if (isPreparingOrPlayingState()) {
                contentView.setInt(R.id.snow_notif_play_pause, "setImageLevel", 1);
            }

            return contentView;
        }

        @NonNull
        public RemoteViews onCreateBigContentView() {
            RemoteViews bigContentView = new RemoteViews(getPackageName(), getBigContentViewLayoutId());

            bigContentView.setImageViewBitmap(R.id.snow_notif_icon, getIcon());
            bigContentView.setTextViewText(R.id.snow_notif_title, getContentTitle());
            bigContentView.setTextViewText(R.id.snow_notif_text, getContentText(getPlayingMusicItem().getArtist()));

            if (isPreparingOrPlayingState()) {
                bigContentView.setInt(R.id.snow_notif_play_pause, "setImageLevel", 1);
            }

            return bigContentView;
        }

        /**
         * 获取 NotificationView 的 content intent。
         *
         * @return 当前 NotificationView 的 content intent，默认为 null
         */
        protected final PendingIntent getContentIntent() {
            return mContentIntent;
        }

        /**
         * 设置当前 NotificationView 的 content intent，该 PendingIntent 对象会在通知栏控制器被点击时触发。
         * <p>
         * 对该方法的调用会在下次更新通知栏控制器时生效。
         *
         * @param pendingIntent 要设置的 content intent，可为 null
         */
        protected final void setContentIntent(PendingIntent pendingIntent) {
            mContentIntent = pendingIntent;
        }

        /**
         * 添加自定义动作，不能为 null。
         * <p>
         * 如果自定义动作已添加，则会忽略。
         *
         * @param customAction 要添加的自定义动作，如果自定义动作已添加，则会忽略
         * @see CustomAction#equals(Object)
         */
        protected final void addCustomAction(@NonNull CustomAction customAction) {
            Preconditions.checkNotNull(customAction);

            if (mAllCustomAction.contains(customAction)) {
                return;
            }

            mAllCustomAction.add(customAction);

            String actionName = customAction.getActionName();
            mPendingIntentMap.put(actionName, addOnStartCommandAction(actionName, customAction.getAction()));
        }

        /**
         * 移除自定义动作。
         *
         * @param customAction 要移除的自定义动作
         */
        protected final void removeCustomAction(CustomAction customAction) {
            if (mAllCustomAction.remove(customAction)) {
                cancelPendingIntent(mPendingIntentMap.remove(customAction.getActionName()));
                removeOnStartCommandAction(customAction.getActionName());
            }
        }

        private void cancelPendingIntent(@Nullable PendingIntent pendingIntent) {
            if (pendingIntent != null) {
                pendingIntent.cancel();
            }
        }

        /**
         * 设置 {@link SimpleRemoteView} 的第一个自定义动作。
         *
         * @param iconId 自定义动作的图标
         * @param action 自定义动作触发时要执行的任务
         */
        public void setCustomAction1(int iconId, Runnable action) {
            CustomAction customAction = new CustomAction(CUSTOM_ACTION_1, R.id.snow_notif_custom_action1, action);
            customAction.setIconId(iconId);
            addCustomAction(customAction);
        }

        /**
         * 设置 {@link SimpleRemoteView} 的第一个自定义动作。
         *
         * @param iconId 自定义动作的图标
         * @param action 自定义动作触发时要执行的任务
         */
        public void setCustomAction2(int iconId, Runnable action) {
            CustomAction customAction = new CustomAction(CUSTOM_ACTION_2, R.id.snow_notif_custom_action2, action);
            customAction.setIconId(iconId);
            customAction.setShowOnContentView(true);
            addCustomAction(customAction);
        }

        /**
         * 用于创建一个可添加到 {@link SimpleRemoteView} 中的自定义动作。
         */
        public static final class CustomAction {
            public static final int IGNORE_ICON_ID = 0;

            private String mActionName;
            private int mViewId;
            private Runnable mAction;

            private int mIconId;
            private boolean mShowOnContentView;

            /**
             * 创建一个自定义动作。
             *
             * @param actionName 自定义动作的名称，请保证该值的唯一性，不能为 null
             * @param viewId     自定义动作要绑定到的 View 的 ID
             * @param action     自定义动作要执行的操作
             */
            public CustomAction(@NonNull String actionName,
                                @IdRes int viewId,
                                @NonNull Runnable action) {
                Preconditions.checkNotNull(actionName);
                Preconditions.checkNotNull(action);

                mActionName = actionName;
                mViewId = viewId;
                mAction = action;

                mIconId = IGNORE_ICON_ID;
            }

            /**
             * 获取自定义动作的名称。
             *
             * @return 自定义动作的名称
             */
            @NonNull
            public String getActionName() {
                return mActionName;
            }

            /**
             * 获取自定义动作要绑定到的 View 的 ID。
             *
             * @return 自定义动作要绑定到的 View 的 ID
             */
            @IdRes
            public int getViewId() {
                return mViewId;
            }

            /**
             * 设置自定义动作的图标的资源 ID。
             * <p>
             * 可以在运行时动态修改，将在下次跟新通知时生效。
             *
             * @param iconId 自定义动作的新图标的资源 ID
             */
            public void setIconId(int iconId) {
                mIconId = iconId;
            }

            /**
             * 设置自定义动作的图标的 ID。
             */
            public int getIconId() {
                return mIconId;
            }

            /**
             * 设置是否在 content view（折叠后的视图）中显示当前的自定义动作。
             */
            public void setShowOnContentView(boolean show) {
                mShowOnContentView = show;
            }

            /**
             * 判断是否在 content view（折叠后的视图）中显示当前的自定义动作。
             */
            public boolean isShowOnContentView() {
                return mShowOnContentView;
            }

            /**
             * 获取当前自定义动作要执行的操作。
             */
            public Runnable getAction() {
                return mAction;
            }

            /**
             * 如果两个 {@code CustomAction} 的 {@code actionName} 是一样的，则判定这两个对象相等。
             */
            @Override
            public boolean equals(Object o) {
                if (this == o) return true;
                if (o == null || getClass() != o.getClass()) return false;
                CustomAction that = (CustomAction) o;
                return Objects.equal(mActionName, that.mActionName);
            }

            @Override
            public int hashCode() {
                return Objects.hashCode(mActionName);
            }
        }
    }

    /**
     * PlayerService 组件工厂，可以通过重写该类的方法来自定义 PlayerService 的部分组件。
     * <p>
     * 子类需要实现 {@link #isCached(MusicItem, Player.SoundQuality)} 方法，该方法用于判断具有特定
     * {@link snow.player.Player.SoundQuality} 的 {@link MusicItem} 是否已缓存。如果播放器仅用于播放本地
     * 音乐，则可以直接返回 {@code true}。
     * <p>
     * 除此之外，还可以选择性的覆盖以下方法来提供其他部分的自定义组件：
     * <ul>
     *     <li>{@link #createMusicPlayer(Context)}：音乐播放器</li>
     *     <li>{@link #createRemoteView()}：通知栏控制器</li>
     *     <li>{@link #createAudioEffectManager()}：音频特性引擎</li>
     *     <li>{@link #createHistoryRecorder()}：历史记录器</li>
     *     <li>{@link #retrieveMusicItemUri(MusicItem, Player.SoundQuality)}：获取歌曲的播放链接</li>
     * </ul>
     * <p>
     * 可以重写 {@link #retrieveMusicItemUri(MusicItem, Player.SoundQuality)} 方法根据音质获取不同的播
     * 放链接。{@link #retrieveMusicItemUri(MusicItem, Player.SoundQuality)} 方法会在异步线程中调用，因
     * 此可以直接在该方法中访问网络。
     * <p>
     * 你可以重写其中的一个或多个方法来使用自定义的组件，重写后的方法需要使用 {@link Inject} 注解进行标记，否
     * 则会被忽略。
     * <p>
     * <b>还有就是，你的 {@link ComponentFactory} 实现还必须提供一个默认的无参构造方法。</b>
     * <p>
     * 例：<br>
     * <pre>
     * public class MyComponentFactory extends PlayerService.ComponentFactory {
     *     ...
     *     &#64;Inject    // 使用 Inject 注解进行标注
     *     &#64;NonNull
     *     &#64;Override
     *     public MusicPlayer createMusicPlayer(Context context) {
     *         return new ExoMusicPlayer(context, mediaSourceFactory);
     *     }
     * }
     * </pre>
     * <p>
     * 最后，还需要对 {@link ComponentFactory} 进行注册。注册方法为，在 {@code AndroidManifest.xml} 文
     * 件中对 {@link PlayerService} 进行注册时使用 {@code <meta-date>} 标签进行指定你的
     * {@link ComponentFactory}。其中，{@code <meta-data>} 标签的 {@code android:name} 属性的值为
     * {@code "component-factory"}，{@code android:value} 属性的值为你的 {@link ComponentFactory} 的完
     * 整类名。
     * <p>
     * 例：<br>
     * <pre>
     * &lt;service android:name="snow.player.PlayerService"&gt;
     *     ...
     *     &lt;meta-data android:name="component-factory" android:value="@string/factory-name"/&gt;
     * &lt;/service&gt;
     * </pre>
     * 注：上例中 {@code android:value} 的值 {@code "@string/factory-name"} 是一个字符串资源，它的值是
     * 你的 {@link ComponentFactory} 的完整类名。
     */
    public static abstract class ComponentFactory {
        /**
         * 查询具有 soundQuality 音质的 MusicItem 表示的的音乐是否已被缓存。
         * <p>
         * 该方法会在异步线程中被调用。
         *
         * @param musicItem    要查询的 MusicItem 对象
         * @param soundQuality 音乐的音质
         * @return 如果已被缓存，则返回 true，否则返回 false
         */
        public abstract boolean isCached(MusicItem musicItem, Player.SoundQuality soundQuality);

        /**
         * 创建一个 {@link MusicPlayer} 对象。
         *
         * @param context Context 对象
         * @return {@link MusicPlayer} 对象，，不能为 null
         */
        @NonNull
        public MusicPlayer createMusicPlayer(Context context) {
            return new MediaMusicPlayer();
        }

        /**
         * 获取音乐的播放链接。
         * <p>
         * 该方法会在异步线程中执行，因此可以在该方法中执行耗时操作，例如访问网络。
         *
         * @param musicItem    要播放的音乐
         * @param soundQuality 要播放的音乐的音质
         * @return 音乐的播放链接，为 null 时播放器会转至 {@link snow.player.Player.PlaybackState#ERROR}
         * 状态
         * @throws Exception 获取音乐播放链接的过程中发生的任何异常
         */
        @SuppressWarnings("RedundantThrows")
        @Nullable
        public Uri retrieveMusicItemUri(MusicItem musicItem, Player.SoundQuality soundQuality) throws Exception {
            return null;
        }

        /**
         * 创建通知栏控制器。
         *
         * @return {@link RemoteView} 对象，可为 null。为 null 时将隐藏通知栏控制器
         */
        @Nullable
        public RemoteView createRemoteView() {
            return null;
        }

        /**
         * 创建音频特效引擎。
         *
         * @return {@link AudioEffectManager} 对象，为 null 时会关闭音频特效
         */
        @Nullable
        public AudioEffectManager createAudioEffectManager() {
            return null;
        }

        /**
         * 创建历史记录器，用于记录播放器的播放历史。
         *
         * @return 如果返回 null，则不会记录播放器的播放历史
         */
        @Nullable
        public HistoryRecorder createHistoryRecorder() {
            return null;
        }
    }

    private boolean isAnnotatedWithInject(Method method) {
        return method.isAnnotationPresent(Inject.class);
    }

    private boolean shouldInject(String methodName, Class<?>... parameterTypes) {
        if (mComponentFactory == null) {
            return false;
        }

        try {
            Method method = mComponentFactory.getClass()
                    .getMethod(methodName, parameterTypes);
            return isAnnotatedWithInject(method);
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        }

        return false;
    }

    private boolean injectMusicPlayer() {
        return shouldInject("createMusicPlayer", Context.class);
    }

    private boolean injectMusicItemUri() {
        return shouldInject("retrieveMusicItemUri", MusicItem.class, Player.SoundQuality.class);
    }

    private boolean injectRemoteView() {
        return shouldInject("createRemoteView");
    }

    private boolean injectAudioEffectManager() {
        return shouldInject("createAudioEffectManager");
    }

    private boolean injectHistoryRecorder() {
        return shouldInject("createHistoryRecorder");
    }
}
