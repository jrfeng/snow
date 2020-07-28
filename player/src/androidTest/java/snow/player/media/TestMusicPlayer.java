package snow.player.media;

import android.net.Uri;
import android.util.Log;

import java.util.Random;

public class TestMusicPlayer implements MusicPlayer {
    private static final String TAG = "TestMusicPlayer";

    private OnPreparedListener mPreparedListener;
    private OnCompletionListener mCompletionListener;
    private OnSeekCompleteListener mSeekCompleteListener;
    private OnStalledListener mStalledListener;
    private OnBufferingUpdateListener mBufferingUpdateListener;
    private OnErrorListener mErrorListener;

    private int mAudioSessionId;
    private int mDuration;

    private boolean mLooping;
    private boolean mPlaying;
    private int mPlayProgress;

    private boolean mInvalid;

    private int mSeekToProgress;

    public TestMusicPlayer(int duration) {
        mDuration = duration;
        mInvalid = false;
        mAudioSessionId = new Random().nextInt(100);
    }

    @Override
    public void prepare(Uri uri) {
        Log.d(TAG, uri.toString());
    }

    @Override
    public void setLooping(boolean looping) {
        mLooping = looping;
    }

    @Override
    public boolean isLooping() {
        return mLooping;
    }

    @Override
    public boolean isPlaying() {
        return mPlaying;
    }

    @Override
    public int getDuration() {
        return mDuration;
    }

    @Override
    public int getProgress() {
        return mPlayProgress;
    }

    @Override
    public void start() {
        if (isInvalid()) {
            return;
        }

        mPlaying = true;
    }

    @Override
    public void pause() {
        if (isInvalid()) {
            return;
        }

        mPlaying = false;
    }

    @Override
    public void stop() {
        if (isInvalid()) {
            return;
        }

        mPlaying = false;
    }

    @Override
    public void release() {
        setInvalid();
        mPlaying = false;
    }

    @Override
    public synchronized boolean isInvalid() {
        return mInvalid;
    }

    private synchronized void setInvalid() {
        mInvalid = true;
    }

    @Override
    public void seekTo(int pos) {
        if (isInvalid()) {
            return;
        }

        if (pos < 0) {
            mSeekToProgress = 0;
            return;
        }

        if (pos > mDuration) {
            mSeekToProgress = mDuration;
            return;
        }

        mSeekToProgress = pos;
    }

    @Override
    public void setVolume(float leftVolume, float rightVolume) {
        // ignore
    }

    @Override
    public void quiet() {
        // ignore
    }

    @Override
    public void dismissQuiet() {
        // ignore
    }

    @Override
    public int getAudioSessionId() {
        return mAudioSessionId;
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

    public void notifyPrepared() {
        if (mPreparedListener != null) {
            mPreparedListener.onPrepared(this);
        }
    }

    public void notifySeekComplete() {
        if (mSeekCompleteListener != null) {
            mPlayProgress = mSeekToProgress;
            mSeekCompleteListener.onSeekComplete(this);
        }
    }

    public void notifyCompletion() {
        if (mCompletionListener != null) {
            mCompletionListener.onCompletion(this);
        }
    }

    public void notifyStalled(boolean stalled) {
        if (mStalledListener != null) {
            mStalledListener.onStalled(stalled);
        }
    }

    public void notifyBufferingUpdate(int buffered, boolean isPercent) {
        if (mBufferingUpdateListener != null) {
            mBufferingUpdateListener.onBufferingUpdate(this, buffered, isPercent);
        }
    }

    public void notifyError(int errorCode) {
        setInvalid();
        mPlaying = false;

        if (mErrorListener != null) {
            mErrorListener.onError(this, errorCode);
        }
    }
}
