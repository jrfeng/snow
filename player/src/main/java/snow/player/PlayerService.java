package snow.player;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import channel.helper.ChannelHelper;
import channel.helper.Dispatcher;
import channel.helper.pipe.CustomActionPipe;

import channel.helper.pipe.MessengerPipe;
import media.helper.HeadsetHookHelper;

import snow.player.media.MediaMusicPlayer;
import snow.player.media.MusicItem;
import snow.player.media.MusicPlayer;
import snow.player.playlist.AbstractPlaylistPlayer;
import snow.player.playlist.PlaylistManager;
import snow.player.playlist.PlaylistPlayer;
import snow.player.radio.AbstractRadioStationPlayer;
import snow.player.radio.RadioStation;
import snow.player.radio.RadioStationPlayer;
import snow.player.playlist.PersistentPlaylistState;
import snow.player.radio.PersistentRadioStationState;
import snow.player.playlist.PlaylistState;
import snow.player.playlist.PlaylistStateListener;
import snow.player.radio.RadioStationState;
import snow.player.radio.RadioStationStateListener;

@SuppressWarnings("EmptyMethod")
public class PlayerService extends MediaBrowserServiceCompat implements PlayerManager {
    public static final String DEFAULT_MEDIA_ROOT_ID = "root";

    private String mPersistentId;
    private int mNotificationId;

    private PlayerConfig mPlayerConfig;
    private PlaylistState mPlaylistState;
    private RadioStationState mRadioStationState;

    private PlaylistManager mPlaylistManager;
    private PlaylistPlayerImp mPlaylistPlayer;
    private RadioStationPlayerImp mRadioStationPlayer;
    private CustomActionPipe mControllerPipe;

    private HashMap<String, OnCommandCallback> mCommandCallbackMap;

    private boolean mForeground;

    private NotificationManager mNotificationManager;

    private Map<String, Runnable> mStartCommandActionMap;

    private MediaSessionCompat mMediaSession;
    private PlaybackStateCompat.Builder mPlaybackStateBuilder;
    private MediaMetadataCompat.Builder mMediaMetadataBuilder;

    private HeadsetHookHelper mHeadsetHookHelper;

    private RemoteViewManager mRemoteViewManager;

    @Nullable
    private AudioEffectEngine mAudioEffectEngine;

    @Override
    public void onCreate() {
        super.onCreate();
        startService(new Intent(this, this.getClass()));

        mPersistentId = getPersistentId();
        mNotificationId = getNotificationId();
        mCommandCallbackMap = new HashMap<>();
        mStartCommandActionMap = new HashMap<>();

        mNotificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

        initPlayerConfig();
        initAudioEffectEngine();
        initPlayerState();
        initPlaylistManager();
        initPlayer();
        initControllerPipe();
        initRemoteViewManager();
        initHeadsetHookHelper();
        initMediaSession();

        updateNotificationView();
    }

    private void initPlayerConfig() {
        mPlayerConfig = new PlayerConfig(this, mPersistentId);
    }

    private void initPlayerState() {
        mPlaylistState = new PersistentPlaylistState(this, mPersistentId);
        mRadioStationState = new PersistentRadioStationState(this, mPersistentId);
    }

    private void initPlaylistManager() {
        mPlaylistManager = new PlaylistManager(this, mPersistentId) {
        };
    }

    private void initPlayer() {
        boolean enableRadioStationPlayer = (getPlayerType() == PlayerType.RADIO_STATION);

        mPlaylistPlayer = new PlaylistPlayerImp(this, mPlayerConfig, mPlaylistState, !enableRadioStationPlayer, mPlaylistManager);
        mRadioStationPlayer = new RadioStationPlayerImp(this, mPlayerConfig, mRadioStationState, enableRadioStationPlayer);
    }

    private void initControllerPipe() {
        final Dispatcher playerManagerDispatcher
                = ChannelHelper.newDispatcher(PlayerManager.class, this);

        final Dispatcher playlistDispatcher =
                ChannelHelper.newDispatcher(PlaylistPlayer.class, mPlaylistPlayer);

        final Dispatcher radioStationDispatcher =
                ChannelHelper.newDispatcher(RadioStationPlayer.class, mRadioStationPlayer);

        mControllerPipe = new CustomActionPipe(new Dispatcher() {
            @Override
            public boolean dispatch(Map<String, Object> data) {
                if (playerManagerDispatcher.dispatch(data)) {
                    return true;
                }

                if (playlistDispatcher.match(data)) {
                    notifyPlayerTypeChanged(PlayerType.PLAYLIST);
                    playlistDispatcher.dispatch(data);
                    return true;
                }

                if (radioStationDispatcher.match(data)) {
                    notifyPlayerTypeChanged(PlayerType.RADIO_STATION);
                    radioStationDispatcher.dispatch(data);
                    return true;
                }

                return false;
            }

            @Override
            public boolean match(Map<String, Object> data) {
                // ignore
                return false;
            }
        });
    }

    private void initRemoteViewManager() {
        RemoteView playlistRemoteView = onCreatePlaylistRemoteView();
        RemoteView radioStationRemoteView = onCreateRadioStationRemoteView();

        if (playlistRemoteView != null) {
            playlistRemoteView.init(this);
        }

        if (radioStationRemoteView != null) {
            radioStationRemoteView.init(this);
        }

        MusicItem musicItem = getPlayingMusicItem();

        if (musicItem != null && playlistRemoteView != null) {
            playlistRemoteView.setPlayingMusicItem(musicItem);
        }

        if (musicItem != null && radioStationRemoteView != null) {
            radioStationRemoteView.setPlayingMusicItem(musicItem);
        }

        mRemoteViewManager = new RemoteViewManager(playlistRemoteView, radioStationRemoteView);
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

    private void initAudioEffectEngine() {
        mAudioEffectEngine = onCreateAudioEffectEngine();

        if (mAudioEffectEngine == null) {
            return;
        }

        AudioEffectEngine.Config config = mPlayerConfig.getAudioEffectConfig();
        if (config == null) {
            config = mAudioEffectEngine.getDefaultConfig();
            mPlayerConfig.setAudioEffectConfig(config);
        }
        mAudioEffectEngine.init(config);
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

    /***
     * 创建一个适用于列表播放器的 {@link RemoteView}，你可以通过覆盖该方法来实现自定义的列表播放器控制器。
     *
     * 该方法默认返回 {@link SimplePlaylistRemoteView}，如果你不需要在通知栏中显示列表播放器控制器，可以覆盖
     * 该方法并返回 null。
     *
     * @return {@link RemoteView} 对象，返回 null 时将隐藏列表播放器控制器
     */
    @Nullable
    protected RemoteView onCreatePlaylistRemoteView() {
        return new SimplePlaylistRemoteView();
    }

    /***
     * 创建一个适用于电台播放器的 {@link RemoteView}，你可以通过覆盖该方法来实现自定义的电台播放器控制器。
     *
     * 该方法默认返回 {@link SimpleRadioStationRemoteView}，如果你不需要在通知栏中显示列表播放器控制器，可以覆盖
     * 该方法并返回 null。
     *
     * @return {@link RemoteView} 对象，返回 null 时将隐藏电台播放器控制器
     */
    @Nullable
    protected RemoteView onCreateRadioStationRemoteView() {
        return new SimpleRadioStationRemoteView();
    }

    /**
     * 创建音频特效引擎。
     *
     * @return 如果返回 null，将会关闭音频特效引擎
     */
    @Nullable
    protected AudioEffectEngine onCreateAudioEffectEngine() {
        return null;
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
    public int onStartCommand(Intent intent, int flags, int startId) {
        MediaButtonReceiver.handleIntent(mMediaSession, intent);

        Runnable task = mStartCommandActionMap.get(intent.getAction());
        if (task != null) {
            task.run();
        }
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        stopForegroundEx(true);

        mRemoteViewManager.onRelease();

        mMediaSession.release();
        mPlaylistPlayer.release();
        mRadioStationPlayer.release();

        mPlaylistPlayer = null;
        mRadioStationPlayer = null;

        if (mAudioEffectEngine != null) {
            mAudioEffectEngine.release();
        }
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
        if (getPlayerType() == PlayerType.RADIO_STATION) {
            mRadioStationPlayer.notifySoundQualityChanged();
            return;
        }

        mPlaylistPlayer.notifySoundQualityChanged();
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
    public void setAudioEffectConfig(AudioEffectEngine.Config config) {
        if (noAudioEffectEngine()) {
            return;
        }

        mAudioEffectEngine.updateConfig(config);
        mPlayerConfig.setAudioEffectConfig(config);
    }

    private boolean noAudioEffectEngine() {
        return mAudioEffectEngine == null;
    }

    /**
     * 对象指定的 audio session id 应用音频特效。
     *
     * @param audioSessionId 当前正在播放的音乐的 audio session id。如果为 0，则可以忽略。
     */
    protected void attachAudioEffect(int audioSessionId) {
        if (noAudioEffectEngine()) {
            return;
        }

        mAudioEffectEngine.attachAudioEffect(audioSessionId);
    }

    /**
     * 取消当前的音频特效。
     */
    protected void detachAudioEffect() {
        if (noAudioEffectEngine()) {
            return;
        }

        mAudioEffectEngine.detachAudioEffect();
    }

    private void notifyAudioEffectEnableChanged() {
        if (getPlayerType() == PlayerType.RADIO_STATION) {
            mRadioStationPlayer.notifyAudioEffectEnableChanged();
            return;
        }

        mPlaylistPlayer.notifyAudioEffectEnableChanged();
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
        if (getPlayerType() == PlayerType.RADIO_STATION) {
            mRadioStationPlayer.notifyOnlyWifiNetworkChanged();
            return;
        }

        mPlaylistPlayer.notifyOnlyWifiNetworkChanged();
    }

    @Override
    public void setIgnoreLossAudioFocus(boolean ignoreLossAudioFocus) {
        mPlayerConfig.setIgnoreLossAudioFocus(ignoreLossAudioFocus);
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
        mPlaylistPlayer.addStateListener(token, ChannelHelper.newEmitter(PlaylistStateListener.class, pipe));
        mRadioStationPlayer.addStateListener(token, ChannelHelper.newEmitter(RadioStationStateListener.class, pipe));
    }

    @Override
    public void unregisterPlayerStateListener(String token) {
        removeOnConfigChangeListener(token);
        mPlaylistPlayer.removeStateListener(token);
        mRadioStationPlayer.removeStateListener(token);
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

    protected final void notifyPlayerTypeChanged(PlayerType playerType) {
        if (playerType == getPlayerType()) {
            return;
        }

        mPlayerConfig.setPlayerType(playerType);
        mPlaylistPlayer.setEnabled(playerType == PlayerType.PLAYLIST);
        mRadioStationPlayer.setEnabled(playerType == PlayerType.RADIO_STATION);

        for (String key : mCommandCallbackMap.keySet()) {
            OnCommandCallback listener = mCommandCallbackMap.get(key);
            if (listener != null) {
                listener.onPlayerTypeChanged(playerType);
            }
        }
    }

    private void syncPlayerState(OnCommandCallback listener) {
        listener.syncPlayerState(getPlayerType(), new PlaylistState(mPlaylistState), new RadioStationState(mRadioStationState));
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
     * 获取播放器类型。
     */
    protected final PlayerType getPlayerType() {
        return mPlayerConfig.getPlayerType();
    }

    /**
     * 获取列表播放器的播放模式。
     */
    protected final PlaylistPlayer.PlayMode getPlaylistPlayMode() {
        return mPlaylistState.getPlayMode();
    }

    /**
     * 获取播放队列携带的额外参数（可为 null）。
     */
    @Nullable
    protected final Bundle getPlaylistExtra() {
        return mPlaylistPlayer.getPlaylistExtra();
    }

    /**
     * 获取电台携带的额外参数（可为 null）。
     */
    @Nullable
    protected final Bundle getRadioStationExtra() {
        return mRadioStationPlayer.getRadioStationExtra();
    }

    /**
     * 当前是否正在播放音乐。
     */
    protected final boolean isPlaying() {
        switch (getPlayerType()) {
            case PLAYLIST:
                return mPlaylistPlayer.isPlaying();
            case RADIO_STATION:
                return mRadioStationPlayer.isPlaying();
            default:
                return false;
        }
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
     * 获取播放器状态。
     * <p>
     * 当播放器类型为 {@link PlayerType#PLAYLIST} 时，返回是实际上是 {@link PlaylistState}；当播放器类型为
     * {@link PlayerType#RADIO_STATION} 时，返回是实际上是 {@link RadioStationState}。
     *
     * @see #getPlayerType()
     * @see #getPlaylistState()
     * @see #getRadioStationState()
     */
    protected final PlayerState getPlayerState() {
        if (getPlayerType() == PlayerType.RADIO_STATION) {
            return mRadioStationState;
        }

        return mPlaylistState;
    }

    /**
     * 获取列表播放器的状态。
     */
    protected final PlaylistState getPlaylistState() {
        return mPlaylistState;
    }

    /**
     * 获取电台播放的状态。
     */
    protected final RadioStationState getRadioStationState() {
        return mRadioStationState;
    }

    public final boolean isPreparingState() {
        return getPlayerState().getPlaybackState() == Player.PlaybackState.PREPARING;
    }

    public final boolean isStalled() {
        return getPlayerState().isStalled();
    }

    /**
     * 获取当前正在播放的音乐的 MusicItem 对象。
     */
    protected final MusicItem getPlayingMusicItem() {
        switch (getPlayerType()) {
            case PLAYLIST:
                return mPlaylistState.getMusicItem();
            case RADIO_STATION:
                return mRadioStationState.getMusicItem();
            default:
                return null;
        }
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
        switch (getPlayerType()) {
            case PLAYLIST:
                return mPlaylistState.getErrorCode();
            case RADIO_STATION:
                return mRadioStationState.getErrorCode();
            default:
                return Player.Error.NO_ERROR;
        }
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
        if (mRemoteViewManager.noRemoteView(getPlayerType())) {
            return;
        }

        MusicItem musicItem = getPlayingMusicItem();
        if (musicItem == null) {
            stopForegroundEx(true);
            return;
        }

        mRemoteViewManager.setPlayingMusicItem(getPlayerType(), musicItem);

        if (isPreparingOrPlayingState() && !isForeground()) {
            startForeground();
            return;
        }

        if (!isPreparingOrPlayingState() && isForeground()) {
            stopForegroundEx(false);
        }

        updateNotification();
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
        if (mRemoteViewManager.noRemoteView(getPlayerType())) {
            return;
        }

        if (getPlayingMusicItem() == null) {
            stopForegroundEx(true);
            return;
        }

        mForeground = true;
        startForeground(mNotificationId, createNotification(getPlayerType()));
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
        if (mRemoteViewManager.noRemoteView(getPlayerType())) {
            return;
        }

        if (getPlayingMusicItem() == null) {
            stopForegroundEx(true);
            return;
        }

        mNotificationManager.notify(mNotificationId, createNotification(getPlayerType()));
    }

    @NonNull
    private Notification createNotification(PlayerType playerType) {
        return mRemoteViewManager.createRemoteView(playerType);
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
        return false;
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

    protected void onPrepareMusicPlayer(MusicPlayer musicPlayer,
                                        MusicItem musicItem,
                                        Player.SoundQuality soundQuality) throws Exception {
        if (!musicPlayer.isInvalid()) {
            musicPlayer.prepare(Uri.parse(musicItem.getUri()));
        }
    }

    /**
     * 获取 “电台” 的下一首音乐。
     * <p>
     * “电台” 播放器专用。如果你需要实现 “电台” 功能，那么需要覆盖该方法，并返回 {@code radioStation} 参数
     * 表示的 “电台” 的下一首歌曲。
     * <p>
     * 该方法会在异步线程中调用。
     *
     * @param radioStation 用于表示电台的 RadioStation 对象
     * @return “电台” 的下一首音乐（返回 null 时会停止播放）
     */
    @SuppressWarnings("SameReturnValue")
    @Nullable
    protected MusicItem getNextMusicItem(RadioStation radioStation) {
        return null;
    }

    private Player getPlayer() {
        if (getPlayerType() == PlayerType.RADIO_STATION) {
            return mRadioStationPlayer;
        }

        return mPlaylistPlayer;
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
        getPlayer().playOrPause();
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
        if (getPlayerType() == PlayerType.RADIO_STATION) {
            mRadioStationPlayer.skipToNext();
            return;
        }

        mPlaylistPlayer.skipToNext();
    }

    /**
     * 上一曲。
     * <p>
     * 但播放器类型为 {@link PlayerType#RADIO_STATION}（电台模式）时，该方法无效。
     */
    protected final void skipToPrevious() {
        if (getPlayerType() == PlayerType.RADIO_STATION) {
            return;
        }

        mPlaylistPlayer.skipToPrevious();
    }

    protected void onPreparing() {
        updateNotificationView();
    }

    protected void onPrepared(int audioSessionId) {
    }

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

    protected void onStalledChanged(boolean stalled) {
        updateNotificationView();
    }

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

    protected void onPlayComplete(MusicItem musicItem) {
    }

    protected void onRequestAudioFocus(boolean success) {
    }

    protected void onLossAudioFocus() {
    }

    protected void onPlayingMusicItemChanged(@Nullable MusicItem musicItem) {
        mMediaSession.setMetadata(getFreshMediaMetadata());
        updateNotificationView();
    }

    protected boolean onMediaButtonEvent(Intent mediaButtonEvent) {
        return mHeadsetHookHelper.handleMediaButton(mediaButtonEvent);
    }

    protected void onCustomAction(String action, Bundle extras) {
        mControllerPipe.dispatch(action, extras);
    }

    private class PlaylistPlayerImp extends AbstractPlaylistPlayer {

        public PlaylistPlayerImp(@NonNull Context context,
                                 @NonNull PlayerConfig playerConfig,
                                 @NonNull PlaylistState playlistState,
                                 boolean enable,
                                 @NonNull PlaylistManager playlistManager) {
            super(context, playerConfig, playlistState, enable, playlistManager);
        }

        @Override
        protected boolean isCached(MusicItem musicItem, SoundQuality soundQuality) {
            return PlayerService.this.isCached(musicItem, soundQuality);
        }

        @NonNull
        @Override
        protected MusicPlayer onCreateMusicPlayer(Context context) {
            return PlayerService.this.onCreateMusicPlayer(context);
        }

        @Override
        protected void onPrepareMusicPlayer(MusicPlayer musicPlayer,
                                            MusicItem musicItem,
                                            SoundQuality soundQuality) throws Exception {
            PlayerService.this.onPrepareMusicPlayer(musicPlayer, musicItem, soundQuality);
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
        protected void onPlayComplete(MusicItem musicItem) {
            super.onPlayComplete(musicItem);
            PlayerService.this.onPlayComplete(musicItem);
        }

        @Override
        protected void onRequestAudioFocus(boolean success) {
            super.onRequestAudioFocus(success);
            PlayerService.this.onRequestAudioFocus(success);
        }

        @Override
        protected void onLossAudioFocus() {
            super.onLossAudioFocus();
            PlayerService.this.onLossAudioFocus();
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

    private class RadioStationPlayerImp extends AbstractRadioStationPlayer {

        public RadioStationPlayerImp(@NonNull Context context,
                                     @NonNull PlayerConfig playerConfig,
                                     @NonNull RadioStationState radioStationState,
                                     boolean enabled) {
            super(context, playerConfig, radioStationState, enabled);
        }

        @Nullable
        @Override
        protected MusicItem getNextMusicItem(RadioStation radioStation) {
            return PlayerService.this.getNextMusicItem(radioStation);
        }

        @Override
        protected boolean isCached(MusicItem musicItem, SoundQuality soundQuality) {
            return PlayerService.this.isCached(musicItem, soundQuality);
        }

        @NonNull
        @Override
        protected MusicPlayer onCreateMusicPlayer(Context context) {
            return PlayerService.this.onCreateMusicPlayer(context);
        }

        @Override
        protected void onPrepareMusicPlayer(MusicPlayer musicPlayer,
                                            MusicItem musicItem,
                                            SoundQuality soundQuality) throws Exception {
            PlayerService.this.onPrepareMusicPlayer(musicPlayer, musicItem, soundQuality);
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
        protected void onPlayComplete(MusicItem musicItem) {
            super.onPlayComplete(musicItem);
            PlayerService.this.onPlayComplete(musicItem);
        }

        @Override
        protected void onRequestAudioFocus(boolean success) {
            super.onRequestAudioFocus(success);
            PlayerService.this.onRequestAudioFocus(success);
        }

        @Override
        protected void onLossAudioFocus() {
            super.onLossAudioFocus();
            PlayerService.this.onLossAudioFocus();
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
            if (getPlayerType() == PlayerType.RADIO_STATION) {
                return;
            }

            mPlaylistPlayer.playOrPause((int) id);
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
            if (getPlayerType() == PlayerType.RADIO_STATION) {
                return;
            }

            if (repeatMode == PlaybackStateCompat.REPEAT_MODE_ONE) {
                mPlaylistPlayer.setPlayMode(PlaylistPlayer.PlayMode.LOOP);
                return;
            }

            mPlaylistPlayer.setPlayMode(PlaylistPlayer.PlayMode.SEQUENTIAL);
        }

        @Override
        public void onSetShuffleMode(int shuffleMode) {
            if (getPlayerType() == PlayerType.RADIO_STATION) {
                return;
            }

            if (shuffleMode == PlaybackStateCompat.SHUFFLE_MODE_NONE ||
                    shuffleMode == PlaybackStateCompat.SHUFFLE_MODE_INVALID) {
                mPlaylistPlayer.setPlayMode(PlaylistPlayer.PlayMode.SEQUENTIAL);
                return;
            }

            mPlaylistPlayer.setPlayMode(PlaylistPlayer.PlayMode.SHUFFLE);
        }
    }

    private static class RemoteViewManager {
        private RemoteView mPlaylistRemoteView;
        private RemoteView mRadioStationRemoteView;

        RemoteViewManager(RemoteView playlistRemoteView, RemoteView radioStationRemoteView) {
            mPlaylistRemoteView = playlistRemoteView;
            mRadioStationRemoteView = radioStationRemoteView;
        }

        boolean noRemoteView(PlayerType playerType) {
            switch (playerType) {
                case PLAYLIST:
                    return mPlaylistRemoteView == null;
                case RADIO_STATION:
                    return mRadioStationRemoteView == null;
            }

            return true;
        }

        void setPlayingMusicItem(PlayerType playerType, MusicItem musicItem) {
            if (noRemoteView(playerType)) {
                return;
            }

            switch (playerType) {
                case PLAYLIST:
                    mPlaylistRemoteView.setPlayingMusicItem(musicItem);
                    break;
                case RADIO_STATION:
                    mRadioStationRemoteView.setPlayingMusicItem(musicItem);
                    break;
            }
        }

        Notification createRemoteView(PlayerType playerType) {
            if (noRemoteView(playerType)) {
                throw new IllegalStateException("player type [" + playerType + "] not set remote view.");
            }

            switch (playerType) {
                case PLAYLIST:
                    return mPlaylistRemoteView.createNotification();
                case RADIO_STATION:
                    return mRadioStationRemoteView.createNotification();
            }

            throw new IllegalStateException("can't create remote view");
        }

        void onRelease() {
            if (mPlaylistRemoteView != null) {
                mPlaylistRemoteView.onRelease();
            }

            if (mRadioStationRemoteView != null) {
                mRadioStationRemoteView.onRelease();
            }
        }
    }

    /**
     * 用于显示通知栏控制器。
     */
    public static abstract class RemoteView {
        private PlayerService mPlayerService;
        private MusicItem mMusicItem;

        private String mChannelId;

        private int[] mIconSize;            // [width, height]
        private int[] mIconCornerRadius;    // [topLeft, topRight, bottomRight, bottomLeft]
        private boolean mNeedReloadIcon;
        private Bitmap mIcon;
        private CustomTarget<Bitmap> mTarget;

        private PendingIntent mContentIntent;

        private List<CustomAction> mAllCustomAction;

        void init(PlayerService playerService) {
            mPlayerService = playerService;
            mMusicItem = new MusicItem();
            mChannelId = "player";
            mIconSize = new int[2];
            mIconCornerRadius = new int[4];
            mAllCustomAction = new ArrayList<>();

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
         * 创建自定义 Notification 视图，不能为 null。
         */
        @NonNull
        public abstract RemoteViews onCreateContentView();

        /**
         * 创建自定义 Big Notification 视图，不能为 null。
         */
        @NonNull
        public abstract RemoteViews onCreateBigContentView();

        /**
         * 获取默认图标的资源 ID
         */
        @DrawableRes
        public abstract int getDefaultIconId();

        /**
         * 该方法会在初次创建 NotificationView 对象时调用，你可以重写该方法来进行一些初始化操作。
         */
        protected void onInit(Context context) {
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
        protected Notification onCreateNotification() {
            RemoteViews contentView = onCreateContentView();
            RemoteViews bigContentView = onCreateBigContentView();

            initAllCustomAction(contentView, true);
            initAllCustomAction(bigContentView, false);

            return new NotificationCompat.Builder(getContext(), mChannelId)
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

        /**
         * 设置 channelId，该值将用于创建 Notification 对象。
         * <p>
         * 默认的 channelId 值为 {@code "player"}
         */
        protected final void setChannelId(@NonNull String channelId) {
            Preconditions.checkNotNull(channelId);
            mChannelId = channelId;
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
         * 获取通知栏控制器用于显示的 content text，该方法会根据播放器状态的不同而返回不同的值。
         * <p>
         * 例如，在 {@link snow.player.Player.PlaybackState#ERROR} 状态时，会返回一个
         * {@code android.R.color.holo_red_dark} 颜色的描述错误信息的 CharSequence 对象；而在
         * {@link snow.player.Player.PlaybackState#PREPARING} 状态时，会返回一个
         * {@code android.R.color.holo_green_dark} 颜色的值为 “准备中…” 的 CharSequence 对象；而在
         * {@link #isStalled()} 返回 true 时，会返回一个 {@code android.R.color.holo_orange_dark} 颜色
         * 的值为 “缓冲中…” 的 CharSequence 对象。其它状态下会将 {@code defaultValue} 原值返回。
         */
        protected final CharSequence getContentText(String defaultValue) {
            String text = defaultValue;
            int textColor = 0;

            Resources res = getContext().getResources();

            if (isError()) {
                text = getErrorMessage();
                textColor = res.getColor(android.R.color.holo_red_dark);
            }

            if (isPreparingState()) {
                text = getContext().getString(R.string.snow_preparing);
                textColor = res.getColor(android.R.color.holo_green_dark);
            }

            if (isStalled()) {
                text = res.getString(R.string.snow_buffering);
                textColor = res.getColor(android.R.color.holo_orange_dark);
            }

            CharSequence contentText = text;

            if (textColor != 0) {
                SpannableString colorText = new SpannableString(text);
                colorText.setSpan(new ForegroundColorSpan(textColor), 0, text.length(), Spanned.SPAN_INCLUSIVE_EXCLUSIVE);

                contentText = colorText;
            }

            return contentText;
        }

        protected final Context getContext() {
            return mPlayerService;
        }

        protected final String getPackageName() {
            return getContext().getPackageName();
        }

        /**
         * 获取列表播放器的播放模式。
         */
        protected final PlaylistPlayer.PlayMode getPlaylistPlayMode() {
            return mPlayerService.getPlaylistPlayMode();
        }

        /**
         * 获取播放队列携带的额外参数（可为 null）。
         */
        @Nullable
        protected final Bundle getPlaylistExtra() {
            return mPlayerService.getPlaylistExtra();
        }

        /**
         * 获取电台携带的额外参数（可为 null）。
         */
        @Nullable
        protected final Bundle getRadioStationExtra() {
            return mPlayerService.getRadioStationExtra();
        }

        /**
         * 添加自定义动作，不能为 null。
         * <p>
         * 如果自定义动作已添加，则会将其替换掉。
         *
         * @param customAction 要添加的自定义动作，如果自定义动作已添加，则会将其替换掉
         * @see CustomAction#equals(Object)
         */
        protected final void addCustomAction(@NonNull CustomAction customAction) {
            Preconditions.checkNotNull(customAction);

            mAllCustomAction.remove(customAction);
            mAllCustomAction.add(customAction);
        }

        /**
         * 移除自定义动作。
         *
         * @param customAction 要移除的自定义动作
         */
        protected final void removeCustomAction(CustomAction customAction) {
            mAllCustomAction.remove(customAction);
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

        private PendingIntent createCustomActionPendingIntent(@NonNull String action, @NonNull Runnable task) {
            return mPlayerService.addOnStartCommandAction(action, task);
        }

        private void initCustomAction(CustomAction customAction, RemoteViews remoteViews) {
            int viewId = customAction.getViewId();
            int iconId = customAction.getIconId();

            remoteViews.setViewVisibility(viewId, View.VISIBLE);

            if (iconId != CustomAction.IGNORE_ICON_ID) {
                remoteViews.setImageViewResource(viewId, iconId);
            }

            remoteViews.setOnClickPendingIntent(viewId,
                    createCustomActionPendingIntent(customAction.getActionName(), customAction.getAction()));
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

            mMusicItem = musicItem;
        }

        boolean isNeedReloadIcon() {
            return mNeedReloadIcon;
        }

        void setNeedReloadIcon(boolean needReloadIcon) {
            mNeedReloadIcon = needReloadIcon;
        }

        /**
         * 用于创建一个可添加到 {@link SimplePlaylistRemoteView} 中的自定义动作。
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
     * 抽象类，该类提供了 RemoteView 的基本实现。
     * <p>
     * 具体请查看它的子类：
     * <ul>
     *     <li>{@link SimplePlaylistRemoteView}</li>
     *     <li>{@link SimpleRadioStationRemoteView}</li>
     * </ul>
     */
    public static abstract class SimpleRemoteView extends RemoteView {
        private static final String ACTION_SKIP_TO_PREVIOUS = "snow.player.action.skip_to_previous";
        private static final String ACTION_SKIP_TO_NEXT = "snow.player.action.skip_to_next";
        private static final String ACTION_PLAY_PAUSE = "snow.player.action.play_pause";
        private static final String CUSTOM_ACTION_1 = "snow.player.action.custom_action_1";
        private static final String CUSTOM_ACTION_2 = "snow.player.action.custom_action_2";

        public abstract boolean enableSkipToPrevious();

        public abstract int getContentViewLayoutId();

        public abstract int getBigContentViewLayoutId();

        @Override
        protected void onInit(Context context) {
            super.onInit(context);

            Resources res = context.getResources();

            setIconSize(res.getDimensionPixelSize(R.dimen.snow_notif_icon_size_big));
            setIconCornerRadius(res.getDimensionPixelSize(R.dimen.snow_notif_icon_corner_radius));

            initCustomAction();
        }

        private void initCustomAction() {
            if (enableSkipToPrevious()) {
                addCustomAction(new CustomAction(ACTION_SKIP_TO_PREVIOUS, R.id.snow_notif_skip_to_previous, new Runnable() {
                    @Override
                    public void run() {
                        skipToPrevious();
                    }
                }));
            }

            addCustomAction(new CustomAction(ACTION_SKIP_TO_NEXT, R.id.snow_notif_skip_to_next, new Runnable() {
                @Override
                public void run() {
                    skipToNext();
                }
            }));

            addCustomAction(new CustomAction(ACTION_PLAY_PAUSE, R.id.snow_notif_play_pause, new Runnable() {
                @Override
                public void run() {
                    playOrPause();
                }
            }));
        }

        @DrawableRes
        @Override
        public int getDefaultIconId() {
            return R.mipmap.snow_notif_default_icon;
        }

        @NonNull
        public RemoteViews onCreateContentView() {
            RemoteViews contentView = new RemoteViews(getPackageName(), getContentViewLayoutId());

            contentView.setImageViewBitmap(R.id.snow_notif_icon, getIcon());
            contentView.setTextViewText(R.id.snow_notif_title, getContentTitle());
            contentView.setTextViewText(R.id.snow_notif_text, getContentText(getPlayingMusicItem().getArtist()));

            if (isPreparingOrPlayingState()) {
                contentView.setImageViewResource(R.id.snow_notif_play_pause, R.drawable.snow_ic_pause);
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
                bigContentView.setImageViewResource(R.id.snow_notif_play_pause, R.drawable.snow_ic_pause);
            }

            return bigContentView;
        }

        /**
         * 设置 {@link SimplePlaylistRemoteView} 的第一个自定义动作。
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
         * 设置 {@link SimplePlaylistRemoteView} 的第一个自定义动作。
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
    }

    /**
     * 该类提供了 列表播放器控制器 的默认实现。
     */
    public static class SimplePlaylistRemoteView extends SimpleRemoteView {
        @Override
        public boolean enableSkipToPrevious() {
            return true;
        }

        @Override
        public int getContentViewLayoutId() {
            return R.layout.snow_simple_playlist_remote_view;
        }

        @Override
        public int getBigContentViewLayoutId() {
            return R.layout.snow_simple_playlist_remote_view_big;
        }
    }

    /**
     * 该类提供了 电台播放器控制器 的默认实现。
     */
    public static class SimpleRadioStationRemoteView extends SimpleRemoteView {
        @Override
        public boolean enableSkipToPrevious() {
            return false;
        }

        @Override
        public int getContentViewLayoutId() {
            return R.layout.snow_simple_radio_station_remote_view;
        }

        @Override
        public int getBigContentViewLayoutId() {
            return R.layout.snow_simple_radio_station_remote_view_big;
        }
    }
}
