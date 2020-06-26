package snow.player;

import android.os.IBinder;

import channel.helper.Channel;
import snow.player.playlist.PlaylistState;
import snow.player.radio.RadioStationState;

/**
 * 该接口定义了用于管理播放器的基本功能。
 */
@Channel
public interface PlayerManager {
    int TYPE_PLAYLIST = 0;
    int TYPE_RADIO_STATION = 1;

    void shutdown();

    /**
     * 注册一个播放器状态监听器。
     *
     * @param token    监听器的 token。注意！不能为 null，且应该保证唯一性。
     * @param listener 监听器的 IBinder 对象（不能为 null）。
     */
    void registerPlayerStateListener(String token, IBinder listener);

    /**
     * 取消已注册的播放器状态监听器。
     *
     * @param token 已注册的监听器的 token（不能为 null）。如果监听器没有注册或者已经取消注册，那么将忽略此操作。
     */
    void unregisterPlayerStateListener(String token);

    /**
     * 用于监听播放器类型发生改变事件。
     */
    interface OnPlayerTypeChangeListener {
        /**
         * 当播放器类型发生改变时会回调该方法。
         *
         * @param playerType 播放器类型。共有两个值：{@link #TYPE_PLAYLIST}：列表播放器；
         *                   {@link #TYPE_RADIO_STATION}：电台播放器。
         */
        void onPlayerTypeChanged(int playerType);
    }

    /**
     * 用于接收服务端发送的命令。
     */
    @Channel
    interface OnCommandCallback extends OnPlayerTypeChangeListener {
        /**
         * 当服务端准备关闭时会回调该方法，此时客户端应主动断开与服务端的连接。
         */
        void onShutdown();

        /**
         * 同步客户端与服务端的播放器状态。
         *
         * @param playerType        播放器类型
         * @param playlistState     列表播放器的状态
         * @param radioStationState 电台播放器的状态
         */
        void syncPlayerState(int playerType, PlaylistState playlistState, RadioStationState radioStationState);
    }
}
