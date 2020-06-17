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
import snow.player.util.ErrorUtil;
import snow.player.util.NetworkUtil;

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

    // ********************************abstract********************************

    /**
     * 查询具有 soundQuality 音质的 MusicItem 表示的的音乐是否已被缓存。
     * <p>
     * 该方法会在异步线程中被调用。
     *
     * @param musicItem    要查询的 MusicItem 对象
     * @param soundQuality 音乐的音质
     * @return 如果已被缓存，则返回 true，否则返回 false
     */
    protected abstract boolean isCached(MusicItem musicItem, int soundQuality);

    /**
     * 获取已缓存的具有 soundQuality 音质的 MusicItem 表示的的音乐的 Uri。
     *
     * @param musicItem    MusicItem 对象
     * @param soundQuality 音乐的音质
     * @return 音乐的 Uri。可为 null，返回 null 时播放器会忽略本次播放。
     */
    @Nullable
    protected abstract Uri getCachedUri(MusicItem musicItem, int soundQuality);

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
    protected abstract Uri getUri(MusicItem musicItem, int soundQuality);

    /**
     * 该方法会在创建 MusicPlayer 对象时调用。
     * <p>
     * 你可以重写该方法来返回你自己的 MusicPlayer 实现。
     *
     * @param uri 要播放的音乐的 uri
     */
    @NonNull
    protected abstract MusicPlayer onCreateMusicPlayer(Uri uri) throws IOException;

    // ********************************end******************************

    protected void attachAudioEffect(int audioSessionId) {
    }

    protected void detachAudioEffect() {
    }

    // *****************************Callback****************************

    protected void onPreparing() {
    }

    protected void onPrepared(int audioSessionId) {
    }

    protected void onPlaying(long progress, long updateTime) {
    }

    protected void onPaused() {
    }

    protected void onStalled() {
    }

    protected void onStopped() {
    }

    protected void onError(int errorCode, String errorMessage) {
    }

    protected void onPlayComplete(MusicItem musicItem) {
    }

    protected void onRequestAudioFocus(boolean success) {
    }

    protected void onLossAudioFocus() {
    }

    protected void onRelease() {
    }

    protected void onPlayingMusicItemChanged(@Nullable MusicItem musicItem) {
    }

    // ********************************end******************************

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
            public void accept(Uri uri) throws Exception {
                try {
                    mMusicPlayer = onCreateMusicPlayer(uri);
                } catch (IOException e) {
                    e.printStackTrace();
                    notifyError(ErrorUtil.ERROR_DATA_LOAD_FAILED,
                            ErrorUtil.getErrorMessage(mApplicationContext, ErrorUtil.ERROR_DATA_LOAD_FAILED));
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
            public void accept(Throwable throwable) throws Exception {
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

    private Uri getCachedUriWrapper(@NonNull MusicItem musicItem, int soundQuality) {
        Uri result = getCachedUri(musicItem, soundQuality);

        if (result == null) {
            notifyError(ErrorUtil.ERROR_FILE_NOT_FOUND,
                    ErrorUtil.getErrorMessage(mApplicationContext, ErrorUtil.ERROR_FILE_NOT_FOUND));
        }

        return result;
    }

    private Single<Uri> getMusicItemUriAsync(@NonNull final MusicItem musicItem) {
        return Single.create(new SingleOnSubscribe<Uri>() {
            @Override
            public void subscribe(SingleEmitter<Uri> emitter) throws Exception {
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
            notifyError(ErrorUtil.ERROR_NETWORK_UNAVAILABLE,
                    ErrorUtil.getErrorMessage(mApplicationContext, ErrorUtil.ERROR_NETWORK_UNAVAILABLE));
            return null;
        }

        if (!mPlayerState.isOnlyWifiNetwork()) {
            return getUri(musicItem, mPlayerState.getSoundQuality());
        }

        if (isWiFiNetwork()) {
            return getUri(musicItem, mPlayerState.getSoundQuality());
        }

        notifyError(ErrorUtil.ERROR_ONLY_WIFI_NETWORK,
                ErrorUtil.getErrorMessage(mApplicationContext, ErrorUtil.ERROR_ONLY_WIFI_NETWORK));

        return null;
    }

    private boolean isWiFiNetwork() {
        return mNetworkUtil.isWifiNetwork();
    }

    private void initAllListener() {
        mOnPreparedListener = new MusicPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(MusicPlayer mp) {
                mp.setLooping(mPlayerState.isLooping());

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
            public void onStalled() {
                notifyStalled();
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

                notifyError(ErrorUtil.ERROR_PLAYER_ERROR,
                        ErrorUtil.getErrorMessage(mApplicationContext, ErrorUtil.ERROR_PLAYER_ERROR));
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

    protected final boolean isPreparing() {
        return mPreparing;
    }

    protected final boolean isPrepared() {
        return mPrepared;
    }

    protected final boolean isPlaying() {
        return isPrepared() && mMusicPlayer.isPlaying();
    }

    protected final int getAudioSessionId() {
        if (isPrepared()) {
            return mMusicPlayer.getAudioSessionId();
        }

        return 0;
    }

    protected final void updatePlayProgress(long progress, long updateTime) {
        mPlayerState.setPlayProgress(progress);
        mPlayerState.setPlayProgressUpdateTime(updateTime);
    }

    public void addStateListener(@NonNull String token, @NonNull T listener) {
        Preconditions.checkNotNull(token);
        Preconditions.checkNotNull(listener);

        mStateListenerMap.put(token, listener);
    }

    public void removeStateListener(@NonNull String token) {
        Preconditions.checkNotNull(token);

        mStateListenerMap.remove(token);
    }

    protected HashMap<String, T> getAllStateListener() {
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

    private void notifyStalled() {
        mPlayerState.setPlaybackState(PlaybackState.STALLED);

        onStalled();

        for (String key : mStateListenerMap.keySet()) {
            PlayerStateListener listener = mStateListenerMap.get(key);
            if (listener != null) {
                listener.onStalled();
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

    @Override
    public void setLooping(boolean looping) {
        mPlayerState.setLooping(looping);

        if (isPrepared()) {
            mMusicPlayer.setLooping(looping);
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
    public void setSoundQuality(int soundQuality) {
        if (soundQuality == mPlayerState.getSoundQuality()) {
            return;
        }

        swapSoundQuality(soundQuality);
    }

    private void swapSoundQuality(int soundQuality) {
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
            notifyError(ErrorUtil.ERROR_ONLY_WIFI_NETWORK,
                    ErrorUtil.getErrorMessage(mApplicationContext, ErrorUtil.ERROR_ONLY_WIFI_NETWORK));
        }
    }

    @Override
    public void setIgnoreLossAudioFocus(boolean ignoreLossAudioFocus) {
        mPlayerState.setIgnoreLossAudioFocus(ignoreLossAudioFocus);
    }
}
