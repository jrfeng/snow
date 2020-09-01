package snow.player.media;

import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.util.Log;

/**
 * 封装了一个 MediaPlayer。
 */
public class MediaMusicPlayer extends AbstractMusicPlayer {
    private static final String TAG = "MediaMusicPlayer";

    private MediaPlayer mMediaPlayer;
    private OnErrorListener mErrorListener;
    private OnStalledListener mStalledListener;

    private boolean mStalled;
    private boolean mInvalid;

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
                    mErrorListener.onError(MediaMusicPlayer.this, toErrorCode(what, extra));
                }
                return true;
            }
        });

        mMediaPlayer.setOnInfoListener(new MediaPlayer.OnInfoListener() {
            @Override
            public boolean onInfo(MediaPlayer mp, int what, int extra) {
                switch (what) {
                    case MediaPlayer.MEDIA_INFO_BUFFERING_START:
                        setStalled(true);
                        return true;
                    case MediaPlayer.MEDIA_INFO_BUFFERING_END:
                        setStalled(false);
                        return true;
                }

                return false;
            }
        });
    }

    private int toErrorCode(int what, int extra) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN_MR1) {
            switch (what) {
                case MediaPlayer.MEDIA_ERROR_UNKNOWN:
                    return ErrorCode.UNKNOWN_ERROR;
                case MediaPlayer.MEDIA_ERROR_SERVER_DIED:
                    return ErrorCode.PLAYER_ERROR;
            }
        }

        switch (extra) {
            case MediaPlayer.MEDIA_ERROR_IO:            // 注意！case 穿透！
            case MediaPlayer.MEDIA_ERROR_TIMED_OUT:
                return ErrorCode.DATA_LOAD_FAILED;
            case MediaPlayer.MEDIA_ERROR_MALFORMED:     // 注意！case 穿透！
            case MediaPlayer.MEDIA_ERROR_UNSUPPORTED:
            case -2147483648/*低级系统错误*/:
                return ErrorCode.PLAYER_ERROR;
            default:
                return ErrorCode.UNKNOWN_ERROR;
        }
    }

    private void setStalled(boolean stalled) {
        mStalled = stalled;
        if (mStalledListener != null) {
            mStalledListener.onStalled(mStalled);
        }
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
    public boolean isStalled() {
        return mStalled;
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
    public void startEx() {
        mMediaPlayer.start();
    }

    @Override
    public void pauseEx() {
        mMediaPlayer.pause();
    }

    @Override
    public void stopEx() {
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

    @Override
    public void releaseEx() {
        setInvalid();
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
        mStalledListener = listener;
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
