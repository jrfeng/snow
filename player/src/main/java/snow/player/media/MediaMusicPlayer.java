package snow.player.media;

import android.content.Context;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.PowerManager;
import android.util.Log;

import androidx.annotation.NonNull;

import java.io.IOException;

import snow.player.util.ErrorUtil;

/**
 * 封装了一个 MediaPlayer。注意！不允许复用 MediaMusicPlayer 对象。一旦调用 {@link #release()}
 * 或者 {@link #stop()} 方法后就不要再调用 MediaMusicPlayer 对象的任何方法。
 */
public class MediaMusicPlayer extends MusicPlayer {
    private static final String TAG = "MediaMusicPlayer";

    private MediaPlayer mMediaPlayer;
    private OnStalledListener mStalledListener;

    public MediaMusicPlayer(@NonNull Context context) {
        super(context);
        mMediaPlayer = new MediaPlayer();
        mMediaPlayer.setWakeMode(context, PowerManager.PARTIAL_WAKE_LOCK);
    }

    @Override
    public void setDataSource(Context context, Uri uri) throws IOException {
        mMediaPlayer.setDataSource(context, uri);
    }

    @Override
    public void prepare() throws IOException {
        mMediaPlayer.prepare();
    }

    @Override
    public void prepareAsync() throws IllegalStateException {
        mMediaPlayer.prepareAsync();
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
        super.start();
        mMediaPlayer.start();
    }

    @Override
    public void pause() {
        super.pause();
        mMediaPlayer.pause();
    }

    @Override
    public void stop() {
        super.stop();
        mMediaPlayer.stop();
    }

    @Override
    public void seekTo(long pos) {
        mMediaPlayer.seekTo((int) pos);
    }

    @Override
    public void setVolume(float leftVolume, float rightVolume) {
        mMediaPlayer.setVolume(leftVolume, rightVolume);
    }

    @Override
    public void volumeDuck() {
        setVolume(0.4F, 0.4F);
    }

    @Override
    public void volumeRestore() {
        setVolume(1.0F, 1.0F);
    }

    @Override
    public void release() {
        super.release();
        mMediaPlayer.release();
        mMediaPlayer = null;
        mStalledListener = null;
    }

    @Override
    public int getAudioSessionId() {
        return mMediaPlayer.getAudioSessionId();
    }

    @Override
    public void setOnPreparedListener(final OnPreparedListener listener) {
        mMediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(MediaPlayer mp) {
                listener.onPrepared(MediaMusicPlayer.this);
            }
        });
    }

    @Override
    public void setOnCompletionListener(final OnCompletionListener listener) {
        mMediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mp) {
                listener.onCompletion(MediaMusicPlayer.this);
            }
        });
    }

    @Override
    public void setOnSeekCompleteListener(final OnSeekCompleteListener listener) {
        mMediaPlayer.setOnSeekCompleteListener(new MediaPlayer.OnSeekCompleteListener() {
            @Override
            public void onSeekComplete(MediaPlayer mp) {
                listener.onSeekComplete(MediaMusicPlayer.this);
            }
        });
    }

    @Override
    public void setOnStalledListener(OnStalledListener listener) {
        mStalledListener = listener;
    }

    @Override
    public void setOnBufferingUpdateListener(final OnBufferingUpdateListener listener) {
        mMediaPlayer.setOnBufferingUpdateListener(new MediaPlayer.OnBufferingUpdateListener() {
            @Override
            public void onBufferingUpdate(MediaPlayer mp, int percent) {
                listener.onBufferingUpdate(MediaMusicPlayer.this, percent);
            }
        });
    }

    @Override
    public void setOnErrorListener(final OnErrorListener listener) {
        mMediaPlayer.setOnErrorListener(new MediaPlayer.OnErrorListener() {
            @Override
            public boolean onError(MediaPlayer mp, int what, int extra) {
                Log.e(TAG, "MediaPlayer Error[what: " + what + ", extra: " + extra + "]");
                releaseWakeLock();

                listener.onError(MediaMusicPlayer.this,
                        ErrorUtil.ERROR_PLAYER_ERROR);
                return true;
            }
        });
    }
}
