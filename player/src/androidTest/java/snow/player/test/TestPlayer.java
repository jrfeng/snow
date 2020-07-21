package snow.player.test;

import android.content.Context;
import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import snow.player.AbstractPlayer;
import snow.player.PlayerConfig;
import snow.player.media.MusicItem;
import snow.player.media.MusicPlayer;
import snow.player.PlayerState;
import snow.player.PlayerStateListener;
import snow.player.playlist.PlaylistManager;

public class TestPlayer extends AbstractPlayer {
    private Tester mTester;

    public TestPlayer(@NonNull Context context,
                      @NonNull PlayerConfig playerConfig,
                      @NonNull PlayerState playerState,
                      @NonNull PlaylistManager playlistManager) {
        super(context, playerConfig, playerState, playlistManager);

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
    protected void onPlaying(int progress, long updateTime) {
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

    @Nullable
    @Override
    protected Uri retrieveMusicItemUri(@NonNull MusicItem musicItem, @NonNull SoundQuality soundQuality) throws Exception {
        return Uri.parse(musicItem.getUri());
    }
}
