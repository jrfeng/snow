package snow.player;

import android.content.Context;
import android.content.res.Resources;

import androidx.annotation.Nullable;

import snow.player.media.MusicItem;

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
    void seekTo(int progress);

    /**
     * 快进。
     */
    void fastForward();

    /**
     * 快退。
     */
    void rewind();

    /**
     * 播放器的首选音质。
     */
    enum SoundQuality {
        /**
         * 标准音质。
         */
        STANDARD,
        /**
         * 低音质。
         */
        LOW,
        /**
         * 高音质。
         */
        HIGH,
        /**
         * 超高音质（无损）。
         */
        SUPER
    }

    /**
     * 播放器的播放状态。
     */
    enum PlaybackState {
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

    /**
     * 预定义错误码。
     */
    final class Error {
        public static final int NO_ERROR = 0;
        public static final int ONLY_WIFI_NETWORK = 1;
        public static final int PLAYER_ERROR = 2;
        public static final int NETWORK_UNAVAILABLE = 3;
        public static final int FILE_NOT_FOUND = 4;
        public static final int DATA_LOAD_FAILED = 5;

        private Error() {
            throw new AssertionError();
        }

        public static String getErrorMessage(Context context, int errorCode) {
            Resources res = context.getResources();

            switch (errorCode) {
                case NO_ERROR:
                    return res.getString(R.string.snow_error_no_error);
                case ONLY_WIFI_NETWORK:
                    return res.getString(R.string.snow_error_only_wifi_network);
                case PLAYER_ERROR:
                    return res.getString(R.string.snow_error_player_error);
                case NETWORK_UNAVAILABLE:
                    return res.getString(R.string.snow_error_network_unavailable);
                case FILE_NOT_FOUND:
                    return res.getString(R.string.snow_error_file_not_found);
                case DATA_LOAD_FAILED:
                    return res.getString(R.string.snow_error_data_load_failed);
                default:
                    return res.getString(R.string.snow_error_unknown_error);
            }
        }
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
        void onPlay(int playProgress, long playProgressUpdateTime);

        /**
         * 当播放器的状态变为 “暂停” 时会回调该方法。
         */
        void onPause();

        /**
         * 当播放器的状态变为 “停止” 时会回调该方法。
         */
        void onStop();

        /**
         * 当播放器发生错误时会回调该方法。
         *
         * @param errorCode    错误码
         * @param errorMessage 错误信息
         */
        void onError(int errorCode, String errorMessage);
    }

    /**
     * 用于监听播放器的 stalled 状态。
     * <p>
     * 当没有足够的缓存数据支撑播放器继续播放时，播放器会进入 stalled 状态。
     */
    interface OnStalledChangeListener {
        /**
         * 当播放器的 stalled 状态发生改变时会回调该方法。
         *
         * @param stalled 没有足够的缓存数据支撑播放器继续播放时，该参数为 true；当播放器缓存了足够的数据可
         *                以继续播放时，该参数为 false。
         */
        void onStalledChanged(boolean stalled);
    }

    /**
     * 用于监听调整播放进度事件。
     */
    interface OnSeekListener {

        /**
         * 该方法会在执行 seek 操作前调用
         */
        void onSeeking();

        /**
         * 该方法会在 seek 操作执行完成时调用。
         *
         * @param progress   当前的播放进度
         * @param updateTime 当前播放进度的更新时间
         */
        void onSeekComplete(int progress, long updateTime);
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
