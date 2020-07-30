package snow.player;

import androidx.annotation.Nullable;

import channel.helper.Channel;
import channel.helper.UseOrdinal;
import snow.player.media.MusicItem;
import snow.player.playlist.PlaylistManager;

/**
 * 该接口定义了音乐播放器的基本功能。
 */
@Channel
public interface Player extends PlaylistManager.OnModifyPlaylistListener {
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
    void playPause();

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
    void playPause(int position);

    /**
     * 设置播放器的播放模式。
     *
     * @param playMode 播放模式
     * @see PlayMode
     */
    void setPlayMode(@UseOrdinal PlayMode playMode);

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
         * @param stalled      没有足够的缓存数据支撑播放器继续播放时，该参数为 true；当播放器缓存了足够的数据可
         *                     以继续播放时，该参数为 false。
         * @param playProgress 当前播放进度
         * @param updateTime   当前播放进度的更新时间
         */
        void onStalledChanged(boolean stalled, int playProgress, long updateTime);
    }

    /**
     * 用于监听调整播放进度事件。
     */
    interface OnSeekCompleteListener {

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
    interface OnBufferedProgressChangeListener {
        /**
         * 当缓冲进度发生改变时会回调该方法。
         *
         * @param bufferedProgress 缓冲进度，范围为 [0, duration]
         */
        void onBufferedProgressChanged(int bufferedProgress);
    }

    /**
     * 用于监听当前正在播放的音乐。
     */
    interface OnPlayingMusicItemChangeListener {
        /**
         * 当正在播放的音乐被改变时（例如：切换歌曲）会回调该方法。
         *
         * @param musicItem    当前正在播放的音乐
         * @param playProgress 歌曲的播放进度
         */
        void onPlayingMusicItemChanged(@Nullable MusicItem musicItem, int playProgress);
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
