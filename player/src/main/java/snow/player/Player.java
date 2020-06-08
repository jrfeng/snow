package snow.player;

import androidx.annotation.Nullable;

/**
 * 该接口定义了音乐播放器的基本功能。
 */
public interface Player {
    /**
     * 开始播放。
     */
    void play();

    /**
     * 暂停播放。
     */
    void pause();

    /**
     * 停止播放。
     */
    void stop();

    /**
     * 开始或暂停播放。
     */
    void playOrPause();

    /**
     * 调整音乐播放进度。
     *
     * @param progress 要调整到的播放进度
     */
    void seekTo(long progress);

    /**
     * 快进。
     */
    void fastForward();

    /**
     * 快退。
     */
    void rewind();

    /**
     * 设置播放器的首选音质（默认为 {@link SoundQuality#STANDARD}）。
     *
     * @param soundQuality 要设置的音质
     * @see SoundQuality#STANDARD
     * @see SoundQuality#LOW
     * @see SoundQuality#HIGH
     * @see SoundQuality#SUPER
     */
    void setSoundQuality(int soundQuality);

    /**
     * 设置是否启用音频特效（如：均衡器）（默认为 false）。
     *
     * @param enabled 是否启用音频特效
     */
    void setAudioEffectEnabled(boolean enabled);

    /**
     * 设置是否只允许在 WiFi 网络下播放音乐（默认为 true）。
     *
     * @param onlyWifiNetwork 是否只允许在 WiFi 网络下播放音乐
     */
    void setOnlyWifiNetwork(boolean onlyWifiNetwork);

    /**
     * 设置是否忽略音频焦点的丢失（默认为 false）。
     * <p>
     * 如果设为 true，即使音频焦点丢失，当前播放器依然会继续播放。简单的说，就是是否可以和其他应用同时播放音频。
     *
     * @param ignoreLossAudioFocus 是否忽略音频焦点的丢失
     */
    void setIgnoreLossAudioFocus(boolean ignoreLossAudioFocus);

    /**
     * 播放器的首选音质。
     */
    class SoundQuality {
        /**
         * 标准音质。
         */
        public static final int STANDARD = 0;
        /**
         * 低音质。
         */
        public static final int LOW = 1;
        /**
         * 高音质。
         */
        public static final int HIGH = 2;
        /**
         * 超高音质（无损）。
         */
        public static final int SUPER = 3;
    }

    /**
     * 播放器的播放状态。
     */
    class PlaybackState {
        /**
         * 未知状态。
         */
        public static final int UNKNOWN = 0;
        /**
         * 准备中。
         */
        public static final int PREPARING = 1;
        /**
         * 准备完毕。
         */
        public static final int PREPARED = 2;
        /**
         * 播放中。
         */
        public static final int PLAYING = 3;
        /**
         * 已暂停。
         */
        public static final int PAUSED = 4;
        /**
         * 已停止。
         */
        public static final int STOPPED = 5;
        /**
         * 缓冲中（该状态下应该显示一个缓冲进度条）。
         */
        public static final int STALLED = 6;
        /**
         * 发送错误。
         */
        public static final int ERROR = 7;
    }

    /**
     * 播放器状态改变监听器。
     */
    interface OnPlaybackStateChangeListener {
        /**
         * 当播放器的状态变为 “准备中” 时会回调该方法。
         */
        void onPreparing();

        /**
         * 当播放器的状态变为 “准备完毕” 时会回调该方法。
         *
         * @param audioSessionId 当前音乐的 audio session id
         */
        void onPrepared(int audioSessionId);

        /**
         * 当播放器的状态变为 “播放中” 时会回调该方法。
         *
         * @param playProgress           当前的播放进度
         * @param playProgressUpdateTime 播放进度的更新时间
         */
        void onPlay(long playProgress, long playProgressUpdateTime);

        /**
         * 当播放器的状态变为 “暂停” 时会回调该方法。
         */
        void onPause();

        /**
         * 当播放器的状态变为 “停止” 时会回调该方法。
         */
        void onStop();

        /**
         * 当播放器的状态变为 “缓冲中” 时会回调该方法。
         */
        void onStalled();

        /**
         * 当播放器发生错误时会回调该方法。
         *
         * @param errorCode    错误码
         * @param errorMessage 错误信息
         */
        void onError(int errorCode, String errorMessage);
    }

    /**
     * 用于监听调整播放进度事件。
     */
    interface OnSeekCompleteListener {
        /**
         * 该方法会在播放进度调整完成时调用。
         */
        void onSeekComplete(long progress);
    }

    /**
     * 用于监听当前正在播放的音乐的缓冲进度。
     */
    interface OnBufferingPercentChangeListener {
        /**
         * 当缓冲进度发生改变时会回调该方法。
         *
         * @param percent    缓冲进度，表示百分比，值为 0~100，
         * @param updateTime 缓冲进度的更新时间
         */
        void onBufferingPercentChanged(int percent, long updateTime);
    }

    /**
     * 用于监听当前正在播放的音乐。
     */
    interface OnPlayingMusicItemChangeListener {
        /**
         * 当正在播放的音乐被改变时（例如：切换歌曲）会回调该方法。
         *
         * @param musicItem 当前正在播放的音乐
         */
        void onPlayingMusicItemChanged(@Nullable MusicItem musicItem);
    }
}
