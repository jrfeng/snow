package snow.player.test;

import android.content.Context;
import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.IOException;

import snow.player.media.MusicItem;
import snow.player.media.MusicPlayer;
import snow.player.playlist.AbstractPlaylistPlayer;
import snow.player.playlist.Playlist;
import snow.player.playlist.PlaylistManager;
import snow.player.playlist.PlaylistState;

public class TestPlaylistPlayer extends AbstractPlaylistPlayer {
    private Tester mTester;

    public TestPlaylistPlayer(@NonNull Context context,
                              @NonNull PlaylistState playlistState,
                              @NonNull PlaylistManager playlistManager) {
        super(context, playlistState, playlistManager);

        mTester = new Tester();
    }

    public Tester tester() {
        return mTester;
    }

    public static class Tester {
        private Runnable mPlayingAction;
        private Runnable mPlaylistAvailableAction;

        public void setPlayingAction(Runnable action) {
            mPlayingAction = action;
        }

        public void setOnPlaylistAvailableAction(Runnable action) {
            mPlaylistAvailableAction = action;
        }
    }

    @Override
    protected void onPlaying(long progress, long updateTime) {
        super.onPlaying(progress, updateTime);

        if (mTester.mPlayingAction != null) {
            mTester.mPlayingAction.run();
            mTester.mPlayingAction = null;
        }
    }

    @Override
    protected void onPlaylistAvailable(Playlist playlist) {
        super.onPlaylistAvailable(playlist);

        if (mTester.mPlaylistAvailableAction != null) {
            mTester.mPlaylistAvailableAction.run();
            mTester.mPlaylistAvailableAction = null;
        }
    }

    @Override
    protected boolean isCached(MusicItem musicItem, SoundQuality soundQuality) {
        return false;
    }

    @Nullable
    @Override
    protected Uri getCachedUri(MusicItem musicItem, SoundQuality soundQuality) {
        return Uri.parse(musicItem.getUri());
    }

    @Nullable
    @Override
    protected Uri getUri(MusicItem musicItem, SoundQuality soundQuality) {
        return Uri.parse(musicItem.getUri());
    }

    @NonNull
    @Override
    protected MusicPlayer onCreateMusicPlayer(Uri uri) throws IOException {
        MusicPlayer musicPlayer = new TestMusicPlayer();
        musicPlayer.setDataSource(uri.toString());
        return musicPlayer;
    }
}
