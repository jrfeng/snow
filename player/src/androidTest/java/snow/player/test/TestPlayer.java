package snow.player.test;

import android.content.Context;
import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.IOException;

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
        private boolean looping;
        private boolean cached;
        private TestMusicPlayer musicPlayer;

        public void setTestMusicPlayer(TestMusicPlayer musicPlayer) {
            this.musicPlayer = musicPlayer;
        }

        public void setLooping(boolean looping) {
            this.looping = looping;
        }

        public void setCached(boolean cached) {
            this.cached = cached;
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
    }

    @Override
    public boolean isLooping() {
        return mTester.looping;
    }

    @Override
    protected boolean isCached(MusicItem musicItem, SoundQuality soundQuality) {
        return mTester.cached;
    }

    @Nullable
    @Override
    protected Uri getCachedUri(MusicItem musicItem, SoundQuality soundQuality) {
        return mTester.getCachedUri();
    }

    @Nullable
    @Override
    protected Uri getUri(MusicItem musicItem, SoundQuality soundQuality) {
        return mTester.getUri();
    }

    @NonNull
    @Override
    protected MusicPlayer onCreateMusicPlayer(Uri uri) throws IOException {
        mTester.musicPlayer.reset();
        mTester.musicPlayer.setDataSource(uri);
        return mTester.musicPlayer;
    }
}
