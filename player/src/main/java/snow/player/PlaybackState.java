package snow.player;

/**
 * 播放器的播放状态。
 */
public enum PlaybackState {
    /**
     * 未知状态。
     */
    UNKNOWN,
    /**
     * 准备中。
     */
    PREPARING,
    /**
     * 准备完毕。
     */
    PREPARED,
    /**
     * 播放中。
     */
    PLAYING,
    /**
     * 已暂停。
     */
    PAUSED,
    /**
     * 已停止。
     */
    STOPPED,
    /**
     * 发生错误。
     */
    ERROR
}
