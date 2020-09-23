package snow.player;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.SystemClock;
import android.support.v4.media.MediaBrowserCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.core.content.res.ResourcesCompat;
import androidx.media.MediaBrowserServiceCompat;
import androidx.media.session.MediaButtonReceiver;

import com.bumptech.glide.Glide;
import com.bumptech.glide.RequestBuilder;
import com.bumptech.glide.load.resource.bitmap.GranularRoundedCorners;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;
import com.google.common.base.Preconditions;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import channel.helper.ChannelHelper;
import channel.helper.Dispatcher;
import channel.helper.DispatcherUtil;
import channel.helper.pipe.CustomActionPipe;

import channel.helper.pipe.SessionEventPipe;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Consumer;
import media.helper.HeadsetHookHelper;

import snow.player.appwidget.AppWidgetPreferences;
import snow.player.effect.AudioEffectManager;
import snow.player.audio.MediaMusicPlayer;
import snow.player.audio.MusicItem;
import snow.player.audio.MusicPlayer;
import snow.player.playlist.Playlist;
import snow.player.playlist.PlaylistEditor;
import snow.player.audio.ErrorCode;
import snow.player.playlist.PlaylistManager;
import snow.player.util.MusicItemUtil;

/**
 * 提供了基本的 {@code player service} 实现，用于在后台播放音乐。
 * <p>
 * 可以使用 {@link PlayerClient} 类建立与 {@link PlayerService} 连接，并对播放器进行控制。
 * <p>
 * <b>MediaSession 框架：</b><br>
 * {@link PlayerService} 继承了 {@link MediaBrowserServiceCompat} 类，因此也可以使用
 * {@link MediaBrowserCompat} 类来建立与 {@link PlayerService} 连接，并对播放器进行控制。不过不推荐这么做，
 * 因为本项目大部的功能都依赖于 {@link PlayerClient} 类，如果不使用 {@link PlayerClient} 类，那么也无法使
 * 用这些功能。
 */
@SuppressWarnings("SameReturnValue")
public class PlayerService extends MediaBrowserServiceCompat
        implements PlayerManager, PlaylistManager, PlaylistEditor, SleepTimer {
    /**
     * 默认的 root id，值为 `"root"`。
     */
    public static final String DEFAULT_MEDIA_ROOT_ID = "root";

    /**
     * 如果你直接使用 {@link MediaBrowserCompat} 连接 PlayerService, 你的客户端可以发送该
     * {@code custom action} 来关闭 PlayerService。PlayerService 在接收到该 {@code custom action} 后
     * 会发出一个名为 {@link #SESSION_EVENT_ON_SHUTDOWN} 的 {@code session event}，客户端在接收到该
     * {@code session event} 后应该主动断开与 PlayerService 的连接。当所有客户端断开与 PlayerService 的
     * 连接后，PlayerService 会自动终止。
     */
    public static final String CUSTOM_ACTION_SHUTDOWN = "snow.player.custom_action.shutdown";

    /**
     * 如果你直接使用 {@link MediaBrowserCompat} 连接 PlayerService, 那么你的客户端应该在接收到该
     * {@code session event} 时主动断开与 PlayerService 的连接。
     */
    public static final String SESSION_EVENT_ON_SHUTDOWN = "snow.player.session_event.on_shutdown";

    private String mPersistentId;

    private PlayerConfig mPlayerConfig;
    private PlayerState mPlayerState;

    private PlaylistManagerImp mPlaylistManager;
    private PlayerImp mPlayer;
    private CustomActionPipe mCustomActionDispatcher;

    private PlayerManager.OnCommandCallback mCommandCallback;

    private boolean mForeground;

    private NotificationManager mNotificationManager;

    private Map<String, CustomAction> mAllCustomAction;

    private MediaSessionCompat mMediaSession;

    private HeadsetHookHelper mHeadsetHookHelper;

    @Nullable
    private NotificationView mNotificationView;

    @Nullable
    private AudioEffectManager mAudioEffectManager;

    @Nullable
    private HistoryRecorder mHistoryRecorder;

    private OnStateChangeListener mSleepTimerStateChangedListener;
    private Disposable mSleepTimerDisposable;
    private PlayerStateHelper mPlayerStateHelper;

    private int mMaxIDLEMinutes = -1;
    private Disposable mIDLETimerDisposable;

    @Override
    public void onCreate() {
        super.onCreate();
        startService(new Intent(this, this.getClass()));

        mPersistentId = getPersistentId();
        mAllCustomAction = new HashMap<>();

        initNotificationManager();
        initPlayerConfig();
        initAudioEffectManager();
        initPlayerState();
        initPlaylistManager();
        initNotificationView();
        initPlayer();
        initCustomActionDispatcher();
        initHeadsetHookHelper();
        initMediaSession();
        initSessionEventEmitter();
        initHistoryRecorder();

        if (mNotificationView != null && mNotificationView.isNotifyOnCreate()) {
            updateNotificationView();
        }

        startIDLETimer();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null) {
            MediaButtonReceiver.handleIntent(mMediaSession, intent);
            handleCustomAction(intent.getAction(), intent.getExtras());
        }

        return super.onStartCommand(intent, flags, startId);
    }

    private void handleCustomAction(String action, Bundle extras) {
        CustomAction customAction = mAllCustomAction.get(action);
        if (customAction != null) {
            customAction.doAction(getPlayer(), extras);
        }
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

        if (!noNotificationView()) {
            stopForegroundEx(true);
            mNotificationView.release();
            mNotificationManager.cancel(mNotificationView.getNotificationId());
        }

        cancelIDLETimer();

        mMediaSession.release();
        mPlayer.release();

        mPlayer = null;

        if (mAudioEffectManager != null) {
            mAudioEffectManager.release();
        }
    }

    private void initNotificationManager() {
        mNotificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    NotificationView.CHANNEL_ID,
                    getString(R.string.snow_notification_channel_name),
                    NotificationManager.IMPORTANCE_LOW);
            channel.setShowBadge(false);
            mNotificationManager.createNotificationChannel(channel);
        }
    }

    private void initPlayerConfig() {
        mPlayerConfig = new PlayerConfig(this, mPersistentId);
    }

    private void initPlayerState() {
        mPlayerState = new PersistentPlayerState(this, mPersistentId);
        mPlayerStateHelper = new PlayerStateHelper(mPlayerState);
    }

    private void initPlaylistManager() {
        mPlaylistManager = new PlaylistManagerImp(this, mPersistentId);
    }

    private void initPlayer() {
        boolean prepare = mNotificationView != null && mNotificationView.isNotifyOnCreate();

        mPlayer = new PlayerImp(this,
                mPlayerConfig,
                mPlayerState,
                mPlaylistManager,
                new AppWidgetPreferences(this, this.getClass()),
                prepare);
    }

    private void initCustomActionDispatcher() {
        final Dispatcher playerManagerDispatcher =
                ChannelHelper.newDispatcher(PlayerManager.class, this);

        final Dispatcher playerDispatcher =
                ChannelHelper.newDispatcher(Player.class, mPlayer);

        final Dispatcher playlistEditorDispatcher =
                ChannelHelper.newDispatcher(PlaylistEditor.class, mPlayer);

        final Dispatcher sleepTimerDispatcher =
                ChannelHelper.newDispatcher(SleepTimer.class, this);

        mCustomActionDispatcher = new CustomActionPipe(
                DispatcherUtil.merge(
                        playerManagerDispatcher,
                        playerDispatcher,
                        playlistEditorDispatcher,
                        sleepTimerDispatcher
                ));
    }

    private void initNotificationView() {
        NotificationView notificationView = onCreateNotificationView();

        if (notificationView == null) {
            return;
        }

        notificationView.init(this);
        MusicItem musicItem = getPlayingMusicItem();

        if (musicItem != null) {
            notificationView.setPlayingMusicItem(musicItem);
        }

        mNotificationView = notificationView;
    }

    private void initHeadsetHookHelper() {
        mHeadsetHookHelper = new HeadsetHookHelper(new HeadsetHookHelper.OnHeadsetHookClickListener() {
            @Override
            public void onHeadsetHookClicked(int clickCount) {
                switch (clickCount) {
                    case 1:
                        getPlayer().playPause();
                        break;
                    case 2:
                        getPlayer().skipToNext();
                        break;
                    case 3:
                        getPlayer().skipToPrevious();
                        break;
                }
            }
        });
    }

    private void initMediaSession() {
        mMediaSession = new MediaSessionCompat(this, this.getClass().getName());
        mPlayer.setMediaSession(mMediaSession);

        mMediaSession.setCallback(onCreateMediaSessionCallback());

        setSessionToken(mMediaSession.getSessionToken());
    }

    private void initSessionEventEmitter() {
        SessionEventPipe sessionEventEmitter = new SessionEventPipe(mMediaSession);
        mCommandCallback = ChannelHelper.newEmitter(PlayerManager.OnCommandCallback.class, sessionEventEmitter);
        mPlayer.setPlayerStateListener(ChannelHelper.newEmitter(PlayerStateListener.class, sessionEventEmitter));
        mSleepTimerStateChangedListener = ChannelHelper.newEmitter(OnStateChangeListener.class, sessionEventEmitter);
    }

    private void initAudioEffectManager() {
        mAudioEffectManager = onCreateAudioEffectManager();

        if (mAudioEffectManager == null) {
            return;
        }

        Bundle config = mPlayerConfig.getAudioEffectConfig();
        mAudioEffectManager.init(config);
    }

    private void initHistoryRecorder() {
        mHistoryRecorder = onCreateHistoryRecorder();
    }

    /**
     * 设置 MediaSessionCompat 的 Flags。
     * <p>
     * 相当于调用 MediaSessionCompat 的 setFlags(int) 方法。
     */
    protected void setMediaSessionFlags(int flags) {
        mMediaSession.setFlags(flags);
    }

    /**
     * 创建一个 {@link MediaSessionCallback} 对象。
     * <p>
     * 如果你需要对 MediaSession 框架的 MediaSessionCompat.Callback 进行定制，则可以覆盖该方法并返回一个
     * {@link MediaSessionCallback} 对象。{@link MediaSessionCallback} 类继承了
     * MediaSessionCompat.Callback 类。
     *
     * @see MediaSessionCallback
     */
    @NonNull
    protected MediaSessionCallback onCreateMediaSessionCallback() {
        return new MediaSessionCallback(this);
    }

    /***
     * 创建一个通知栏控制器，你可以通过覆盖该方法来提供自定义的通知栏控制器。
     *
     * 该方法默认返回 {@link MediaNotificationView}，如果你不需要在通知栏中显示控制器，可以覆盖该方法并返回 null。
     *
     * @return {@link NotificationView} 对象，返回 null 时将隐藏通知栏控制器
     */
    @Nullable
    protected NotificationView onCreateNotificationView() {
        return new MediaNotificationView();
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
    public void setSoundQuality(SoundQuality soundQuality) {
        if (soundQuality == mPlayerConfig.getSoundQuality()) {
            return;
        }

        mPlayerConfig.setSoundQuality(soundQuality);
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
        if (mPlayer.getPlaybackState() == PlaybackState.PLAYING) {
            getPlayer().pause();
        }

        notifyOnShutdown();
        stopSelf();
    }

    @Override
    public void syncPlayerState(final String clientToken) {
        mCommandCallback.onSyncPlayerState(clientToken, new PlayerState(mPlayerState));
    }

    /**
     * 设置 {@link PlayerService} 处于空闲状态（暂停或者停止后）的最大存活时间。
     * <p>
     * 当播放器处于空闲状态（暂停或者停止）的时间超出 minutes 分钟后将自动终止 {@link PlayerService}。
     * 将 minutes 设置为小于等于 0 时将关闭此功能（即使播放器处于空闲状态，也不会自动终止 {@link PlayerService}）。
     * <p>
     * 默认未启用该功能。
     *
     * @param minutes 最大的空闲时间，设置为小于等于 0 时将关闭此功能（即使播放器处于空闲状态，也不会自动终止 {@link PlayerService}）。
     */
    public final void setMaxIDLETime(int minutes) {
        mMaxIDLEMinutes = minutes;
        if (minutes <= 0) {
            cancelIDLETimer();
        }
    }

    private void startIDLETimer() {
        cancelIDLETimer();
        if (mMaxIDLEMinutes <= 0) {
            return;
        }

        mIDLETimerDisposable = Observable.timer(mMaxIDLEMinutes, TimeUnit.MINUTES)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Consumer<Long>() {
                    @Override
                    public void accept(Long aLong) {
                        shutdown();
                    }
                });
    }

    private void cancelIDLETimer() {
        if (mIDLETimerDisposable != null && !mIDLETimerDisposable.isDisposed()) {
            mIDLETimerDisposable.dispose();
        }
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
     * 自定义动作可通过构造一个具有指定 action 的 Intent 对象并调用 startService 方法触发。
     * <p>
     * 例：
     * <pre>
     * Intent intent = new Intent(context, PlayerService.class);
     * intent.setAction(action);
     * ...
     * context.startService(intent);
     * </pre>
     *
     * @param action       自定义动作的名称
     * @param customAction 自定义动作要执行的任务，不能为 null
     */
    protected final void addCustomAction(@NonNull String action, @NonNull CustomAction customAction) {
        mAllCustomAction.put(action, customAction);
    }

    /**
     * 用于创建触发当前 PlayerService 中的自定义动作的 PendingIntent 对象。
     *
     * @param action 自定义动作的名称
     * @return 可触发自定义动作的 PendingIntent 对象
     */
    protected final PendingIntent getCustomActionPendingIntent(@NonNull String action) {
        return getCustomActionPendingIntent(action, this, this.getClass());
    }

    /**
     * 工具方法，用于创建触发自定义动作的 PendingIntent 对象。
     *
     * @param action  自定义动作的名称
     * @param context Context 对象
     * @param service 自定义动作关联到的 PlayerService 的 Class 对象
     * @return 可触发自定义动作的 PendingIntent 对象
     */
    public static PendingIntent getCustomActionPendingIntent(@NonNull String action,
                                                             Context context,
                                                             Class<? extends PlayerService> service) {
        Intent intent = new Intent(context, service);
        intent.setAction(action);

        return PendingIntent.getService(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
    }

    /**
     * 移除一个自定义动作。
     *
     * @param action 自定义动作的名称
     */
    protected final void removeCustomAction(@NonNull String action) {
        mAllCustomAction.remove(action);
    }

    private void notifyOnShutdown() {
        cancel();
        mCommandCallback.onShutdown();
        mMediaSession.sendSessionEvent(SESSION_EVENT_ON_SHUTDOWN, null);
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
     * 获取播放队列的播放模式。
     *
     * @return 播放队列的播放模式。
     * @see PlayMode
     */
    public final PlayMode getPlayMode() {
        return mPlayerState.getPlayMode();
    }

    /**
     * 获取播放队列携带的额外参数（可为 null）。
     */
    @Nullable
    public final Bundle getPlaylistExtra() {
        return mPlayer.getPlaylistExtra();
    }

    /**
     * 获取播放器当前的播放状态。
     *
     * @return 播放器当前的播放状态。
     */
    @NonNull
    public final PlaybackState getPlaybackState() {
        return mPlayer.getPlaybackState();
    }

    /**
     * 当前播放器是否处于 {@code stalled} 状态。
     *
     * @return 当缓冲区没有足够的数据支持播放器继续播放时，该方法会返回 {@code true}，否则返回 false
     */
    public final boolean isStalled() {
        return mPlayer.isStalled();
    }

    /**
     * 获取当前正在播放的音乐的 MusicItem 对象。
     */
    public final MusicItem getPlayingMusicItem() {
        return mPlayerState.getMusicItem();
    }

    /**
     * 播放器是否发生了错误。
     */
    public final boolean isError() {
        return getErrorCode() != ErrorCode.NO_ERROR;
    }

    /**
     * 获取错误码。
     * <p>
     * 该方法的返回值仅在发生错误（{@link #isError()} 方法返回 true）时才有意义。
     */
    public final int getErrorCode() {
        return mPlayerState.getErrorCode();
    }

    /**
     * 获取错误信息。
     * <p>
     * 该方法的返回值仅在发生错误（{@link #isError()} 方法返回 true）时才有意义。
     */
    public final String getErrorMessage() {
        return ErrorCode.getErrorMessage(this, getErrorCode());
    }

    /**
     * 要求 Service 更新 NotificationView，如果没有设置 NotificationView，则忽略本次操作。
     */
    public final void updateNotificationView() {
        if (noNotificationView()) {
            return;
        }

        MusicItem musicItem = getPlayingMusicItem();
        if (musicItem == null) {
            stopForegroundEx(true);
            return;
        }

        mNotificationView.setPlayingMusicItem(musicItem);

        if (shouldBeForeground() && !isForeground()) {
            startForeground();
            return;
        }

        if (!shouldBeForeground() && isForeground()) {
            stopForegroundEx(false);
        }

        updateNotification();
    }

    private boolean noNotificationView() {
        return mNotificationView == null;
    }

    private boolean shouldBeForeground() {
        return mPlayer.getPlaybackState() == PlaybackState.PLAYING;
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
        startForeground(mNotificationView.getNotificationId(),
                mNotificationView.createNotification());
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

    /**
     * 设置一个新的播放列表。
     *
     * @param playlist 播放列表（不能为 null）
     */
    public final void setPlaylist(@NonNull Playlist playlist) {
        setPlaylist(playlist, 0, false);
    }

    /**
     * 设置一个新的播放列表。
     *
     * @param playlist 播放列表（不能为 null）
     * @param play     是否立即播放列表中的第一首音乐
     */
    public final void setPlaylist(@NonNull Playlist playlist, boolean play) {
        setPlaylist(playlist, 0, play);
    }

    /**
     * 往列表中插入了一首新的歌曲。
     * <p>
     * 如果播放列表中已包含指定歌曲，则会将它移动到 position 位置，如果不存在，则会将歌曲插入到 position 位置。
     *
     * @param position  歌曲插入的位置
     * @param musicItem 要插入的歌曲，不能为 null
     * @throws IllegalArgumentException 如果 position 的值小于 0，则抛出该异常
     */
    @Override
    public void insertMusicItem(int position, @NonNull MusicItem musicItem) throws IllegalArgumentException {
        if (position < 0) {
            throw new IllegalArgumentException("position must >= 0.");
        }
        Preconditions.checkNotNull(musicItem);
        mPlayer.insertMusicItem(position, musicItem);
    }

    @Override
    public void appendMusicItem(@NonNull MusicItem musicItem) {
        Preconditions.checkNotNull(musicItem);
        mPlayer.appendMusicItem(musicItem);
    }

    /**
     * 移动播放列表中某首歌曲的位置。
     *
     * @param fromPosition 歌曲在列表中的位置
     * @param toPosition   歌曲要移动到的位置。如果 {@code toPosition == fromPosition}，则会忽略本次调用
     * @throws IllegalArgumentException 如果 fromPosition 或者 toPosition 参数小于 0，则抛出该异常
     */
    @Override
    public void moveMusicItem(int fromPosition, int toPosition) throws IllegalArgumentException {
        if (fromPosition < 0) {
            throw new IllegalArgumentException("fromPosition must >= 0.");
        }

        if (toPosition < 0) {
            throw new IllegalArgumentException("toPosition must >= 0.");
        }

        mPlayer.moveMusicItem(fromPosition, toPosition);
    }

    @Override
    public void removeMusicItem(@NonNull MusicItem musicItem) {
        Preconditions.checkNotNull(musicItem);
        mPlayer.removeMusicItem(musicItem);
    }

    @Override
    public void setNextPlay(@NonNull MusicItem musicItem) {
        Preconditions.checkNotNull(musicItem);
        mPlayer.setNextPlay(musicItem);
    }

    /**
     * 设置一个新的播放列表。
     *
     * @param playlist 播放列表（不能为 null）
     * @param position 播放列表中要播放的歌曲的位置
     * @param play     是否立即播放 {@code position} 参数指定处的音乐
     * @throws IllegalArgumentException 如果 position 参数小于 0，则会抛出该异常
     */
    @Override
    public final void setPlaylist(@NonNull Playlist playlist, final int position, final boolean play)
            throws IllegalArgumentException {
        Preconditions.checkNotNull(playlist);
        if (position < 0) {
            throw new IllegalArgumentException("position must >= 0.");
        }

        mPlayer.setPlaylist(playlist, position, play);
    }

    private void updateNotification() {
        if (noNotificationView()) {
            return;
        }

        if (getPlayingMusicItem() == null) {
            stopForegroundEx(true);
            return;
        }

        mNotificationManager.notify(mNotificationView.getNotificationId(),
                mNotificationView.createNotification());
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
    protected boolean isCached(MusicItem musicItem, SoundQuality soundQuality) {
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
    protected MusicPlayer onCreateMusicPlayer(@NonNull Context context, @NonNull MusicItem musicItem, @NonNull Uri uri) {
        return new MediaMusicPlayer(uri);
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
    protected Uri onRetrieveMusicItemUri(@NonNull MusicItem musicItem, @NonNull SoundQuality soundQuality) throws Exception {
        return Uri.parse(musicItem.getUri());
    }

    /**
     * 获取播放器的 Player 对象。可用于对播放器进行控制。
     */
    @NonNull
    public final Player getPlayer() {
        return mPlayer;
    }

    private void onStopped() {
        if (noNotificationView()) {
            return;
        }

        if (mNotificationView.isKeepOnStopped()) {
            updateNotificationView();
            return;
        }

        stopForegroundEx(true);
    }

    private void playingMusicItemChanged(@Nullable MusicItem musicItem) {
        if (mHistoryRecorder != null && musicItem != null) {
            mHistoryRecorder.recordHistory(musicItem);
        }
        onPlayingMusicItemChanged(musicItem);
    }

    /**
     * 正在播放的音乐发生了改变（例如，切换播放的歌曲）。
     *
     * @param musicItem 当前正在播放的歌曲
     */
    protected void onPlayingMusicItemChanged(@Nullable MusicItem musicItem) {
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
        if (CUSTOM_ACTION_SHUTDOWN.equals(action)) {
            shutdown();
            return;
        }

        mCustomActionDispatcher.dispatch(action, extras);
    }

    @Override
    public int getPlaylistSize() {
        return mPlayer.getPlaylistSize();
    }

    @Override
    public void getPlaylist(@NonNull Callback callback) {
        Preconditions.checkNotNull(callback);
        mPlaylistManager.getPlaylist(callback);
    }

    /**
     * 启动睡眠定时器。
     *
     * @param time   睡眠时间（单位：毫秒）。播放器会在经过 time 时间后暂停播放。
     * @param action 定时器的的时间到时要执行的操作。
     * @throws IllegalArgumentException 如果 time 小于 0，则抛出该异常。
     */
    @Override
    public void start(long time, @NonNull final TimeoutAction action) throws IllegalArgumentException {
        if (time < 0) {
            throw new IllegalArgumentException("time must >= 0");
        }
        Preconditions.checkNotNull(action);

        disposeLastSleepTimer();

        if (getPlayingMusicItem() == null) {
            return;
        }

        if (time == 0) {
            getPlayer().pause();
            return;
        }

        mSleepTimerDisposable = Observable.timer(time, TimeUnit.MILLISECONDS)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Consumer<Long>() {
                    @Override
                    public void accept(Long aLong) {
                        switch (action) {
                            case PAUSE:
                                PlayerService.this.getPlayer().pause();
                                break;
                            case STOP:
                                PlayerService.this.getPlayer().stop();
                                break;
                            case SHUTDOWN:
                                PlayerService.this.shutdown();
                                break;
                        }
                        notifySleepTimerEnd();
                    }
                });

        long startTime = SystemClock.elapsedRealtime();
        mPlayerStateHelper.onSleepTimerStart(time, startTime, action);
        mSleepTimerStateChangedListener.onStart(time, startTime, action);
    }

    /**
     * 取消睡眠定时器。
     */
    @Override
    public void cancel() {
        disposeLastSleepTimer();
        notifySleepTimerEnd();
    }

    private void disposeLastSleepTimer() {
        if (mSleepTimerDisposable == null || mSleepTimerDisposable.isDisposed()) {
            return;
        }

        mSleepTimerDisposable.dispose();
    }

    private void notifySleepTimerEnd() {
        mPlayerStateHelper.onSleepTimerEnd();
        mSleepTimerStateChangedListener.onEnd();
    }

    private class PlayerImp extends AbstractPlayer {

        public PlayerImp(@NonNull Context context,
                         @NonNull PlayerConfig playerConfig,
                         @NonNull PlayerState playlistState,
                         @NonNull PlaylistManagerImp playlistManager,
                         @NonNull AppWidgetPreferences pref,
                         boolean prepare) {
            super(context, playerConfig, playlistState, playlistManager, pref, prepare);
        }

        @Override
        protected boolean isCached(MusicItem musicItem, SoundQuality soundQuality) {
            return PlayerService.this.isCached(musicItem, soundQuality);
        }

        @NonNull
        @Override
        protected MusicPlayer onCreateMusicPlayer(@NonNull Context context, @NonNull MusicItem musicItem, @NonNull Uri uri) {
            return PlayerService.this.onCreateMusicPlayer(context, musicItem, uri);
        }

        @Nullable
        @Override
        protected Uri retrieveMusicItemUri(@NonNull MusicItem musicItem, @NonNull SoundQuality soundQuality) throws Exception {
            return PlayerService.this.onRetrieveMusicItemUri(musicItem, soundQuality);
        }

        @Override
        protected void onPreparing() {
            super.onPreparing();
            PlayerService.this.updateNotificationView();
            PlayerService.this.cancelIDLETimer();
        }

        @Override
        protected void onPrepared(int audioSessionId) {
            super.onPrepared(audioSessionId);
            PlayerService.this.updateNotificationView();
        }

        @Override
        protected void onPlaying(int progress, long updateTime) {
            super.onPlaying(progress, updateTime);
            PlayerService.this.updateNotificationView();
            PlayerService.this.cancelIDLETimer();
        }

        @Override
        protected void onPaused() {
            super.onPaused();
            PlayerService.this.updateNotificationView();
            PlayerService.this.startIDLETimer();
        }

        @Override
        protected void onStalledChanged(boolean stalled) {
            super.onStalledChanged(stalled);
            PlayerService.this.updateNotificationView();
        }

        @Override
        protected void onStopped() {
            super.onStopped();
            PlayerService.this.onStopped();
            PlayerService.this.startIDLETimer();
        }

        @Override
        protected void onError(int errorCode, String errorMessage) {
            super.onError(errorCode, errorMessage);
            PlayerService.this.updateNotificationView();
        }

        @Override
        protected void onPlayingMusicItemChanged(@Nullable MusicItem musicItem) {
            super.onPlayingMusicItemChanged(musicItem);
            PlayerService.this.updateNotificationView();
            PlayerService.this.playingMusicItemChanged(musicItem);
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

    /**
     * 该类继承了 MediaSessionCompat.Callback 类，如果你需要对 MediaSession 框架的
     * 的 MediaSessionCompat.Callback 进行定制，则可以覆盖 {@link #onCreateMediaSessionCallback()} 方法，
     * 并返回一个自定义的 {@link MediaSessionCallback} 实现。
     * <p>
     * 注意！在覆盖 {@link MediaSessionCallback} 的方法时，使用 {@code super.xxx} 回调超类被覆盖的方法，
     * 因为 {@link PlayerService} 的部分功能依赖这些方法。如果没有使用 {@code super.xxx}
     * 回调超类被覆盖的方法，则这部分功能将无法正常工作。
     *
     * @see #onCreateMediaSessionCallback()
     */
    public static class MediaSessionCallback extends MediaSessionCompat.Callback {
        private PlayerService mPlayerService;
        private Player mPlayer;

        public MediaSessionCallback(@NonNull PlayerService playerService) {
            Preconditions.checkNotNull(playerService);
            mPlayerService = playerService;
            mPlayer = mPlayerService.getPlayer();
        }

        /**
         * 获取当前 {@link MediaSessionCallback} 关联到的 {@link PlayerService} 对象。
         */
        @NonNull
        public PlayerService getPlayerService() {
            return mPlayerService;
        }

        /**
         * 获取播放器的 MediaSessionCompat 对象。
         */
        public MediaSessionCompat getMediaSession() {
            return mPlayerService.getMediaSession();
        }

        /**
         * 获取播放器的 {@link Player} 对象。用于对播放器进行控制。
         */
        public Player getPlayer() {
            return mPlayer;
        }

        @Override
        public boolean onMediaButtonEvent(Intent mediaButtonEvent) {
            if (mPlayerService.onMediaButtonEvent(mediaButtonEvent)) {
                return true;
            }

            return super.onMediaButtonEvent(mediaButtonEvent);
        }

        @Override
        public void onCustomAction(String action, Bundle extras) {
            mPlayerService.onCustomAction(action, extras);
        }

        @Override
        public void onPlay() {
            mPlayer.play();
        }

        @Override
        public void onSkipToQueueItem(long id) {
            mPlayer.skipToPosition((int) id);
        }

        @Override
        public void onPause() {
            mPlayer.pause();
        }

        @Override
        public void onSkipToNext() {
            mPlayer.skipToNext();
        }

        @Override
        public void onSkipToPrevious() {
            mPlayer.skipToPrevious();
        }

        @Override
        public void onFastForward() {
            mPlayer.fastForward();
        }

        @Override
        public void onRewind() {
            mPlayer.rewind();
        }

        @Override
        public void onStop() {
            mPlayer.stop();
        }

        @Override
        public void onSeekTo(long pos) {
            mPlayer.seekTo((int) pos);
        }

        @Override
        public void onSetRepeatMode(int repeatMode) {
            if (repeatMode == PlaybackStateCompat.REPEAT_MODE_ONE) {
                mPlayer.setPlayMode(PlayMode.LOOP);
                return;
            }

            mPlayer.setPlayMode(PlayMode.SEQUENTIAL);
        }

        @Override
        public void onSetShuffleMode(int shuffleMode) {
            if (shuffleMode == PlaybackStateCompat.SHUFFLE_MODE_NONE ||
                    shuffleMode == PlaybackStateCompat.SHUFFLE_MODE_INVALID) {
                mPlayer.setPlayMode(PlayMode.SEQUENTIAL);
                return;
            }

            mPlayer.setPlayMode(PlayMode.SHUFFLE);
        }
    }

    /**
     * 通知栏控制器的基类。
     */
    public static abstract class NotificationView {
        /**
         * 通知的 channelId 值，值为：{@code "player"}
         */
        public static final String CHANNEL_ID = "player";

        private PlayerService mPlayerService;
        private MusicItem mMusicItem;

        private int[] mIconSize;            // [width, height]
        private int[] mIconCornerRadius;    // [topLeft, topRight, bottomRight, bottomLeft]
        private boolean mNeedReloadIcon;
        private Bitmap mIcon;
        private Bitmap mDefaultIcon;
        private CustomTarget<Bitmap> mTarget;

        private boolean mNotifyOnCreate;
        private boolean mKeepOnStopped;
        private boolean mReleased;

        void init(PlayerService playerService) {
            mPlayerService = playerService;
            mMusicItem = new MusicItem();
            mIconSize = new int[2];
            mIconCornerRadius = new int[4];

            mDefaultIcon = getDefaultIcon();
            mIcon = mDefaultIcon;

            setIconSize(playerService.getResources().getDimensionPixelSize(R.dimen.snow_notif_icon_size_big));
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

        private void release() {
            mReleased = true;
            Glide.with(getContext())
                    .clear(mTarget);
            mTarget = null;
            mIcon = null;
            mDefaultIcon = null;
            onRelease();
        }

        /**
         * 该方法会在初次创建 NotificationView 对象时调用，你可以重写该方法来进行一些初始化操作。
         */
        protected void onInit(Context context) {
        }

        /**
         * 该方法会在 Service 销毁时调用，可以在该方法中释放占用的资源。
         */
        @SuppressWarnings("EmptyMethod")
        protected void onRelease() {
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

        /**
         * 获取用于触发自定义动作的 PendingIntent 对象。
         *
         * @param action 自定义动作的名称
         * @return 可触发自定义动作的 PendingIntent 对象
         */
        public final PendingIntent getCustomActionPendingIntent(@NonNull String action) {
            return mPlayerService.getCustomActionPendingIntent(action);
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
                    .load(mDefaultIcon)
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
         * 创建一个新的 Notification 对象，不能为 null。
         *
         * @return Notification 对象，不能为 null。
         */
        @NonNull
        public abstract Notification onCreateNotification();

        /**
         * 返回 Notification 的 ID。
         */
        public abstract int getNotificationId();

        /**
         * 获取默认图标。
         *
         * @return 通知的默认图标，不能为 null
         */
        @NonNull
        public Bitmap getDefaultIcon() {
            Context context = getContext();
            BitmapDrawable drawable = (BitmapDrawable) ResourcesCompat.getDrawable(
                    context.getResources(),
                    R.mipmap.snow_notif_default_icon,
                    context.getTheme());

            if (drawable == null) {
                throw new NullPointerException();
            }

            return drawable.getBitmap();
        }

        /**
         * 关闭播放器。
         */
        public final void shutdown() {
            mPlayerService.shutdown();
        }

        /**
         * 获取当前通知栏控制器的图标。
         */
        public final Bitmap getIcon() {
            return mIcon;
        }

        /**
         * 设置当前通知栏控制器的图标。
         * <p>
         * 调用该方法后会自动更新通知栏控制器，以应用最新设置的图标。
         */
        public final void setIcon(@NonNull Bitmap icon) {
            mIcon = icon;
            invalidate();
        }

        /**
         * 设置图标的尺寸（默认尺寸为 0）。
         * <p>
         * 建议子类覆盖 {@link #onInit(Context)} 方法，并在该方法中完成图标尺寸的设置。
         */
        public final void setIconSize(int size) {
            setIconSize(size, size);
        }

        /**
         * 分别设置图标的宽高尺寸（默认宽高为 0）。
         * <p>
         * 建议子类覆盖 {@link #onInit(Context)} 方法，并在该方法中完成图标宽高尺寸的设置。
         */
        public final void setIconSize(int width, int height) {
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
        public final void setIconCornerRadius(int radius) {
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
        public final void setIconCornerRadius(int topLeft, int topRight, int bottomRight, int bottomLeft) {
            mIconCornerRadius[0] = topLeft;
            mIconCornerRadius[1] = topRight;
            mIconCornerRadius[2] = bottomRight;
            mIconCornerRadius[3] = bottomLeft;
        }

        /**
         * 设置是否在 {@link PlayerService} 创建后立即显示通知栏控制器（默认为 false）。
         */
        public void setNotifyOnCreate(boolean notifyOnCreate) {
            mNotifyOnCreate = notifyOnCreate;
        }

        /**
         * 判断是否在 {@link PlayerService} 创建后立即显示通知栏控制器（默认为 false）。
         */
        public boolean isNotifyOnCreate() {
            return mNotifyOnCreate;
        }

        /**
         * 设置是否在停止播放后依然保留通知栏控制器（默认为 false）。
         */
        public void setKeepOnStopped(boolean keepOnStopped) {
            mKeepOnStopped = keepOnStopped;
        }

        /**
         * 判断是否在停止播放后依然保留通知栏控制器（默认为 false）。
         */
        public boolean isKeepOnStopped() {
            return mKeepOnStopped;
        }

        /**
         * 获取通知栏控制器的 content title
         */
        public final CharSequence getContentTitle() {
            return MusicItemUtil.getTitle(getContext(), getPlayingMusicItem());
        }

        /**
         * 这是一个帮助方法，用于获取通知栏控制器用于显示的 content text，该方法会根据播放器状态的不同而返回
         * 不同的 CharSequence 值。
         * <p>
         * 例如，在 {@link PlaybackState#ERROR} 状态时，会返回一个
         * {@code android.R.color.holo_red_dark} 颜色的描述错误信息的 CharSequence 对象；而在
         * {@code preparing} 状态时，会返回一个
         * {@code android.R.color.holo_green_dark} 颜色的值为 “准备中…” 的 CharSequence 对象；而在
         * {@link #isStalled()} 返回 true 时，会返回一个 {@code android.R.color.holo_orange_dark} 颜色
         * 的值为 “缓冲中…” 的 CharSequence 对象。其它状态下会将 {@code defaultValue} 原值返回。
         *
         * @param contentText context text 的值，如果播放器处于正常播放器状态，该方法会将这个值原样返回，
         *                    如果播放器正在缓冲，或发生了错误，则将返回一个提示字符串
         */
        public final CharSequence getContentText(String contentText) {
            String value = contentText;
            int textColor = 0;

            Resources res = getContext().getResources();

            if (isError()) {
                value = getErrorMessage();
                textColor = res.getColor(android.R.color.holo_red_dark);
            }

            if (isPreparing()) {
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

        /**
         * 获取 Context 对象。
         */
        public final Context getContext() {
            return mPlayerService;
        }

        /**
         * 获取当前应用的包名。
         */
        public final String getPackageName() {
            return getContext().getPackageName();
        }

        /**
         * 获取播放器的播放模式。
         */
        public final PlayMode getPlayMode() {
            return mPlayerService.getPlayMode();
        }

        /**
         * 获取播放队列携带的额外参数（可为 null）。
         */
        @Nullable
        public final Bundle getPlaylistExtra() {
            return mPlayerService.getPlaylistExtra();
        }

        /**
         * 添加自定义动作。
         *
         * @param action       自定在动作的名称，请保证该值的唯一性
         * @param customAction 要执行的任务
         */
        public final void addCustomAction(@NonNull String action, @NonNull CustomAction customAction) {
            mPlayerService.addCustomAction(action, customAction);
        }

        /**
         * 播放器当前是否处正在准备中。
         *
         * @return 如果播放器正在准备中，则返回 true，否则返回 false
         */
        public final boolean isPreparing() {
            return mPlayerService.isPreparing();
        }

        /**
         * 播放器是否已准备完毕。
         *
         * @return 播放器是否已准备完毕。
         */
        public final boolean isPrepared() {
            return mPlayerService.isPrepared();
        }

        /**
         * 判断当前播放器是否处于 {@code stalled} 状态。
         *
         * @return 当缓冲区没有足够的数据支持播放器继续播放时，该方法会返回 {@code true}，否则返回 false
         */
        public final boolean isStalled() {
            return mPlayerService.isStalled();
        }

        /**
         * 获取播放器的播放状态。
         *
         * @return 播放器的播放状态
         */
        @NonNull
        public final PlaybackState getPlaybackState() {
            return mPlayerService.getPlaybackState();
        }

        /**
         * 当前是否正在播放音乐。
         */
        public final boolean isPlayingState() {
            return getPlaybackState() == PlaybackState.PLAYING;
        }

        /**
         * 播放器是否发生了错误。
         */
        public final boolean isError() {
            return getPlaybackState() == PlaybackState.ERROR;
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
         * 检查 {@link NotificationView} 是否已被释放。
         *
         * @return 如果已被释放，则返回 true，此时不应该再调用 {@link NotificationView} 的任何方法。
         */
        public final boolean isReleased() {
            return mReleased;
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
         * 要求 Service 更新 NotificationView，如果没有设置 NotificationView，则忽略本次操作。
         */
        public final void invalidate() {
            if (mReleased) {
                return;
            }

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
     * 播放器当前是否处正在准备中。
     *
     * @return 如果播放器正在准备中，则返回 true，否则返回 false
     */
    public final boolean isPreparing() {
        return mPlayer.isPreparing();
    }

    /**
     * 播放器当前是否处已准备完毕。
     *
     * @return 如果播放器已准备完毕，则返回 true，否则返回 false
     */
    public final boolean isPrepared() {
        return mPlayer.isPrepared();
    }

    /**
     * 通知栏控制器，使用 Android 系统提供的样式。通知的样式为：<a target="_blank" href="https://developer.android.google.cn/reference/androidx/media/app/NotificationCompat.MediaStyle?hl=en">NotificationCompat.MediaStyle</a>
     * <p>
     * 该类是个抽象类，可以通过实现 {@link #onBuildMediaStyle(androidx.media.app.NotificationCompat.MediaStyle)}
     * 方法和实现 {@link #onBuildNotification(NotificationCompat.Builder)} 来对当前 NotificationView
     * 的外观进行定制。
     * <p>
     * 更多信息，请参考官方文档： <a target="_blank" href="https://developer.android.google.cn/training/notify-user/expanded#media-style">https://developer.android.google.cn/training/notify-user/expanded#media-style</a>
     */
    public static class MediaNotificationView extends NotificationView {
        private PendingIntent mSkipToPrevious;
        private PendingIntent mPlayPause;
        private PendingIntent mSkipToNext;

        @Override
        protected void onInit(Context context) {
            initAllPendingIntent();
        }

        private void initAllPendingIntent() {
            mSkipToPrevious = MediaButtonReceiver.buildMediaButtonPendingIntent(getContext(), PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS);
            mPlayPause = MediaButtonReceiver.buildMediaButtonPendingIntent(getContext(), PlaybackStateCompat.ACTION_PLAY_PAUSE);
            mSkipToNext = MediaButtonReceiver.buildMediaButtonPendingIntent(getContext(), PlaybackStateCompat.ACTION_SKIP_TO_NEXT);
        }

        public final PendingIntent doSkipToPrevious() {
            return mSkipToPrevious;
        }

        public final PendingIntent doPlayPause() {
            return mPlayPause;
        }

        public final PendingIntent doSkipToNext() {
            return mSkipToNext;
        }

        @NonNull
        @Override
        public Notification onCreateNotification() {
            androidx.media.app.NotificationCompat.MediaStyle mediaStyle =
                    new androidx.media.app.NotificationCompat.MediaStyle()
                            .setMediaSession(getMediaSession().getSessionToken());

            onBuildMediaStyle(mediaStyle);

            NotificationCompat.Builder builder = new NotificationCompat.Builder(getContext(), CHANNEL_ID)
                    .setSmallIcon(getSmallIconId())
                    .setLargeIcon(getIcon())
                    .setContentTitle(getContentTitle())
                    .setContentText(getContentText(MusicItemUtil.getArtist(getContext(), getPlayingMusicItem())))
                    .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                    .setPriority(NotificationCompat.PRIORITY_LOW)
                    .setShowWhen(false)
                    .setAutoCancel(false)
                    .setStyle(mediaStyle);

            onBuildNotification(builder);

            return builder.build();
        }

        @Override
        public int getNotificationId() {
            return 1024;
        }

        /**
         * Notification 的 small icon 的资源 id。
         *
         * @return drawable 资源的 ID
         */
        @DrawableRes
        public int getSmallIconId() {
            return R.drawable.snow_ic_notification_small_icon;
        }

        /**
         * 该方法会在创建 {@code NotificationCompat.MediaStyle} 对象期间调用。
         * <p>
         * 可以在该方法中对 {@code NotificationCompat.MediaStyle} 对象进行配置。例如，调用
         * {@code setShowActionsInCompactView (int... actions)} 方法设置要在紧凑的通知视图中显示的操作。
         */
        protected void onBuildMediaStyle(androidx.media.app.NotificationCompat.MediaStyle mediaStyle) {
            mediaStyle.setShowActionsInCompactView(1, 2);
        }

        /**
         * 该方法会在创建 {@code NotificationCompat.Builder} 期间调用。
         * <p>
         * 可以在该方法中对 {@code NotificationCompat.Builder} 对象进行配置。例如，调用
         * {@code addAction (int icon, CharSequence title, PendingIntent intent)} 向通知添加操作。
         * <p>
         * 该方法中使用 addAction 方法添加了三个按钮：上一曲、播放/暂停、下一曲。如果你不需要这三个按钮，
         * 则你在覆盖该方法时可以不回调超类方法。
         */
        protected void onBuildNotification(NotificationCompat.Builder builder) {
            builder.addAction(R.drawable.snow_ic_skip_previous, "skip_to_previous", doSkipToPrevious());

            if (isPlayingState()) {
                builder.addAction(R.drawable.snow_ic_pause, "pause", doPlayPause());
            } else {
                builder.addAction(R.drawable.snow_ic_play, "play", doPlayPause());
            }

            builder.addAction(R.drawable.snow_ic_skip_next, "skip_to_next", doSkipToNext());
        }
    }

    /**
     * 自定义动作。
     *
     * @see PlayerService#addCustomAction(String, CustomAction)
     */
    public interface CustomAction {
        /**
         * 要执行的任务。
         *
         * @param player 播放器
         * @param extras 携带的额外参数。通过调用 Intent.getExtras() 方法获取，可为 null
         */
        void doAction(@NonNull Player player, @Nullable Bundle extras);
    }


}
