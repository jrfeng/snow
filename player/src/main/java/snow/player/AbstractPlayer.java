package snow.player;

import android.content.Context;
import android.media.AudioManager;
import android.net.Uri;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.common.base.Preconditions;

import java.io.IOException;
import java.util.HashMap;
import java.util.NoSuchElementException;

import io.reactivex.Single;
import io.reactivex.SingleEmitter;
import io.reactivex.SingleOnSubscribe;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Consumer;
import io.reactivex.schedulers.Schedulers;
import media.helper.AudioFocusHelper;
import media.helper.BecomeNoiseHelper;
import snow.player.media.MusicItem;
import snow.player.media.MusicPlayer;
import snow.player.state.PlayerState;
import snow.player.state.PlayerStateListener;
import snow.player.util.NetworkUtil;

/**
 * 该类实现了 {@link Player} 接口，并实现了其大部分功能。
 *
 * @param <T> {@link PlayerStateListener} 播放器状态监听器。
 */
public abstract class AbstractPlayer<T extends PlayerStateListener> implements Player {
    private static final int FORWARD_STEP = 15_000;     // 15 秒, 单位：毫秒 ms

    private Context mApplicationContext;
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

    private Runnable mPreparedAction;
    private Runnable mSeekCompleteAction;

    private AudioFocusHelper mAudioFocusHelper;
    private BecomeNoiseHelper mBecomeNoiseHelper;

    private NetworkUtil mNetworkUtil;

    private Disposable mDisposable;

    /**
     * @param context     Context 对象，不能为 null。
     * @param playerState PlayerState 对象，不能为 null。
     */
    public AbstractPlayer(@NonNull Context context, @NonNull PlayerState playerState) {
        Preconditions.checkNotNull(context);
        Preconditions.checkNotNull(playerState);

        mApplicationContext = context.getApplicationContext();
        mPlayerState = playerState;

        mStateListenerMap = new HashMap<>();

        initAllListener();
        initAllHelper();

        mNetworkUtil.subscribeNetworkState();
    }

    /**
     * 是否循环播放。
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
     * 获取已缓存的具有 soundQuality 音质的 MusicItem 表示的的音乐的 Uri。
     *
     * @param musicItem    MusicItem 对象
     * @param soundQuality 音乐的音质
     * @return 音乐的 Uri。可为 null，返回 null 时播放器会忽略本次播放。
     */
    @Nullable
    protected abstract Uri getCachedUri(MusicItem musicItem, SoundQuality soundQuality);

    /**
     * 从网络获取具有 soundQuality 音质的 MusicItem 表示的的音乐的 Uri。
     * <p>
     * 该方法会在异步线程中被调用。
     *
     * @param musicItem    MusicItem 对象
     * @param soundQuality 音乐的音质
     * @return 音乐的 Uri。可为 null，返回 null 时播放器会忽略本次播放。
     */
    @Nullable
    protected abstract Uri getUri(MusicItem musicItem, SoundQuality soundQuality);

    /**
     * 该方法会在创建 MusicPlayer 对象时调用。
     * <p>
     * 你可以重写该方法来返回你自己的 MusicPlayer 实现。
     *
     * @param uri 要播放的音乐的 uri
     */
    @NonNull
    protected abstract MusicPlayer onCreateMusicPlayer(Uri uri) throws IOException;

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
    protected void onPlaying(long progress, long updateTime) {
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
     * 释放播放器所占用的资源。注意！调用该方法后，就不允许在使用当前 Player 对象了，否则会导致不可预见的错误。
     */
    public final void release() {
        disposeLastGetMusicItemUri();
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
     * 准备当前播放器所持有的 {@link MusicItem} 对象。
     *
     * @param preparedAction 在音乐播放器准备完成后要执行的操作
     */
    protected final void prepareMusicPlayer(@Nullable Runnable preparedAction) {
        releaseMusicPlayer();
        notifyBufferingPercentChanged(0, System.currentTimeMillis());

        MusicItem musicItem = mPlayerState.getMusicItem();
        if (musicItem == null) {
            return;
        }

        if (mDisposable != null) {
            mDisposable.dispose();
        }

        disposeLastGetMusicItemUri();
        mDisposable = getMusicItemUriAsync(musicItem)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(onGetMusicItemUriSuccess(preparedAction), ignoreError());
    }

    private void disposeLastGetMusicItemUri() {
        if (mDisposable != null) {
            mDisposable.dispose();
        }
    }

    private Consumer<Uri> onGetMusicItemUriSuccess(@Nullable final Runnable preparedAction) {
        return new Consumer<Uri>() {
            @Override
            public void accept(Uri uri) {
                try {
                    mMusicPlayer = onCreateMusicPlayer(uri);
                } catch (IOException e) {
                    e.printStackTrace();
                    notifyError(Error.DATA_LOAD_FAILED,
                            Error.getErrorMessage(mApplicationContext, Error.DATA_LOAD_FAILED));
                    return;
                }

                attachListeners(mMusicPlayer);

                mPreparedAction = preparedAction;
                notifyPreparing();
                mMusicPlayer.prepareAsync();
            }
        };
    }

    private Consumer<Throwable> ignoreError() {
        return new Consumer<Throwable>() {
            @Override
            public void accept(Throwable throwable) {
                // ignore
            }
        };
    }

    /**
     * 获取 MusicItem 的播放链接。该方法会在异步线程中被调用。
     *
     * @return MusicItem 的播放链接。获取失败时返回 null。
     */
    @Nullable
    private Uri getMusicItemUri(@NonNull MusicItem musicItem) {
        if (isCached(musicItem, mPlayerState.getSoundQuality())) {
            notifyBufferingPercentChanged(100, System.currentTimeMillis());
            return getCachedUriWrapper(musicItem, mPlayerState.getSoundQuality());
        }

        return getUriFromInternet(musicItem);
    }

    private Uri getCachedUriWrapper(@NonNull MusicItem musicItem, SoundQuality soundQuality) {
        Uri result = getCachedUri(musicItem, soundQuality);

        if (result == null) {
            notifyError(Error.FILE_NOT_FOUND,
                    Error.getErrorMessage(mApplicationContext, Error.FILE_NOT_FOUND));
        }

        return result;
    }

    private Single<Uri> getMusicItemUriAsync(@NonNull final MusicItem musicItem) {
        return Single.create(new SingleOnSubscribe<Uri>() {
            @Override
            public void subscribe(SingleEmitter<Uri> emitter) {
                Uri uri = getMusicItemUri(musicItem);

                if (emitter.isDisposed()) {
                    return;
                }

                if (uri != null) {
                    emitter.onSuccess(uri);
                    return;
                }

                emitter.onError(new NoSuchElementException());
            }
        });
    }

    private Uri getUriFromInternet(MusicItem musicItem) {
        if (!mNetworkUtil.networkAvailable()) {
            notifyError(Error.NETWORK_UNAVAILABLE,
                    Error.getErrorMessage(mApplicationContext, Error.NETWORK_UNAVAILABLE));
            return null;
        }

        if (!mPlayerState.isOnlyWifiNetwork()) {
            return getUri(musicItem, mPlayerState.getSoundQuality());
        }

        if (isWiFiNetwork()) {
            return getUri(musicItem, mPlayerState.getSoundQuality());
        }

        notifyError(Error.ONLY_WIFI_NETWORK,
                Error.getErrorMessage(mApplicationContext, Error.ONLY_WIFI_NETWORK));

        return null;
    }

    private boolean isWiFiNetwork() {
        return mNetworkUtil.isWifiNetwork();
    }

    private void initAllListener() {
        mOnPreparedListener = new MusicPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(MusicPlayer mp) {
                mp.setLooping(isLooping());

                if (mPlayerState.isAudioEffectEnabled()) {
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
                notifySeekComplete(mp.getCurrentPosition());

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

                releaseMusicPlayer();

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
                if (mPlayerState.isIgnoreLossAudioFocus()) {
                    return;
                }

                pause();
                onLossAudioFocus();
            }

            @Override
            public void onLossTransient() {
                if (mPlayerState.isIgnoreLossAudioFocus()) {
                    return;
                }

                playing = isPlaying();
                pause();
                onLossAudioFocus();
            }

            @Override
            public void onLossTransientCanDuck() {
                if (mPlayerState.isIgnoreLossAudioFocus()) {
                    return;
                }

                playing = isPlaying();
                if (playing) {
                    mMusicPlayer.volumeDuck();
                }
            }

            @Override
            public void onGain(boolean lossTransient, boolean lossTransientCanDuck) {
                if (mPlayerState.isIgnoreLossAudioFocus()) {
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
                    mMusicPlayer.volumeRestore();
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

                checkNetworkType(mPlayerState.isOnlyWifiNetwork(), wifiNetwork);
            }
        });
    }

    private boolean notBufferingEnd() {
        return mPlayerState.getBufferingPercent() < 100;
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
    protected final void updatePlayProgress(long progress, long updateTime) {
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

    private void notifyPlaying(long progress, long updateTime) {
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
        updatePlayProgress(mMusicPlayer.getCurrentPosition(), System.currentTimeMillis());

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
     * @param musicItem         本次要播放的音乐
     * @param playingOnPrepared 是否在播放器准备完毕后开始播放
     */
    protected final void notifyPlayingMusicItemChanged(@Nullable MusicItem musicItem, final boolean playingOnPrepared) {
        releaseMusicPlayer();
        updatePlayProgress(0, System.currentTimeMillis());

        mPlayerState.setMusicItem(musicItem);
        onPlayingMusicItemChanged(musicItem);

        prepareMusicPlayer(new Runnable() {
            @Override
            public void run() {
                if (playingOnPrepared) {
                    play();
                }
            }
        });

        for (String key : mStateListenerMap.keySet()) {
            PlayerStateListener listener = mStateListenerMap.get(key);
            if (listener != null) {
                listener.onPlayingMusicItemChanged(musicItem);
            }
        }
    }

    private void notifySeekComplete(long position) {
        mPlayerState.setPlayProgress(position);
        mPlayerState.setPlayProgressUpdateTime(System.currentTimeMillis());

        for (String key : mStateListenerMap.keySet()) {
            PlayerStateListener listener = mStateListenerMap.get(key);
            if (listener != null) {
                listener.onSeekComplete(position);
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
            notifyPlaying(mMusicPlayer.getCurrentPosition(), System.currentTimeMillis());
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
            notifyPaused();
        }
    }

    @Override
    public void stop() {
        if (isPreparing()) {
            mPreparedAction = new Runnable() {
                @Override
                public void run() {
                    stop();
                }
            };
            return;
        }

        if (isPrepared()) {
            mMusicPlayer.stop();
            notifyStopped();
        }
    }

    @Override
    public void playOrPause() {
        if (isPlaying()) {
            pause();
        } else {
            play();
        }
    }

    private void seekTo(final long progress, Runnable seekCompleteAction) {
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
    }

    @Override
    public void seekTo(final long progress) {
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

        long progress = Math.min(mMusicPlayer.getDuration(),
                mMusicPlayer.getCurrentPosition() + FORWARD_STEP);

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

        long progress = Math.max(0, mMusicPlayer.getCurrentPosition() - FORWARD_STEP);

        seekTo(progress);
    }

    @Override
    public void setSoundQuality(SoundQuality soundQuality) {
        if (soundQuality == mPlayerState.getSoundQuality()) {
            return;
        }

        swapSoundQuality(soundQuality);
    }

    private void swapSoundQuality(SoundQuality soundQuality) {
        mPlayerState.setSoundQuality(soundQuality);

        if (!isPrepared()) {
            return;
        }

        final boolean playing = mMusicPlayer.isPlaying();
        final long position = mMusicPlayer.getCurrentPosition();

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

    @Override
    public void setAudioEffectEnabled(boolean enabled) {
        mPlayerState.setAudioEffectEnabled(enabled);

        if (!isPrepared()) {
            return;
        }

        if (enabled) {
            attachAudioEffect(getAudioSessionId());
            return;
        }

        detachAudioEffect();
    }

    @Override
    public void setOnlyWifiNetwork(boolean onlyWifiNetwork) {
        mPlayerState.setOnlyWifiNetwork(onlyWifiNetwork);

        if (!isPrepared()) {
            return;
        }

        checkNetworkType(onlyWifiNetwork, mNetworkUtil.isWifiNetwork());
    }

    private void checkNetworkType(boolean onlyWifiNetwork, boolean isWifiNetwork) {
        if (onlyWifiNetwork && !isWifiNetwork && notBufferingEnd()) {
            pause();
            releaseMusicPlayer();
            notifyError(Error.ONLY_WIFI_NETWORK,
                    Error.getErrorMessage(mApplicationContext, Error.ONLY_WIFI_NETWORK));
        }
    }

    @Override
    public void setIgnoreLossAudioFocus(boolean ignoreLossAudioFocus) {
        mPlayerState.setIgnoreLossAudioFocus(ignoreLossAudioFocus);
    }
}
