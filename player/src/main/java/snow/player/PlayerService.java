package snow.player;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ServiceInfo;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
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
import com.bumptech.glide.integration.okhttp3.OkHttpUrlLoader;
import com.bumptech.glide.load.model.GlideUrl;
import com.bumptech.glide.load.resource.bitmap.GranularRoundedCorners;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;
import com.google.common.base.Preconditions;

import java.io.InputStream;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import channel.helper.ChannelHelper;
import channel.helper.Dispatcher;
import channel.helper.DispatcherUtil;
import channel.helper.pipe.CustomActionPipe;

import channel.helper.pipe.SessionEventPipe;
import media.helper.HeadsetHookHelper;

import okhttp3.OkHttpClient;
import snow.player.appwidget.AppWidgetPreferences;
import snow.player.effect.AudioEffectManager;
import snow.player.media.MediaMusicPlayer;
import snow.player.media.MusicItem;
import snow.player.media.MusicPlayer;
import snow.player.playlist.PlaylistEditor;
import snow.player.media.ErrorCode;
import snow.player.util.OkHttpUtil;

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
public class PlayerService extends MediaBrowserServiceCompat implements PlayerManager {
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

    private static final String NAME_COMPONENT_FACTORY = "component-factory";

    private String mPersistentId;
    private int mNotificationId;

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

    private NotificationView mNotificationView;

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
        mAllCustomAction = new HashMap<>();

        initNotificationManager();
        initComponentFactory();
        initCustomActions();
        initPlayerConfig();
        initAudioEffectManager();
        initPlayerState();
        initPlaylistManager();
        initPlayer();
        initControllerPipe();
        initRemoteViewManager();
        initHeadsetHookHelper();
        initMediaSession();
        initSessionEventEmitter();
        initHistoryRecorder();

        updateNotificationView();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        MediaButtonReceiver.handleIntent(mMediaSession, intent);
        handleCustomAction(intent.getAction(), intent.getExtras());

        return super.onStartCommand(intent, flags, startId);
    }

    /**
     * 处理 CustomAction，如果已处理，则返回 true，否则返回 false。
     */
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

        stopForegroundEx(true);

        mNotificationView.release();
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
            mComponentFactory.init(getApplicationContext());
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

    private void initCustomActions() {
        if (injectCustomActions()) {
            mAllCustomAction.putAll(mComponentFactory.getCustomActions());
        }
    }

    private void initPlayerConfig() {
        mPlayerConfig = new PlayerConfig(this, mPersistentId);
    }

    private void initPlayerState() {
        mPlayerState = new PersistentPlayerState(this, mPersistentId);
    }

    private void initPlaylistManager() {
        mPlaylistManager = new PlaylistManagerImp(this, mPersistentId, true);
    }

    private void initPlayer() {
        mPlayer = new PlayerImp(this,
                mPlayerConfig,
                mPlayerState,
                mPlaylistManager,
                new AppWidgetPreferences(this, this.getClass()));
    }

    private void initControllerPipe() {
        final Dispatcher playerManagerDispatcher =
                ChannelHelper.newDispatcher(PlayerManager.class, this);

        final Dispatcher playerDispatcher =
                ChannelHelper.newDispatcher(Player.class, mPlayer);

        final Dispatcher playlistEditorDispatcher =
                ChannelHelper.newDispatcher(PlaylistEditor.class, mPlayer);

        final Dispatcher onNewPlaylistDispatcher =
                ChannelHelper.newDispatcher(PlaylistEditor.OnNewPlaylistListener.class, mPlayer);

        mCustomActionDispatcher = new CustomActionPipe(DispatcherUtil.merge(playerManagerDispatcher,
                playerDispatcher,
                playlistEditorDispatcher,
                onNewPlaylistDispatcher));
    }

    private void initRemoteViewManager() {
        NotificationView notificationView = createNotificationView();

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
        mMediaSession.setCallback(new MediaSessionCallbackImp());
        setSessionToken(mMediaSession.getSessionToken());
    }

    private void initSessionEventEmitter() {
        SessionEventPipe sessionEventEmitter = new SessionEventPipe(mMediaSession);
        mCommandCallback = ChannelHelper.newEmitter(PlayerManager.OnCommandCallback.class, sessionEventEmitter);
        mPlayer.setPlayerStateListener(ChannelHelper.newEmitter(PlayerStateListener.class, sessionEventEmitter));
    }

    private void initAudioEffectManager() {
        mAudioEffectManager = createAudioEffectManager();

        if (mAudioEffectManager == null) {
            return;
        }

        Bundle config = mPlayerConfig.getAudioEffectConfig();
        mAudioEffectManager.init(config);
    }

    private void initHistoryRecorder() {
        mHistoryRecorder = createHistoryRecorder();
    }

    @Nullable
    protected final NotificationView createNotificationView() {
        if (injectNotificationView()) {
            return mComponentFactory.createNotificationView();
        }

        return onCreateNotificationView();
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
    public void setSoundQuality(SoundQuality soundQuality) {
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
        if (isPlayingState()) {
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
     * 获取 Notification 的 ID（默认返回 1）。
     */
    @SuppressWarnings("SameReturnValue")
    protected int getNotificationId() {
        return 1;
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

    private boolean isPlayingState() {
        return mPlayer.getPlaybackState() == PlaybackState.PLAYING;
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
        if (noNotificationView()) {
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
        return mNotificationView.createNotification();
    }

    // 请不要在主线程中调用该方法，因为该方法可能会执行耗时操作
    protected final boolean isCachedEx(MusicItem musicItem, SoundQuality soundQuality) {
        if (injectIsCached()) {
            return mComponentFactory.isCached(musicItem, soundQuality);
        }

        return isCached(musicItem, soundQuality);
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

    @NonNull
    protected final MusicPlayer createMusicPlayer(@NonNull Context context, @NonNull MusicItem musicItem, @NonNull Uri uri) {
        if (injectMusicPlayer()) {
            return mComponentFactory.createMusicPlayer(context, musicItem, uri);
        }

        return onCreateMusicPlayer(this, uri);
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
    protected MusicPlayer onCreateMusicPlayer(@NonNull Context context, @NonNull Uri uri) {
        return new MediaMusicPlayer(uri);
    }

    // 请不要在主线程中调用该方法，因为该方法可能会执行耗时操作
    protected final Uri retrieveMusicItemUri(@NonNull MusicItem musicItem, @NonNull SoundQuality soundQuality) throws Exception {
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

    /**
     * 正在准备播放器。
     */
    protected void onPreparing() {
        if (noNotificationView()) {
            return;
        }
        mNotificationView.onPreparing();
    }

    /**
     * 播放器准备完毕。
     *
     * @param audioSessionId 播放器的 audio session id
     */
    protected void onPrepared(int audioSessionId) {
        if (noNotificationView()) {
            return;
        }
        mNotificationView.onPrepared();
    }

    /**
     * 播放器开始播放。
     *
     * @param progress   播放进度
     * @param updateTime 播放进度的更新时间
     */
    protected void onPlaying(int progress, long updateTime) {
        if (noNotificationView()) {
            return;
        }
        mNotificationView.onPlaying();
    }

    /**
     * 已暂停播放。
     */
    protected void onPaused() {
        if (noNotificationView()) {
            return;
        }
        mNotificationView.onPaused();
    }

    /**
     * 播放器的 {@code stalled} 状态发生了改变。
     *
     * @param stalled 播放器的 {@code stalled} 状态。当缓冲区中没有足够的数据支持继续播放时，该参数为 true，
     *                否则为 false
     */
    protected void onStalledChanged(boolean stalled) {
        if (noNotificationView()) {
            return;
        }
        mNotificationView.onStalledChanged(stalled);
    }

    /**
     * 播放器已停止播放。
     */
    protected void onStopped() {
        if (noNotificationView()) {
            return;
        }
        mNotificationView.onStopped();
    }

    /**
     * 播放器发生了错误。
     *
     * @param errorCode    错误码
     * @param errorMessage 错误信息
     */
    protected void onError(int errorCode, String errorMessage) {
        if (noNotificationView()) {
            return;
        }
        mNotificationView.onError(errorCode, errorMessage);
    }

    protected void onPlayingMusicItemChanged(@Nullable MusicItem musicItem) {
        updateNotificationView();

        if (mHistoryRecorder != null && musicItem != null) {
            mHistoryRecorder.recordHistory(musicItem);
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
        if (CUSTOM_ACTION_SHUTDOWN.equals(action)) {
            shutdown();
            return;
        }

        mCustomActionDispatcher.dispatch(action, extras);
    }

    private class PlayerImp extends AbstractPlayer {

        public PlayerImp(@NonNull Context context,
                         @NonNull PlayerConfig playerConfig,
                         @NonNull PlayerState playlistState,
                         @NonNull PlaylistManagerImp playlistManager,
                         @NonNull AppWidgetPreferences pref) {
            super(context, playerConfig, playlistState, playlistManager, pref);
        }

        @Override
        protected boolean isCached(MusicItem musicItem, SoundQuality soundQuality) {
            return PlayerService.this.isCachedEx(musicItem, soundQuality);
        }

        @NonNull
        @Override
        protected MusicPlayer onCreateMusicPlayer(@NonNull Context context, @NonNull MusicItem musicItem, @NonNull Uri uri) {
            return PlayerService.this.createMusicPlayer(context, musicItem, uri);
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
            PlayerService.this.getPlayer().play();
        }

        @Override
        public void onSkipToQueueItem(long id) {
            mPlayer.playPause((int) id);
        }

        @Override
        public void onPause() {
            PlayerService.this.getPlayer().pause();
        }

        @Override
        public void onSkipToNext() {
            PlayerService.this.getPlayer().skipToNext();
        }

        @Override
        public void onSkipToPrevious() {
            PlayerService.this.getPlayer().skipToPrevious();
        }

        @Override
        public void onFastForward() {
            PlayerService.this.getPlayer().fastForward();
        }

        @Override
        public void onRewind() {
            PlayerService.this.getPlayer().rewind();
        }

        @Override
        public void onStop() {
            PlayerService.this.getPlayer().stop();
        }

        @Override
        public void onSeekTo(long pos) {
            PlayerService.this.getPlayer().seekTo((int) pos);
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

        void init(PlayerService playerService) {
            mPlayerService = playerService;
            mMusicItem = new MusicItem();
            mIconSize = new int[2];
            mIconCornerRadius = new int[4];

            mDefaultIcon = getDefaultIcon();
            mIcon = mDefaultIcon;

            setIconSize(playerService.getResources().getDimensionPixelSize(R.dimen.snow_notif_icon_size_big));
            setNeedReloadIcon(true);
            initGlide();
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

        private void initGlide() {
            OkHttpClient.Builder clientBuilder = new OkHttpClient.Builder()
                    .followRedirects(true)
                    .followSslRedirects(true)
                    .retryOnConnectionFailure(true)
                    .connectTimeout(5, TimeUnit.SECONDS)
                    .readTimeout(5, TimeUnit.SECONDS);

            OkHttpUtil.enableTls12OnPreLollipop(clientBuilder);

            OkHttpUrlLoader.Factory factory = new OkHttpUrlLoader.Factory(clientBuilder.build());
            Glide.get(mPlayerService)
                    .getRegistry()
                    .replace(GlideUrl.class, InputStream.class, factory);
        }

        private void release() {
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
        protected void onRelease() {
        }

        /**
         * 正在准备播放器。
         */
        protected void onPreparing() {
            invalidate();
        }

        /**
         * 播放器准备完毕。
         */
        protected void onPrepared() {
            invalidate();
        }

        /**
         * 播放器开始播放。
         */
        protected void onPlaying() {
            invalidate();
        }

        /**
         * 已暂停播放。
         */
        protected void onPaused() {
            invalidate();
        }

        /**
         * 播放器的 {@code stalled} 状态发生了改变。
         *
         * @param stalled 播放器的 {@code stalled} 状态。当缓冲区中没有足够的数据支持继续播放时，该参数为 true，
         *                否则为 false
         */
        protected void onStalledChanged(boolean stalled) {
            invalidate();
        }

        /**
         * 播放器已停止播放。
         */
        protected void onStopped() {
            invalidate();
        }

        /**
         * 播放器发生了错误。
         *
         * @param errorCode    错误码
         * @param errorMessage 错误信息
         */
        protected void onError(int errorCode, String errorMessage) {
            invalidate();
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
        protected final PendingIntent getCustomActionPendingIntent(@NonNull String action) {
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
        protected abstract Notification onCreateNotification();

        /**
         * 获取默认图标。
         *
         * @return 通知的默认图标，不能为 null
         */
        @NonNull
        protected Bitmap getDefaultIcon() {
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
        protected final CharSequence getContentText(String contentText) {
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
        protected final Context getContext() {
            return mPlayerService;
        }

        /**
         * 获取当前应用的包名。
         */
        protected final String getPackageName() {
            return getContext().getPackageName();
        }

        /**
         * 获取播放器的播放模式。
         */
        protected final PlayMode getPlayMode() {
            return mPlayerService.getPlayMode();
        }

        /**
         * 获取播放队列携带的额外参数（可为 null）。
         */
        @Nullable
        protected final Bundle getPlaylistExtra() {
            return mPlayerService.getPlaylistExtra();
        }

        /**
         * 添加自定义动作。
         *
         * @param action       自定在动作的名称，请保证该值的唯一性
         * @param customAction 要执行的任务
         */
        protected final void addCustomAction(@NonNull String action, @NonNull CustomAction customAction) {
            mPlayerService.addCustomAction(action, customAction);
        }

        /**
         * 当前是否正在播放音乐。
         */
        public final boolean isPlayingState() {
            return mPlayerService.isPlayingState();
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
         * 判断当前播放器是否处于 {@code stalled} 状态。
         *
         * @return 当缓冲区没有足够的数据支持播放器继续播放时，该方法会返回 {@code true}，否则返回 false
         */
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
        protected Notification onCreateNotification() {
            androidx.media.app.NotificationCompat.MediaStyle mediaStyle =
                    new androidx.media.app.NotificationCompat.MediaStyle()
                            .setMediaSession(getMediaSession().getSessionToken());

            onBuildMediaStyle(mediaStyle);

            NotificationCompat.Builder builder = new NotificationCompat.Builder(getContext(), CHANNEL_ID)
                    .setSmallIcon(getSmallIconId())
                    .setLargeIcon(getIcon())
                    .setContentTitle(getContentTitle())
                    .setContentText(getContentText(getPlayingMusicItem().getArtist()))
                    .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                    .setPriority(NotificationCompat.PRIORITY_LOW)
                    .setShowWhen(false)
                    .setStyle(mediaStyle);

            onBuildNotification(builder);

            return builder.build();
        }

        /**
         * Notification 的 small icon 的资源 id。
         *
         * @return drawable 资源的 ID
         */
        @DrawableRes
        protected int getSmallIconId() {
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
     * 可以通过提供一个 {@link ComponentFactory} 工厂来自定义播放器的部分组件。
     * <p>
     * 通过继承并覆盖 {@link ComponentFactory} 类的以下方法来提供自定义组件：
     * <ul>
     *     <li>{@link #createMusicPlayer(Context, MusicItem, Uri)}：音乐播放器</li>
     *     <li>{@link #createNotificationView()}：通知栏控制器</li>
     *     <li>{@link #createAudioEffectManager()}：音频特效引擎管理器</li>
     *     <li>{@link #createHistoryRecorder()}：历史记录器</li>
     *     <li>{@link #isCached(MusicItem, SoundQuality)}：查询具有特定 {@link SoundQuality} 的 {@link MusicItem} 是否已缓存</li>
     *     <li>{@link #retrieveMusicItemUri(MusicItem, SoundQuality)}：获取歌曲的播放链接</li>
     *     <li>{@link #getCustomActions()}：添加自定义动作</li>
     * </ul>
     * <p>
     * 可以覆盖上面的一个或多个方法来使用自定义组件，重写后的方法需要使用 {@link Inject} 注解进行标记否则会被忽略。
     * 另外，还可以覆盖 {@link #init(Context)} 方法来完成一些初始化工作。
     * <p>
     * 如果你打算播放来自网络的音乐，建议覆盖 {@link #isCached(MusicItem, SoundQuality)} 方法，该方法用于
     * 判断具有特定 {@link SoundQuality} 的 {@link MusicItem} 是否已缓存。该方法会在异步线程中执行，因此可
     * 以在该方法中执行耗时操作，例如访问本地数据库。如果播放器仅用于播放本地音乐，则可以覆盖
     * {@link #isCached(MusicItem, SoundQuality)} 方法并直接返回 true 即可。
     * <p>
     * 此外，还可以覆盖 {@link #retrieveMusicItemUri(MusicItem, SoundQuality)} 方法用来根据音质获取不同的播放链接。
     * 该方法会在异步线程中调用，因此可以直接在该方法中访问网络或数据库。
     * <p>
     * <b>步骤：</b>
     * <ol>
     *     <li>创建一个类并继承 {@link ComponentFactory} 类（该类必须提供一个无参构造方法）；</li>
     *     <li>（可选）覆盖 {@link #init(Context)} 方法以进行某些初始化操作；</li>
     *     <li>覆盖 {@link ComponentFactory} 的工厂方法，并使用 {@link Inject} 注解进行标注；</li>
     *     <li>在 AndroidManifest.xml 文件中对你的 {@link ComponentFactory} 进行注册。</li>
     * </ol>
     * <p>
     * <b>例：</b><br>
     * <pre>
     * package snow.player.debug;
     * ...
     *
     * public class MyFactory extends PlayerService.ComponentFactory {
     *     ...
     *
     *     &#64;Override
     *     public void init(Context context) {
     *         // （可选）进行某些初始化操作
     *         // 如果没有任何需要执行的初始化操作，则可以不覆盖该方法
     *     }
     *
     *     &#64;Inject    // 使用 &#64;Inject 注解对工厂方法进行标记，否则会被忽略
     *     &#64;NonNull
     *     &#64;Override
     *     public MusicPlayer createMusicPlayer(Context context) {
     *         return new ExoMusicPlayer(context, mediaSourceFactory);
     *     }
     * }
     * </pre>
     * <p>
     * <b>在 AndroidManifest.xml 文件中对 {@link ComponentFactory} 进行注册：</b>
     * <p>
     * <b>注册方法</b>：在 &lt;service&gt; 标签中使用 &lt;meta-date&gt; 标签指定你的 {@link ComponentFactory}
     * 的完整类名。其中，&lt;meta-date&gt; 标签的 {@code android:name} 属性的值为
     * {@code "component-factory"}，{@code android:value} 属性的值为你的 {@link ComponentFactory}
     * 的完整类名。
     * <p>
     * <b>例：</b><br>
     * <pre>
     * &lt;service android:name="snow.player.PlayerService"&gt;
     *     ...
     *     &lt;meta-data android:name="component-factory" android:value="@string/factory-name"/&gt;
     * &lt;/service&gt;
     * </pre>
     * 注：上例中 {@code android:value} 的值是 {@code "@string/factory-name"}，这是一个字符串资源，
     * 该字符串资源的值是你的 {@link ComponentFactory} 的完整类名（例如："snow.player.debug.MyFactory"）。
     */
    public static abstract class ComponentFactory {
        /**
         * 初始化 ComponentFactory。你可以覆盖该方法来完成一些初始化工作。
         *
         * @param applicationContext 应用程序的 Context 对象
         */
        public void init(Context applicationContext) {
        }

        /**
         * 查询具有 {@link SoundQuality} 音质的 {@link MusicItem} 是否已被缓存。
         * <p>
         * 该方法会在异步线程中被调用。如果播放器仅用于播放本地音乐，则只需覆盖该方法，并直接返回 {@code true}
         * 即可。
         *
         * @param musicItem    要查询的 MusicItem 对象
         * @param soundQuality 音乐的音质
         * @return 如果歌曲已被缓存，则返回 true，否则返回 false
         */
        public boolean isCached(MusicItem musicItem, SoundQuality soundQuality) {
            return false;
        }

        /**
         * 创建一个 {@link MusicPlayer} 对象。
         *
         * @param context   Context 对象
         * @param musicItem 播放器将要播放的歌曲
         * @param uri       播放器将要播放的歌曲的 URI
         * @return {@link MusicPlayer} 对象，，不能为 null
         */
        @NonNull
        public MusicPlayer createMusicPlayer(@NonNull Context context, @NonNull MusicItem musicItem, @NonNull Uri uri) {
            return new MediaMusicPlayer(uri);
        }

        /**
         * 获取音乐的播放链接。
         * <p>
         * 该方法会在异步线程中执行，因此可以在该方法中执行耗时操作，例如访问网络。
         *
         * @param musicItem    要播放的音乐
         * @param soundQuality 要播放的音乐的音质
         * @return 音乐的播放链接，为 null 时播放器会转至 {@link PlaybackState#ERROR}
         * 状态
         * @throws Exception 获取音乐播放链接的过程中发生的任何异常
         */
        @SuppressWarnings("RedundantThrows")
        @Nullable
        public Uri retrieveMusicItemUri(MusicItem musicItem, SoundQuality soundQuality) throws Exception {
            return null;
        }

        /**
         * 创建通知栏控制器。
         *
         * @return {@link NotificationView} 对象，可为 null。为 null 时将隐藏通知栏控制器
         */
        @Nullable
        public NotificationView createNotificationView() {
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

        /**
         * 返回所有要添加的自定义动作。
         * <p>
         * 该方法应该返回一个 Map，该 Map 包含了所有要添加的自定义动作。Map 的 key 是自定义动作的名称，请保证
         * 其唯一性，并且符合 Android 的 action 格式（例如：{@code snow.player.action.PLAY_PAUSE}）。
         * <p>
         * 你可以使用 Map 的 key 值创建一个 Intent 对象来调用启动 PlayerService。PlayerService 会在其
         * onStartCommand 方法中检测 Intent 的 Action 是否匹配了一个自定义动作，如果匹配成功，则会执行对应
         * 的 CustomAction。
         * <p>
         * 例：
         * <pre>
         * Intent intent = new Intent(context, PlayerService.class);
         * intent.setAction(key_custom_action);
         * ...
         * context.startService(intent);
         * </pre>
         */
        @NonNull
        public Map<String, CustomAction> getCustomActions() {
            return new HashMap<>();
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

    private boolean injectIsCached() {
        return shouldInject("isCached", MusicItem.class, SoundQuality.class);
    }

    private boolean injectMusicPlayer() {
        return shouldInject("createMusicPlayer", Context.class, MusicItem.class, Uri.class);
    }

    private boolean injectMusicItemUri() {
        return shouldInject("retrieveMusicItemUri", MusicItem.class, SoundQuality.class);
    }

    private boolean injectNotificationView() {
        return shouldInject("createNotificationView");
    }

    private boolean injectAudioEffectManager() {
        return shouldInject("createAudioEffectManager");
    }

    private boolean injectHistoryRecorder() {
        return shouldInject("createHistoryRecorder");
    }

    private boolean injectCustomActions() {
        return shouldInject("getCustomActions");
    }

    /**
     * 自定义动作。
     *
     * @see PlayerService#addCustomAction(String, CustomAction)
     * @see ComponentFactory#getCustomActions()
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
