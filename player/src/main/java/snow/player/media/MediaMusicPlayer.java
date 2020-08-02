package snow.player.media;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.media.MediaPlayer;
import android.net.Uri;
import android.util.Log;

import snow.player.util.ErrorUtil;

/**
 * 封装了一个 MediaPlayer。
 */
public class MediaMusicPlayer implements MusicPlayer {
    private static final String TAG = "MediaMusicPlayer";

    private MediaPlayer mMediaPlayer;
    private OnErrorListener mErrorListener;

    private boolean mInvalid;

    private ObjectAnimator mStartVolumeAnimator;
    private ObjectAnimator mPauseVolumeAnimator;

    /**
     * 创建一个 {@link MediaMusicPlayer} 对象。
     */
    public MediaMusicPlayer() {
        mMediaPlayer = new MediaPlayer();
        mInvalid = false;

        mMediaPlayer.setOnErrorListener(new MediaPlayer.OnErrorListener() {
            @Override
            public boolean onError(MediaPlayer mp, int what, int extra) {
                Log.e(TAG, "MediaPlayer Error[what: " + what + ", extra: " + extra + "]");

                setInvalid();

                if (mErrorListener != null) {
                    mErrorListener.onError(MediaMusicPlayer.this, ErrorUtil.PLAYER_ERROR);
                }
                return true;
            }
        });

        initVolumeAnimator();
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
                mMediaPlayer.pause();
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                mMediaPlayer.pause();
            }
        });
    }

    @Override
    public void prepare(Uri uri) throws Exception {
        if (isInvalid()) {
            return;
        }

        try {
            mMediaPlayer.setDataSource(uri.toString());
            mMediaPlayer.prepareAsync();
        } catch (Exception e) {
            setInvalid();
            throw e;
        }
    }

    @Override
    public void setLooping(boolean looping) {
        mMediaPlayer.setLooping(looping);
    }

    @Override
    public boolean isLooping() {
        return mMediaPlayer.isLooping();
    }

    @Override
    public boolean isPlaying() {
        return mMediaPlayer.isPlaying();
    }

    @Override
    public int getDuration() {
        return mMediaPlayer.getDuration();
    }

    @Override
    public int getProgress() {
        return mMediaPlayer.getCurrentPosition();
    }

    @Override
    public void start() {
        cancelVolumeAnim();
        setVolume(0.0F);
        mMediaPlayer.start();
        mStartVolumeAnimator.start();
    }

    @Override
    public void pause() {
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
        cancelVolumeAnim();
        mMediaPlayer.stop();
    }

    @Override
    public void seekTo(int pos) {
        mMediaPlayer.seekTo(pos);
    }

    @Override
    public void setVolume(float leftVolume, float rightVolume) {
        mMediaPlayer.setVolume(leftVolume, rightVolume);
    }

    /**
     * 该方法相当于 {@code setVolume(volume, volume)}
     *
     * @see #setVolume(float, float)
     */
    public void setVolume(float volume) {
        setVolume(volume, volume);
    }

    @Override
    public void quiet() {
        setVolume(0.2F, 0.2F);
    }

    @Override
    public void dismissQuiet() {
        setVolume(1.0F, 1.0F);
    }

    @Override
    public synchronized void release() {
        setInvalid();
        cancelVolumeAnim();
        if (mMediaPlayer != null) {
            mMediaPlayer.release();
            mMediaPlayer = null;
        }
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
        return mMediaPlayer.getAudioSessionId();
    }

    @Override
    public void setOnPreparedListener(final OnPreparedListener listener) {
        if (listener == null) {
            mMediaPlayer.setOnPreparedListener(null);
            return;
        }

        mMediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(MediaPlayer mp) {
                listener.onPrepared(MediaMusicPlayer.this);
            }
        });
    }

    @Override
    public void setOnCompletionListener(final OnCompletionListener listener) {
        if (listener == null) {
            mMediaPlayer.setOnCompletionListener(null);
            return;
        }

        mMediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mp) {
                listener.onCompletion(MediaMusicPlayer.this);
            }
        });
    }

    @Override
    public void setOnSeekCompleteListener(final OnSeekCompleteListener listener) {
        if (listener == null) {
            mMediaPlayer.setOnSeekCompleteListener(null);
            return;
        }

        mMediaPlayer.setOnSeekCompleteListener(new MediaPlayer.OnSeekCompleteListener() {
            @Override
            public void onSeekComplete(MediaPlayer mp) {
                listener.onSeekComplete(MediaMusicPlayer.this);
            }
        });
    }

    @Override
    public void setOnStalledListener(final OnStalledListener listener) {
        if (listener == null) {
            mMediaPlayer.setOnInfoListener(null);
            return;
        }

        mMediaPlayer.setOnInfoListener(new MediaPlayer.OnInfoListener() {
            @Override
            public boolean onInfo(MediaPlayer mp, int what, int extra) {
                switch (what) {
                    case MediaPlayer.MEDIA_INFO_BUFFERING_START:
                        listener.onStalled(true);
                        return true;
                    case MediaPlayer.MEDIA_INFO_BUFFERING_END:
                        listener.onStalled(false);
                        return true;
                }

                return false;
            }
        });
    }

    @Override
    public void setOnBufferingUpdateListener(final OnBufferingUpdateListener listener) {
        if (listener == null) {
            mMediaPlayer.setOnBufferingUpdateListener(null);
            return;
        }

        mMediaPlayer.setOnBufferingUpdateListener(new MediaPlayer.OnBufferingUpdateListener() {
            @Override
            public void onBufferingUpdate(MediaPlayer mp, int percent) {
                listener.onBufferingUpdate(MediaMusicPlayer.this, percent, true);
            }
        });
    }

    @Override
    public void setOnErrorListener(OnErrorListener listener) {
        mErrorListener = listener;
    }
}
