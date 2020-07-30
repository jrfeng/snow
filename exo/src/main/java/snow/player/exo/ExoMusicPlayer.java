package snow.player.exo;

import android.annotation.SuppressLint;
import android.content.Context;
import android.net.Uri;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;

import com.google.android.exoplayer2.ExoPlaybackException;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.source.MediaSourceFactory;

import snow.player.media.MusicPlayer;
import snow.player.util.ErrorUtil;

/**
 * 封装了一个 SimpleExoPlayer
 */
public class ExoMusicPlayer implements MusicPlayer {
    private static final String TAG = "ExoMusicPlayer";

    private MediaSourceFactory mMediaSourceFactory;
    private SimpleExoPlayer mSimpleExoPlayer;
    private Player.EventListener mEventListener;

    private OnPreparedListener mPreparedListener;
    private OnCompletionListener mCompletionListener;
    private OnSeekCompleteListener mSeekCompleteListener;
    private OnStalledListener mStalledListener;
    private OnBufferingUpdateListener mBufferingUpdateListener;
    private OnErrorListener mErrorListener;

    private boolean mStalled;
    private boolean mInvalid;

    public ExoMusicPlayer(@NonNull Context context, @NonNull MediaSourceFactory mediaSourceFactory) {
        mMediaSourceFactory = mediaSourceFactory;
        initEventListener();
        initExoPlayer(context);
    }

    private void initEventListener() {
        mEventListener = new Player.EventListener() {
            @Override
            public void onLoadingChanged(boolean isLoading) {
                if (mBufferingUpdateListener != null) {
                    mBufferingUpdateListener.onBufferingUpdate(ExoMusicPlayer.this,
                            (int) mSimpleExoPlayer.getBufferedPosition(),
                            false);
                }
            }

            @Override
            public void onPlayerStateChanged(boolean playWhenReady, int playbackState) {
                switch (playbackState) {
                    case Player.STATE_READY:
                        onReady();
                        break;
                    case Player.STATE_BUFFERING:
                        onStalled(true);
                        break;
                    case Player.STATE_ENDED:
                        onEnd();
                        break;
                    case Player.STATE_IDLE:
                        // ignore
                        break;
                }
            }

            private void onReady() {
                if (mPreparedListener != null) {
                    mPreparedListener.onPrepared(ExoMusicPlayer.this);
                    mPreparedListener = null;
                }

                if (mStalled) {
                    onStalled(false);
                }
            }

            private void onStalled(boolean stalled) {
                mStalled = stalled;
                if (mStalledListener != null) {
                    mStalledListener.onStalled(mStalled);
                }
            }

            private void onEnd() {
                if (mCompletionListener != null) {
                    mCompletionListener.onCompletion(ExoMusicPlayer.this);
                }
            }

            @Override
            public void onPlayerError(ExoPlaybackException error) {
                setInvalid();

                Log.e(TAG, error.toString());
                error.printStackTrace();

                if (mErrorListener != null) {
                    mErrorListener.onError(ExoMusicPlayer.this, toErrorCode(error));
                }
            }

            @Override
            public void onSeekProcessed() {
                if (mSeekCompleteListener != null) {
                    mSeekCompleteListener.onSeekComplete(ExoMusicPlayer.this);
                }
            }
        };
    }

    @SuppressLint("SwitchIntDef")
    private int toErrorCode(ExoPlaybackException error) {
        switch (error.type) {
            case ExoPlaybackException.TYPE_SOURCE:
                return ErrorUtil.DATA_LOAD_FAILED;
            case ExoPlaybackException.TYPE_REMOTE:
                return ErrorUtil.NETWORK_ERROR;
            default:
                return ErrorUtil.PLAYER_ERROR;
        }
    }

    private void initExoPlayer(Context context) {
        mSimpleExoPlayer = new SimpleExoPlayer.Builder(context)
                .setLooper(Looper.getMainLooper())
                .build();
        mSimpleExoPlayer.addListener(mEventListener);
    }

    @Override
    public void prepare(Uri uri) {
        if (isInvalid()) {
            return;
        }

        try {
            MediaSource mediaSource = mMediaSourceFactory.createMediaSource(uri);
            mSimpleExoPlayer.prepare(mediaSource);
        } catch (Exception e) {
            setInvalid();
            throw e;
        }
    }

    @Override
    public void setLooping(boolean looping) {
        if (looping) {
            mSimpleExoPlayer.setRepeatMode(Player.REPEAT_MODE_ONE);
            return;
        }

        mSimpleExoPlayer.setRepeatMode(Player.REPEAT_MODE_OFF);
    }

    @Override
    public boolean isLooping() {
        return mSimpleExoPlayer.getRepeatMode() == Player.REPEAT_MODE_ONE;
    }

    @Override
    public boolean isPlaying() {
        return mSimpleExoPlayer.isPlaying();
    }

    @Override
    public int getDuration() {
        return (int) mSimpleExoPlayer.getDuration();
    }

    @Override
    public int getProgress() {
        return (int) mSimpleExoPlayer.getCurrentPosition();
    }

    @Override
    public void start() {
        mSimpleExoPlayer.setPlayWhenReady(true);
    }

    @Override
    public void pause() {
        mSimpleExoPlayer.setPlayWhenReady(false);
    }

    @Override
    public void stop() {
        mSimpleExoPlayer.stop();
    }

    @Override
    public void seekTo(int pos) {
        mSimpleExoPlayer.seekTo(pos);
    }

    @Override
    public void setVolume(float leftVolume, float rightVolume) {
        mSimpleExoPlayer.setVolume(Math.max(leftVolume, rightVolume));
    }

    @Override
    public void quiet() {
        mSimpleExoPlayer.setVolume(0.2F);
    }

    @Override
    public void dismissQuiet() {
        mSimpleExoPlayer.setVolume(1.0F);
    }

    @Override
    public void release() {
        setInvalid();
        mSimpleExoPlayer.release();
    }

    @Override
    public synchronized boolean isInvalid() {
        return mInvalid;
    }

    private synchronized void setInvalid() {
        mInvalid = true;
    }

    @Override
    public int getAudioSessionId() {
        return mSimpleExoPlayer.getAudioSessionId();
    }

    @Override
    public void setOnPreparedListener(OnPreparedListener listener) {
        mPreparedListener = listener;
    }

    @Override
    public void setOnCompletionListener(OnCompletionListener listener) {
        mCompletionListener = listener;
    }

    @Override
    public void setOnSeekCompleteListener(OnSeekCompleteListener listener) {
        mSeekCompleteListener = listener;
    }

    @Override
    public void setOnStalledListener(OnStalledListener listener) {
        mStalledListener = listener;
    }

    @Override
    public void setOnBufferingUpdateListener(OnBufferingUpdateListener listener) {
        mBufferingUpdateListener = listener;
    }

    @Override
    public void setOnErrorListener(OnErrorListener listener) {
        mErrorListener = listener;
    }
}
