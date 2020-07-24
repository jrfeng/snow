package snow.player;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.media.AudioManager;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.PowerManager;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.google.common.base.Preconditions;

import java.util.HashMap;
import java.util.Random;

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
import snow.player.playlist.Playlist;
import snow.player.playlist.PlaylistManager;
import snow.player.util.NetworkUtil;

/**
 * 该类实现了 {@link Player} 接口，并实现了其大部分功能。
 */
public abstract class AbstractPlayer implements Player {
    private static final int FORWARD_STEP = 15_000;     // 15 秒, 单位：毫秒 ms

    private Context mApplicationContext;
    private PlayerConfig mPlayerConfig;
    private PlayerState mPlayerState;
    private HashMap<String, PlayerStateListener> mStateListenerMap;

    private MusicPlayer.OnPreparedListener mOnPreparedListener;
    private MusicPlayer.OnCompletionListener mOnCompletionListener;
    private MusicPlayer.OnSeekCompleteListener mOnSeekCompleteListener;
    private MusicPlayer.OnStalledListener mOnStalledListener;
    private MusicPlayer.OnBufferingUpdateListener mOnBufferingUpdateListener;
    private MusicPlayer.OnErrorListener mOnErrorListener;

    private AudioFocusHelper mAudioFocusHelper;
    private BecomeNoiseHelper mBecomeNoiseHelper;
    private NetworkUtil mNetworkUtil;

    private MusicPlayer mMusicPlayer;

    private boolean mPreparing;
    private boolean mPrepared;
    private boolean mLoadingPlaylist;

    private Runnable mPreparedAction;
    private Runnable mSeekCompleteAction;
    private Runnable mPlaylistLoadedAction;

    private PlaylistManager mPlaylistManager;
    private Playlist mPlaylist;

    private Random mRandom;
    private Disposable mRetrieveUriDisposable;

    private boolean mReleased;

    /**
     * @param context      {@link Context} 对象，不能为 null
     * @param playerConfig {@link PlayerConfig} 对象，保存了播放器的初始配置信息，不能为 null
     * @param playerState  {@link PlayerState} 对象，保存了播放器的初始状态，不能为 null
     */
    public AbstractPlayer(@NonNull Context context,
                          @NonNull PlayerConfig playerConfig,
                          @NonNull PlayerState playerState,
                          @NonNull PlaylistManager playlistManager) {
        Preconditions.checkNotNull(context);
        Preconditions.checkNotNull(playerConfig);
        Preconditions.checkNotNull(playerState);
        Preconditions.checkNotNull(playlistManager);

        mApplicationContext = context.getApplicationContext();
        mPlayerConfig = playerConfig;
        mPlayerState = playerState;
        mPlaylistManager = playlistManager;
        mStateListenerMap = new HashMap<>();

        initAllListener();
        initAllHelper();

        mNetworkUtil.subscribeNetworkState();
        reloadPlaylist();
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
    protected abstract boolean isCached(MusicItem musicItem, SoundQuality soundQuality);

    /**
     * 该方法会在创建 MusicPlayer 对象时调用。
     * <p>
     * 你可以重写该方法来返回你自己的 MusicPlayer 实现。
     */
    @NonNull
    protected abstract MusicPlayer onCreateMusicPlayer(Context context);

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
    @Nullable
    protected abstract Uri retrieveMusicItemUri(@NonNull MusicItem musicItem, @NonNull Player.SoundQuality soundQuality) throws Exception;

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
     * 该方法会在当前播放的 MusicItem 对象改变时调用。
     *
     * @param musicItem 本次要播放的 MusicItem 对象（可能为 null）。
     */
    protected void onPlayingMusicItemChanged(@Nullable MusicItem musicItem) {
    }

    /**
     * 释放播放器所占用的资源。注意！调用该方法后，就不允许在使用当前 Player 对象了，否则会导致不可预见的错误。
     */
    public void release() {
        mReleased = true;
        disposeRetrieveUri();
        releaseMusicPlayer();

        mStateListenerMap.clear();

        mAudioFocusHelper.abandonAudioFocus();
        mBecomeNoiseHelper.unregisterBecomeNoiseReceiver();
        mNetworkUtil.unsubscribeNetworkState();

        mAudioFocusHelper = null;
        mBecomeNoiseHelper = null;
        mNetworkUtil = null;

        mPreparedAction = null;
        mSeekCompleteAction = null;
        mPlaylistLoadedAction = null;
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
     * 准备当前播放器所持有的 {@link MusicItem} 对象（测试用）。
     *
     * @param preparedAction 在音乐播放器准备完成后要执行的操作
     */
    private void prepareMusicPlayer(@Nullable Runnable preparedAction) {
        releaseMusicPlayer();

        notifyBufferingPercentChanged(0, System.currentTimeMillis());

        MusicItem musicItem = mPlayerState.getMusicItem();
        if (musicItem == null) {
            return;
        }

        if (mPlayerConfig.isOnlyWifiNetwork() && !isWiFiNetwork()) {
            onError(Error.ONLY_WIFI_NETWORK, Error.getErrorMessage(mApplicationContext, Error.ONLY_WIFI_NETWORK));
            return;
        }

        disposeRetrieveUri();

        mRetrieveUriDisposable = getMusicItemUri(musicItem, mPlayerConfig.getSoundQuality())
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(prepare(preparedAction), notifyError());
    }

    private void disposeRetrieveUri() {
        if (mRetrieveUriDisposable != null && !mRetrieveUriDisposable.isDisposed()) {
            mRetrieveUriDisposable.dispose();
        }
    }

    private Single<Uri> getMusicItemUri(@NonNull final MusicItem musicItem, @NonNull final SoundQuality soundQuality) {
        return Single.create(new SingleOnSubscribe<Uri>() {
            @Override
            public void subscribe(SingleEmitter<Uri> emitter) {
                Uri uri = null;

                try {
                    uri = retrieveMusicItemUri(musicItem, soundQuality);
                } catch (Exception e) {
                    emitter.onError(e);
                }

                if (emitter.isDisposed()) {
                    return;
                }

                if (uri == null) {
                    emitter.onError(new IllegalStateException("get uri failed."));
                    return;
                }

                emitter.onSuccess(uri);
            }
        });
    }

    private Consumer<Uri> prepare(final @Nullable Runnable preparedAction) {
        return new Consumer<Uri>() {
            @Override
            public void accept(Uri uri) {
                mMusicPlayer = new MusicPlayerWrapper(mApplicationContext, onCreateMusicPlayer(mApplicationContext));
                attachListeners(mMusicPlayer);

                mPreparedAction = preparedAction;
                notifyPreparing();

                try {
                    onPrepareMusicPlayer(mMusicPlayer, uri);
                } catch (Exception e) {
                    e.printStackTrace();
                    notifyError(Error.DATA_LOAD_FAILED, Error.getErrorMessage(mApplicationContext, Error.DATA_LOAD_FAILED));
                }
            }
        };
    }

    private void onPrepareMusicPlayer(MusicPlayer musicPlayer, Uri uri) throws Exception {
        if (!musicPlayer.isInvalid()) {
            musicPlayer.prepare(uri);
        }
    }

    private Consumer<Throwable> notifyError() {
        return new Consumer<Throwable>() {
            @Override
            public void accept(Throwable throwable) {
                throwable.printStackTrace();
                notifyError(Error.GET_URL_FAILED, Error.getErrorMessage(mApplicationContext, Error.GET_URL_FAILED));
            }
        };
    }

    private boolean isWiFiNetwork() {
        return mNetworkUtil.isWifiNetwork();
    }

    private void initAllListener() {
        mOnPreparedListener = new MusicPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(MusicPlayer mp) {
                if (mReleased) {
                    return;
                }

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
                if (mPlayerState.getPlayMode() == PlayMode.LOOP) {
                    return;
                }

                skipToNext();
            }
        };

        mOnSeekCompleteListener = new MusicPlayer.OnSeekCompleteListener() {
            @Override
            public void onSeekComplete(MusicPlayer mp) {
                if (mReleased) {
                    return;
                }

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
                pause();
            }

            @Override
            public void onLossTransient() {
                playing = isPlaying();
                pause();
            }

            @Override
            public void onLossTransientCanDuck() {
                playing = isPlaying();
                if (playing) {
                    mMusicPlayer.quiet();
                }
            }

            @Override
            public void onGain(boolean lossTransient, boolean lossTransientCanDuck) {
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
     * 释放当前播放器所持有的 {@link MusicPlayer} 对象（测试用）。
     */
    private void releaseMusicPlayer() {
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
    public final boolean isPreparing() {
        return mPreparing;
    }

    /**
     * 缓存区是否没有足够的数据继续播放。
     */
    public final boolean isStalled() {
        return mPlayerState.isStalled();
    }

    /**
     * 播放器释放已经准备完毕。
     *
     * @return 当播放器已经准备完毕时返回 true，否则返回 false。
     */
    public final boolean isPrepared() {
        return mPrepared;
    }

    /**
     * 是否正在播放。
     *
     * @return 当正在播放时返回 true，否则返回 false。
     */
    public final boolean isPlaying() {
        return isPrepared() && mMusicPlayer.isPlaying();
    }

    /**
     * 是否已暂停。
     */
    public final boolean isPaused() {
        return mPlayerState.getPlaybackState() == PlaybackState.PAUSED;
    }

    /**
     * 是否已停止。
     */
    public final boolean isStopped() {
        return mPlayerState.getPlaybackState() == PlaybackState.STOPPED;
    }

    /**
     * 是否发生了错误。
     */
    public final boolean isError() {
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
    public final int getAudioSessionId() {
        if (isPrepared()) {
            return mMusicPlayer.getAudioSessionId();
        }

        return 0;
    }

    private void updatePlayProgress(int progress, long updateTime) {
        mPlayerState.setPlayProgress(progress);
        mPlayerState.setPlayProgressUpdateTime(updateTime);
    }

    /**
     * 添加播放器状态监听器。
     *
     * @param token    监听器的 token（不能为 null），请务必保证该参数的唯一性。
     * @param listener 监听器（不能为 null）。
     */
    public final void addStateListener(@NonNull String token, @NonNull PlayerStateListener listener) {
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
     * 通知当前正在播放的音乐以改变（测试用）。
     *
     * @param musicItem 本次要播放的音乐
     * @param play      是否播放歌曲
     */
    private void notifyPlayingMusicItemChanged(@Nullable MusicItem musicItem, boolean play) {
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

        if (mLoadingPlaylist) {
            mPlaylistLoadedAction = new Runnable() {
                @Override
                public void run() {
                    play();
                }
            };
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

        if (mLoadingPlaylist) {
            mPlaylistLoadedAction = new Runnable() {
                @Override
                public void run() {
                    pause();
                }
            };
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

        if (mLoadingPlaylist) {
            mPlaylistLoadedAction = null;
            releaseMusicPlayer();
            notifyStopped();
            return;
        }

        if (isPrepared()) {
            mMusicPlayer.stop();
        }

        releaseMusicPlayer();
        notifyStopped();
    }

    @Override
    public void playPause() {
        if (isPlaying() | isPreparing()) {
            pause();
        } else {
            play();
        }
    }

    private void seekTo(final int progress, final Runnable seekCompleteAction) {
        if (mLoadingPlaylist) {
            mPlaylistLoadedAction = new Runnable() {
                @Override
                public void run() {
                    seekTo(progress, seekCompleteAction);
                }
            };
            return;
        }

        if (isPreparing()) {
            mPreparedAction = new Runnable() {
                @Override
                public void run() {
                    seekTo(progress, seekCompleteAction);
                }
            };
            return;
        }

        if (!isPrepared()) {
            return;
        }

        mSeekCompleteAction = seekCompleteAction;
        notifySeeking();
        mMusicPlayer.seekTo(progress);
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

    private void reloadPlaylist(){
        reloadPlaylist(false, false);
    }

    private void reloadPlaylist(final boolean playingMusicChanged, final boolean play) {
        mLoadingPlaylist = true;
        mPlaylistManager.getPlaylistAsync(new PlaylistManager.Callback() {
            @Override
            public void onFinished(@NonNull final Playlist playlist) {
                if (mReleased) {
                    return;
                }

                mPlaylist = playlist;
                mLoadingPlaylist = false;

                if (playingMusicChanged) {
                    MusicItem musicItem = null;
                    if (mPlaylist.size() > 0) {
                        musicItem = mPlaylist.get(mPlayerState.getPosition());
                    }

                    notifyPlayingMusicItemChanged(musicItem, play);
                }

                if (mPlaylistLoadedAction != null) {
                    mPlaylistLoadedAction.run();
                    mPlaylistLoadedAction = null;
                }
            }
        });
    }

    private int getRandomPosition(int exclude) {
        if (mPlaylist == null || getPlaylistSize() < 2) {
            return 0;
        }

        if (mRandom == null) {
            mRandom = new Random();
        }

        int position = mRandom.nextInt(getPlaylistSize());

        if (position != exclude) {
            return position;
        }

        return getRandomPosition(exclude);
    }

    private void notifyPlayingMusicItemPositionChanged(int position) {
        mPlayerState.setPosition(position);

        for (String key : mStateListenerMap.keySet()) {
            PlayerStateListener listener = mStateListenerMap.get(key);
            if (listener != null) {
                listener.onPositionChanged(position);
            }
        }
    }

    private void notifyPlayModeChanged(PlayMode playMode) {
        mPlayerState.setPlayMode(playMode);

        for (String key : mStateListenerMap.keySet()) {
            PlayerStateListener listener = mStateListenerMap.get(key);
            if (listener != null) {
                listener.onPlayModeChanged(playMode);
            }
        }
    }

    private void notifyPlaylistChanged(int position) {
        mPlayerState.setPosition(position);

        for (String key : mStateListenerMap.keySet()) {
            PlayerStateListener listener = mStateListenerMap.get(key);
            if (listener != null) {
                // 注意！playlistManager 参数为 null，客户端接收到该事件后，应该将其替换为自己的 PlaylistManager 对象
                listener.onPlaylistChanged(null, position);
            }
        }
    }

    /**
     * 获取播放列表的大小。
     *
     * @return 播放列表的大小
     */
    protected final int getPlaylistSize() {
        return mPlaylistManager.getPlaylistSize();
    }

    /**
     * 获取播放列表携带的额外参数。
     *
     * @return 播放列表携带的额外参数，可能为 null
     */
    @Nullable
    public final Bundle getPlaylistExtra() {
        if (mPlaylist == null) {
            return null;
        }

        return mPlaylist.getExtra();
    }

    public boolean isLooping() {
        return mPlayerState.getPlayMode() == PlayMode.LOOP;
    }

    @Override
    public void skipToNext() {
        if (mLoadingPlaylist) {
            mPlaylistLoadedAction = new Runnable() {
                @Override
                public void run() {
                    skipToNext();
                }
            };
            return;
        }

        if (getPlaylistSize() < 1) {
            return;
        }

        int position = mPlayerState.getPosition();

        switch (mPlayerState.getPlayMode()) {
            case SEQUENTIAL:   // 注意！case 穿透
            case LOOP:
                position = getNextPosition(position);
                break;
            case SHUFFLE:
                position = getRandomPosition(position);
                break;
        }

        notifyPlayingMusicItemChanged(mPlaylist.get(position), true);
        notifyPlayingMusicItemPositionChanged(position);
    }

    private int getNextPosition(int currentPosition) {
        int position = currentPosition + 1;

        if (position >= getPlaylistSize()) {
            return 0;
        }

        return position;
    }

    @Override
    public void skipToPrevious() {
        if (mLoadingPlaylist) {
            mPlaylistLoadedAction = new Runnable() {
                @Override
                public void run() {
                    skipToPrevious();
                }
            };
            return;
        }

        if (getPlaylistSize() < 1) {
            return;
        }

        int position = mPlayerState.getPosition();

        switch (mPlayerState.getPlayMode()) {
            case SEQUENTIAL:   // 注意！case 穿透
            case LOOP:
                position = getPreviousPosition(position);
                break;
            case SHUFFLE:
                position = getRandomPosition(position);
                break;
        }

        notifyPlayingMusicItemChanged(mPlaylist.get(position), true);
        notifyPlayingMusicItemPositionChanged(position);
    }

    private int getPreviousPosition(int currentPosition) {
        int position = currentPosition - 1;

        if (position < 0) {
            return getPlaylistSize() - 1;
        }

        return position;
    }

    @Override
    public void playPause(final int position) {
        if (mLoadingPlaylist) {
            mPlaylistLoadedAction = new Runnable() {
                @Override
                public void run() {
                    playPause(position);
                }
            };
            return;
        }

        if (position == mPlayerState.getPosition()) {
            playPause();
            return;
        }

        notifyPlayingMusicItemChanged(mPlaylist.get(position), true);
        notifyPlayingMusicItemPositionChanged(position);
    }

    @Override
    public void setPlayMode(@NonNull PlayMode playMode) {
        Preconditions.checkNotNull(playMode);
        if (playMode == mPlayerState.getPlayMode()) {
            return;
        }

        notifyPlayModeChanged(playMode);
    }

    @Override
    public void onNewPlaylist(final int position, final boolean play) {
        stop();
        notifyPlaylistChanged(position);
        reloadPlaylist(true, play);
    }

    @Override
    public void onMusicItemMoved(int fromPosition, int toPosition) {
        int position = mPlayerState.getPosition();
        if (notInRegion(position, fromPosition, toPosition)) {
            notifyPlaylistChanged(position);
            reloadPlaylist();
            return;
        }

        if (fromPosition < position) {
            position -= 1;
        } else if (fromPosition == position) {
            position = toPosition;
        } else {
            position += 1;
        }

        notifyPlaylistChanged(position);
        reloadPlaylist();
    }

    @Override
    public void onMusicItemInserted(int position, MusicItem musicItem) {
        int playingPosition = mPlayerState.getPosition();

        if (position <= playingPosition) {
            playingPosition += 1;
        }

        notifyPlaylistChanged(playingPosition);
        reloadPlaylist();
    }

    @Override
    public void onMusicItemRemoved(final MusicItem musicItem) {
        int removePosition = mPlaylist.indexOf(musicItem);
        if (removePosition < 0) {
            return;
        }

        if (getPlaylistSize() < 1) {
            notifyPlaylistChanged(-1);
            reloadPlaylist(true, false);
            return;
        }

        int position = mPlayerState.getPosition();

        if (removePosition < position) {
            position -= 1;
        } else if (removePosition == position) {
            position = getNextPosition(position - 1);
            notifyPlayingMusicItemPositionChanged(position);
            reloadPlaylist(true, isPlaying());
            return;
        }

        notifyPlaylistChanged(position);
        reloadPlaylist();
    }

    private boolean notInRegion(int position, int fromPosition, int toPosition) {
        return position > Math.max(fromPosition, toPosition) || position < Math.min(fromPosition, toPosition);
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

        private Handler mMainHandler;

        MusicPlayerWrapper(@NonNull Context context, @NonNull MusicPlayer musicPlayer) {
            Preconditions.checkNotNull(context);
            Preconditions.checkNotNull(musicPlayer);

            mApplicationContext = context.getApplicationContext();
            mMusicPlayer = musicPlayer;
            mMainHandler = new Handler(Looper.getMainLooper());

            initWakeLock(context);
            initDelegateMusicPlayer();
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

        private boolean isMainThread() {
            return Looper.myLooper() == Looper.getMainLooper();
        }

        private void runOnMainThread(Runnable task) {
            if (isMainThread()) {
                task.run();
                return;
            }

            mMainHandler.post(task);
        }

        private void initDelegateMusicPlayer() {
            mMusicPlayer.setOnCompletionListener(new OnCompletionListener() {
                @Override
                public void onCompletion(final MusicPlayer mp) {
                    runOnMainThread(new Runnable() {
                        @Override
                        public void run() {
                            releaseWakeLock();

                            if (mCompletionListener != null) {
                                mCompletionListener.onCompletion(mp);
                            }
                        }
                    });
                }
            });

            mMusicPlayer.setOnErrorListener(new OnErrorListener() {
                @Override
                public void onError(final MusicPlayer mp, final int errorCode) {
                    runOnMainThread(new Runnable() {
                        @Override
                        public void run() {
                            releaseWakeLock();

                            if (mErrorListener != null) {
                                mErrorListener.onError(mp, errorCode);
                            }
                        }
                    });
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
            mMusicPlayer.start();
        }

        @Override
        public void pause() {
            releaseWakeLock();
            mMusicPlayer.pause();
        }

        @Override
        public void stop() {
            releaseWakeLock();
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
        public void setOnPreparedListener(final OnPreparedListener listener) {
            if (listener == null) {
                mMusicPlayer.setOnPreparedListener(null);
                return;
            }

            mMusicPlayer.setOnPreparedListener(new OnPreparedListener() {
                @Override
                public void onPrepared(final MusicPlayer mp) {
                    runOnMainThread(new Runnable() {
                        @Override
                        public void run() {
                            listener.onPrepared(mp);
                        }
                    });
                }
            });
        }

        @Override
        public void setOnCompletionListener(OnCompletionListener listener) {
            mCompletionListener = listener;
        }

        @Override
        public void setOnSeekCompleteListener(final OnSeekCompleteListener listener) {
            if (listener == null) {
                mMusicPlayer.setOnSeekCompleteListener(null);
                return;
            }

            mMusicPlayer.setOnSeekCompleteListener(new OnSeekCompleteListener() {
                @Override
                public void onSeekComplete(final MusicPlayer mp) {
                    runOnMainThread(new Runnable() {
                        @Override
                        public void run() {
                            listener.onSeekComplete(mp);
                        }
                    });
                }
            });
        }

        @Override
        public void setOnStalledListener(final OnStalledListener listener) {
            if (listener == null) {
                mMusicPlayer.setOnStalledListener(null);
                return;
            }

            mMusicPlayer.setOnStalledListener(new OnStalledListener() {
                @Override
                public void onStalled(final boolean stalled) {
                    runOnMainThread(new Runnable() {
                        @Override
                        public void run() {
                            listener.onStalled(stalled);
                        }
                    });
                }
            });
        }

        @Override
        public void setOnBufferingUpdateListener(final OnBufferingUpdateListener listener) {
            if (listener == null) {
                mMusicPlayer.setOnBufferingUpdateListener(null);
                return;
            }

            mMusicPlayer.setOnBufferingUpdateListener(new OnBufferingUpdateListener() {
                @Override
                public void onBufferingUpdate(final MusicPlayer mp, final int percent) {
                    runOnMainThread(new Runnable() {
                        @Override
                        public void run() {
                            listener.onBufferingUpdate(mp, percent);
                        }
                    });
                }
            });
        }

        @Override
        public void setOnErrorListener(OnErrorListener listener) {
            mErrorListener = listener;
        }
    }
}
