package snow.player;

import android.os.IBinder;

import channel.helper.Channel;
import channel.helper.UseOrdinal;
import snow.player.playlist.PlaylistState;
import snow.player.radio.RadioStationState;

/**
 * 该接口定义了用于管理播放器的基本功能。
 */
@Channel
public interface PlayerManager {
    /**
     * 播放器类型
     */
    enum PlayerType {
        /**
         * 列表播放器
         */
        PLAYLIST,
        /**
         * 电台播放器
         */
        RADIO_STATION
    }

    /**
     * 设置播放器的首选音质（默认为 {@link Player.SoundQuality#STANDARD}）。
     *
     * @param soundQuality 要设置的音质
     * @see Player.SoundQuality#STANDARD
     * @see Player.SoundQuality#LOW
     * @see Player.SoundQuality#HIGH
     * @see Player.SoundQuality#SUPER
     */
    void setSoundQuality(@UseOrdinal Player.SoundQuality soundQuality);

    /**
     * 修改音频特性的配置。
     */
    void setAudioEffectConfig(AudioEffectEngine.Config config);

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
     * 关闭播放器并终止 Service。
     */
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
         * @param playerType 播放器类型。共有两个值：{@link PlayerType#PLAYLIST}：列表播放器；
         *                   {@link PlayerType#RADIO_STATION}：电台播放器。
         */
        void onPlayerTypeChanged(@UseOrdinal PlayerType playerType);
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
        void syncPlayerState(@UseOrdinal PlayerType playerType, PlaylistState playlistState, RadioStationState radioStationState);
    }
}
