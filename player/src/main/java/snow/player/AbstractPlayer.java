package snow.player;

import android.Manifest;
import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.content.pm.PackageManager;
import android.media.AudioManager;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.os.PowerManager;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.google.common.base.Preconditions;

import java.util.HashMap;

import media.helper.AudioFocusHelper;
import media.helper.BecomeNoiseHelper;
import snow.player.media.MusicItem;
import snow.player.media.MusicPlayer;
import snow.player.util.NetworkUtil;

/**
 * 该类实现了 {@link Player} 接口，并实现了其大部分功能。
 *
 * @param <T> {@link PlayerStateListener} 播放器状态监听器。
 */
public abstract class AbstractPlayer<T extends PlayerStateListener> implements Player {
    private static final int FORWARD_STEP = 15_000;     // 15 秒, 单位：毫秒 ms

    private Context mApplicationContext;
    private PlayerConfig mPlayerConfig;
    private PlayerState mPlayerState;
    private HashMap<String, T> mStateListenerMap;

    private MusicPlayer mMusicPlayer;

    private MusicPlayer.OnPreparedListener mOnPreparedListener;
    private MusicPlayer.OnCompletionListener mOnCompletionListener;
    private MusicPlayer.OnSeekCompleteListener mOnSeekCompleteListener;
    private MusicPlayer.OnStalledListener mOnStalledListener;
    private MusicPlayer.OnBufferingUpdateListener mOnBufferingUpdateListener;
    private MusicPlayer.OnErrorListener mOnErrorListener;

    private volatile boolean mPreparing;
    private volatile boolean mPrepared;

    private volatile Runnable mPreparedAction;
    private volatile Runnable mSeekCompleteAction;

    private AudioFocusHelper mAudioFocusHelper;
    private BecomeNoiseHelper mBecomeNoiseHelper;

    private NetworkUtil mNetworkUtil;

    private boolean mEnabled;

    /**
     * @param context      {@link Context} 对象，不能为 null
     * @param playerConfig {@link PlayerConfig} 对象，保存了播放器的初始配置信息，不能为 null
     * @param playerState  {@link PlayerState} 对象，保存了播放器的初始状态，不能为 null
     * @param enabled      是否启用当前播放器，如果为 {@code false}，则当前播放器不会响应任何操作
     */
    public AbstractPlayer(@NonNull Context context,
                          @NonNull PlayerConfig playerConfig,
                          @NonNull PlayerState playerState,
                          boolean enabled) {
        Preconditions.checkNotNull(context);
        Preconditions.checkNotNull(playerConfig);
        Preconditions.checkNotNull(playerState);

        mApplicationContext = context.getApplicationContext();
        mPlayerConfig = playerConfig;
        mPlayerState = playerState;
        mEnabled = enabled;

        mStateListenerMap = new HashMap<>();

        initAllListener();
        initAllHelper();

        mNetworkUtil.subscribeNetworkState();
    }

    /**
     * 当前的播放模式是否是单曲循环。
     *
     * @see snow.player.playlist.PlaylistPlayer.PlayMode
     */
    public abstract boolean isLooping();

    /**
     * 查询具有 soundQuality 音质的 MusicItem 表示的的音乐是否已被缓存。
     * <p>
     * 该方法会在异步线程中被调用。
     *
     * @param musicItem    要查询的 MusicItem 对象
     * @param soundQuality 音乐的音质
     * @return 如果已被缓存，则返回 true，否则返回 false
     */
    protected abstract boolean isCached(MusicItem musicItem, SoundQuality soundQuality);

    /**
     * 该方法会在创建 MusicPlayer 对象时调用。
     * <p>
     * 你可以重写该方法来返回你自己的 MusicPlayer 实现。
     */
    @NonNull
    protected abstract MusicPlayer onCreateMusicPlayer(Context context);

    /**
     * 该方法会在准备播放器时调用。
     * <p>
     * 该方法会在主线程上执行，如果准备操作是个耗时操作，你应该再异步线程中执行它。如果你还需要在异步线程中执行
     * 其他操作（如：获取音乐的 URI），那么应该在调用 {@link MusicPlayer#prepare(Uri)} 方法之前使用
     * {@link MusicPlayer#isInvalid()} 方法检查播放器是否已失效，如果 {@link MusicPlayer#isInvalid()}
     * 方法返回 true，则此时不应该再调用 {@link MusicPlayer} 的任何方法。
     *
     * @param musicPlayer  当前播放器对象
     * @param musicItem    要播放的音乐
     * @param soundQuality 要播放的音乐的首选音质
     * @throws Exception 如果该方法抛出任何异常，则本次播放将会自动停止
     */
    protected abstract void onPrepareMusicPlayer(MusicPlayer musicPlayer,
                                                 MusicItem musicItem,
                                                 SoundQuality soundQuality) throws Exception;

    /**
     * 对象指定的 audio session id 应用音频特效。
     *
     * @param audioSessionId 当前正在播放的音乐的 audio session id。如果为 0，则可以忽略。
     */
    protected void attachAudioEffect(int audioSessionId) {
    }

    /**
     * 取消当前的音频特效。
     */
    protected void detachAudioEffect() {
    }

    /**
     * 该方法会在开始准备音乐播放器时调用。
     */
    protected void onPreparing() {
    }

    /**
     * 该方法会在音乐播放器准备完毕后调用。
     *
     * @param audioSessionId 当前正准备播放的音乐的 audio session id。
     */
    protected void onPrepared(int audioSessionId) {
    }

    /**
     * 该方法会在开始播放时调用。
     *
     * @param progress   当前的播放进度。
     * @param updateTime 播放进度的更新时间。
     */
    protected void onPlaying(int progress, long updateTime) {
    }

    /**
     * 该方法会在暂停播放时调用。
     */
    protected void onPaused() {
    }

    /**
     * 该方法会在 stalled 暂停改变时调用。
     * <p>
     * 你可以根据该 stalled 参数的值来显示或隐藏缓冲进度条。如果缓冲区没有足够的数据支撑继续播放时，则该参数为
     * true，当缓冲区缓存了足够的数据可以继续播放时，该参数为 false。
     *
     * @param stalled 如果缓冲区没有足够的数据继续播放时，则该参数为 true，当缓冲区缓存了足够的数据可以继续
     *                播放时，该参数为 false。
     */
    protected void onStalledChanged(boolean stalled) {
    }

    /**
     * 该方法会在停止播放时调用。
     */
    protected void onStopped() {
    }

    /**
     * 该方法会在错误发生时调用。
     *
     * @param errorCode    错误码
     * @param errorMessage 错误信息
     * @see Player.Error
     */
    protected void onError(int errorCode, String errorMessage) {
    }

    /**
     * 该方法会在播放完毕时调用。
     *
     * @param musicItem 本次播放的 MusicItem 对象。
     */
    protected void onPlayComplete(MusicItem musicItem) {
    }

    /**
     * 该方法会在申请音频焦点时调用。
     *
     * @param success 音频焦点申请成功时为 true，否则为 false。
     */
    protected void onRequestAudioFocus(boolean success) {
    }

    /**
     * 该方法会在丢失音频焦点时调用。
     */
    protected void onLossAudioFocus() {
    }

    /**
     * 该方法会在播放器释放时调用。
     * <p>
     * 你可以重写该方法来释放占用的资源。
     */
    protected void onRelease() {
    }

    /**
     * 该方法会在当前播放的 MusicItem 对象改变时调用。
     *
     * @param musicItem 本次要播放的 MusicItem 对象（可能为 null）。
     */
    protected void onPlayingMusicItemChanged(@Nullable MusicItem musicItem) {
    }

    /**
     * 设置是否启用当前播放器。
     *
     * @param enabled 是否启用当前播放器，为 {@code true} 时启用当前播放器，为 {@code false} 时不启用
     *                当前播放器，此时如果播放器正在播放，会立即暂停并释放掉内部的 {@link MusicPlayer}
     */
    public final void setEnabled(boolean enabled) {
        if (mEnabled == enabled) {
            return;
        }

        mEnabled = enabled;

        if (!mEnabled) {
            pause();
            releaseMusicPlayer();
        }
    }

    /**
     * 当前播放器释放已启用。
     */
    protected final boolean isEnabled() {
        return mEnabled;
    }

    /**
     * 释放播放器所占用的资源。注意！调用该方法后，就不允许在使用当前 Player 对象了，否则会导致不可预见的错误。
     */
    public final void release() {
        releaseMusicPlayer();

        mStateListenerMap.clear();

        mAudioFocusHelper.abandonAudioFocus();
        mBecomeNoiseHelper.unregisterBecomeNoiseReceiver();
        mNetworkUtil.unsubscribeNetworkState();

        mAudioFocusHelper = null;
        mBecomeNoiseHelper = null;
        mNetworkUtil = null;

        onRelease();
    }

    /**
     * 当前正在播放的音乐。
     *
     * @return 如果没有正在播放的音乐，则返回 null
     */
    @Nullable
    protected final MusicItem getMusicItem() {
        return mPlayerState.getMusicItem();
    }

    /**
     * 准备当前播放器所持有的 {@link MusicItem} 对象。
     *
     * @param preparedAction 在音乐播放器准备完成后要执行的操作
     */
    protected final void prepareMusicPlayer(@Nullable Runnable preparedAction) {
        releaseMusicPlayer();

        if (!isEnabled()) {
            return;
        }

        notifyBufferingPercentChanged(0, System.currentTimeMillis());

        MusicItem musicItem = mPlayerState.getMusicItem();
        if (musicItem == null) {
            return;
        }

        if (mPlayerConfig.isOnlyWifiNetwork() && !isWiFiNetwork()) {
            onError(Error.ONLY_WIFI_NETWORK, Error.getErrorMessage(mApplicationContext, Error.ONLY_WIFI_NETWORK));
            return;
        }

        try {
            mMusicPlayer = new MusicPlayerWrapper(mApplicationContext, onCreateMusicPlayer(mApplicationContext));
            attachListeners(mMusicPlayer);

            mPreparedAction = preparedAction;
            notifyPreparing();
            onPrepareMusicPlayer(mMusicPlayer, getMusicItem(), mPlayerConfig.getSoundQuality());
        } catch (Exception e) {
            e.printStackTrace();
            notifyError(Error.DATA_LOAD_FAILED, Error.getErrorMessage(mApplicationContext, Error.DATA_LOAD_FAILED));
        }
    }

    private boolean isWiFiNetwork() {
        return mNetworkUtil.isWifiNetwork();
    }

    private void initAllListener() {
        mOnPreparedListener = new MusicPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(MusicPlayer mp) {
                mp.setLooping(isLooping());

                if (mPlayerConfig.isAudioEffectEnabled()) {
                    attachAudioEffect(mp.getAudioSessionId());
                }

                notifyPrepared(mp.getAudioSessionId());

                if (mPlayerState.getPlayProgress() > 0) {
                    seekTo(mPlayerState.getPlayProgress(), mPreparedAction);
                    return;
                }

                if (mPreparedAction != null) {
                    mPreparedAction.run();
                    mPreparedAction = null;
                }
            }
        };

        mOnCompletionListener = new MusicPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MusicPlayer mp) {
                onPlayComplete(mPlayerState.getMusicItem());
            }
        };

        mOnSeekCompleteListener = new MusicPlayer.OnSeekCompleteListener() {
            @Override
            public void onSeekComplete(MusicPlayer mp) {
                notifySeekComplete(mp.getProgress());

                if (mSeekCompleteAction != null) {
                    mSeekCompleteAction.run();
                    mSeekCompleteAction = null;
                }
            }
        };

        mOnStalledListener = new MusicPlayer.OnStalledListener() {
            @Override
            public void onStalled(boolean stalled) {
                notifyStalled(stalled);
            }
        };

        mOnBufferingUpdateListener = new MusicPlayer.OnBufferingUpdateListener() {
            @Override
            public void onBufferingUpdate(MusicPlayer mp, int percent) {
                notifyBufferingPercentChanged(percent, System.currentTimeMillis());
            }
        };

        mOnErrorListener = new MusicPlayer.OnErrorListener() {
            @Override
            public void onError(MusicPlayer mp, int errorCode) {
                Log.e("MusicPlayer", "errorCode:" + errorCode);

                notifyError(Error.PLAYER_ERROR,
                        Error.getErrorMessage(mApplicationContext, Error.PLAYER_ERROR));
            }
        };
    }

    private void initAllHelper() {
        mAudioFocusHelper = new AudioFocusHelper(mApplicationContext, new AudioFocusHelper.OnAudioFocusChangeListener() {
            private boolean playing;

            @Override
            public void onLoss() {
                if (mPlayerConfig.isIgnoreLossAudioFocus()) {
                    return;
                }

                pause();
                onLossAudioFocus();
            }

            @Override
            public void onLossTransient() {
                if (mPlayerConfig.isIgnoreLossAudioFocus()) {
                    return;
                }

                playing = isPlaying();
                pause();
                onLossAudioFocus();
            }

            @Override
            public void onLossTransientCanDuck() {
                if (mPlayerConfig.isIgnoreLossAudioFocus()) {
                    return;
                }

                playing = isPlaying();
                if (playing) {
                    mMusicPlayer.quiet();
                }
            }

            @Override
            public void onGain(boolean lossTransient, boolean lossTransientCanDuck) {
                if (mPlayerConfig.isIgnoreLossAudioFocus()) {
                    return;
                }

                if (!playing) {
                    return;
                }

                if (lossTransient) {
                    play();
                    return;
                }

                if (lossTransientCanDuck && isPlaying()) {
                    mMusicPlayer.dismissQuiet();
                }
            }
        });

        mBecomeNoiseHelper = new BecomeNoiseHelper(mApplicationContext, new BecomeNoiseHelper.OnBecomeNoiseListener() {
            @Override
            public void onBecomeNoise() {
                pause();
            }
        });

        mNetworkUtil = NetworkUtil.newInstance(mApplicationContext, new NetworkUtil.OnNetworkStateChangeListener() {
            @Override
            public void onNetworkStateChanged(boolean connected, boolean wifiNetwork) {
                if (!isPrepared()) {
                    return;
                }

                checkNetworkType(mPlayerConfig.isOnlyWifiNetwork(), wifiNetwork);
            }
        });
    }

    private void attachListeners(MusicPlayer musicPlayer) {
        musicPlayer.setOnPreparedListener(mOnPreparedListener);
        musicPlayer.setOnCompletionListener(mOnCompletionListener);
        musicPlayer.setOnSeekCompleteListener(mOnSeekCompleteListener);
        musicPlayer.setOnStalledListener(mOnStalledListener);
        musicPlayer.setOnBufferingUpdateListener(mOnBufferingUpdateListener);
        musicPlayer.setOnErrorListener(mOnErrorListener);
    }

    /**
     * 释放当前播放器所持有的 {@link MusicPlayer} 对象。
     */
    protected final void releaseMusicPlayer() {
        if (mMusicPlayer != null) {
            mMusicPlayer.release();
            mMusicPlayer = null;
        }

        mPreparing = false;
        mPrepared = false;

        mPreparedAction = null;
        mSeekCompleteAction = null;
    }

    /**
     * 播放器释放处于准备中状态。
     *
     * @return 当播放器处于准备中状态时返回 true，否则返回false。
     */
    protected final boolean isPreparing() {
        return mPreparing;
    }

    /**
     * 缓存区是否没有足够的数据继续播放。
     */
    protected final boolean isStalled() {
        return mPlayerState.isStalled();
    }

    /**
     * 播放器释放已经准备完毕。
     *
     * @return 当播放器已经准备完毕时返回 true，否则返回 false。
     */
    protected final boolean isPrepared() {
        return mPrepared;
    }

    /**
     * 是否正在播放。
     *
     * @return 当正在播放时返回 true，否则返回 false。
     */
    protected final boolean isPlaying() {
        return isPrepared() && mMusicPlayer.isPlaying();
    }

    /**
     * 是否已暂停。
     */
    protected final boolean isPaused() {
        return mPlayerState.getPlaybackState() == PlaybackState.PAUSED;
    }

    /**
     * 是否已停止。
     */
    protected final boolean isStopped() {
        return mPlayerState.getPlaybackState() == PlaybackState.STOPPED;
    }

    /**
     * 是否发生了错误。
     */
    protected final boolean isError() {
        return mPlayerState.getPlaybackState() == PlaybackState.ERROR;
    }

    /**
     * 获取当前正在播放的应用的 audio session id。
     * <p>
     * 如果当前没有播放任何音乐，或者播放器还没有准备完毕（{@link #isPrepared()} 返回了 false），则该方法会
     * 返回 0。
     *
     * @return 当前正在播放的应用的 audio session id。
     */
    protected final int getAudioSessionId() {
        if (isPrepared()) {
            return mMusicPlayer.getAudioSessionId();
        }

        return 0;
    }

    /**
     * 更新播放进度。
     *
     * @param progress   播放进度
     * @param updateTime 播放进度更新时间
     */
    protected final void updatePlayProgress(int progress, long updateTime) {
        mPlayerState.setPlayProgress(progress);
        mPlayerState.setPlayProgressUpdateTime(updateTime);
    }

    /**
     * 添加播放器状态监听器。
     *
     * @param token    监听器的 token（不能为 null），请务必保证该参数的唯一性。
     * @param listener 监听器（不能为 null）。
     */
    public final void addStateListener(@NonNull String token, @NonNull T listener) {
        Preconditions.checkNotNull(token);
        Preconditions.checkNotNull(listener);

        mStateListenerMap.put(token, listener);
    }

    /**
     * 移除播放器状态监听器。
     *
     * @param token 监听器的 token（不能为 null）
     */
    public final void removeStateListener(@NonNull String token) {
        Preconditions.checkNotNull(token);

        mStateListenerMap.remove(token);
    }

    /**
     * 获取所有已注册的播放器状态监听器。
     *
     * @return 所有已注册的播放器状态监听器
     */
    protected final HashMap<String, T> getAllStateListener() {
        return mStateListenerMap;
    }

    private void notifyPreparing() {
        mPlayerState.setPlaybackState(PlaybackState.PREPARING);

        mPreparing = true;
        mPrepared = false;

        onPreparing();

        for (String key : mStateListenerMap.keySet()) {
            PlayerStateListener listener = mStateListenerMap.get(key);
            if (listener != null) {
                listener.onPreparing();
            }
        }
    }

    private void notifyPrepared(int audioSessionId) {
        mPlayerState.setPlaybackState(PlaybackState.PREPARED);
        mPlayerState.setAudioSessionId(audioSessionId);

        mPreparing = false;
        mPrepared = true;

        onPrepared(audioSessionId);

        for (String key : mStateListenerMap.keySet()) {
            PlayerStateListener listener = mStateListenerMap.get(key);
            if (listener != null) {
                listener.onPrepared(audioSessionId);
            }
        }
    }

    private void notifyPlaying(int progress, long updateTime) {
        mPlayerState.setPlaybackState(PlaybackState.PLAYING);
        updatePlayProgress(progress, updateTime);

        int result = mAudioFocusHelper.requestAudioFocus(AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN);
        onRequestAudioFocus(result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED);
        mBecomeNoiseHelper.registerBecomeNoiseReceiver();

        onPlaying(progress, updateTime);

        for (String key : mStateListenerMap.keySet()) {
            PlayerStateListener listener = mStateListenerMap.get(key);
            if (listener != null) {
                listener.onPlay(progress, updateTime);
            }
        }
    }

    private void notifyPaused() {
        mPlayerState.setPlaybackState(PlaybackState.PAUSED);

        if (mMusicPlayer != null) {
            updatePlayProgress(mMusicPlayer.getProgress(), System.currentTimeMillis());
        }

        mAudioFocusHelper.abandonAudioFocus();
        mBecomeNoiseHelper.unregisterBecomeNoiseReceiver();

        onPaused();

        for (String key : mStateListenerMap.keySet()) {
            PlayerStateListener listener = mStateListenerMap.get(key);
            if (listener != null) {
                listener.onPause();
            }
        }
    }

    private void notifyStopped() {
        mPlayerState.setPlaybackState(PlaybackState.STOPPED);
        updatePlayProgress(0, System.currentTimeMillis());

        mAudioFocusHelper.abandonAudioFocus();
        mBecomeNoiseHelper.unregisterBecomeNoiseReceiver();

        onStopped();

        for (String key : mStateListenerMap.keySet()) {
            PlayerStateListener listener = mStateListenerMap.get(key);
            if (listener != null) {
                listener.onStop();
            }
        }
    }

    private void notifyStalled(boolean stalled) {
        mPlayerState.setStalled(stalled);

        onStalledChanged(stalled);

        for (String key : mStateListenerMap.keySet()) {
            PlayerStateListener listener = mStateListenerMap.get(key);
            if (listener != null) {
                listener.onStalledChanged(stalled);
            }
        }
    }

    private void notifyError(int errorCode, String errorMessage) {
        releaseMusicPlayer();

        mPlayerState.setPlaybackState(PlaybackState.ERROR);
        mPlayerState.setErrorCode(errorCode);
        mPlayerState.setErrorMessage(errorMessage);
        updatePlayProgress(0, System.currentTimeMillis());

        mAudioFocusHelper.abandonAudioFocus();
        mBecomeNoiseHelper.unregisterBecomeNoiseReceiver();

        onError(errorCode, errorMessage);

        for (String key : mStateListenerMap.keySet()) {
            PlayerStateListener listener = mStateListenerMap.get(key);
            if (listener != null) {
                listener.onError(errorCode, errorMessage);
            }
        }
    }

    private void notifyBufferingPercentChanged(int percent, long updateTime) {
        mPlayerState.setBufferingPercent(percent);
        mPlayerState.setBufferingPercentUpdateTime(updateTime);

        for (String key : mStateListenerMap.keySet()) {
            PlayerStateListener listener = mStateListenerMap.get(key);
            if (listener != null) {
                listener.onBufferingPercentChanged(percent, updateTime);
            }
        }
    }

    /**
     * 通知当前正在播放的音乐以改变。
     *
     * @param musicItem 本次要播放的音乐
     * @param play      是否播放歌曲
     */
    protected final void notifyPlayingMusicItemChanged(@Nullable MusicItem musicItem, boolean play) {
        releaseMusicPlayer();
        updatePlayProgress(0, System.currentTimeMillis());

        mPlayerState.setMusicItem(musicItem);
        onPlayingMusicItemChanged(musicItem);

        if (play) {
            play();
        }

        for (String key : mStateListenerMap.keySet()) {
            PlayerStateListener listener = mStateListenerMap.get(key);
            if (listener != null) {
                listener.onPlayingMusicItemChanged(musicItem);
            }
        }
    }

    private void notifySeeking() {
        for (String key : mStateListenerMap.keySet()) {
            PlayerStateListener listener = mStateListenerMap.get(key);
            if (listener != null) {
                listener.onSeeking();
            }
        }
    }

    private void notifySeekComplete(int position) {
        mPlayerState.setPlayProgress(position);
        mPlayerState.setPlayProgressUpdateTime(System.currentTimeMillis());

        for (String key : mStateListenerMap.keySet()) {
            PlayerStateListener listener = mStateListenerMap.get(key);
            if (listener != null) {
                listener.onSeekComplete(position, mPlayerState.getPlayProgressUpdateTime());
            }
        }
    }

    @Override
    public void play() {
        if (isPlaying()) {
            return;
        }

        if (isPreparing()) {
            mPreparedAction = new Runnable() {
                @Override
                public void run() {
                    play();
                }
            };
            return;
        }

        if (isPrepared()) {
            mMusicPlayer.start();
            notifyPlaying(mMusicPlayer.getProgress(), System.currentTimeMillis());
            return;
        }

        prepareMusicPlayer(new Runnable() {
            @Override
            public void run() {
                play();
            }
        });
    }

    @Override
    public void pause() {
        if (isPaused() | isStopped() | isError()) {
            return;
        }

        if (isPreparing()) {
            mPreparedAction = new Runnable() {
                @Override
                public void run() {
                    pause();
                }
            };
            return;
        }

        if (isPlaying()) {
            mMusicPlayer.pause();
        }

        notifyPaused();
    }

    @Override
    public void stop() {
        if (isStopped()) {
            return;
        }

        if (isPrepared()) {
            mMusicPlayer.stop();
        }

        releaseMusicPlayer();
        notifyStopped();
    }

    @Override
    public void playOrPause() {
        if (isPlaying() | isPreparing()) {
            pause();
        } else {
            play();
        }
    }

    private void seekTo(final int progress, Runnable seekCompleteAction) {
        if (isPreparing()) {
            mPreparedAction = new Runnable() {
                @Override
                public void run() {
                    seekTo(progress);
                }
            };
            return;
        }

        if (!isPrepared()) {
            return;
        }

        mSeekCompleteAction = seekCompleteAction;
        mMusicPlayer.seekTo(progress);
        notifySeeking();
    }

    @Override
    public void seekTo(final int progress) {
        seekTo(progress, null);
    }

    @Override
    public void fastForward() {
        if (isPreparing()) {
            mPreparedAction = new Runnable() {
                @Override
                public void run() {
                    fastForward();
                }
            };
            return;
        }

        if (!isPrepared()) {
            return;
        }

        int progress = Math.min(mMusicPlayer.getDuration(),
                mMusicPlayer.getProgress() + FORWARD_STEP);

        seekTo(progress);
    }

    @Override
    public void rewind() {
        if (isPreparing()) {
            mPreparedAction = new Runnable() {
                @Override
                public void run() {
                    rewind();
                }
            };
            return;
        }

        if (!isPrepared()) {
            return;
        }

        int progress = Math.max(0, mMusicPlayer.getProgress() - FORWARD_STEP);

        seekTo(progress);
    }

    /**
     * 通知播放器，当前的 {@link snow.player.Player.SoundQuality} 已改变。
     * <p>
     * 该方法应该在调用与当前播放器管理的 {@link PlayerConfig} 对象的
     * {@link PlayerConfig#setSoundQuality(SoundQuality)} 方法后调用。
     */
    public final void notifySoundQualityChanged() {
        if (!isPrepared()) {
            return;
        }

        final boolean playing = mMusicPlayer.isPlaying();
        final int position = mMusicPlayer.getProgress();

        releaseMusicPlayer();
        prepareMusicPlayer(new Runnable() {
            @Override
            public void run() {
                if (position > 0) {
                    seekTo(position);
                }

                if (playing) {
                    play();
                }
            }
        });
    }

    /**
     * 通知播放器，当前的 {@code audioEffectEnabled} 状态已改变。
     * <p>
     * 该方法应该在调用与当前播放器管理的 {@link PlayerConfig} 对象的
     * {@link PlayerConfig#setAudioEffectEnabled(boolean)} 方法后调用。
     */
    public final void notifyAudioEffectEnableChanged() {
        if (!isPrepared()) {
            return;
        }

        if (mPlayerConfig.isAudioEffectEnabled()) {
            attachAudioEffect(getAudioSessionId());
            return;
        }

        detachAudioEffect();
    }

    /**
     * 通知播放器，当前的 {@code onlyWifiNetwork} 状态已改变。
     * <p>
     * 该方法应该在调用与当前播放器管理的 {@link PlayerConfig} 对象的
     * {@link PlayerConfig#setOnlyWifiNetwork(boolean)} 方法后调用。
     */
    public final void notifyOnlyWifiNetworkChanged() {
        if (!isPrepared()) {
            return;
        }

        checkNetworkType(mPlayerConfig.isOnlyWifiNetwork(), mNetworkUtil.isWifiNetwork());
    }

    private void checkNetworkType(boolean onlyWifiNetwork, boolean isWifiNetwork) {
        if (onlyWifiNetwork && !isWifiNetwork && !isCached(getMusicItem(), mPlayerConfig.getSoundQuality())) {
            pause();
            releaseMusicPlayer();
            notifyError(Error.ONLY_WIFI_NETWORK,
                    Error.getErrorMessage(mApplicationContext, Error.ONLY_WIFI_NETWORK));
        }
    }

    /**
     * MusicPlayer 包装器，额外添加了申请 WakeLock 唤醒锁功能。且只会在当前应用程序具有
     * android.permission.WAKE_LOCK 权限时才会申请 WakeLock 唤醒锁。
     * <p>
     * 唤醒锁会在播放时申请，在暂停、停止或者出错时释放。
     */
    private static class MusicPlayerWrapper implements MusicPlayer {
        private static final String TAG = "MusicPlayer";

        private Context mApplicationContext;
        private MusicPlayer mMusicPlayer;
        private PowerManager.WakeLock mWakeLock;
        private WifiManager.WifiLock mWifiLock;

        private OnCompletionListener mCompletionListener;
        private OnErrorListener mErrorListener;

        private ObjectAnimator mStartVolumeAnimator;
        private ObjectAnimator mPauseVolumeAnimator;

        MusicPlayerWrapper(@NonNull Context context, @NonNull MusicPlayer musicPlayer) {
            Preconditions.checkNotNull(context);
            Preconditions.checkNotNull(musicPlayer);

            mApplicationContext = context.getApplicationContext();
            mMusicPlayer = musicPlayer;

            initWakeLock(context);
            initDelegateMusicPlayer();
            initVolumeAnimator();
        }

        private void initWakeLock(Context context) {
            PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
            WifiManager wm = (WifiManager) context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);

            String tag = "player:MusicPlayer";

            if (pm != null) {
                mWakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, tag);
                mWakeLock.setReferenceCounted(false);
            }

            if (wm != null) {
                mWifiLock = wm.createWifiLock(WifiManager.WIFI_MODE_FULL, tag);
                mWifiLock.setReferenceCounted(false);
            }
        }

        private void initDelegateMusicPlayer() {
            mMusicPlayer.setOnCompletionListener(new OnCompletionListener() {
                @Override
                public void onCompletion(MusicPlayer mp) {
                    releaseWakeLock();

                    if (mCompletionListener != null) {
                        mCompletionListener.onCompletion(mp);
                    }
                }
            });

            mMusicPlayer.setOnErrorListener(new OnErrorListener() {
                @Override
                public void onError(MusicPlayer mp, int errorCode) {
                    releaseWakeLock();

                    if (mErrorListener != null) {
                        mErrorListener.onError(mp, errorCode);
                    }
                }
            });
        }

        private void initVolumeAnimator() {
            long volumeAnimDuration = 400L;

            mStartVolumeAnimator = ObjectAnimator.ofFloat(this, "volume", 0.0F, 1.0F);
            mStartVolumeAnimator.setDuration(volumeAnimDuration);
            mStartVolumeAnimator.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationCancel(Animator animation) {
                    setVolume(1.0F);
                }
            });

            mPauseVolumeAnimator = ObjectAnimator.ofFloat(this, "volume", 1.0F, 0.0F);
            mPauseVolumeAnimator.setDuration(volumeAnimDuration);
            mPauseVolumeAnimator.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationCancel(Animator animation) {
                    mMusicPlayer.pause();
                }

                @Override
                public void onAnimationEnd(Animator animation) {
                    mMusicPlayer.pause();
                }
            });
        }

        private void requireWakeLock() {
            if (wakeLockPermissionDenied()) {
                Log.w(TAG, "Forget to request 'android.permission.WAKE_LOCK' permission?");
                return;
            }

            if (mWakeLock != null && !mWakeLock.isHeld()) {
                mWakeLock.acquire(getDuration() + 5_000);
            }

            if (mWifiLock != null && !mWifiLock.isHeld()) {
                mWifiLock.acquire();
            }
        }

        private boolean wakeLockPermissionDenied() {
            return PackageManager.PERMISSION_DENIED ==
                    ContextCompat.checkSelfPermission(mApplicationContext, Manifest.permission.WAKE_LOCK);
        }

        private void releaseWakeLock() {
            if (mWakeLock != null && mWakeLock.isHeld()) {
                mWakeLock.release();
            }

            if (mWifiLock != null && mWifiLock.isHeld()) {
                mWifiLock.release();
            }
        }

        @Override
        public void prepare(Uri uri) throws Exception {
            mMusicPlayer.prepare(uri);
        }

        @Override
        public void setLooping(boolean looping) {
            mMusicPlayer.setLooping(looping);
        }

        @Override
        public boolean isLooping() {
            return mMusicPlayer.isLooping();
        }

        @Override
        public boolean isPlaying() {
            return mMusicPlayer.isPlaying();
        }

        @Override
        public int getDuration() {
            return mMusicPlayer.getDuration();
        }

        @Override
        public int getProgress() {
            return mMusicPlayer.getProgress();
        }

        @Override
        public void start() {
            requireWakeLock();
            cancelVolumeAnim();
            setVolume(0.0F);
            mMusicPlayer.start();
            mStartVolumeAnimator.start();
        }

        @Override
        public void pause() {
            releaseWakeLock();
            cancelVolumeAnim();
            mPauseVolumeAnimator.start();
        }

        private void cancelVolumeAnim() {
            if (mStartVolumeAnimator.isStarted()) {
                mStartVolumeAnimator.cancel();
            }

            if (mPauseVolumeAnimator.isStarted()) {
                mPauseVolumeAnimator.cancel();
            }
        }

        @Override
        public void stop() {
            releaseWakeLock();
            cancelVolumeAnim();
            mMusicPlayer.stop();
        }

        @Override
        public void seekTo(int pos) {
            mMusicPlayer.seekTo(pos);
        }

        @Override
        public void setVolume(float leftVolume, float rightVolume) {
            mMusicPlayer.setVolume(leftVolume, rightVolume);
        }

        public void setVolume(float volume) {
            setVolume(volume, volume);
        }

        @Override
        public void quiet() {
            mMusicPlayer.quiet();
        }

        @Override
        public void dismissQuiet() {
            mMusicPlayer.dismissQuiet();
        }

        @Override
        public void release() {
            cancelVolumeAnim();
            mMusicPlayer.release();
        }

        @Override
        public boolean isInvalid() {
            return mMusicPlayer.isInvalid();
        }

        @Override
        public int getAudioSessionId() {
            return mMusicPlayer.getAudioSessionId();
        }

        @Override
        public void setOnPreparedListener(OnPreparedListener listener) {
            mMusicPlayer.setOnPreparedListener(listener);
        }

        @Override
        public void setOnCompletionListener(OnCompletionListener listener) {
            mCompletionListener = listener;
        }

        @Override
        public void setOnSeekCompleteListener(OnSeekCompleteListener listener) {
            mMusicPlayer.setOnSeekCompleteListener(listener);
        }

        @Override
        public void setOnStalledListener(OnStalledListener listener) {
            mMusicPlayer.setOnStalledListener(listener);
        }

        @Override
        public void setOnBufferingUpdateListener(OnBufferingUpdateListener listener) {
            mMusicPlayer.setOnBufferingUpdateListener(listener);
        }

        @Override
        public void setOnErrorListener(OnErrorListener listener) {
            mErrorListener = listener;
        }
    }
}
