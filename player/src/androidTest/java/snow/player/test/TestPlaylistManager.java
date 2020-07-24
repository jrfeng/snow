package snow.player.test;

import android.content.Context;

import androidx.annotation.NonNull;

import snow.player.playlist.Playlist;
import snow.player.playlist.PlaylistManager;

public class TestPlaylistManager extends PlaylistManager {
    private Playlist mPlaylist;
    private Callback mCallback;

    public TestPlaylistManager(@NonNull Context context, Playlist playlist) {
        super(context, "TestPlaylistManager");
        mPlaylist = playlist;
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
        mCallback = callback;
    }

    public void updateInternalPlaylist(Playlist playlist) {
        mPlaylist = playlist;
    }

    public void notifyPlaylistLoaded() {
        if (mCallback != null) {
            mCallback.onFinished(mPlaylist);
        }
    }
}
