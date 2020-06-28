package snow.player.test;

import android.content.Context;

import androidx.annotation.NonNull;

import snow.player.playlist.Playlist;
import snow.player.playlist.PlaylistManager;

public class TestPlaylistManager extends PlaylistManager {
    private Tester mTester;
    private Playlist mPlaylist;

    public TestPlaylistManager(@NonNull Context context, Playlist playlist) {
        super(context, "TestPlaylistManager");
        mPlaylist = playlist;
        mTester = new Tester();
    }

    public Tester tester() {
        return mTester;
    }

    public class Tester {
        private long mLoadPlaylistDelay = 100;

        public void setLoadPlaylistDelay(long loadPlaylistDelay) {
            mLoadPlaylistDelay = loadPlaylistDelay;
        }

        public void setPlaylist(Playlist playlist) {
            mPlaylist = playlist;
        }
    }

    @Override
    public int getPlaylistSize() {
        return mPlaylist.size();
    }

    @NonNull
    @Override
    public Playlist getPlaylist() {
        return mPlaylist;
    }

    @Override
    public void getPlaylistAsync(final Callback callback) {
        new Thread() {
            @Override
            public void run() {
                try {
                    Thread.sleep(mTester.mLoadPlaylistDelay);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                callback.onFinished(mPlaylist);
            }
        }.start();
    }
}
