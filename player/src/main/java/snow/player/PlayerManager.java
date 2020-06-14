package snow.player;

import android.os.IBinder;

import channel.helper.Channel;
import snow.player.state.PlaylistState;
import snow.player.state.RadioStationState;

/**
 * 该接口定义了用于管理播放器的基本功能。
 */
@Channel
public interface PlayerManager {
    int TYPE_PLAYLIST = 0;
    int TYPE_RADIO_STATION = 1;

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

    @Channel
    interface OnConfigChangeListener {
        void onPlayerTypeChanged(int playerType);

        void syncPlayerState(PlaylistState playlistState, RadioStationState radioStationState);
    }
}
