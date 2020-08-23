package snow.player.playlist;

import androidx.annotation.NonNull;

/**
 * 用于获取播放器的播放队列。
 */
public interface PlaylistManager {

    /**
     * 获取当前播放队列的大小。
     * <p>
     * 这是个轻量级操作，可在 UI 线程上直接运行。
     *
     * @return 当前播放队列的大小。
     */
    int getPlaylistSize();

    /**
     * 以异步的方式获取播放队列。
     */
    void getPlaylist(@NonNull Callback callback);

    /**
     * {@link #getPlaylist(Callback)} 方法的回调接口。
     */
    interface Callback {
        /**
         * 当获取播放列表成功时会调用该方法。
         *
         * @param playlist 当前播放列表
         */
        void onFinished(@NonNull Playlist playlist);
    }
}
