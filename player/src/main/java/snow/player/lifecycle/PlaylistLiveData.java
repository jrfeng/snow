package snow.player.lifecycle;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;

import com.google.common.base.Preconditions;

import java.util.ArrayList;

import snow.player.Player;
import snow.player.PlayerClient;
import snow.player.media.MusicItem;
import snow.player.playlist.Playlist;
import snow.player.playlist.PlaylistManager;

/**
 * 用于监听 {@link PlayerClient} 对象的播放列表。
 */
public class PlaylistLiveData extends LiveData<Playlist>
        implements Player.OnPlaylistChangeListener {
    private static final String TAG = "PlaylistLiveData";
    private PlayerClient mPlayerClient;

    public PlaylistLiveData() {
        super(new Playlist(new ArrayList<MusicItem>()));
    }

    /**
     * 初始化 {@link PlaylistLiveData}。
     *
     * @param playerClient 要监听的 PlayerClient 对象
     */
    public void init(@NonNull PlayerClient playerClient) {
        Preconditions.checkNotNull(playerClient);
        mPlayerClient = playerClient;
    }

    /**
     * 释放 PlaylistLiveData，调用该方法后请不要再尝试使用 PlaylistLiveData，因为这可能会导致未知错误。
     */
    public void release() {
        onInactive();
        mPlayerClient = null;
    }

    private boolean notInit() {
        return mPlayerClient == null;
    }

    @Override
    protected void onActive() {
        if (notInit()) {
            Log.e(TAG, "PlaylistLiveData not init.");
            return;
        }

        mPlayerClient.addOnPlaylistChangeListener(this);
        updatePlaylist(mPlayerClient.getPlaylistManager());
    }

    @Override
    protected void onInactive() {
        if (notInit()) {
            return;
        }
        mPlayerClient.removeOnPlaylistChangeListener(this);
    }

    @Override
    public void onPlaylistChanged(PlaylistManager playlistManager, int position) {
        updatePlaylist(playlistManager);
    }

    private void updatePlaylist(PlaylistManager playlistManager) {
        playlistManager.getPlaylistAsync(new PlaylistManager.Callback() {
            @Override
            public void onFinished(@NonNull Playlist playlist) {
                setValue(playlist);
            }
        });
    }
}
