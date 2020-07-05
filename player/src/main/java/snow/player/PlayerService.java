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
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.media.MediaBrowserServiceCompat;
import androidx.media.session.MediaButtonReceiver;

import com.bumptech.glide.Glide;
import com.bumptech.glide.RequestBuilder;
import com.bumptech.glide.load.resource.bitmap.RoundedCorners;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;
import com.google.common.base.Preconditions;

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

    @Nullable
    private NotificationView mNotificationView;

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
        initPlayerState();
        initPlaylistManager();
        initPlayer();
        initControllerPipe();
        initNotificationView();
        initHeadsetHookHelper();
        initMediaSession();

        updateNotificationView();
    }

    private void initPlayerConfig() {
        mPlayerConfig = new PersistentPlayerConfig(this, mPersistentId);
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
        boolean enableRadioStationPlayer = (getPlayerType() == TYPE_RADIO_STATION);

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
                    notifyPlayerTypeChanged(PlayerManager.TYPE_PLAYLIST);
                    playlistDispatcher.dispatch(data);
                    return true;
                }

                if (radioStationDispatcher.match(data)) {
                    notifyPlayerTypeChanged(PlayerManager.TYPE_RADIO_STATION);
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

    private void initNotificationView() {
        mNotificationView = onCreateNotificationView();

        if (noNotificationView()) {
            return;
        }

        mNotificationView.init(this,
                addOnStartCommandAction("snow.player.action.skip_to_previous", new Runnable() {
                    @Override
                    public void run() {
                        skipToPrevious();
                    }
                }),
                addOnStartCommandAction("snow.player.action.play_pause", new Runnable() {
                    @Override
                    public void run() {
                        playOrPause();
                    }
                }),
                addOnStartCommandAction("snow.player.action.skip_to_next", new Runnable() {
                    @Override
                    public void run() {
                        skipToNext();
                    }
                }));

        MusicItem musicItem = getPlayingMusicItem();
        if (musicItem != null) {
            mNotificationView.setPlayingMusicItem(musicItem);
        }
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

    private boolean noNotificationView() {
        return mNotificationView == null;
    }

    /***
     * 该方法用来创建 {@link NotificationView}，你可以通过覆盖该方法来实现自定义的 {@link NotificationView}。
     *
     * 该方法默认返回 {@link SimpleNotificationView}，如果你不需要 NotificationView，可以覆盖该方法并返
     * 回 null。
     *
     * @return {@link NotificationView} 对象，返回 null 时将隐藏 NotificationView
     */
    @Nullable
    protected NotificationView onCreateNotificationView() {
        return new SimpleNotificationView();
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

        if (mNotificationView != null) {
            mNotificationView.onRelease();
        }

        mPlaylistPlayer.release();
        mRadioStationPlayer.release();

        mPlaylistPlayer = null;
        mRadioStationPlayer = null;
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
        if (getPlayerType() == TYPE_RADIO_STATION) {
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

    private void notifyAudioEffectEnableChanged() {
        if (getPlayerType() == TYPE_RADIO_STATION) {
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
        if (getPlayerType() == TYPE_RADIO_STATION) {
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

    protected final void notifyPlayerTypeChanged(int playerType) {
        if (playerType == getPlayerType()) {
            return;
        }

        mPlayerConfig.setPlayerType(playerType);
        mPlaylistPlayer.setEnabled(playerType == PlayerManager.TYPE_PLAYLIST);
        mRadioStationPlayer.setEnabled(playerType == PlayerManager.TYPE_RADIO_STATION);

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
    protected final int getPlayerType() {
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
            case TYPE_PLAYLIST:
                return mPlaylistPlayer.isPlaying();
            case TYPE_RADIO_STATION:
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
     * 当播放器类型为 {@link #TYPE_PLAYLIST} 时，返回是实际上是 {@link PlaylistState}；当播放器类型为
     * {@link #TYPE_RADIO_STATION} 时，返回是实际上是 {@link RadioStationState}。
     *
     * @see #getPlayerType()
     * @see #getPlaylistState()
     * @see #getRadioStationState()
     */
    protected final PlayerState getPlayerState() {
        if (getPlayerType() == TYPE_RADIO_STATION) {
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
            case TYPE_PLAYLIST:
                return mPlaylistState.getMusicItem();
            case TYPE_RADIO_STATION:
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
            case TYPE_PLAYLIST:
                return mPlaylistState.getErrorCode();
            case TYPE_RADIO_STATION:
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
        if (noNotificationView()) {
            return;
        }

        MusicItem musicItem = getPlayingMusicItem();
        if (musicItem == null) {
            stopForegroundEx(true);
            return;
        }

        mNotificationView.setPlayingMusicItem(musicItem);

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
        if (noNotificationView()) {
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
        if (noNotificationView()) {
            return;
        }

        if (getPlayingMusicItem() == null) {
            stopForegroundEx(true);
            return;
        }

        mNotificationManager.notify(mNotificationId, createNotification(getPlayerType()));
    }

    @NonNull
    private Notification createNotification(int playerType) {
        if (mNotificationView == null) {
            throw new IllegalStateException("NotificationView is null");
        }
        return mNotificationView.createNotification(playerType);
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
        if (getPlayerType() == TYPE_RADIO_STATION) {
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
        if (getPlayerType() == TYPE_RADIO_STATION) {
            mRadioStationPlayer.skipToNext();
            return;
        }

        mPlaylistPlayer.skipToNext();
    }

    /**
     * 上一曲。
     * <p>
     * 但播放器类型为 {@link PlayerManager#TYPE_RADIO_STATION}（电台模式）时，该方法无效。
     */
    protected final void skipToPrevious() {
        if (getPlayerType() == TYPE_RADIO_STATION) {
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

        if (noNotificationView()) {
            return;
        }

        mNotificationView.setNeedReloadIcon(true);
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
            if (getPlayerType() == TYPE_RADIO_STATION) {
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
            if (getPlayerType() == TYPE_RADIO_STATION) {
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
            if (getPlayerType() == TYPE_RADIO_STATION) {
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

    /**
     * 用于显示通知栏控制器。
     */
    public static abstract class NotificationView {
        private PlayerService mPlayerService;

        private MusicItem mMusicItem;

        private boolean mNeedReloadIcon;

        private PendingIntent mSkipToPrevious;
        private PendingIntent mPlayOrPause;
        private PendingIntent mSkipToNext;

        void init(PlayerService playerService,
                  PendingIntent skipPrevious,
                  PendingIntent playOrPause,
                  PendingIntent skipNext) {
            mMusicItem = new MusicItem();
            mNeedReloadIcon = true;

            mPlayerService = playerService;
            mSkipToPrevious = skipPrevious;
            mPlayOrPause = playOrPause;
            mSkipToNext = skipNext;

            onInit(mPlayerService);
        }

        /**
         * 重写该方法创建一个新的 Notification 对象。
         *
         * @param playerType 播放器类型
         * @return Notification 对象，不能为 null。
         */
        @NonNull
        public abstract Notification onCreateNotification(int playerType);

        /**
         * 重写该方法以重新加载当前正在播放的音乐的图标。
         */
        protected void reloadIcon() {
            setNeedReloadIcon(false);
        }

        /**
         * 该方法会在初次创建 NotificationView 对象时调用，你可以重写该方法来进行一些初始化操作。
         */
        protected void onInit(Context context) {
        }

        /**
         * 该方法会在 Service 销毁时调用，可以在该方法中是否占用的资源。
         */
        protected void onRelease() {
        }

        protected final Context getContext() {
            return mPlayerService;
        }

        protected final String getPackageName() {
            return getContext().getPackageName();
        }

        /**
         * 获取播放器类型。
         */
        protected final int getPlayerType() {
            return mPlayerService.getPlayerType();
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
         * 添加一个自定义动作。
         * <p>
         * 该方法会返回一个 PendingIntent 对象，该 PendingIntent 对象会使用指定的 action 启动当前 Service。当
         * Service 在 {@link #onStartCommand(Intent, int, int)} 方法中检测到该 action 时，会执行其对应的 task。
         */
        protected final PendingIntent addCustomAction(@NonNull String action, @NonNull Runnable task) {
            return mPlayerService.addOnStartCommandAction(action, task);
        }

        /**
         * 移除一个已添加的自定义动作。
         * <p>
         * 需要指出的时，当一个自定义动作被移除后，其对应的 PendingIntent 仍然可以用来启动当前 Service，只不过
         * {@link #onStartCommand(Intent, int, int)} 不会在响应对应的 action。建议在调用该方法后同时取消注册
         * 时返回的那个 PendingIntent 对象（调用 PendingIntent 的 cancel() 方法进行取消）。
         */
        protected final void removeCustomAction(@NonNull String action) {
            mPlayerService.removeOnStartCommandAction(action);
        }

        /**
         * 获取用来触发 “上一曲” 的 PendingIntent 对象。
         */
        protected final PendingIntent getSkipToPreviousPendingIntent() {
            return mSkipToPrevious;
        }

        /**
         * 获取用来触发 “播放/暂停” 的 PendingIntent 对象。
         */
        protected final PendingIntent getPlayOrPausePendingIntent() {
            return mPlayOrPause;
        }

        /**
         * 获取用来触发 “下一曲” 的 PendingIntent 对象。
         */
        protected final PendingIntent getSkipToNextPendingIntent() {
            return mSkipToNext;
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
        public MediaSessionCompat getMediaSession() {
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
        Notification createNotification(int playerType) {
            if (isNeedReloadIcon()) {
                reloadIcon();
            }
            return onCreateNotification(playerType);
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
    }

    /**
     * 该类提供了 NotificationView 的默认实现。
     */
    public static class SimpleNotificationView extends NotificationView {
        private static final String CUSTOM_ACTION_1 = "snow.player.custom_action_1";
        private static final String CUSTOM_ACTION_2 = "snow.player.custom_action_2";
        private static final String COMPAT_CUSTOM_ACTION = "snow.player.compat_custom_action";

        private PendingIntent mContentIntent;

        private Bitmap mIcon;
        private CustomTarget<Bitmap> mTarget;
        private int mIconCornerRadius;

        private int mDefaultIconId;

        private CustomAction mCustomAction1;
        private CustomAction mCustomAction2;
        private CustomAction mCompatCustomAction;

        @Override
        protected void onInit(Context context) {
            super.onInit(context);
            mDefaultIconId = R.mipmap.snow_notif_default_icon;

            Resources res = context.getResources();

            mIconCornerRadius = res.getDimensionPixelSize(R.dimen.snow_notif_icon_corner_radius);
            int iconSize = res.getDimensionPixelSize(R.dimen.snow_notif_icon_size_big);

            mTarget = new CustomTarget<Bitmap>(iconSize, iconSize) {
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

        @NonNull
        @Override
        public Notification onCreateNotification(int playerType) {
            return new NotificationCompat.Builder(getContext(), "player")
                    .setSmallIcon(R.drawable.snow_ic_music)
                    .setStyle(new androidx.media.app.NotificationCompat.DecoratedMediaCustomViewStyle())
                    .setCustomContentView(onCreateContentView(playerType))
                    .setCustomBigContentView(onCreateBigContentView(playerType))
                    .setContentIntent(getContentIntent())
                    .build();
        }

        @Override
        protected void onRelease() {
            super.onRelease();
            Glide.with(getContext()).clear(mTarget);
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
         * 设置默认的通知栏图标资源。
         * <p>
         * 如果你需要修改默认的通知栏图标，建议在 {@link #onInit(Context)} 调用该方法进行设置。
         */
        protected final void setDefaultIconId(@DrawableRes int resId) {
            mDefaultIconId = resId;
        }

        /**
         * 获取默认的通知栏图标资源。
         */
        @DrawableRes
        protected final int getDefaultIconId() {
            return mDefaultIconId;
        }

        /**
         * 设置第 1 号自定义动作。
         *
         * @param customAction 第 1 号自定义动作，可为 null。为 null 时相当于清除已设置的自定义动作。
         */
        public final void setCustomAction1(CustomAction customAction) {
            mCustomAction1 = customAction;
        }

        /**
         * 获取第 1 号自定义动作。
         */
        public final CustomAction getCustomAction1() {
            return mCustomAction1;
        }

        /**
         * 设置第 2 号自定义动作。
         *
         * @param customAction 第 2 号自定义动作，可为 null。为 null 时相当于清除已设置的自定义动作。
         */
        public final void setCustomAction2(CustomAction customAction) {
            mCustomAction2 = customAction;
        }

        /**
         * 获取第 2 号自定义动作。
         */
        public final CustomAction getCustomAction2() {
            return mCustomAction2;
        }

        /**
         * 设置 compat 自定义动作。
         * <p>
         * 该自定义动作主要用于兼容低版本的通知栏控制器。默认情况下，使用的是第 2 号自定义动作，如果没有设置
         * 第 2 号自定义动作，则使用第 1 号自定义动作。
         * <p>
         * 通过不需要调用该方法，除非你需要修改上述默认的行为。
         *
         * @param compatCustomAction 通常将其指定为第 1 号或第 2 号自定义动作。
         */
        public final void setCompatCustomAction(CustomAction compatCustomAction) {
            mCompatCustomAction = compatCustomAction;
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

        /**
         * 加载图标。
         * <p>
         * 你可以覆盖该方法来实现你自己的图标加载逻辑。
         */
        @Override
        protected void reloadIcon() {
            super.reloadIcon();

            Glide.with(getContext())
                    .clear(mTarget);

            Glide.with(getContext())
                    .asBitmap()
                    .load(getPlayingMusicItem().getIconUri())
                    .error(loadEmbeddedIcon())
                    .transform(new RoundedCorners(mIconCornerRadius))
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
                    .transform(new RoundedCorners(mIconCornerRadius));
        }

        private RequestBuilder<Bitmap> loadDefaultIcon() {
            return Glide.with(getContext())
                    .asBitmap()
                    .load(getDefaultIconId())
                    .transform(new RoundedCorners(mIconCornerRadius));
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
         * 根据 {@code playerType} 参数创建对应的 content view 对象。
         * <p>
         * 你可以覆盖该方法来自定义通知栏控制器的外观（折叠后）。注意！覆盖该方法时，请同时覆盖
         * {@link #onCreateBigContentView(int)} 方法。
         *
         * @see #onCreateBigContentView(int)
         */
        protected RemoteViews onCreateContentView(int playerType) {
            RemoteViews contentView = new RemoteViews(getPackageName(),
                    R.layout.snow_simple_notification_view);

            contentView.setImageViewBitmap(R.id.snow_notif_icon, getIcon());
            contentView.setTextViewText(R.id.snow_notif_title, getContentTitle());
            contentView.setTextViewText(R.id.snow_notif_text, getContentText(getPlayingMusicItem().getArtist()));

            contentView.setOnClickPendingIntent(R.id.snow_notif_play_pause, getPlayOrPausePendingIntent());
            contentView.setOnClickPendingIntent(R.id.snow_notif_skip_to_next, getSkipToNextPendingIntent());

            if (isPreparingOrPlayingState()) {
                contentView.setImageViewResource(R.id.snow_notif_play_pause, R.drawable.snow_ic_pause);
            }

            if (playerType == TYPE_RADIO_STATION) {
                contentView.setViewVisibility(R.id.snow_notif_radio_tip, View.VISIBLE);
            }

            CustomAction customAction = mCompatCustomAction;
            if (customAction == null) {
                customAction = mCustomAction2;
            }
            if (customAction == null) {
                customAction = mCustomAction1;
            }
            initCustomAction(COMPAT_CUSTOM_ACTION, customAction, contentView, playerType, R.id.snow_notif_compat_custom_action);

            return contentView;
        }

        /**
         * 根据 {@code playerType} 参数创建对应的 big content view 对象。
         * <p>
         * 你可以覆盖该方法来自定义通知栏控制器的外观（大）。注意！覆盖该方法时，请同时覆盖
         * {@link #onCreateContentView(int)} 方法。
         *
         * @see #onCreateContentView(int)
         */
        protected RemoteViews onCreateBigContentView(int playerType) {
            RemoteViews bigContentView = new RemoteViews(getPackageName(),
                    R.layout.snow_simple_notification_view_big);

            bigContentView.setImageViewBitmap(R.id.snow_notif_icon, getIcon());
            bigContentView.setTextViewText(R.id.snow_notif_title, getContentTitle());
            bigContentView.setTextViewText(R.id.snow_notif_text, getContentText(getPlayingMusicItem().getArtist()));

            bigContentView.setOnClickPendingIntent(R.id.snow_notif_play_pause, getPlayOrPausePendingIntent());
            bigContentView.setOnClickPendingIntent(R.id.snow_notif_skip_to_next, getSkipToNextPendingIntent());

            if (isPreparingOrPlayingState()) {
                bigContentView.setImageViewResource(R.id.snow_notif_play_pause, R.drawable.snow_ic_pause);
            }

            if (playerType == TYPE_RADIO_STATION) {
                bigContentView.setViewVisibility(R.id.snow_notif_radio_tip, View.VISIBLE);
                bigContentView.setImageViewResource(R.id.snow_notif_skip_to_previous, R.drawable.snow_ic_skip_previous_disabled);
            } else {
                bigContentView.setOnClickPendingIntent(R.id.snow_notif_skip_to_previous, getSkipToPreviousPendingIntent());
            }

            initCustomAction(CUSTOM_ACTION_1, mCustomAction1, bigContentView, playerType, R.id.snow_notif_custom_action1);
            initCustomAction(CUSTOM_ACTION_2, mCustomAction2, bigContentView, playerType, R.id.snow_notif_custom_action2);

            return bigContentView;
        }

        private void initCustomAction(String customActionKey,
                                      CustomAction customAction,
                                      RemoteViews bigContentView,
                                      int playerType,
                                      int viewId) {
            if (customAction == null) {
                return;
            }

            int iconId = customAction.getIconId();
            PendingIntent pendingIntent =
                    getCustomActionPendingIntent(playerType, customActionKey, customAction);

            bigContentView.setViewVisibility(viewId, View.VISIBLE);
            bigContentView.setImageViewResource(viewId, iconId);

            if (pendingIntent != null) {
                bigContentView.setOnClickPendingIntent(viewId, pendingIntent);
            }
        }

        private PendingIntent getCustomActionPendingIntent(int playerType,
                                                           String customActionName,
                                                           CustomAction customAction) {
            if (customAction.getAction() == null) {
                return null;
            }

            if (playerType == TYPE_RADIO_STATION && customAction.isDisabledInRadioStationType()) {
                return null;
            }

            return addCustomAction(customActionName, customAction.getAction());
        }

        /**
         * 用于创建一个可添加到 {@link SimpleNotificationView} 中的自定义动作。
         */
        public static class CustomAction {
            private int mIconId;
            private boolean mDisabledInRadioStationType;
            private int mDisabledIconId;
            private Runnable mAction;

            /**
             * 创建一个自定义动作，该自定义动作没有要执行的操作。
             *
             * @param iconId 自定义动作的图标。
             */
            public CustomAction(@DrawableRes int iconId) {
                this(iconId, null);
            }

            /**
             * 创建一个自定义动作。
             *
             * @param iconId 自定义动作的图标的资源 ID。
             * @param action 自定义动作要执行的操作。
             */
            public CustomAction(@DrawableRes int iconId, Runnable action) {
                mIconId = iconId;
                mAction = action;
            }

            /**
             * 设置自定义动作的图标的资源 ID。
             * <p>
             * 对该方法的调用会在下次更新通知栏控制器时生效。
             *
             * @param iconId 自定义动作的新图标的资源 ID
             */
            public void setIconId(int iconId) {
                mIconId = iconId;
            }

            public int getIconId() {
                return mIconId;
            }

            /**
             * 设置在 “电台” 模式下 disable 当前自定义动作。
             *
             * @param disabledDrawableId 当前自定义动作在 disable 状态下的图标的资源 ID
             */
            public void setDisabledInRadioStationType(int disabledDrawableId) {
                mDisabledInRadioStationType = true;
                mDisabledIconId = disabledDrawableId;
            }

            /**
             * 判断是否在 “电台” 模式下 disable 当前自定义动作。
             *
             * @return 默认返回 false
             */
            public boolean isDisabledInRadioStationType() {
                return mDisabledInRadioStationType;
            }

            /**
             * 获取当前自定义动作在 disable 状态下的图标的资源 ID。
             */
            public int getDisabledIconId() {
                return mDisabledIconId;
            }

            /**
             * 设置当前自定义动作要执行的操作。
             * <p>
             * 对该方法的调用会在下次更新通知栏控制器时生效。
             */
            public void setAction(Runnable action) {
                mAction = action;
            }

            /**
             * 获取当前自定义动作要执行的操作。
             */
            public Runnable getAction() {
                return mAction;
            }
        }
    }
}
