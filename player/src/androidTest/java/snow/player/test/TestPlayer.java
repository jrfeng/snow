package snow.player.test;

import android.content.Context;
import android.net.Uri;

import androidx.annotation.NonNull;

import snow.player.AbstractPlayer;
import snow.player.media.MusicItem;
import snow.player.media.MusicPlayer;
import snow.player.PlayerState;
import snow.player.PlayerStateListener;

public class TestPlayer extends AbstractPlayer<PlayerStateListener> {
    private Tester mTester;

    public TestPlayer(@NonNull Context context, @NonNull PlayerState playerState) {
        super(context, playerState);

        mTester = new Tester();
    }

    public Tester tester() {
        return mTester;
    }

    public class Tester {
        private boolean mLooping;
        private boolean mCached;
        private TestMusicPlayer mTestMusicPlayer;
        private int mEffectAudioSessionId;
        private Runnable mDoOnPlaying;
        private Runnable mDoOnError;

        public void setTestMusicPlayer(TestMusicPlayer musicPlayer) {
            mTestMusicPlayer = musicPlayer;
        }

        public void setLooping(boolean looping) {
            this.mLooping = looping;
        }

        public void setCached(boolean cached) {
            this.mCached = cached;
        }

        public Uri getCachedUri() {
            if (getMusicItem() == null) {
                return null;
            }

            return Uri.parse(getMusicItem().getUri());
        }

        public Uri getUri() {
            if (getMusicItem() == null) {
                return null;
            }

            return Uri.parse(getMusicItem().getUri());
        }

        public int getEffectAudioSessionId() {
            return mEffectAudioSessionId;
        }

        public void doOnPlaying(Runnable action) {
            mDoOnPlaying = action;
        }

        public void doOnError(Runnable action) {
            mDoOnError = action;
        }
    }

    @Override
    protected void onPlaying(long progress, long updateTime) {
        super.onPlaying(progress, updateTime);

        if (mTester.mDoOnPlaying != null) {
            mTester.mDoOnPlaying.run();
            mTester.mDoOnPlaying = null;
        }
    }

    @Override
    protected void onError(int errorCode, String errorMessage) {
        super.onError(errorCode, errorMessage);

        if (mTester.mDoOnError != null) {
            mTester.mDoOnError.run();
            mTester.mDoOnError = null;
        }
    }

    @Override
    protected void attachAudioEffect(int audioSessionId) {
        super.attachAudioEffect(audioSessionId);
        mTester.mEffectAudioSessionId = audioSessionId;
    }

    @Override
    protected void detachAudioEffect() {
        super.detachAudioEffect();
        mTester.mEffectAudioSessionId = 0;
    }

    @Override
    public boolean isLooping() {
        return mTester.mLooping;
    }

    @Override
    protected boolean isCached(MusicItem musicItem, SoundQuality soundQuality) {
        return mTester.mCached;
    }

    @NonNull
    @Override
    protected MusicPlayer onCreateMusicPlayer(Context context) {
        mTester.mTestMusicPlayer.reset();
        return mTester.mTestMusicPlayer;
    }

    @Override
    protected void onPrepareMusicPlayer(MusicPlayer musicPlayer,
                                        MusicItem musicItem,
                                        SoundQuality soundQuality) throws Exception {
        if (musicPlayer.isInvalid()) {
            return;
        }

        musicPlayer.prepare(Uri.parse(musicItem.getUri()));
    }
}
