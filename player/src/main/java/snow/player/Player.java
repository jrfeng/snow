package snow.player;

import android.content.Context;
import android.content.res.Resources;

import androidx.annotation.Nullable;

import channel.helper.UseOrdinal;
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
     * 设置是否循环播放。
     *
     * @param looping 是否循环播放。
     */
    void setLooping(boolean looping);

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
    void setSoundQuality(@UseOrdinal SoundQuality soundQuality);

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
         * 发送错误。
         */
        public static final int ERROR = 7;
    }

    /**
     * 预定义错误码。
     */
    class Error {
        public static final int NO_ERROR = 0;
        public static final int ONLY_WIFI_NETWORK = 1;
        public static final int PLAYER_ERROR = 2;
        public static final int NETWORK_UNAVAILABLE = 3;
        public static final int FILE_NOT_FOUND = 4;
        public static final int DATA_LOAD_FAILED = 5;

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
