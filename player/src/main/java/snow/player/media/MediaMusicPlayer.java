package snow.player.media;

import android.media.MediaPlayer;
import android.net.Uri;
import android.util.Log;

import snow.player.Player;

/**
 * 封装了一个 MediaPlayer。
 */
public class MediaMusicPlayer implements MusicPlayer {
    private static final String TAG = "MediaMusicPlayer";

    private MediaPlayer mMediaPlayer;
    private OnErrorListener mErrorListener;

    private boolean mInvalid;

    public MediaMusicPlayer() {
        mMediaPlayer = new MediaPlayer();
        mInvalid = false;

        mMediaPlayer.setOnErrorListener(new MediaPlayer.OnErrorListener() {
            @Override
            public boolean onError(MediaPlayer mp, int what, int extra) {
                Log.e(TAG, "MediaPlayer Error[what: " + what + ", extra: " + extra + "]");

                setInvalid();

                if (mErrorListener != null) {
                    mErrorListener.onError(MediaMusicPlayer.this, Player.Error.PLAYER_ERROR);
                }
                return true;
            }
        });
    }

    @Override
    public void prepare(Uri uri) throws Exception {
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
    public int getCurrentPosition() {
        return mMediaPlayer.getCurrentPosition();
    }

    @Override
    public void start() {
        mMediaPlayer.start();
    }

    @Override
    public void pause() {
        mMediaPlayer.pause();
    }

    @Override
    public void stop() {
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
    public void quiet() {
        setVolume(0.4F, 0.4F);
    }

    @Override
    public void dismissQuiet() {
        setVolume(1.0F, 1.0F);
    }

    @Override
    public synchronized void release() {
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
                listener.onBufferingUpdate(MediaMusicPlayer.this, percent);
            }
        });
    }

    @Override
    public void setOnErrorListener(OnErrorListener listener) {
        mErrorListener = listener;
    }
}
