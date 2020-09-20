package snow.player;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.RemoteException;
import android.support.v4.media.MediaBrowserCompat;
import android.support.v4.media.session.MediaControllerCompat;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleObserver;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.OnLifecycleEvent;

import com.google.common.base.Preconditions;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import channel.helper.ChannelHelper;
import channel.helper.DispatcherUtil;
import channel.helper.pipe.CustomActionPipe;
import channel.helper.pipe.SessionEventPipe;
import snow.player.audio.MusicItem;
import snow.player.playlist.Playlist;
import snow.player.playlist.PlaylistEditor;
import snow.player.playlist.PlaylistManager;
import snow.player.audio.ErrorCode;

/**
 * 播放器客户端，用于向播放器发送各种控制命令。
 */
public class PlayerClient implements Player, PlaylistEditor {
    private Context mApplicationContext;
    private Class<? extends PlayerService> mPlayerService;
    private String mClientToken;
    private String mPersistentId;

    private MediaBrowserCompat mMediaBrowser;
    private MediaControllerCompat mMediaController;
    private MediaControllerCompat.Callback mMediaControllerCallback;
    private SessionEventPipe mSessionEventDispatcher;

    private PlayerConfig mPlayerConfig;
    private PlayerManager mPlayerManager;
    private PlayerManager.OnCommandCallback mCommandCallback;

    private OnConnectCallback mConnectCallback;

    private Player mPlayer;
    private PlaylistEditor mPlaylistEditor;
    private PlaylistManagerImp mPlaylistManager;
    private PlayerStateHolder mPlayerStateHolder;

    private List<OnConnectStateChangeListener> mAllConnectStateChangeListener;

    private PlayerClient(Context context, Class<? extends PlayerService> playerService) {
        mApplicationContext = context.getApplicationContext();
        mPlayerService = playerService;
        mClientToken = UUID.randomUUID().toString();
        mPersistentId = mPlayerService.getName();

        mPlayerConfig = new PlayerConfig(context, mPersistentId);
        mAllConnectStateChangeListener = new ArrayList<>();

        initMediaBrowser();
        initPlaylistManager();
        initPlayerStateHolder();
        initCommandCallback();
        initSessionEventDispatcher();
        initMediaControllerCallback();
    }

    /**
     * 创建一个 PlayerClient 对象。
     *
     * @param context       Context 对象，不能为 null
     * @param playerService PlayerService 或者其子类的 Class 对象，不能为 null
     * @return PlayerClient 对象
     */
    public static PlayerClient newInstance(@NonNull Context context,
                                           @NonNull Class<? extends PlayerService> playerService) {
        Preconditions.checkNotNull(context);
        Preconditions.checkNotNull(playerService);

        if (serviceNotFound(context, playerService)) {
            throw new IllegalArgumentException("PlayerService not found, Please check your 'AndroidManifest.xml'");
        }

        return new PlayerClient(context, playerService);
    }

    private static boolean serviceNotFound(Context context, Class<? extends PlayerService> playerService) {
        PackageManager pm = context.getPackageManager();
        Intent intent = new Intent(context, playerService);
        return pm.resolveService(intent, 0) == null;
    }

    private void initMediaBrowser() {
        mMediaBrowser = new MediaBrowserCompat(mApplicationContext,
                new ComponentName(mApplicationContext, mPlayerService),
                new MediaBrowserCompat.ConnectionCallback() {
                    @Override
                    public void onConnected() {
                        try {
                            mMediaController = new MediaControllerCompat(mApplicationContext, mMediaBrowser.getSessionToken());

                            mMediaController.registerCallback(mMediaControllerCallback, new Handler(Looper.getMainLooper()));
                            initCustomActionEmitter(mMediaController);
                            mPlayerManager.syncPlayerState(mClientToken);

                            if (mConnectCallback != null) {
                                mConnectCallback.onConnected(true);
                                mConnectCallback = null;
                            }
                        } catch (RemoteException e) {
                            mMediaBrowser.disconnect();
                            onConnectionFailed();
                        }
                    }

                    @Override
                    public void onConnectionFailed() {
                        onDisconnected();

                        if (mConnectCallback != null) {
                            mConnectCallback.onConnected(false);
                            mConnectCallback = null;
                        }
                    }
                }, null);
    }

    private void initPlaylistManager() {
        mPlaylistManager = new PlaylistManagerImp(mApplicationContext, mPersistentId, false);
    }

    private void initPlayerStateHolder() {
        mPlayerStateHolder = new PlayerStateHolder(mPlaylistManager);
    }

    private void initCommandCallback() {
        mCommandCallback = new PlayerManager.OnCommandCallback() {
            @Override
            public void onShutdown() {
                disconnect();
            }

            @Override
            public void onSyncPlayerState(@NonNull String clientToken, @NonNull PlayerState playerState) {
                if (!clientToken.equals(mClientToken)) {
                    return;
                }

                setConnected(true);
                mPlayerStateHolder.setPlayerState(playerState);
                notifyConnectStateChanged(true);
            }
        };
    }

    private void initCustomActionEmitter(MediaControllerCompat mediaController) {
        CustomActionPipe customActionEmitter = new CustomActionPipe(mediaController.getTransportControls());

        mPlayer = ChannelHelper.newEmitter(Player.class, customActionEmitter);
        mPlaylistEditor = ChannelHelper.newEmitter(PlaylistEditor.class, customActionEmitter);

        mPlayerManager = ChannelHelper.newEmitter(PlayerManager.class, customActionEmitter);
    }

    private void initSessionEventDispatcher() {
        mSessionEventDispatcher = new SessionEventPipe(DispatcherUtil.merge(
                ChannelHelper.newDispatcher(PlayerManager.OnCommandCallback.class, mCommandCallback),
                ChannelHelper.newDispatcher(PlayerStateListener.class, mPlayerStateHolder)
        ));
    }

    private void initMediaControllerCallback() {
        mMediaControllerCallback = new MediaControllerCompat.Callback() {
            @Override
            public void onSessionEvent(String event, Bundle extras) {
                mSessionEventDispatcher.dispatch(event, extras);
            }
        };
    }

    private void onDisconnected() {
        setConnected(false);
        notifyConnectStateChanged(false);
    }

    private void notifyConnectStateChanged(boolean connected) {
        for (OnConnectStateChangeListener listener : mAllConnectStateChangeListener) {
            listener.onConnectStateChanged(connected);
        }
    }

    /**
     * 连接播放器。
     */
    public void connect() {
        if (isConnected()) {
            return;
        }

        mMediaBrowser.connect();
    }

    /**
     * 连接播放器
     *
     * @param callback 回调接口，用于接收连接结果
     */
    public void connect(OnConnectCallback callback) {
        if (isConnected()) {
            callback.onConnected(true);
            return;
        }

        mConnectCallback = callback;
        connect();
    }

    /**
     * 断开与播放器的连接。
     * <p>
     * 断开连接后，可以调用 {@link #connect()} 或者 {@link #connect(OnConnectCallback)} 方法再次进行连接。
     */
    public void disconnect() {
        if (!isConnected()) {
            return;
        }

        onDisconnected();
        mMediaController.unregisterCallback(mMediaControllerCallback);
        mMediaBrowser.disconnect();
    }

    /**
     * 判断播放器是否已连接。
     *
     * @return 如果播放器已连接则返回 true，否则返回 false
     */
    public boolean isConnected() {
        return mMediaBrowser.isConnected();
    }

    /**
     * 添加一个监听器用来监听 PlayerClient 连接断开事件。
     *
     * @param listener 要添加的事件监听器，如果已添加，则会忽略本次调用
     */
    public void addOnConnectStateChangeListener(OnConnectStateChangeListener listener) {
        if (mAllConnectStateChangeListener.contains(listener)) {
            return;
        }

        mAllConnectStateChangeListener.add(listener);
        listener.onConnectStateChanged(isConnected());
    }

    /**
     * 添加一个 {@link OnConnectStateChangeListener} 监听器用来监听 {@link PlayerClient} 的连接成功与断开连接事件。
     *
     * @param owner    LifecycleOwner 对象。监听器会在该 LifecycleOwner 对象销毁时自动注销，避免内存泄露
     * @param listener 要添加的事件监听器，如果已添加，则会忽略本次调用
     */
    public void addOnConnectStateChangeListener(LifecycleOwner owner, final OnConnectStateChangeListener listener) {
        if (isDestroyed(owner)) {
            return;
        }

        addOnConnectStateChangeListener(listener);
        owner.getLifecycle().addObserver(new DestroyObserver(new Runnable() {
            @Override
            public void run() {
                removeOnConnectStateChangeListener(listener);
            }
        }));
    }

    /**
     * 移除已添加的 {@link OnConnectStateChangeListener} 监听器对象。
     *
     * @param listener 要移除的监听器
     */
    public void removeOnConnectStateChangeListener(OnConnectStateChangeListener listener) {
        mAllConnectStateChangeListener.remove(listener);
    }

    /**
     * 获取 {@link MediaControllerCompat} 对象。
     *
     * @return {@link MediaControllerCompat} 对象，如果还没有建立连接（{@link #isConnected()} 返回
     * {@code false}），那么该方法可能会返回 null
     */
    @Nullable
    public MediaControllerCompat getMediaController() {
        return mMediaController;
    }

    /**
     * 设置播放器的首选音质（默认为 {@link SoundQuality#STANDARD}）。
     * <p>
     * 该方法只在连接到播放器后（{@link #isConnected()} 返回 true）才有效。
     *
     * @param soundQuality 要设置的音质，不能为 null
     * @see SoundQuality#STANDARD
     * @see SoundQuality#LOW
     * @see SoundQuality#HIGH
     * @see SoundQuality#SUPER
     * @see #getSoundQuality()
     */
    public void setSoundQuality(@NonNull SoundQuality soundQuality) {
        Preconditions.checkNotNull(soundQuality);
        if (!isConnected()) {
            return;
        }

        mPlayerManager.setSoundQuality(soundQuality);
    }

    /**
     * 修改音频特效的配置。
     *
     * @param config 要设置的音频特效配置，不能为 null
     */
    public void setAudioEffectConfig(@NonNull Bundle config) {
        Preconditions.checkNotNull(config);
        if (!isConnected()) {
            return;
        }

        mPlayerManager.setAudioEffectConfig(config);
    }

    /**
     * 设置是否启用音频特效（如：均衡器）（默认为 false）。
     * <p>
     * 该方法只在连接到播放器后（{@link #isConnected()} 返回 true）才有效。
     *
     * @param enabled 是否启用音频特效
     * @see #isAudioEffectEnabled()
     */
    public void setAudioEffectEnabled(boolean enabled) {
        if (!isConnected()) {
            return;
        }

        mPlayerManager.setAudioEffectEnabled(enabled);
    }

    /**
     * 设置是否只允许在 WiFi 网络下播放音乐（默认为 false）。
     * <p>
     * 该方法只在连接到播放器后（{@link #isConnected()} 返回 true）才有效。
     *
     * @param onlyWifiNetwork 是否只允许在 WiFi 网络下播放音乐
     * @see #isOnlyWifiNetwork()
     */
    public void setOnlyWifiNetwork(boolean onlyWifiNetwork) {
        if (!isConnected()) {
            return;
        }

        mPlayerManager.setOnlyWifiNetwork(onlyWifiNetwork);
    }

    /**
     * 获取当前播放器的首选音质。
     *
     * @see #setSoundQuality(SoundQuality)
     */
    public SoundQuality getSoundQuality() {
        return mPlayerConfig.getSoundQuality();
    }

    /**
     * 获取音频特效的配置。
     * <p>
     * 为了确保返回正确的值，请在连接成功后再调用该方法。
     *
     * @return 音频特效的配置
     */
    @Nullable
    public Bundle getAudioEffectConfig() {
        return mPlayerConfig.getAudioEffectConfig();
    }

    /**
     * 是否已启用音频特效。
     *
     * @see #setAudioEffectEnabled(boolean)
     */
    public boolean isAudioEffectEnabled() {
        return mPlayerConfig.isAudioEffectEnabled();
    }

    /**
     * 是否只允许使用 Wifi 网络（默认为 false）。
     *
     * @see #setOnlyWifiNetwork(boolean)
     */
    public boolean isOnlyWifiNetwork() {
        return mPlayerConfig.isOnlyWifiNetwork();
    }

    /**
     * 关闭播放器。
     * <p>
     * 调用该方法后，后台的播放器会自动关闭，并断开所有客户端的连接。通常不建议客户端调用此方法，如果客户端需要
     * 断开与播放器的连接，注意 {@link #disconnect()} 方法即可。
     */
    public void shutdown() {
        if (isConnected()) {
            mPlayerManager.shutdown();
        }
    }

    private void setConnected(boolean connected) {
        mPlaylistManager.setEditable(connected);
        mPlayerStateHolder.setConnected(connected);
    }

    private boolean notConnected() {
        return !mMediaBrowser.isConnected();
    }

    /**
     * 获取当前 PlayerController 的 PlaylistManager 对象。
     * <p>
     * 注意！只允许在播放器已连接（{@link #isConnected()} 返回 true）时，才允许使用 PlaylistManager
     * 修改播放队列。当然，即使未连接，使用 PlaylistManager 访问播放队列还是允许的。
     *
     * @return 当前 PlayerController 的 PlaylistManager 对象。
     */
    public PlaylistManager getPlaylistManager() {
        return mPlaylistManager;
    }

    /**
     * 设置一个新的播放列表。
     *
     * @param playlist 播放列表（不能为 null）
     */
    public void setPlaylist(@NonNull Playlist playlist) {
        setPlaylist(playlist, 0, false);
    }

    /**
     * 设置一个新的播放列表。
     *
     * @param playlist 播放列表（不能为 null）
     * @param play     是否立即播放列表中的音乐
     */
    public void setPlaylist(@NonNull Playlist playlist, boolean play) {
        setPlaylist(playlist, 0, play);
    }

    /**
     * 设置一个新的播放列表。
     *
     * @param playlist 播放列表（不能为 null）
     * @param position 播放列表中要播放的歌曲的位置
     * @param play     是否立即播放 {@code position} 参数指定处的音乐
     * @throws IllegalArgumentException 如果 position 的值小于 0，则抛出该异常
     */
    @Override
    public void setPlaylist(@NonNull Playlist playlist, int position, boolean play) throws IllegalArgumentException {
        Preconditions.checkNotNull(playlist);
        if (position < 0) {
            throw new IllegalArgumentException("position must >= 0.");
        }

        if (!isConnected()) {
            return;
        }

        mPlaylistEditor.setPlaylist(playlist, position, play);
    }

    /**
     * 获取当前的播放队列。
     *
     * @param callback 回调接口，该接口的回调方法会在主线程中调用
     */
    public void getPlaylist(@NonNull PlaylistManager.Callback callback) {
        Preconditions.checkNotNull(callback);
        mPlaylistManager.getPlaylist(callback);
    }

    /**
     * 获取播放列表中包含的歌曲的数量。
     */
    public int getPlaylistSize() {
        return mPlaylistManager.getPlaylistSize();
    }

    /**
     * 获取播放进度（单位：毫秒）。
     *
     * @return 播放进度
     */
    public int getPlayProgress() {
        return mPlayerStateHolder.mPlayerState.getPlayProgress();
    }

    /**
     * 获取播放进度的更新时间。<b>注意！这是基于 SystemClock.elapsedRealtime() 的时间。</b>
     *
     * @return 播放进度的更新时间
     */
    public long getPlayProgressUpdateTime() {
        return mPlayerStateHolder.mPlayerState.getPlayProgressUpdateTime();
    }

    /**
     * 是否单曲循环播放。
     *
     * @return 当播放模式为 {@link PlayMode#LOOP} 时返回 true，否则返回 false
     */
    public boolean isLooping() {
        return getPlayMode() == PlayMode.LOOP;
    }

    /**
     * 获取当前正在播放的音乐。
     *
     * @return 当前正在播放的音乐，如果当前没有任何播放的音乐，则返回 null
     */
    @Nullable
    public MusicItem getPlayingMusicItem() {
        return mPlayerStateHolder.mPlayerState.getMusicItem();
    }

    /**
     * 获取当前正在播放的音乐的持续时间。
     *
     * @return 当前正在播放的音乐的持续时间，如果当前没有任何播放的音乐，则返回 0
     */
    public int getPlayingMusicItemDuration() {
        MusicItem musicItem = getPlayingMusicItem();
        if (musicItem == null) {
            return 0;
        }

        return musicItem.getDuration();
    }

    /**
     * 获取当前播放状态。
     *
     * @return 当前播放状态
     * @see PlaybackState
     */
    public PlaybackState getPlaybackState() {
        return mPlayerStateHolder.mPlayerState.getPlaybackState();
    }

    /**
     * 获取 audio session id。
     *
     * @return 如果 audio session id 不可用，则返回 0
     */
    public int getAudioSessionId() {
        return mPlayerStateHolder.mPlayerState.getAudioSessionId();
    }

    /**
     * 获取当前的缓存进度。
     *
     * @return 当前缓存进度，使用整数表示的百分比值，范围为 [0, 100]
     */
    public int getBufferedProgress() {
        return mPlayerStateHolder.mPlayerState.getBufferedProgress();
    }

    /**
     * 播放器当前是否处于 {@link PlaybackState#PLAYING} 状态。
     *
     * @return 如果播放器当前处于 {@link PlaybackState#PLAYING} 状态则返回 true，否则返回 false
     */
    public boolean isPlaying() {
        return mPlayerStateHolder.mPlayerState.getPlaybackState() == PlaybackState.PLAYING;
    }

    /**
     * 当前播放器是否处于 stalled 状态。
     * <p>
     * stalled 状态用于表示当前缓冲区是否有足够的数据继续播放，如果缓冲区没有足够的数据支撑继续播放，则该
     * 方法会返回 true，如果缓冲区有足够的数据可以继续播放，则返回 false。
     */
    public boolean isStalled() {
        return mPlayerStateHolder.mPlayerState.isStalled();
    }

    /**
     * 播放器是否正在准备中。
     *
     * @return 如果播放器正在准备中，则返回 true，否则返回 false
     */
    public boolean isPreparing() {
        return mPlayerStateHolder.mPlayerState.isPreparing();
    }

    /**
     * 播放器是否准备完毕。
     *
     * @return 如果播放器已准备完毕，则返回 true，否则返回 false
     */
    public boolean isPrepared() {
        return mPlayerStateHolder.mPlayerState.isPrepared();
    }

    /**
     * 播放器是否发生了错误。
     */
    public boolean isError() {
        return getErrorCode() != ErrorCode.NO_ERROR;
    }

    /**
     * 获取错误码。
     *
     * @return 错误码。如果播放器没有发生错误，则返回 {@link ErrorCode#NO_ERROR}
     * @see ErrorCode
     */
    public int getErrorCode() {
        return mPlayerStateHolder.mPlayerState.getErrorCode();
    }

    /**
     * 获取错误信息。
     *
     * @return 错误信息。该方法的返回值只在错误发生时才有意义
     * @see #isError()
     * @see #getErrorCode()
     */
    public String getErrorMessage() {
        return mPlayerStateHolder.mPlayerState.getErrorMessage();
    }

    /**
     * 获取当前播放模式。
     *
     * @return 当前播放模式。
     */
    public PlayMode getPlayMode() {
        return mPlayerStateHolder.mPlayerState.getPlayMode();
    }

    /**
     * 获取当前播放列表的播放位置。
     *
     * @return 当前播放列表的播放位置
     */
    public int getPlayPosition() {
        return mPlayerStateHolder.mPlayerState.getPlayPosition();
    }

    /**
     * 下一曲。
     * <p>
     * 该方法只在连接到播放器后（{@link #isConnected()} 返回 true）才有效。
     */
    @Override
    public void skipToNext() {
        if (notConnected()) {
            return;
        }

        mPlayer.skipToNext();
    }

    /**
     * 上一曲。
     * <p>
     * 该方法只在连接到播放器后（{@link #isConnected()} 返回 true）才有效。
     */
    @Override
    public void skipToPrevious() {
        if (notConnected()) {
            return;
        }

        mPlayer.skipToPrevious();
    }

    /**
     * 播放 position 处的音乐，如果播放列表中 position 处的音乐是当前正在播放的音乐，则忽略本次调用。
     * <p>
     * 该方法只在连接到播放器后（{@link #isConnected()} 返回 true）才有效。
     *
     * @param position 要播放的音乐的 position 值（从 0 开始计算）。
     * @throws IllegalArgumentException 如果 position 值小于 0，则会抛出该异常。
     */
    @Override
    public void skipToPosition(int position) throws IllegalArgumentException {
        if (position < 0) {
            throw new IllegalArgumentException("position music >= 0");
        }

        if (notConnected()) {
            return;
        }

        mPlayer.skipToPosition(position);
    }

    /**
     * 播放或暂停播放列表中指定索引处的音乐。
     * <p>
     * 该方法只在连接到播放器后（{@link #isConnected()} 返回 true）才有效。
     *
     * @param position 目标位置。
     * @throws IllegalArgumentException 如果 position 的值小于 0，则会抛出该异常。
     */
    @Override
    public void playPause(int position) throws IllegalArgumentException {
        if (position < 0) {
            throw new IllegalArgumentException("position music >= 0");
        }

        if (notConnected()) {
            return;
        }

        mPlayer.playPause(position);
    }

    /**
     * 设置播放模式。
     * <p>
     * 该方法只在连接到播放器后（{@link #isConnected()} 返回 true）才有效。
     *
     * @param playMode 播放模式
     */
    @Override
    public void setPlayMode(@NonNull PlayMode playMode) {
        Preconditions.checkNotNull(playMode);
        if (notConnected()) {
            return;
        }

        mPlayer.setPlayMode(playMode);
    }

    /**
     * 开始播放。
     * <p>
     * 该方法只在连接到播放器后（{@link #isConnected()} 返回 true）才有效。
     */
    @Override
    public void play() {
        if (notConnected()) {
            return;
        }

        mPlayer.play();
    }

    /**
     * 暂停播放。
     * <p>
     * 该方法只在连接到播放器后（{@link #isConnected()} 返回 true）才有效。
     */
    @Override
    public void pause() {
        if (notConnected()) {
            return;
        }

        mPlayer.pause();
    }

    /**
     * 停止播放。
     * <p>
     * 该方法只在连接到播放器后（{@link #isConnected()} 返回 true）才有效。
     */
    @Override
    public void stop() {
        if (notConnected()) {
            return;
        }

        mPlayer.stop();
    }

    /**
     * 播放/暂停。
     * <p>
     * 该方法只在连接到播放器后（{@link #isConnected()} 返回 true）才有效。
     */
    @Override
    public void playPause() {
        if (notConnected()) {
            return;
        }

        mPlayer.playPause();
    }

    /**
     * 调整音乐播放进度（单位：毫秒）。
     * <p>
     * 该方法只在连接到播放器后（{@link #isConnected()} 返回 true）才有效。
     *
     * @param progress 要调整到的播放进度（单位：毫秒）
     */
    @Override
    public void seekTo(int progress) {
        if (notConnected()) {
            return;
        }

        mPlayer.seekTo(progress);
    }

    /**
     * 判断是否禁用了所有的 seek 操作。
     * <p>
     * 默认为 false，如果该方法返回 true，则会同时禁用 seekTo、fastForward、rewind 操作。
     *
     * @return 是否禁用了所有的 seek 操作
     */
    public boolean isForbidSeek() {
        if (notConnected()) {
            return false;
        }

        return mPlayerStateHolder.mPlayerState.isForbidSeek();
    }

    /**
     * 快进。
     * <p>
     * 该方法只在连接到播放器后（{@link #isConnected()} 返回 true）才有效。
     */
    @Override
    public void fastForward() {
        if (notConnected()) {
            return;
        }

        mPlayer.fastForward();
    }

    /**
     * 快退。
     * <p>
     * 该方法只在连接到播放器后（{@link #isConnected()} 返回 true）才有效。
     */
    @Override
    public void rewind() {
        if (notConnected()) {
            return;
        }

        mPlayer.rewind();
    }

    /**
     * 添加一个播放器播放状态监听器。
     * <p>
     * 如果监听器已添加，则忽略本次调用。
     *
     * @param listener 播放器播放状态监听器
     * @see Player.OnPlaybackStateChangeListener
     */
    public void addOnPlaybackStateChangeListener(Player.OnPlaybackStateChangeListener listener) {
        mPlayerStateHolder.addOnPlaybackStateChangeListener(listener);
    }

    /**
     * 添加一个播放器播放状态监听器。
     * <p>
     * 如果监听器已添加，则忽略本次调用。
     * <p>
     * 事件监听器会在 LifecycleOwner 销毁时自动注销，以避免发生内容泄露。
     *
     * @param owner    LifecycleOwner 对象
     * @param listener 播放器播放状态监听器
     * @see Player.OnPlaybackStateChangeListener
     */
    public void addOnPlaybackStateChangeListener(LifecycleOwner owner,
                                                 final Player.OnPlaybackStateChangeListener listener) {
        if (isDestroyed(owner)) {
            return;
        }

        addOnPlaybackStateChangeListener(listener);
        owner.getLifecycle().addObserver(new DestroyObserver(new Runnable() {
            @Override
            public void run() {
                removeOnPlaybackStateChangeListener(listener);
            }
        }));
    }

    /**
     * 移除播放器播放状态监听器。
     *
     * @param listener 要移除的监听器。
     */
    public void removeOnPlaybackStateChangeListener(Player.OnPlaybackStateChangeListener listener) {
        mPlayerStateHolder.removeOnPlaybackStateChangeListener(listener);
    }

    /**
     * 添加一个播放器准备（prepare）状态监听器。
     *
     * @param listener 如果监听器已存在，则忽略本次添加
     */
    public void addOnPrepareListener(Player.OnPrepareListener listener) {
        mPlayerStateHolder.addOnPrepareListener(listener);
    }

    /**
     * 添加一个播放器准备（prepare）状态监听器。
     * <p>
     * 事件监听器会在 LifecycleOwner 销毁时自动注销，以避免发生内容泄露。
     *
     * @param owner    LifecycleOwner 对象
     * @param listener 如果监听器已存在，则忽略本次添加
     */
    public void addOnPrepareListener(LifecycleOwner owner, final Player.OnPrepareListener listener) {
        if (isDestroyed(owner)) {
            return;
        }

        addOnPrepareListener(listener);
        owner.getLifecycle().addObserver(new DestroyObserver(new Runnable() {
            @Override
            public void run() {
                removeOnPrepareListener(listener);
            }
        }));
    }

    /**
     * 移除一个播放器准备（prepare）状态监听器。
     *
     * @param listener 要移除的监听器
     */
    public void removeOnPrepareListener(Player.OnPrepareListener listener) {
        mPlayerStateHolder.removeOnPrepareListener(listener);
    }

    /**
     * 添加一个 stalled 状态监听器。
     * <p>
     * 如果监听器已添加，则忽略本次调用。
     *
     * @param listener 要添加的监听器
     * @see Player.OnStalledChangeListener
     */
    public void addOnStalledChangeListener(Player.OnStalledChangeListener listener) {
        mPlayerStateHolder.addOnStalledChangeListener(listener);
    }

    /**
     * 添加一个 stalled 状态监听器。
     * <p>
     * 如果监听器已添加，则忽略本次调用。
     * <p>
     * 事件监听器会在 LifecycleOwner 销毁时自动注销，以避免发生内容泄露。
     *
     * @param listener 要添加的监听器
     * @see Player.OnStalledChangeListener
     */
    public void addOnStalledChangeListener(LifecycleOwner owner,
                                           final Player.OnStalledChangeListener listener) {
        if (isDestroyed(owner)) {
            return;
        }

        addOnStalledChangeListener(listener);
        owner.getLifecycle().addObserver(new DestroyObserver(new Runnable() {
            @Override
            public void run() {
                removeOnStalledChangeListener(listener);
            }
        }));
    }

    /**
     * 移除 stalled 状态监听器。
     *
     * @param listener 要移除的监听器。
     */
    public void removeOnStalledChangeListener(Player.OnStalledChangeListener listener) {
        mPlayerStateHolder.removeOnStalledChangeListener(listener);
    }

    /**
     * 添加一个缓存进度监听器。
     * <p>
     * 如果监听器已添加，则忽略本次调用。
     *
     * @param listener 要添加的监听器
     * @see Player.OnBufferedProgressChangeListener
     */
    public void addOnBufferedProgressChangeListener(OnBufferedProgressChangeListener listener) {
        mPlayerStateHolder.addOnBufferedProgressChangeListener(listener);
    }

    /**
     * 添加一个缓存进度监听器。
     * <p>
     * 如果监听器已添加，则忽略本次调用。
     * <p>
     * 事件监听器会在 LifecycleOwner 销毁时自动注销，以避免发生内容泄露。
     *
     * @param owner    LifecycleOwner 对象
     * @param listener 要添加的监听器
     */
    public void addOnBufferedProgressChangeListener(LifecycleOwner owner,
                                                    final OnBufferedProgressChangeListener listener) {
        if (isDestroyed(owner)) {
            return;
        }

        addOnBufferedProgressChangeListener(listener);
        owner.getLifecycle().addObserver(new DestroyObserver(new Runnable() {
            @Override
            public void run() {
                removeOnBufferedProgressChangeListener(listener);
            }
        }));
    }

    /**
     * 移除缓存进度监听器。
     *
     * @param listener 要移除的监听器
     */
    public void removeOnBufferedProgressChangeListener(OnBufferedProgressChangeListener listener) {
        mPlayerStateHolder.removeOnBufferedProgressChangeListener(listener);
    }

    /**
     * 添加一个监听当前播放的 MusicItem 改变事件的监听器。
     * <p>
     * 如果监听器已添加，则忽略本次调用。
     *
     * @param listener 要添加的监听器
     * @see Player.OnPlayingMusicItemChangeListener
     */
    public void addOnPlayingMusicItemChangeListener(Player.OnPlayingMusicItemChangeListener listener) {
        mPlayerStateHolder.addOnPlayingMusicItemChangeListener(listener);
    }

    /**
     * 添加一个监听当前播放的 MusicItem 改变事件的监听器。
     * <p>
     * 如果监听器已添加，则忽略本次调用。
     * <p>
     * 事件监听器会在 LifecycleOwner 销毁时自动注销，以避免发生内容泄露。
     *
     * @param listener 要添加的监听器
     * @see Player.OnPlayingMusicItemChangeListener
     */
    public void addOnPlayingMusicItemChangeListener(LifecycleOwner owner,
                                                    final Player.OnPlayingMusicItemChangeListener listener) {
        if (isDestroyed(owner)) {
            return;
        }

        addOnPlayingMusicItemChangeListener(listener);
        owner.getLifecycle().addObserver(new DestroyObserver(new Runnable() {
            @Override
            public void run() {
                removeOnPlayingMusicItemChangeListener(listener);
            }
        }));
    }

    /**
     * 移除当前播放的 MusicItem 改变事件监听器
     *
     * @param listener 要移除的监听器
     */
    public void removeOnPlayingMusicItemChangeListener(Player.OnPlayingMusicItemChangeListener listener) {
        mPlayerStateHolder.removeOnPlayingMusicItemChangeListener(listener);
    }

    /**
     * 添加一个用于监听播放器播放进度调整完毕事件的监听器。
     * <p>
     * 如果监听器已添加，则忽略本次调用。
     *
     * @param listener 要添加的监听器
     * @see Player.OnSeekCompleteListener
     */
    public void addOnSeekCompleteListener(OnSeekCompleteListener listener) {
        mPlayerStateHolder.addOnSeekCompleteListener(listener);
    }

    /**
     * 添加一个用于监听播放器播放进度调整完毕事件的监听器。
     * <p>
     * 如果监听器已添加，则忽略本次调用。
     * <p>
     * 事件监听器会在 LifecycleOwner 销毁时自动注销，以避免发生内容泄露。
     *
     * @param listener 要添加的监听器
     * @see Player.OnSeekCompleteListener
     */
    public void addOnSeekCompleteListener(LifecycleOwner owner,
                                          final OnSeekCompleteListener listener) {
        if (isDestroyed(owner)) {
            return;
        }

        addOnSeekCompleteListener(listener);
        owner.getLifecycle().addObserver(new DestroyObserver(new Runnable() {
            @Override
            public void run() {
                removeOnSeekCompleteListener(listener);
            }
        }));
    }

    /**
     * 移除用于监听播放器播放进度调整完毕的监听器。
     *
     * @param listener 要移除的监听器
     */
    public void removeOnSeekCompleteListener(OnSeekCompleteListener listener) {
        mPlayerStateHolder.removeOnSeekCompleteListener(listener);
    }

    /**
     * 添加一个用于监听播放列表改变事件的监听器。
     * <p>
     * 如果监听器已添加，则忽略本次调用。
     *
     * @param listener 要添加的监听器
     * @see Player.OnPlaylistChangeListener
     */
    public void addOnPlaylistChangeListener(Player.OnPlaylistChangeListener listener) {
        mPlayerStateHolder.addOnPlaylistChangeListener(listener);
    }

    /**
     * 添加一个用于监听播放列表改变事件的监听器。
     * <p>
     * 如果监听器已添加，则忽略本次调用。
     * <p>
     * 事件监听器会在 LifecycleOwner 销毁时自动注销，以避免发生内容泄露。
     *
     * @param listener 要添加的监听器
     * @see Player.OnPlaylistChangeListener
     */
    public void addOnPlaylistChangeListener(LifecycleOwner owner,
                                            final Player.OnPlaylistChangeListener listener) {
        if (isDestroyed(owner)) {
            return;
        }

        addOnPlaylistChangeListener(listener);
        owner.getLifecycle().addObserver(new DestroyObserver(new Runnable() {
            @Override
            public void run() {
                removeOnPlaylistChangeListener(listener);
            }
        }));
    }

    /**
     * 移除用于监听播放列表改变事件的监听器。
     *
     * @param listener 要移除的监听器
     */
    public void removeOnPlaylistChangeListener(Player.OnPlaylistChangeListener listener) {
        mPlayerStateHolder.removeOnPlaylistChangeListener(listener);
    }

    /**
     * 添加一个用于监听播放模式改变的监听器。
     * <p>
     * 如果监听器已添加，则忽略本次调用。
     *
     * @param listener 要添加的监听器
     * @see Player.OnPlayModeChangeListener
     */
    public void addOnPlayModeChangeListener(Player.OnPlayModeChangeListener listener) {
        mPlayerStateHolder.addOnPlayModeChangeListener(listener);
    }

    /**
     * 添加一个用于监听播放模式改变的监听器。
     * <p>
     * 如果监听器已添加，则忽略本次调用。
     * <p>
     * 事件监听器会在 LifecycleOwner 销毁时自动注销，以避免发生内容泄露。
     *
     * @param listener 要添加的监听器
     * @see Player.OnPlayModeChangeListener
     */
    public void addOnPlayModeChangeListener(LifecycleOwner owner,
                                            final Player.OnPlayModeChangeListener listener) {
        if (isDestroyed(owner)) {
            return;
        }

        addOnPlayModeChangeListener(listener);
        owner.getLifecycle().addObserver(new DestroyObserver(new Runnable() {
            @Override
            public void run() {
                removeOnPlayModeChangeListener(listener);
            }
        }));
    }

    /**
     * 移除一个用于监听播放模式改变的监听器。
     *
     * @param listener 要移除的事件监听器
     */
    public void removeOnPlayModeChangeListener(Player.OnPlayModeChangeListener listener) {
        mPlayerStateHolder.removeOnPlayModeChangeListener(listener);
    }

    /**
     * 监听播放器状态。
     * <p>
     * 如果监听器已添加，则忽略本次调用。
     *
     * @param listener 要添加的监听器
     */
    public void addOnPlaybackStateChangeListener(OnPlaybackStateChangeListener listener) {
        mPlayerStateHolder.addOnPlaybackStateChangeListener(listener);
    }

    /**
     * 监听播放器状态。
     * <p>
     * 如果监听器已添加，则忽略本次调用。
     * <p>
     * 事件监听器会在 LifecycleOwner 销毁时自动注销，以避免发生内容泄露。
     *
     * @param listener 要添加的监听器
     */
    public void addOnPlaybackStateChangeListener(LifecycleOwner owner,
                                                 final OnPlaybackStateChangeListener listener) {
        if (isDestroyed(owner)) {
            return;
        }

        addOnPlaybackStateChangeListener(listener);
        owner.getLifecycle().addObserver(new DestroyObserver(new Runnable() {
            @Override
            public void run() {
                removeOnPlaybackStateChangeListener(listener);
            }
        }));
    }

    /**
     * 移除播放器状态监听器。
     *
     * @param listener 要移除的监听器
     */
    public void removeOnPlaybackStateChangeListener(OnPlaybackStateChangeListener listener) {
        mPlayerStateHolder.removeOnPlaybackStateChangeListener(listener);
    }

    /**
     * 添加一个 audio session id 监听器。
     * <p>
     * 如果监听器已添加，则忽略本次调用。
     *
     * @param listener 要添加的监听器
     */
    public void addOnAudioSessionChangeListener(OnAudioSessionChangeListener listener) {
        mPlayerStateHolder.addOnAudioSessionChangeListener(listener);
    }

    /**
     * 监听播放器的 audio session id
     * <p>
     * 如果监听器已添加，则忽略本次调用。
     * <p>
     * 事件监听器会在 LifecycleOwner 销毁时自动注销，以避免发生内容泄露。
     *
     * @param listener 要添加的监听器
     */
    public void addOnAudioSessionChangeListener(LifecycleOwner owner,
                                                final OnAudioSessionChangeListener listener) {
        if (isDestroyed(owner)) {
            return;
        }

        addOnAudioSessionChangeListener(listener);
        owner.getLifecycle().addObserver(new DestroyObserver(new Runnable() {
            @Override
            public void run() {
                removeOnAudioSessionChangeListener(listener);
            }
        }));
    }

    /**
     * 移除 audio session id 监听器。
     *
     * @param listener 要移除的监听器
     */
    public void removeOnAudioSessionChangeListener(OnAudioSessionChangeListener listener) {
        mPlayerStateHolder.removeOnAudioSessionChangeListener(listener);
    }

    private boolean isDestroyed(LifecycleOwner owner) {
        return owner.getLifecycle().getCurrentState() == Lifecycle.State.DESTROYED;
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
        if (!isConnected()) {
            return;
        }

        mPlaylistEditor.insertMusicItem(position, musicItem);
    }

    @Override
    public void appendMusicItem(@NonNull MusicItem musicItem) {
        Preconditions.checkNotNull(musicItem);
        if (!isConnected()) {
            return;
        }

        mPlaylistEditor.appendMusicItem(musicItem);
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

        if (!isConnected()) {
            return;
        }

        mPlaylistEditor.moveMusicItem(fromPosition, toPosition);
    }

    @Override
    public void removeMusicItem(@NonNull MusicItem musicItem) {
        Preconditions.checkNotNull(musicItem);
        if (!isConnected()) {
            return;
        }

        mPlaylistEditor.removeMusicItem(musicItem);
    }

    @Override
    public void setNextPlay(@NonNull MusicItem musicItem) {
        Preconditions.checkNotNull(musicItem);
        if (!isConnected()) {
            return;
        }

        mPlaylistEditor.setNextPlay(musicItem);
    }

    /**
     * 用于监听播放器是否连接成功。
     */
    public interface OnConnectCallback {
        /**
         * 该方法会在连接成功或失败时调用。
         *
         * @param success 如果为 true，则表示连接成功，否则为 false
         */
        void onConnected(boolean success);
    }

    /**
     * 用于监听 PlayerClient 的连接成功与断开连接事件。
     */
    public interface OnConnectStateChangeListener {
        /**
         * 当 PlayerClient 连接成功或者断开连接时会回调该方法。
         *
         * @param connected 是否已连接
         */
        void onConnectStateChanged(boolean connected);
    }

    /**
     * 用于监听播放器播放状态的改变。
     */
    public interface OnPlaybackStateChangeListener {
        /**
         * 该方法会在播放器的播放状态发生改变时调用。
         *
         * @param playbackState 当前的播放器状态
         * @param stalled       当前播放器是否处于 stalled 状态。当缓冲区没有足够的数据继续播放时，
         *                      该参数为 true，否则为 false
         */
        void onPlaybackStateChanged(PlaybackState playbackState, boolean stalled);
    }

    /**
     * 用于监听播放器的 audio session id 改变事件。
     * <p>
     * 当切换播放的歌曲时，播放器的 audio session id 会发生改变。
     */
    public interface OnAudioSessionChangeListener {
        /**
         * 该方法会在播放器的 audio session id 发生改变时调用。
         *
         * @param audioSessionId 最新的 audio session id
         */
        void onAudioSessionChanged(int audioSessionId);
    }

    // 用于管理与同步播放器状态
    private static class PlayerStateHolder implements PlayerStateListener,
            Player.OnPlaylistChangeListener,
            Player.OnPlayModeChangeListener {
        private PlayerState mPlayerState;
        private PlayerStateHelper mPlayerStateHelper;
        private PlaylistManager mPlaylistManager;
        private boolean mNotConnected;

        private List<Player.OnPlaybackStateChangeListener> mAllPlaybackStateChangeListener;
        private List<Player.OnPrepareListener> mAllPrepareListener;
        private List<Player.OnStalledChangeListener> mAllStalledChangeListener;
        private List<OnBufferedProgressChangeListener> mAllBufferedProgressChangeListener;
        private List<Player.OnPlayingMusicItemChangeListener> mAllPlayingMusicItemChangeListener;
        private List<OnSeekCompleteListener> mAllSeekListener;
        private List<Player.OnPlaylistChangeListener> mAllPlaylistChangeListener;
        private List<Player.OnPlayModeChangeListener> mAllPlayModeChangeListener;

        private List<PlayerClient.OnPlaybackStateChangeListener> mClientAllPlaybackStateChangeListener;
        private List<PlayerClient.OnAudioSessionChangeListener> mAllAudioSessionChangeListener;

        PlayerStateHolder(PlaylistManager playlistManager) {
            mPlaylistManager = playlistManager;
            initPlayerState(new PlayerState());
            mNotConnected = true;

            mAllPlaybackStateChangeListener = new ArrayList<>();
            mAllPrepareListener = new ArrayList<>();
            mAllStalledChangeListener = new ArrayList<>();
            mAllBufferedProgressChangeListener = new ArrayList<>();
            mAllPlayingMusicItemChangeListener = new ArrayList<>();
            mAllSeekListener = new ArrayList<>();
            mAllPlaylistChangeListener = new ArrayList<>();
            mAllPlayModeChangeListener = new ArrayList<>();
            mClientAllPlaybackStateChangeListener = new ArrayList<>();
            mAllAudioSessionChangeListener = new ArrayList<>();
        }

        void setPlayerState(PlayerState playerState) {
            initPlayerState(playerState);

            if (notConnected()) {
                return;
            }

            notifyPlaylistChanged();
            notifyPlayModeChanged();
            notifyPlayingMusicItemChanged();
            notifyPrepareStateChanged();
            notifyPlaybackStateChanged();
            notifyOnBufferedProgressChanged();

            if (mPlayerState.isStalled()) {
                notifyStalledChanged();
            }
        }

        void initPlayerState(PlayerState playerState) {
            mPlayerState = playerState;
            mPlayerStateHelper = new PlayerStateHelper(mPlayerState);
        }

        boolean notConnected() {
            return mNotConnected;
        }

        void setConnected(boolean connected) {
            mNotConnected = !connected;
        }

        void addOnPlaybackStateChangeListener(Player.OnPlaybackStateChangeListener listener) {
            if (mAllPlaybackStateChangeListener.contains(listener)) {
                return;
            }

            mAllPlaybackStateChangeListener.add(listener);
            notifyPlaybackStateChanged(listener);
        }

        void removeOnPlaybackStateChangeListener(Player.OnPlaybackStateChangeListener listener) {
            mAllPlaybackStateChangeListener.remove(listener);
        }

        void addOnPrepareListener(Player.OnPrepareListener listener) {
            if (mAllPrepareListener.contains(listener)) {
                return;
            }

            mAllPrepareListener.add(listener);
            notifyPrepareStateChanged(listener);
        }

        void removeOnPrepareListener(Player.OnPrepareListener listener) {
            mAllPrepareListener.remove(listener);
        }

        void addOnStalledChangeListener(Player.OnStalledChangeListener listener) {
            if (mAllStalledChangeListener.contains(listener)) {
                return;
            }

            mAllStalledChangeListener.add(listener);
            notifyStalledChanged(listener);
        }

        void removeOnStalledChangeListener(Player.OnStalledChangeListener listener) {
            mAllStalledChangeListener.remove(listener);
        }

        void addOnBufferedProgressChangeListener(OnBufferedProgressChangeListener listener) {
            if (mAllBufferedProgressChangeListener.contains(listener)) {
                return;
            }

            mAllBufferedProgressChangeListener.add(listener);
            notifyOnBufferedProgressChanged(listener);
        }

        void removeOnBufferedProgressChangeListener(OnBufferedProgressChangeListener listener) {
            mAllBufferedProgressChangeListener.remove(listener);
        }

        void addOnPlayingMusicItemChangeListener(Player.OnPlayingMusicItemChangeListener listener) {
            if (mAllPlayingMusicItemChangeListener.contains(listener)) {
                return;
            }

            mAllPlayingMusicItemChangeListener.add(listener);
            notifyPlayingMusicItemChanged(listener);
        }

        void removeOnPlayingMusicItemChangeListener(Player.OnPlayingMusicItemChangeListener listener) {
            mAllPlayingMusicItemChangeListener.remove(listener);
        }

        void addOnSeekCompleteListener(OnSeekCompleteListener listener) {
            if (mAllSeekListener.contains(listener)) {
                return;
            }

            mAllSeekListener.add(listener);
            notifySeekComplete(listener);
        }

        void removeOnSeekCompleteListener(OnSeekCompleteListener listener) {
            mAllSeekListener.remove(listener);
        }

        void addOnPlaylistChangeListener(Player.OnPlaylistChangeListener listener) {
            if (mAllPlaylistChangeListener.contains(listener)) {
                return;
            }

            mAllPlaylistChangeListener.add(listener);
            notifyPlaylistChanged(listener);
        }

        void removeOnPlaylistChangeListener(Player.OnPlaylistChangeListener listener) {
            mAllPlaylistChangeListener.remove(listener);
        }

        void addOnPlayModeChangeListener(Player.OnPlayModeChangeListener listener) {
            if (mAllPlayModeChangeListener.contains(listener)) {
                return;
            }

            mAllPlayModeChangeListener.add(listener);
            notifyPlayModeChanged();
        }

        void removeOnPlayModeChangeListener(Player.OnPlayModeChangeListener listener) {
            mAllPlayModeChangeListener.remove(listener);
        }

        void addOnPlaybackStateChangeListener(OnPlaybackStateChangeListener listener) {
            if (mClientAllPlaybackStateChangeListener.contains(listener)) {
                return;
            }

            mClientAllPlaybackStateChangeListener.add(listener);
            notifyClientPlaybackStateChanged(listener);
        }

        void removeOnPlaybackStateChangeListener(OnPlaybackStateChangeListener listener) {
            mClientAllPlaybackStateChangeListener.remove(listener);
        }

        void addOnAudioSessionChangeListener(OnAudioSessionChangeListener listener) {
            if (mAllAudioSessionChangeListener.contains(listener)) {
                return;
            }

            mAllAudioSessionChangeListener.add(listener);
            notifyAudioSessionChanged(listener);
        }

        void removeOnAudioSessionChangeListener(OnAudioSessionChangeListener listener) {
            mAllAudioSessionChangeListener.remove(listener);
        }

        private void notifyPlaybackStateChanged(Player.OnPlaybackStateChangeListener listener) {
            if (notConnected()) {
                return;
            }

            switch (mPlayerState.getPlaybackState()) {
                case PLAYING:
                    listener.onPlay(mPlayerState.isStalled(), mPlayerState.getPlayProgress(), mPlayerState.getPlayProgressUpdateTime());
                    break;
                case PAUSED:
                    listener.onPause(mPlayerState.getPlayProgress(), mPlayerState.getPlayProgressUpdateTime());
                    break;
                case STOPPED:
                    listener.onStop();
                    break;
                case ERROR:
                    listener.onError(mPlayerState.getErrorCode(), mPlayerState.getErrorMessage());
                    break;
                default:
                    break;
            }
        }

        private void notifyPlaybackStateChanged() {
            if (notConnected()) {
                return;
            }

            for (Player.OnPlaybackStateChangeListener listener : mAllPlaybackStateChangeListener) {
                notifyPlaybackStateChanged(listener);
            }

            notifyClientPlaybackStateChanged();
        }

        private void notifyPrepareStateChanged(Player.OnPrepareListener listener) {
            if (notConnected()) {
                return;
            }

            if (mPlayerState.isPreparing()) {
                listener.onPreparing();
                return;
            }

            if (mPlayerState.isPrepared()) {
                listener.onPrepared(mPlayerState.getAudioSessionId());
            }
        }

        private void notifyPrepareStateChanged() {
            if (notConnected()) {
                return;
            }

            for (Player.OnPrepareListener listener : mAllPrepareListener) {
                notifyPrepareStateChanged(listener);
            }
        }

        private void notifyStalledChanged(Player.OnStalledChangeListener listener) {
            if (notConnected()) {
                return;
            }

            listener.onStalledChanged(mPlayerState.isStalled(),
                    mPlayerState.getPlayProgress(),
                    mPlayerState.getPlayProgressUpdateTime());
        }

        private void notifyStalledChanged() {
            if (notConnected()) {
                return;
            }

            for (Player.OnStalledChangeListener listener : mAllStalledChangeListener) {
                notifyStalledChanged(listener);
            }
        }

        private void notifyOnBufferedProgressChanged(OnBufferedProgressChangeListener listener) {
            if (notConnected()) {
                return;
            }

            listener.onBufferedProgressChanged(mPlayerState.getBufferedProgress());
        }

        private void notifyOnBufferedProgressChanged() {
            if (notConnected()) {
                return;
            }

            for (OnBufferedProgressChangeListener listener : mAllBufferedProgressChangeListener) {
                notifyOnBufferedProgressChanged(listener);
            }
        }

        private void notifyPlayingMusicItemChanged(Player.OnPlayingMusicItemChangeListener listener) {
            if (notConnected()) {
                return;
            }

            listener.onPlayingMusicItemChanged(mPlayerState.getMusicItem(), mPlayerState.getPlayPosition(), mPlayerState.getPlayProgress());
        }

        private void notifyPlayingMusicItemChanged() {
            if (notConnected()) {
                return;
            }

            for (Player.OnPlayingMusicItemChangeListener listener : mAllPlayingMusicItemChangeListener) {
                notifyPlayingMusicItemChanged(listener);
            }
        }

        private void notifySeekComplete(OnSeekCompleteListener listener) {
            if (notConnected()) {
                return;
            }

            listener.onSeekComplete(mPlayerState.getPlayProgress(), mPlayerState.getPlayProgressUpdateTime(), mPlayerState.isStalled());
        }

        private void notifySeekComplete() {
            if (notConnected()) {
                return;
            }

            for (OnSeekCompleteListener listener : mAllSeekListener) {
                notifySeekComplete(listener);
            }
        }

        private void notifyPlaylistChanged(Player.OnPlaylistChangeListener listener) {
            if (notConnected()) {
                return;
            }

            listener.onPlaylistChanged(mPlaylistManager, mPlayerState.getPlayPosition());
        }

        private void notifyPlaylistChanged() {
            if (notConnected()) {
                return;
            }

            for (Player.OnPlaylistChangeListener listener : mAllPlaylistChangeListener) {
                notifyPlaylistChanged(listener);
            }
        }

        private void notifyPlayModeChanged(Player.OnPlayModeChangeListener listener) {
            if (notConnected()) {
                return;
            }

            listener.onPlayModeChanged(mPlayerState.getPlayMode());
        }

        private void notifyPlayModeChanged() {
            if (notConnected()) {
                return;
            }

            for (Player.OnPlayModeChangeListener listener : mAllPlayModeChangeListener) {
                notifyPlayModeChanged(listener);
            }
        }

        private void notifyClientPlaybackStateChanged() {
            if (notConnected()) {
                return;
            }

            for (OnPlaybackStateChangeListener listener : mClientAllPlaybackStateChangeListener) {
                notifyClientPlaybackStateChanged(listener);
            }
        }

        private void notifyClientPlaybackStateChanged(OnPlaybackStateChangeListener listener) {
            if (notConnected()) {
                return;
            }

            listener.onPlaybackStateChanged(mPlayerState.getPlaybackState(), mPlayerState.isStalled());
        }

        private void notifyAudioSessionChanged() {
            if (notConnected()) {
                return;
            }

            for (OnAudioSessionChangeListener listener : mAllAudioSessionChangeListener) {
                notifyAudioSessionChanged(listener);
            }
        }

        private void notifyAudioSessionChanged(OnAudioSessionChangeListener listener) {
            if (notConnected()) {
                return;
            }

            listener.onAudioSessionChanged(mPlayerState.getAudioSessionId());
        }

        @Override
        public void onPreparing() {
            boolean error = mPlayerState.getPlaybackState() == PlaybackState.ERROR;
            mPlayerStateHelper.onPreparing();

            if (error) {
                notifyPlaybackStateChanged();
            }

            notifyPrepareStateChanged();
        }

        @Override
        public void onPrepared(int audioSessionId) {
            mPlayerStateHelper.onPrepared(audioSessionId);

            notifyPrepareStateChanged();
            notifyAudioSessionChanged();
        }

        @Override
        public void onPlay(boolean stalled, int playProgress, long playProgressUpdateTime) {
            mPlayerStateHelper.onPlay(stalled, playProgress, playProgressUpdateTime);

            notifyPlaybackStateChanged();
        }

        @Override
        public void onPause(int playProgress, long updateTime) {
            mPlayerStateHelper.onPaused(playProgress, updateTime);

            notifyPlaybackStateChanged();
        }

        @Override
        public void onStop() {
            mPlayerStateHelper.onStopped();

            notifyPlaybackStateChanged();
        }

        @Override
        public void onError(int errorCode, String errorMessage) {
            mPlayerStateHelper.onError(errorCode, errorMessage);

            notifyPlaybackStateChanged();
        }

        @Override
        public void onSeekComplete(int progress, long updateTime, boolean stalled) {
            mPlayerStateHelper.onSeekComplete(progress, updateTime, stalled);

            notifySeekComplete();
        }

        @Override
        public void onBufferedProgressChanged(int bufferedProgress) {
            mPlayerStateHelper.onBufferedChanged(bufferedProgress);

            notifyOnBufferedProgressChanged();
        }

        @Override
        public void onPlayingMusicItemChanged(@Nullable MusicItem musicItem, int position, int playProgress) {
            boolean error = mPlayerState.getPlaybackState() == PlaybackState.ERROR;
            mPlayerStateHelper.onPlayingMusicItemChanged(musicItem, position, playProgress);

            notifyPlayingMusicItemChanged();

            if (error) {
                notifyPlaybackStateChanged();
            }
        }

        @Override
        public void onStalledChanged(boolean stalled, int playProgress, long updateTime) {
            mPlayerStateHelper.onStalled(stalled, playProgress, updateTime);

            notifyStalledChanged();
        }

        @Override
        public void onPlaylistChanged(PlaylistManager playlistManager, int position) {
            mPlayerStateHelper.onPlaylistChanged(position);

            notifyPlaylistChanged();
        }

        @Override
        public void onPlayModeChanged(PlayMode playMode) {
            mPlayerStateHelper.onPlayModeChanged(playMode);

            notifyPlayModeChanged();
        }
    }

    private static class DestroyObserver implements LifecycleObserver {
        private Runnable mOnDestroyAction;

        DestroyObserver(@NonNull Runnable action) {
            Preconditions.checkNotNull(action);
            mOnDestroyAction = action;
        }

        @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        public void onDestroy() {
            mOnDestroyAction.run();
        }
    }
}
