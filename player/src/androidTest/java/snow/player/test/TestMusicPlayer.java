package snow.player.test;

import android.net.Uri;

import java.util.Random;

import snow.player.media.MusicPlayer;

public class TestMusicPlayer implements MusicPlayer {
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
    private int mCurrentPosition;

    private boolean mInvalid;

    private Tester mTester;

    public TestMusicPlayer() {
        mInvalid = false;
        mAudioSessionId = new Random().nextInt(100);

        mTester = new Tester(this);
    }

    public Tester tester() {
        return mTester;
    }

    public void reset() {
        mInvalid = false;
        mLooping = false;
        mPlaying = false;
        mCurrentPosition = 0;

        mTester.reset();
    }

    public static class Tester {
        private TestMusicPlayer mMusicPlayer;
        private boolean mError;
        private long mPreparedTime = 100;

        Tester(TestMusicPlayer testMusicPlayer) {
            mMusicPlayer = testMusicPlayer;
        }

        public void setPreparedTime(long ms) {
            mPreparedTime = ms;
        }

        public void setPlayPosition(int currentPosition) {
            mMusicPlayer.mCurrentPosition = currentPosition;
        }

        public void completion() {
            mMusicPlayer.mCompletionListener.onCompletion(mMusicPlayer);
        }

        public void stalled(boolean stalled) {
            mMusicPlayer.mStalledListener.onStalled(stalled);
        }

        public void bufferingUpdate(int percent) {
            mMusicPlayer.mBufferingUpdateListener.onBufferingUpdate(mMusicPlayer, percent);
        }

        public void setError(boolean error, int errorCode) {
            mError = error;

            if (mError) {
                mMusicPlayer.setInvalid();
                mMusicPlayer.mPlaying = false;
                mMusicPlayer.mErrorListener.onError(mMusicPlayer, errorCode);
            }
        }

        public void setDuration(int duration) {
            mMusicPlayer.mDuration = duration;
        }

        private void reset() {
            mError = false;
        }
    }

    @Override
    public void prepare(Uri uri) {
        new Thread() {
            @Override
            public void run() {
                try {
                    Thread.sleep(mTester.mPreparedTime);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                if (mPreparedListener != null) {
                    mPreparedListener.onPrepared(TestMusicPlayer.this);
                }
            }
        }.start();
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
        return mCurrentPosition;
    }

    @Override
    public void start() {
        if (mTester.mError) {
            return;
        }

        mPlaying = true;
    }

    @Override
    public void pause() {
        if (mTester.mError) {
            return;
        }

        mPlaying = false;
    }

    @Override
    public void stop() {
        if (mTester.mError) {
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
        if (mTester.mError) {
            return;
        }

        mCurrentPosition = pos;

        if (mSeekCompleteListener != null) {
            mSeekCompleteListener.onSeekComplete(this);
        }
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
}
