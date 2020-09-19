package snow.player.lifecycle;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;

import com.google.common.base.Preconditions;

import java.util.ArrayList;

import snow.player.Player;
import snow.player.PlayerClient;
import snow.player.audio.MusicItem;
import snow.player.playlist.Playlist;
import snow.player.playlist.PlaylistManager;

/**
 * 用于监听 {@link PlayerClient} 的播放列表。
 * <p>
 * {@link PlaylistLiveData} 是惰性的，它只会在 onActive 时开始监听 {@link PlayerClient} 的播放列表，
 * 并且会在 onInactive 时自动取消对 {@link PlayerClient} 的播放列表的监听。
 * <p>
 * 最后，当你不再需要 {@link PlaylistLiveData} 时，应该调用 {@link #release()} 方法将其释放。
 */
public class PlaylistLiveData extends LiveData<Playlist>
        implements Player.OnPlaylistChangeListener {
    private static final String TAG = "PlaylistLiveData";
    private PlayerClient mPlayerClient;
    private boolean mActive;

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

        if (mActive) {
            observePlaylist();
        }
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
        mActive = true;
        if (notInit()) {
            Log.e(TAG, "PlaylistLiveData not init.");
            return;
        }

        observePlaylist();
    }

    @Override
    protected void onInactive() {
        mActive = false;
        if (notInit()) {
            return;
        }
        mPlayerClient.removeOnPlaylistChangeListener(this);
    }

    @Override
    public void onPlaylistChanged(PlaylistManager playlistManager, int position) {
        updatePlaylist(playlistManager);
    }

    private void observePlaylist() {
        mPlayerClient.addOnPlaylistChangeListener(this);
        updatePlaylist(mPlayerClient.getPlaylistManager());
    }

    private void updatePlaylist(PlaylistManager playlistManager) {
        playlistManager.getPlaylist(new PlaylistManager.Callback() {
            @Override
            public void onFinished(@NonNull Playlist playlist) {
                setValue(playlist);
            }
        });
    }
}
