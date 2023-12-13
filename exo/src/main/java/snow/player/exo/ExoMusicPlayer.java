package snow.player.exo;

import static androidx.media3.common.C.WAKE_MODE_NETWORK;

import android.annotation.SuppressLint;
import android.content.Context;
import android.net.Uri;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import androidx.annotation.OptIn;
import androidx.media3.common.util.UnstableApi;
import androidx.media3.exoplayer.ExoPlaybackException;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.common.MediaItem;
import androidx.media3.common.PlaybackException;
import androidx.media3.common.PlaybackParameters;
import androidx.media3.common.Player;
import androidx.media3.exoplayer.source.MediaSource;
import androidx.media3.exoplayer.source.MediaSourceFactory;

import snow.player.audio.ErrorCode;
import snow.player.audio.MusicPlayer;

/**
 * 封装了一个 SimpleExoPlayer
 */
public class ExoMusicPlayer implements MusicPlayer {
    private static final String TAG = "ExoMusicPlayer";

    private ExoPlayer mExoPlayer;
    private Player.Listener mEventListener;

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

    private boolean mPreparing;
    private boolean mPlayerReady;

    private float mVolume = 1.0F;

    /**
     * 创建一个 {@link ExoMusicPlayer} 对象。
     *
     * @param context            Context 对象，不能为 null
     * @param mediaSourceFactory MediaSourceFactory 对象，不能为 null
     * @param uri                要播放的 Uri，不能为 null
     */
    @OptIn(markerClass = UnstableApi.class)
    @Deprecated
    public ExoMusicPlayer(@NonNull Context context, @NonNull MediaSourceFactory mediaSourceFactory, @NonNull Uri uri) {
        this(context, (MediaSource.Factory) mediaSourceFactory, uri);
    }

    /**
     * 创建一个 {@link ExoMusicPlayer} 对象。
     *
     * @param context            Context 对象，不能为 null
     * @param mediaSourceFactory MediaSourceFactory 对象，不能为 null
     * @param uri                要播放的 Uri，不能为 null
     */
    @OptIn(markerClass = UnstableApi.class)
    public ExoMusicPlayer(@NonNull Context context, @NonNull MediaSource.Factory mediaSourceFactory, @NonNull Uri uri) {
        initEventListener();
        initExoPlayer(context, mediaSourceFactory);

        mExoPlayer.setMediaItem(MediaItem.fromUri(uri));
    }

    /**
     * 创建一个 {@link ExoMusicPlayer} 对象。
     *
     * @param context Context 对象，不能为 null
     * @param uri     要播放的 Uri，不能为 null
     */
    @OptIn(markerClass = UnstableApi.class)
    public ExoMusicPlayer(@NonNull Context context, @NonNull Uri uri) {
        this(context, MediaItem.fromUri(uri));
    }

    /**
     * 创建一个 {@link ExoMusicPlayer} 对象。
     *
     * @param context   Context 对象，不能为 null
     * @param mediaItem 要播放的 MediaItem，不能为 null
     */
    @OptIn(markerClass = UnstableApi.class)
    public ExoMusicPlayer(@NonNull Context context, @NonNull MediaItem mediaItem) {
        initEventListener();
        initExoPlayer(context, null);

        mExoPlayer.setMediaItem(mediaItem);
    }

    private void initEventListener() {
        mEventListener = new Player.Listener() {
            @Override
            public void onIsLoadingChanged(boolean isLoading) {
                if (mBufferingUpdateListener != null) {
                    mBufferingUpdateListener.onBufferingUpdate(ExoMusicPlayer.this,
                            (int) mExoPlayer.getBufferedPosition(),
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
                mPlayerReady = true;
                notifyPrepared();

                if (isStalled()) {
                    setStalled(false);
                }
            }

            private void onEnd() {
                if (mCompletionListener != null) {
                    mCompletionListener.onCompletion(ExoMusicPlayer.this);
                }
            }

            @OptIn(markerClass = UnstableApi.class)
            @Override
            public void onPlayerError(@NonNull PlaybackException error) {
                setInvalid();

                Log.e(TAG, error.toString());
                error.printStackTrace();

                if (mErrorListener != null) {
                    mErrorListener.onError(ExoMusicPlayer.this, toErrorCode(error));
                }
            }

            @Override
            public void onPositionDiscontinuity(
                    @NonNull Player.PositionInfo oldPosition,
                    @NonNull Player.PositionInfo newPosition,
                    int reason
            ) {
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
        if (isPrepared() && mStalledListener != null) {
            mStalledListener.onStalled(mStalled);
        }
    }

    @OptIn(markerClass = UnstableApi.class)
    @SuppressLint("SwitchIntDef")
    private int toErrorCode(PlaybackException error) {
        if (!(error instanceof ExoPlaybackException)) {
            return ErrorCode.UNKNOWN_ERROR;
        }

        switch (((ExoPlaybackException) error).type) {
            case ExoPlaybackException.TYPE_SOURCE:
                return ErrorCode.DATA_LOAD_FAILED;
            case ExoPlaybackException.TYPE_REMOTE:
                return ErrorCode.NETWORK_ERROR;
            case ExoPlaybackException.TYPE_RENDERER:
                return ErrorCode.PLAYER_ERROR;
            default:
                return ErrorCode.UNKNOWN_ERROR;
        }
    }

    @OptIn(markerClass = UnstableApi.class)
    private void initExoPlayer(Context context, @Nullable MediaSource.Factory mediaSourceFactory) {
        ExoPlayer.Builder builder = new ExoPlayer.Builder(context)
                .setWakeMode(WAKE_MODE_NETWORK)
                .setLooper(Looper.getMainLooper());

        if (mediaSourceFactory != null) {
            builder.setMediaSourceFactory(mediaSourceFactory);
        }

        mExoPlayer = builder.build();
        mExoPlayer.addListener(mEventListener);
    }

    private void notifyPrepared() {
        if (mPreparing && isPrepared()) {
            mPreparing = false;
            onPrepared();
        }
    }

    private boolean isPrepared() {
        return mPlayerReady;
    }

    private void onPrepared() {
        if (mPreparedListener != null) {
            mPreparedListener.onPrepared(ExoMusicPlayer.this);
        }

        if (mStalled && mStalledListener != null) {
            mStalledListener.onStalled(true);
        }
    }

    @Override
    public void prepare() {
        if (isInvalid()) {
            return;
        }

        mPreparing = true;
        mExoPlayer.prepare();
    }

    @Override
    public void setLooping(boolean looping) {
        if (looping) {
            mExoPlayer.setRepeatMode(Player.REPEAT_MODE_ONE);
            return;
        }

        mExoPlayer.setRepeatMode(Player.REPEAT_MODE_OFF);
    }

    @Override
    public boolean isLooping() {
        return mExoPlayer.getRepeatMode() == Player.REPEAT_MODE_ONE;
    }

    @Override
    public boolean isStalled() {
        return mStalled;
    }

    @Override
    public boolean isPlaying() {
        return isPrepared() && (mExoPlayer.isPlaying() || mExoPlayer.getPlayWhenReady());
    }

    @Override
    public int getDuration() {
        return (int) mExoPlayer.getDuration();
    }

    @Override
    public int getProgress() {
        return (int) mExoPlayer.getCurrentPosition();
    }

    @Override
    public void start() {
        mExoPlayer.setPlayWhenReady(true);
    }

    @Override
    public void pause() {
        mExoPlayer.setPlayWhenReady(false);
    }

    @Override
    public void stop() {
        mExoPlayer.stop();
    }

    @Override
    public void quiet() {
        float volume = mVolume * 0.5F;
        mExoPlayer.setVolume(volume);
    }

    @Override
    public void dismissQuiet() {
        mExoPlayer.setVolume(mVolume);
    }

    @Override
    public void release() {
        setInvalid();
        mExoPlayer.release();
    }

    @Override
    public void seekTo(int pos) {
        mExoPlayer.seekTo(pos);
    }

    @Override
    public void setVolume(float volume) {
        mVolume = volume;
        mExoPlayer.setVolume(volume);
    }

    @Override
    public float getVolume() {
        return mExoPlayer.getVolume();
    }

    @Override
    public void setSpeed(float speed) {
        PlaybackParameters parameters = new PlaybackParameters(speed);
        mExoPlayer.setPlaybackParameters(parameters);
    }

    @Override
    public synchronized boolean isInvalid() {
        return mInvalid;
    }

    private synchronized void setInvalid() {
        mInvalid = true;
    }

    @OptIn(markerClass = UnstableApi.class)
    @Override
    public int getAudioSessionId() {
        return mExoPlayer.getAudioSessionId();
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
