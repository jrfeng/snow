package snow.player;

/**
 * 播放器的播放模式。
 *
 * @see PlayerClient#setPlayMode(PlayMode)
 */
public enum PlayMode {
    /**
     * 播放列表循环。
     */
    PLAYLIST_LOOP,
    /**
     * 单曲循环。
     */
    LOOP,
    /**
     * 随机播放。
     */
    SHUFFLE
}
