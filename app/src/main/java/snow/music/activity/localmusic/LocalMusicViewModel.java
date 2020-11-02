package snow.music.activity.localmusic;

import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModel;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import snow.music.store.Music;
import snow.player.playlist.Playlist;

public class LocalMusicViewModel extends ViewModel {
    private List<Music> mMusicList;
    private Playlist mPlaylist;
    private long mPlaylistLastModified;

    public LocalMusicViewModel() {
        mMusicList = Collections.emptyList();
        mPlaylist = new Playlist.Builder().build();
    }

    public void setMusicList(@NonNull List<Music> musicList) {
        mMusicList = musicList;
    }

    @NonNull
    public List<Music> getMusicList() {
        return mMusicList;
    }

    public void setPlaylist(@NonNull Playlist playlist) {
        mPlaylist = playlist;
    }

    @NonNull
    public Playlist getPlaylist() {
        return mPlaylist;
    }

    public void setPlaylistLastModified(long playlistLastModified) {
        mPlaylistLastModified = playlistLastModified;
    }

    public long getPlaylistLastModified() {
        return mPlaylistLastModified;
    }
}
