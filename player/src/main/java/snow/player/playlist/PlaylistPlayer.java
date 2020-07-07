package snow.player.playlist;

import channel.helper.Channel;
import channel.helper.UseOrdinal;
import snow.player.Player;

/**
 * 该接口定义 Playlist 播放器的基本结构。
 */
@Channel
public interface PlaylistPlayer extends Player, PlaylistManager.OnModifyPlaylistListener {
    /**
     * 下一首音乐。
     */
    void skipToNext();

    /**
     * 上一曲。
     */
    void skipToPrevious();

    /**
     * 播放队列中 position 位置处的音乐。
     */
    void playOrPause(int position);

    /**
     * 设置播放器的播放模式。
     *
     * @param playMode 播放模式
     * @see PlayMode
     */
    void setPlayMode(@UseOrdinal PlayMode playMode);

    /**
     * 播放器的播放模式。
     */
    enum PlayMode {
        /**
         * 顺序播放。
         */
        SEQUENTIAL,
        /**
         * 单曲循环。
         */
        LOOP,
        /**
         * 随机播放。
         */
        SHUFFLE
    }

    /**
     * 用于监听播放队列被替换/修改事件。
     * <p>
     * 当设置新的播放队列或者修改已有的播放队列时，该监听器会被调用。
     */
    interface OnPlaylistChangeListener {
        /**
         * 播放队列被替换或修改时会调用该方法。
         *
         * @param playlistManager 用于管理播放队列的 PlaylistManager 对象。可以通过该对象获取到最新的播放队列。
         * @param position        播放队列的播放位置。
         */
        void onPlaylistChanged(PlaylistManager playlistManager, int position);
    }

    /**
     * 用于监听播放模式改变事件。
     */
    interface OnPlayModeChangeListener {
        /**
         * 当播放模式被改变时会调用该方法。
         *
         * @param playMode 当前的播放模式。
         * @see PlayMode#SEQUENTIAL
         * @see PlayMode#LOOP
         * @see PlayMode#SHUFFLE
         */
        void onPlayModeChanged(@UseOrdinal PlayMode playMode);
    }

    /**
     * 用于监听当前正在播放的音乐的位置（position）改变事件。
     */
    interface OnPositionChangeListener {
        /**
         * 当正在播放的音乐的位置（position）发生改变时会调用该方法。
         *
         * @param position 当前正在播放的音乐的新位置
         */
        void onPositionChanged(int position);
    }
}
