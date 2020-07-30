package snow.player;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
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

import channel.helper.ChannelHelper;
import channel.helper.DispatcherUtil;
import channel.helper.pipe.CustomActionPipe;
import channel.helper.pipe.MessengerPipe;
import snow.player.media.MusicItem;
import snow.player.playlist.Playlist;
import snow.player.playlist.PlaylistManager;
import snow.player.util.ErrorUtil;

/**
 * 播放器客户端，用于向播放器发生各种控制命令。
 */
@SuppressWarnings("unused")
public class PlayerClient implements Player {
    private Context mApplicationContext;
    private Class<? extends PlayerService> mPlayerService;
    private String mToken;

    private MediaBrowserCompat mMediaBrowser;
    private MediaControllerCompat mMediaController;

    private PlayerConfig mPlayerConfig;
    private PlayerManager mPlayerManager;
    private PlayerManager.OnCommandCallback mCommandCallback;

    private OnConnectCallback mConnectCallback;

    private Player mPlayer;
    private PlaylistManagerImp mPlaylistManager;
    private PlayerStateHolder mPlayerStateHolder;

    private List<OnDisconnectListener> mAllDisconnectListener;

    private PlayerClient(Context context, Class<? extends PlayerService> playerService) {
        mApplicationContext = context.getApplicationContext();
        mPlayerService = playerService;
        mToken = generateToken();

        mPlayerConfig = new PlayerConfig(context, mToken);
        mAllDisconnectListener = new ArrayList<>();

        initMediaBrowser();
        initPlaylistManager();
        initPlayerStateHolder();
        initCommandCallback();
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

    private String generateToken() {
        return mPlayerService.getName();
    }

    private void initMediaBrowser() {
        mMediaBrowser = new MediaBrowserCompat(mApplicationContext,
                new ComponentName(mApplicationContext, mPlayerService),
                new MediaBrowserCompat.ConnectionCallback() {
                    @Override
                    public void onConnected() {
                        try {
                            mMediaController = new MediaControllerCompat(mApplicationContext, mMediaBrowser.getSessionToken());

                            PlayerClient.this.onConnected(mMediaController);

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
        mPlaylistManager = new PlaylistManagerImp(mApplicationContext, mToken);
        mPlaylistManager.setOnModifyPlaylistListener(this);
    }

    private void initPlayerStateHolder() {
        mPlayerStateHolder = new PlayerStateHolder(mPlaylistManager, mPlayerConfig);
    }

    private void initCommandCallback() {
        mCommandCallback = new PlayerManager.OnCommandCallback() {
            @Override
            public void onShutdown() {
                disconnect();
            }

            @Override
            public void syncPlayerState(PlayerState playerState) {
                mPlayerStateHolder.setPlayerState(playerState);
            }
        };
    }

    private void onConnected(MediaControllerCompat mediaController) {
        setConnected(true);

        // send custom action to MediaSession
        CustomActionPipe customActionPipe = new CustomActionPipe(mediaController.getTransportControls());
        iniPlayer(customActionPipe);
        initPlayerManager(customActionPipe);
    }

    private void iniPlayer(CustomActionPipe customActionPipe) {
        mPlayer = ChannelHelper.newEmitter(Player.class, customActionPipe);
    }

    private void initPlayerManager(CustomActionPipe customActionPipe) {
        // listen player state change and other command
        MessengerPipe listenerPipe = new MessengerPipe(DispatcherUtil.merge(
                ChannelHelper.newDispatcher(PlayerManager.OnCommandCallback.class, mCommandCallback),
                ChannelHelper.newDispatcher(PlayerStateListener.class, getPlayerStateListener())
        ));

        mPlayerManager = ChannelHelper.newEmitter(PlayerManager.class, customActionPipe);
        mPlayerManager.registerPlayerStateListener(mToken, listenerPipe.getBinder());
    }

    private void onDisconnected() {
        setConnected(false);

        for (OnDisconnectListener listener : mAllDisconnectListener) {
            listener.onDisconnected();
        }

        if (isConnected()) {
            mPlayerManager.unregisterPlayerStateListener(mToken);
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
        mMediaBrowser.disconnect();
    }

    /**
     * 播放器释放已连接。
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
    public void addOnDisconnectListener(OnDisconnectListener listener) {
        if (mAllDisconnectListener.contains(listener)) {
            return;
        }

        mAllDisconnectListener.add(listener);
        if (notConnected()) {
            listener.onDisconnected();
        }
    }

    /**
     * 添加一个监听器用来监听 PlayerClient 连接断开事件。
     *
     * @param owner    LifecycleOwner 对象。监听器会在该 LifecycleOwner 对象销毁时自动注销，避免内存泄露
     * @param listener 要添加的事件监听器，如果已添加，则会忽略本次调用
     */
    public void addOnDisconnectListener(LifecycleOwner owner, final OnDisconnectListener listener) {
        if (isDestroyed(owner)) {
            return;
        }

        addOnDisconnectListener(listener);
        owner.getLifecycle().addObserver(new DestroyObserver(new Runnable() {
            @Override
            public void run() {
                removeOnDisconnectListener(listener);
            }
        }));
    }

    /**
     * 移除已添加的 OnDisconnectListener 监听器对象。
     *
     * @param listener 要移除的监听器
     */
    public void removeOnDisconnectListener(OnDisconnectListener listener) {
        mAllDisconnectListener.remove(listener);
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

    private PlayerStateListener getPlayerStateListener() {
        return mPlayerStateHolder;
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
     */
    public void setPlaylist(@NonNull Playlist playlist, int position, boolean play) {
        Preconditions.checkNotNull(playlist);
        if (!isConnected()) {
            return;
        }

        mPlaylistManager.setPlaylist(playlist, position, play);
    }

    /**
     * 获取当前的播放队列。
     *
     * @param callback 回调接口，该接口的回调方法会在主线程中调用
     */
    public void getPlaylistAsync(@NonNull PlaylistManager.Callback callback) {
        Preconditions.checkNotNull(callback);
        mPlaylistManager.getPlaylistAsync(callback);
    }

    /**
     * 设置指定歌曲 “下一次播放”。
     *
     * @param musicItem 要设定为 “下一次播放” 的歌曲，如果歌曲已存在播放列表中，则会移动到 “下一曲播放” 的位
     *                  置，如果歌曲不存在，则插入到 “下一曲播放” 位置
     */
    public void setNextPlay(@NonNull MusicItem musicItem) {
        Preconditions.checkNotNull(musicItem);
        if (!isConnected()) {
            return;
        }

        if (musicItem.equals(getPlayingMusicItem())) {
            return;
        }

        mPlaylistManager.insertMusicItem(getPlayPosition() + 1, musicItem);
    }

    /**
     * 移动列表中指定歌曲的位置。
     *
     * @param fromPosition 要移动的歌曲的位置
     * @param toPosition   歌曲要移动到的位置
     */
    public void moveMusicItem(int fromPosition, int toPosition) {
        if (!isConnected()) {
            return;
        }

        mPlaylistManager.moveMusicItem(fromPosition, toPosition);
    }

    /**
     * 从播放列表中移除指定歌曲。
     *
     * @param musicItem 要移除的歌曲
     */
    public void removeMusicItem(@NonNull MusicItem musicItem) {
        Preconditions.checkNotNull(musicItem);
        if (!isConnected()) {
            return;
        }

        mPlaylistManager.removeMusicItem(musicItem);
    }

    /**
     * 获取播放列表中包含的歌曲的数量。
     */
    public int getPlaylistSize() {
        return mPlaylistManager.getPlaylistSize();
    }

    /**
     * 获取播放进度。
     *
     * @return 播放进度
     */
    public int getPlayProgress() {
        return mPlayerStateHolder.mPlayerState.getPlayProgress();
    }

    /**
     * 获取播放进度的更新时间。
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
     * 播放器是否发生了错误。
     */
    public boolean isError() {
        return getErrorCode() != ErrorUtil.NO_ERROR;
    }

    /**
     * 获取错误码。
     *
     * @return 错误码。如果播放器没有发生错误，则返回 {@link ErrorUtil#NO_ERROR}
     * @see ErrorUtil
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
        return mPlayerStateHolder.mPlayerState.getPosition();
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
     * 播放或暂停播放列表中指定索引处的音乐。
     * <p>
     * 该方法只在连接到播放器后（{@link #isConnected()} 返回 true）才有效。
     *
     * @param position 目标位置。
     */
    @Override
    public void playPause(int position) {
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
     * 调整音乐播放进度。
     * <p>
     * 该方法只在连接到播放器后（{@link #isConnected()} 返回 true）才有效。
     *
     * @param progress 要调整到的播放进度
     */
    @Override
    public void seekTo(int progress) {
        if (notConnected()) {
            return;
        }

        mPlayer.seekTo(progress);
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

    @Override
    public void onNewPlaylist(int position, boolean play) {
        if (notConnected()) {
            return;
        }

        mPlayer.onNewPlaylist(position, play);
    }

    @Override
    public void onMusicItemMoved(int fromPosition, int toPosition) {
        if (notConnected()) {
            return;
        }

        mPlayer.onMusicItemMoved(fromPosition, toPosition);
    }

    @Override
    public void onMusicItemInserted(int position, MusicItem musicItem) {
        if (notConnected()) {
            return;
        }

        mPlayer.onMusicItemInserted(position, musicItem);
    }

    @Override
    public void onMusicItemRemoved(MusicItem musicItem) {
        if (notConnected()) {
            return;
        }

        mPlayer.onMusicItemRemoved(musicItem);
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
     * @see OnBufferedProgressChangeListener
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
     * @see OnSeekCompleteListener
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
     * @see OnSeekCompleteListener
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
     * 添加一个监听播放列表播放位置改变事件的监听器。
     * <p>
     * 如果监听器已添加，则忽略本次调用。
     *
     * @param listener 要添加的监听器
     * @see Player.OnPositionChangeListener
     */
    public void addOnPositionChangeListener(Player.OnPositionChangeListener listener) {
        mPlayerStateHolder.addOnPositionChangeListener(listener);
    }

    /**
     * 添加一个监听播放列表播放位置改变事件的监听器。
     * <p>
     * 如果监听器已添加，则忽略本次调用。
     * <p>
     * 事件监听器会在 LifecycleOwner 销毁时自动注销，以避免发生内容泄露。
     *
     * @param listener 要添加的监听器
     * @see Player.OnPositionChangeListener
     */
    public void addOnPositionChangeListener(LifecycleOwner owner,
                                            final Player.OnPositionChangeListener listener) {
        if (isDestroyed(owner)) {
            return;
        }

        addOnPositionChangeListener(listener);
        owner.getLifecycle().addObserver(new DestroyObserver(new Runnable() {
            @Override
            public void run() {
                removeOnPositionChangeListener(listener);
            }
        }));
    }

    /**
     * 移除一个监听播放列表播放位置改变事件的监听器。
     *
     * @param listener 要移除的监听器
     */
    public void removeOnPositionChangeListener(Player.OnPositionChangeListener listener) {
        mPlayerStateHolder.removeOnPositionChangeListener(listener);
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
     * 用于监听播放器连接状态的回调接口。
     */
    public interface OnConnectCallback {
        /**
         * 该方法会在连接成功或失败时调用。
         *
         * @param success 是否连接成功，如果为 true，则表示连接成功，否则为 false
         */
        void onConnected(boolean success);
    }

    public interface OnDisconnectListener {
        void onDisconnected();
    }

    /**
     * 用于具体播放器播放状态的改变。
     */
    public interface OnPlaybackStateChangeListener {
        /**
         * 该方法会在播放器的播放状态发生改变时调用。
         *
         * @param playbackState 当前的播放器状态
         */
        void onPlaybackStateChanged(PlaybackState playbackState);
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

    private static class PlaylistManagerImp extends PlaylistManager {
        protected PlaylistManagerImp(Context context, String playlistId) {
            super(context, playlistId);
        }

        @Override
        protected void setEditable(boolean editable) {
            super.setEditable(editable);
        }
    }

    // 用于管理与同步播放器状态
    private static class PlayerStateHolder implements PlayerStateListener,
            Player.OnPositionChangeListener,
            Player.OnPlaylistChangeListener,
            Player.OnPlayModeChangeListener {
        private PlayerState mPlayerState;
        private PlaylistManager mPlaylistManager;
        private boolean mNotConnected;

        private List<Player.OnPlaybackStateChangeListener> mAllPlaybackStateChangeListener;
        private List<Player.OnStalledChangeListener> mAllStalledChangeListener;
        private List<OnBufferedProgressChangeListener> mAllBufferedProgressChangeListener;
        private List<Player.OnPlayingMusicItemChangeListener> mAllPlayingMusicItemChangeListener;
        private List<OnSeekCompleteListener> mAllSeekListener;
        private List<Player.OnPlaylistChangeListener> mAllPlaylistChangeListener;
        private List<Player.OnPlayModeChangeListener> mAllPlayModeChangeListener;
        private List<Player.OnPositionChangeListener> mAllPositionChangeListener;

        private List<PlayerClient.OnPlaybackStateChangeListener> mClientAllPlaybackStateChangeListener;
        private List<PlayerClient.OnAudioSessionChangeListener> mAllAudioSessionChangeListener;

        PlayerStateHolder(PlaylistManager playlistManager, PlayerConfig playerConfig) {
            mPlaylistManager = playlistManager;
            mPlayerState = new PlayerState();
            mNotConnected = true;

            mAllPlaybackStateChangeListener = new ArrayList<>();
            mAllStalledChangeListener = new ArrayList<>();
            mAllBufferedProgressChangeListener = new ArrayList<>();
            mAllPlayingMusicItemChangeListener = new ArrayList<>();
            mAllSeekListener = new ArrayList<>();
            mAllPlaylistChangeListener = new ArrayList<>();
            mAllPlayModeChangeListener = new ArrayList<>();
            mAllPositionChangeListener = new ArrayList<>();
            mClientAllPlaybackStateChangeListener = new ArrayList<>();
            mAllAudioSessionChangeListener = new ArrayList<>();
        }

        void setPlayerState(PlayerState playerState) {
            mPlayerState = playerState;

            if (notConnected()) {
                return;
            }

            notifyPlaylistChanged();
            notifyPlayModeChanged();
            notifyPlayingMusicItemChanged();
            notifyPlaybackStateChanged();
            notifyOnBufferedProgressChanged();

            if (mPlayerState.isStalled()) {
                notifyStalledChanged();
            }
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

        void addOnPositionChangeListener(Player.OnPositionChangeListener listener) {
            if (mAllPositionChangeListener.contains(listener)) {
                return;
            }

            mAllPositionChangeListener.add(listener);
            notifyPositionChanged(listener);
        }

        void removeOnPositionChangeListener(Player.OnPositionChangeListener listener) {
            mAllPositionChangeListener.remove(listener);
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
                case PREPARING:
                    listener.onPreparing();
                    break;
                case PREPARED:
                    listener.onPrepared(mPlayerState.getAudioSessionId());
                    break;
                case PLAYING:
                    listener.onPlay(mPlayerState.getPlayProgress(), mPlayerState.getPlayProgressUpdateTime());
                    break;
                case PAUSED:
                    listener.onPause();
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

        private void notifyStalledChanged(Player.OnStalledChangeListener listener) {
            if (notConnected()) {
                return;
            }

            listener.onStalledChanged(mPlayerState.isStalled());
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

            listener.onPlayingMusicItemChanged(mPlayerState.getMusicItem(), mPlayerState.getPlayProgress());
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

            listener.onSeekComplete(mPlayerState.getPlayProgress(), mPlayerState.getPlayProgressUpdateTime());
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

            listener.onPlaylistChanged(mPlaylistManager, mPlayerState.getPosition());
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

        private void notifyPositionChanged(Player.OnPositionChangeListener listener) {
            if (notConnected()) {
                return;
            }

            listener.onPositionChanged(mPlayerState.getPosition());
        }

        private void notifyPositionChanged() {
            if (notConnected()) {
                return;
            }

            for (Player.OnPositionChangeListener listener : mAllPositionChangeListener) {
                notifyPositionChanged(listener);
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

            listener.onPlaybackStateChanged(mPlayerState.getPlaybackState());
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
            mPlayerState.setPlaybackState(PlaybackState.PREPARING);

            notifyPlaybackStateChanged();
        }

        @Override
        public void onPrepared(int audioSessionId) {
            mPlayerState.setPlaybackState(PlaybackState.PREPARED);
            mPlayerState.setAudioSessionId(audioSessionId);

            notifyPlaybackStateChanged();
            notifyAudioSessionChanged();
        }

        @Override
        public void onPlay(int playProgress, long playProgressUpdateTime) {
            mPlayerState.setPlaybackState(PlaybackState.PLAYING);
            mPlayerState.setPlayProgress(playProgress);
            mPlayerState.setPlayProgressUpdateTime(playProgressUpdateTime);

            notifyPlaybackStateChanged();
        }

        @Override
        public void onPause() {
            mPlayerState.setPlaybackState(PlaybackState.PAUSED);

            notifyPlaybackStateChanged();
        }

        @Override
        public void onStop() {
            mPlayerState.setPlaybackState(PlaybackState.STOPPED);
            resetPlayProgress();

            notifyPlaybackStateChanged();
        }

        private void resetPlayProgress() {
            mPlayerState.setPlayProgress(0);
            mPlayerState.setPlayProgressUpdateTime(System.currentTimeMillis());
        }

        @Override
        public void onError(int errorCode, String errorMessage) {
            resetPlayProgress();
            mPlayerState.setPlaybackState(PlaybackState.ERROR);
            mPlayerState.setErrorCode(errorCode);
            mPlayerState.setErrorMessage(errorMessage);

            notifyPlaybackStateChanged();
        }

        @Override
        public void onSeekComplete(int progress, long updateTime) {
            mPlayerState.setPlayProgress(progress);
            mPlayerState.setPlayProgressUpdateTime(updateTime);

            notifySeekComplete();
        }

        @Override
        public void onBufferedProgressChanged(int bufferedProgress) {
            mPlayerState.setBufferedProgress(bufferedProgress);

            notifyOnBufferedProgressChanged();
        }

        @Override
        public void onPlayingMusicItemChanged(@Nullable MusicItem musicItem, int playProgress) {
            mPlayerState.setMusicItem(musicItem);
            mPlayerState.setPlayProgress(playProgress);

            notifyPlayingMusicItemChanged();
        }

        @Override
        public void onStalledChanged(boolean stalled) {
            mPlayerState.setStalled(stalled);

            notifyStalledChanged();
        }

        @Override
        public void onPlaylistChanged(PlaylistManager playlistManager, int position) {
            mPlayerState.setPosition(position);

            notifyPlaylistChanged();
        }

        @Override
        public void onPlayModeChanged(PlayMode playMode) {
            mPlayerState.setPlayMode(playMode);

            notifyPlayModeChanged();
        }

        @Override
        public void onPositionChanged(int position) {
            mPlayerState.setPosition(position);

            notifyPositionChanged();
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
