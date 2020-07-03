package snow.player.exo;

import android.content.Context;
import android.net.Uri;
import android.os.Looper;

import androidx.annotation.NonNull;

import com.google.android.exoplayer2.ExoPlaybackException;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.source.MediaSourceFactory;
import com.google.android.exoplayer2.source.ProgressiveMediaSource;
import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory;
import com.google.android.exoplayer2.util.Util;

import snow.player.media.MusicPlayer;

/**
 * 封装了一个 SimpleExoPlayer
 */
public class ExoMusicPlayer implements MusicPlayer {
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
                    int percent = (int) ((mSimpleExoPlayer.getBufferedPosition() * 1.0) / mSimpleExoPlayer.getDuration()) * 100;
                    mBufferingUpdateListener.onBufferingUpdate(ExoMusicPlayer.this, percent);
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
                if (mErrorListener != null) {
                    mErrorListener.onError(ExoMusicPlayer.this, snow.player.Player.Error.PLAYER_ERROR);
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

    private void initExoPlayer(Context context) {
        mSimpleExoPlayer = new SimpleExoPlayer.Builder(context)
                .setLooper(Looper.getMainLooper())
                .build();
        mSimpleExoPlayer.addListener(mEventListener);
    }

    @Override
    public void prepare(Uri uri) {
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
    public int getCurrentPosition() {
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
