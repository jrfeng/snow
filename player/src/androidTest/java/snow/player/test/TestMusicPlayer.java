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
        Random random = new Random();

        mAudioSessionId = random.nextInt(100);
        mDuration = 60_000 + random.nextInt(300_000);

        mTester = new Tester(this);
    }

    public Tester tester() {
        return mTester;
    }

    public void reset() {
        mLooping = false;
        mPlaying = false;
        mCurrentPosition = 0;
    }

    public static class Tester {
        private TestMusicPlayer musicPlayer;

        private boolean badDataSource;
        private boolean error;

        private int seekToPosition;

        Tester(TestMusicPlayer testMusicPlayer) {
            musicPlayer = testMusicPlayer;
        }

        /**
         * 调用该方法后，会在调用 {@link MusicPlayer#setDataSource(String)} 方法时抛出 IOException 异常。请在调用
         * {@link MusicPlayer#setDataSource(String)} 方法前调用该方法。
         */
        public void badDataSource() {
            badDataSource = true;
        }

        public void setPlayPosition(int currentPosition) {
            musicPlayer.mCurrentPosition = currentPosition;
        }

        public void prepareSuccess() {
            musicPlayer.mPreparedListener.onPrepared(musicPlayer);
        }

        public void completion() {
            musicPlayer.mCompletionListener.onCompletion(musicPlayer);
        }

        public void seekComplete() {
            musicPlayer.mCurrentPosition = seekToPosition;
            musicPlayer.mSeekCompleteListener.onSeekComplete(musicPlayer);
        }

        public void stalled(boolean stalled) {
            musicPlayer.mStalledListener.onStalled(stalled);
        }

        public void bufferingUpdate(int percent) {
            musicPlayer.mBufferingUpdateListener.onBufferingUpdate(musicPlayer, percent);
        }

        public void error(int errorCode) {
            error = true;

            musicPlayer.mPlaying = false;
            musicPlayer.mErrorListener.onError(musicPlayer, errorCode);
        }
    }

    @Override
    public void setDataSource(String path) throws IOException {
        if (mTester.badDataSource) {
            throw new IOException("Test: bad data source");
        }
    }

    @Override
    public void prepareAsync() {

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
        if (mTester.error) {
            return;
        }

        mPlaying = true;
    }

    @Override
    public void pause() {
        if (mTester.error) {
            return;
        }

        mPlaying = false;
    }

    @Override
    public void stop() {
        if (mTester.error) {
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
        if (mTester.error) {
            return;
        }

        mTester.seekToPosition = pos;
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
