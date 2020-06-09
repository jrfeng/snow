package snow.player;

import android.content.Context;
import android.media.AudioManager;
import android.net.Uri;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.common.base.Preconditions;

import java.util.HashMap;

import media.helper.AudioFocusHelper;
import media.helper.BecomeNoiseHelper;
import snow.player.state.PlayerState;
import snow.player.state.PlayerStateListener;
import snow.player.util.ErrorUtil;
import snow.player.util.NetworkUtil;

public abstract class AbstractPlayer implements Player {
    private static final int FORWARD_STEP = 15_000;     // 15 秒, 单位：毫秒 ms

    private Context mApplicationContext;
    private PlayerState mPlayerState;
    private HashMap<String, PlayerStateListener> mPlayerStateListenerMap;

    private MusicPlayer mMusicPlayer;

    private MusicPlayer.OnPreparedListener mOnPreparedListener;
    private MusicPlayer.OnCompletionListener mOnCompletionListener;
    private MusicPlayer.OnSeekCompleteListener mOnSeekCompleteListener;
    private MusicPlayer.OnStalledListener mOnStalledListener;
    private MusicPlayer.OnBufferingUpdateListener mOnBufferingUpdateListener;
    private MusicPlayer.OnErrorListener mOnErrorListener;

    private boolean mPreparing;
    private boolean mPrepared;

    private Runnable mPreparedAction;
    private Runnable mSeekCompleteAction;

    private AudioFocusHelper mAudioFocusHelper;
    private BecomeNoiseHelper mBecomeNoiseHelper;

    private NetworkUtil mNetworkUtil;

    private int mBufferingPercent;

    public AbstractPlayer(@NonNull Context context, @NonNull PlayerState playerState) {
        Preconditions.checkNotNull(context);
        Preconditions.checkNotNull(playerState);

        mApplicationContext = context.getApplicationContext();
        mPlayerState = playerState;

        mPlayerStateListenerMap = new HashMap<>();

        initAllListener();
        initAllHelper();

        mNetworkUtil.subscribeNetworkState();
    }

    // ********************************abstract********************************

    protected abstract boolean isCached(MusicItem musicItem, int soundQuality);

    @Nullable
    protected abstract Uri getCachedUri(MusicItem musicItem, int soundQuality);

    @Nullable
    protected abstract Uri getUri(MusicItem musicItem, int soundQuality);

    @NonNull
    protected abstract MusicPlayer onCreateMusicPlayer(Uri uri);

    // ********************************abstract end******************************

    protected void onPlayComplete() {
    }

    protected void attachAudioEffect(int audioSessionId) {
    }

    protected void detachAudioEffect() {
    }

    protected void onRequestAudioFocus(boolean success) {
    }

    protected void onError(int errorCode, String errorMessage) {
    }

    protected void onStalled() {
    }

    protected void onRelease() {
    }

    protected final void release() {
        releaseMusicPlayer();
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
        mBufferingPercent = 0;

        MusicItem musicItem = mPlayerState.getMusicItem();
        if (musicItem == null) {
            return;
        }

        Uri uri = getMusicItemUri(musicItem);

        if (uri == null) {
            return;
        }

        mMusicPlayer = onCreateMusicPlayer(uri);
        attachListeners(mMusicPlayer);

        mPreparedAction = preparedAction;
        notifyPreparing();
        mMusicPlayer.prepareAsync();
    }

    @Nullable
    private Uri getMusicItemUri(@NonNull MusicItem musicItem) {
        if (isCached(musicItem, mPlayerState.getSoundQuality())) {
            mBufferingPercent = 100;
            return getCachedUri(musicItem, mPlayerState.getSoundQuality());
        }

        return getUriFromInternet(musicItem);
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
                onPlayComplete();
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
            }

            @Override
            public void onLossTransient() {
                if (mPlayerState.isIgnoreLossAudioFocus()) {
                    return;
                }

                playing = isPlaying();
                pause();
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
        return mBufferingPercent < 100;
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
        if (isPrepared()) {
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

    public void addPlayerStateListener(@NonNull String token, @NonNull PlayerStateListener listener) {
        Preconditions.checkNotNull(token);
        Preconditions.checkNotNull(listener);

        mPlayerStateListenerMap.put(token, listener);
    }

    public void removePlayerStateListener(@NonNull String token) {
        Preconditions.checkNotNull(token);

        mPlayerStateListenerMap.remove(token);
    }

    private void notifyPreparing() {
        mPlayerState.setPlaybackState(PlaybackState.PREPARING);

        mPreparing = true;
        mPrepared = false;

        for (String key : mPlayerStateListenerMap.keySet()) {
            PlayerStateListener listener = mPlayerStateListenerMap.get(key);
            if (listener != null) {
                listener.onPreparing();
            }
        }
    }

    private void notifyPrepared(int audioSessionId) {
        mPlayerState.setPlaybackState(PlaybackState.PREPARED);

        mPreparing = false;
        mPrepared = true;

        for (String key : mPlayerStateListenerMap.keySet()) {
            PlayerStateListener listener = mPlayerStateListenerMap.get(key);
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

        for (String key : mPlayerStateListenerMap.keySet()) {
            PlayerStateListener listener = mPlayerStateListenerMap.get(key);
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

        for (String key : mPlayerStateListenerMap.keySet()) {
            PlayerStateListener listener = mPlayerStateListenerMap.get(key);
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

        for (String key : mPlayerStateListenerMap.keySet()) {
            PlayerStateListener listener = mPlayerStateListenerMap.get(key);
            if (listener != null) {
                listener.onStop();
            }
        }
    }

    private void notifyStalled() {
        mPlayerState.setPlaybackState(PlaybackState.STALLED);
        onStalled();

        for (String key : mPlayerStateListenerMap.keySet()) {
            PlayerStateListener listener = mPlayerStateListenerMap.get(key);
            if (listener != null) {
                listener.onStalled();
            }
        }
    }

    private void notifyError(int errorCode, String errorMessage) {
        mPlayerState.setPlaybackState(PlaybackState.ERROR);

        updatePlayProgress(0, System.currentTimeMillis());

        mAudioFocusHelper.abandonAudioFocus();
        mBecomeNoiseHelper.unregisterBecomeNoiseReceiver();

        onError(errorCode, errorMessage);

        for (String key : mPlayerStateListenerMap.keySet()) {
            PlayerStateListener listener = mPlayerStateListenerMap.get(key);
            if (listener != null) {
                listener.onError(errorCode, errorMessage);
            }
        }
    }

    private void notifyBufferingPercentChanged(int percent, long updateTime) {
        mBufferingPercent = percent;

        for (String key : mPlayerStateListenerMap.keySet()) {
            PlayerStateListener listener = mPlayerStateListenerMap.get(key);
            if (listener != null) {
                listener.onBufferingPercentChanged(percent, updateTime);
            }
        }
    }

    protected final void notifyPlayingMusicItemChanged(@Nullable MusicItem musicItem, final boolean playingOnPrepared) {
        releaseMusicPlayer();
        updatePlayProgress(0, System.currentTimeMillis());

        mPlayerState.setMusicItem(musicItem);

        prepareMusicPlayer(new Runnable() {
            @Override
            public void run() {
                if (playingOnPrepared) {
                    play();
                }
            }
        });

        for (String key : mPlayerStateListenerMap.keySet()) {
            PlayerStateListener listener = mPlayerStateListenerMap.get(key);
            if (listener != null) {
                listener.onPlayingMusicItemChanged(musicItem);
            }
        }
    }

    private void notifySeekComplete(long position) {
        mPlayerState.setPlayProgress(position);

        for (String key : mPlayerStateListenerMap.keySet()) {
            PlayerStateListener listener = mPlayerStateListenerMap.get(key);
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
        final boolean playing = isPlaying();
        seekTo(progress, new Runnable() {
            @Override
            public void run() {
                if (playing) {
                    mMusicPlayer.start();
                }
            }
        });
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
