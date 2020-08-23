package snow.player.playlist;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import snow.player.media.MusicItem;

/**
 * 用于管理音乐播放器的播放队列。
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
     * 设置新的播放列表，并将播放队列的播放位置设为 position 值，同时设置是否在 prepare 完成后自动播放音乐。
     *
     * @param playlist 新的播放列表（不能为 null）
     * @param position 要设置的播放位置值（小于 0 时，相当于设为 0）
     * @param play     否在自动播放 position 位置处的音乐
     */
    void setPlaylist(@NonNull Playlist playlist, int position, boolean play);

    /**
     * 设置一个监听器用于监听设置新的播放列表事件
     *
     * @param listener 监听器，如果为 null，会清除已设置的监听器
     */
    void setOnNewPlaylistListener(@Nullable OnNewPlaylistListener listener);

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

    /**
     * 用于监听设置新的播放列表事件。
     */
    interface OnNewPlaylistListener {

        /**
         * 设置了一个新的播放列表。
         *
         * @param musicItem 要播放的音乐
         * @param position  要播放的音乐在播放列表中的位置，不能小于 0
         * @param play      是否播放 position 处的音乐
         */
        void onNewPlaylist(MusicItem musicItem, int position, boolean play);
    }
}
