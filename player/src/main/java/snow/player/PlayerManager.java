package snow.player;

import android.os.Bundle;

import androidx.annotation.NonNull;

import channel.helper.Channel;
import channel.helper.UseOrdinal;

/**
 * 该接口定义了用于管理播放器的基本功能。
 */
@Channel
public interface PlayerManager {

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
     * 修改音频特效的配置。
     */
    void setAudioEffectConfig(Bundle config);

    /**
     * 设置是否启用音频特效（如：均衡器）（默认为 false）。
     *
     * @param enabled 是否启用音频特效
     */
    void setAudioEffectEnabled(boolean enabled);

    /**
     * 设置是否只允许在 WiFi 网络下联网（默认为 false）。
     *
     * @param onlyWifiNetwork 是否只允许在 WiFi 网络下联网
     */
    void setOnlyWifiNetwork(boolean onlyWifiNetwork);

    /**
     * 关闭播放器并终止 Service。
     */
    void shutdown();

    /**
     * 同步客户端与服务端的状态信息。
     * <p>
     * 该方法会在客户端连接成功后调用，以同步客户端与服务端的状态信息。
     *
     * @param clientToken 客户端的 token。不能为 null，且应该保证该参数的唯一性。该 token 会在
     *                    {@link OnCommandCallback#onSyncPlayerState(String, PlayerState)} 方法中返回，
     *                    用于鉴别是否是当前客户端客户端。
     */
    void syncPlayerState(String clientToken);

    /**
     * 用于接收服务端发送的命令。
     */
    @Channel
    interface OnCommandCallback {
        /**
         * 当服务端准备关闭时会回调该方法，此时客户端应主动断开与服务端的连接。
         */
        void onShutdown();

        /**
         * 用于在客户端成功连接后，同步客户端与服务端的状态。
         *
         * @param clientToken   客户端的 token
         * @param playlistState 列表播放器的状态
         */
        @SuppressWarnings("NullableProblems")
        void onSyncPlayerState(@NonNull String clientToken, @NonNull PlayerState playlistState);
    }
}
