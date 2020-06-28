package snow.player.test;

import java.io.IOException;
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

    private Tester mTester;

    public TestMusicPlayer() {
        mTester = new Tester(this);
    }

    public Tester tester() {
        return mTester;
    }

    public void reset() {
        mLooping = false;
        mPlaying = false;
        mCurrentPosition = 0;

        mTester.reset();
    }

    public static class Tester {
        private TestMusicPlayer mMusicPlayer;

        private boolean mBadDataSource;
        private boolean mError;

        private long mPreparedTime = 100;

        Tester(TestMusicPlayer testMusicPlayer) {
            mMusicPlayer = testMusicPlayer;
        }

        /**
         * 调用该方法后，会在调用 {@link MusicPlayer#setDataSource(String)} 方法时抛出 IOException 异常。请在调用
         * {@link MusicPlayer#setDataSource(String)} 方法前调用该方法。
         */
        public void badDataSource() {
            mBadDataSource = true;
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
                mMusicPlayer.mPlaying = false;
                mMusicPlayer.mErrorListener.onError(mMusicPlayer, errorCode);
            }
        }

        public void setDuration(int duration) {
            mMusicPlayer.mDuration = duration;
        }

        private void reset() {
            mBadDataSource = false;
            mError = false;
        }
    }

    @Override
    public void setDataSource(String path) throws IOException {
        if (mTester.mBadDataSource) {
            throw new IOException("Test: bad data source");
        }

        Random random = new Random();
        mAudioSessionId = random.nextInt(100);
    }

    @Override
    public void prepareAsync() {
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
    public int getCurrentPosition() {
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
        mPlaying = false;
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
