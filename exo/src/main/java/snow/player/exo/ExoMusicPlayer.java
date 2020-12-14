package snow.player.exo;

import android.annotation.SuppressLint;
import android.content.Context;
import android.net.Uri;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.exoplayer2.ExoPlaybackException;
import com.google.android.exoplayer2.MediaItem;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.source.MediaSourceFactory;

import snow.player.audio.AbstractMusicPlayer;
import snow.player.audio.ErrorCode;

/**
 * 封装了一个 SimpleExoPlayer
 */
public class ExoMusicPlayer extends AbstractMusicPlayer {
    private static final String TAG = "ExoMusicPlayer";

    private SimpleExoPlayer mSimpleExoPlayer;
    private Player.EventListener mEventListener;

    @Nullable
    private OnPreparedListener mPreparedListener;
    @Nullable
    private OnCompletionListener mCompletionListener;
    @Nullable
    private OnSeekCompleteListener mSeekCompleteListener;
    @Nullable
    private OnStalledListener mStalledListener;
    @Nullable
    private OnBufferingUpdateListener mBufferingUpdateListener;
    @Nullable
    private OnErrorListener mErrorListener;
    @Nullable
    private OnRepeatListener mRepeatListener;

    private boolean mStalled;
    private boolean mInvalid;

    /**
     * 创建一个 {@link ExoMusicPlayer} 对象。
     *
     * @param context            Context 对象，不能为 null
     * @param mediaSourceFactory MediaSourceFactory 对象，不能为 null
     * @param uri                要播放的 Uri，不能为 null
     */
    public ExoMusicPlayer(@NonNull Context context, @NonNull MediaSourceFactory mediaSourceFactory, @NonNull Uri uri) {
        initEventListener();
        initExoPlayer(context);

        MediaSource mediaSource = mediaSourceFactory.createMediaSource(MediaItem.fromUri(uri));
        mSimpleExoPlayer.setMediaSource(mediaSource);
    }

    /**
     * 创建一个 {@link ExoMusicPlayer} 对象。
     *
     * @param context Context 对象，不能为 null
     * @param uri     要播放的 Uri，不能为 null
     */
    public ExoMusicPlayer(@NonNull Context context, @NonNull Uri uri) {
        this(context, MediaItem.fromUri(uri));
    }

    /**
     * 创建一个 {@link ExoMusicPlayer} 对象。
     *
     * @param context   Context 对象，不能为 null
     * @param mediaItem 要播放的 MediaItem，不能为 null
     */
    public ExoMusicPlayer(@NonNull Context context, @NonNull MediaItem mediaItem) {
        initEventListener();
        initExoPlayer(context);

        mSimpleExoPlayer.setMediaItem(mediaItem);
    }

    private void initEventListener() {
        mEventListener = new Player.EventListener() {
            @Override
            public void onIsLoadingChanged(boolean isLoading) {
                if (mBufferingUpdateListener != null) {
                    mBufferingUpdateListener.onBufferingUpdate(ExoMusicPlayer.this,
                            (int) mSimpleExoPlayer.getBufferedPosition(),
                            false);
                }
            }

            @Override
            public void onPlaybackStateChanged(int state) {
                switch (state) {
                    case Player.STATE_READY:
                        onReady();
                        break;
                    case Player.STATE_BUFFERING:
                        setStalled(true);
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

                if (isStalled()) {
                    setStalled(false);
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
            public void onPositionDiscontinuity(int reason) {
                if (reason == Player.DISCONTINUITY_REASON_SEEK
                        && mSeekCompleteListener != null) {
                    mSeekCompleteListener.onSeekComplete(ExoMusicPlayer.this);
                }
            }

            @Override
            public void onMediaItemTransition(@Nullable MediaItem mediaItem, int reason) {
                if (reason == Player.MEDIA_ITEM_TRANSITION_REASON_REPEAT
                        && mRepeatListener != null) {
                    mRepeatListener.onRepeat(ExoMusicPlayer.this);
                }
            }
        };
    }

    private void setStalled(boolean stalled) {
        mStalled = stalled;
        if (mStalledListener != null) {
            mStalledListener.onStalled(mStalled);
        }
    }

    @SuppressLint("SwitchIntDef")
    private int toErrorCode(ExoPlaybackException error) {
        switch (error.type) {
            case ExoPlaybackException.TYPE_SOURCE:
                return ErrorCode.DATA_LOAD_FAILED;
            case ExoPlaybackException.TYPE_REMOTE:
                return ErrorCode.NETWORK_ERROR;
            case ExoPlaybackException.TYPE_OUT_OF_MEMORY:
                return ErrorCode.OUT_OF_MEMORY;
            case ExoPlaybackException.TYPE_RENDERER:
                return ErrorCode.PLAYER_ERROR;
            default:
                return ErrorCode.UNKNOWN_ERROR;
        }
    }

    private void initExoPlayer(Context context) {
        mSimpleExoPlayer = new SimpleExoPlayer.Builder(context)
                .setLooper(Looper.getMainLooper())
                .build();
        mSimpleExoPlayer.addListener(mEventListener);
    }

    @Override
    public void prepare() {
        if (isInvalid()) {
            return;
        }

        mSimpleExoPlayer.prepare();
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
    public boolean isStalled() {
        return mStalled;
    }

    @Override
    public boolean isPlaying() {
        return mSimpleExoPlayer.isPlaying() || mSimpleExoPlayer.getPlayWhenReady();
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
    public void startEx() {
        mSimpleExoPlayer.setPlayWhenReady(true);
    }

    @Override
    public void pauseEx() {
        mSimpleExoPlayer.setPlayWhenReady(false);
    }

    @Override
    public void stopEx() {
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
    public void releaseEx() {
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
    public void setOnPreparedListener(@Nullable OnPreparedListener listener) {
        mPreparedListener = listener;
    }

    @Override
    public void setOnCompletionListener(@Nullable OnCompletionListener listener) {
        mCompletionListener = listener;
    }

    @Override
    public void setOnRepeatListener(@Nullable OnRepeatListener listener) {
        mRepeatListener = listener;
    }

    @Override
    public void setOnSeekCompleteListener(@Nullable OnSeekCompleteListener listener) {
        mSeekCompleteListener = listener;
    }

    @Override
    public void setOnStalledListener(@Nullable OnStalledListener listener) {
        mStalledListener = listener;
    }

    @Override
    public void setOnBufferingUpdateListener(@Nullable OnBufferingUpdateListener listener) {
        mBufferingUpdateListener = listener;
    }

    @Override
    public void setOnErrorListener(@Nullable OnErrorListener listener) {
        mErrorListener = listener;
    }
}
