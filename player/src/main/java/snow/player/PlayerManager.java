package snow.player;

import android.os.IBinder;

import androidx.annotation.NonNull;

import channel.helper.Channel;

/**
 * 该接口定义了用于管理播放器的基本功能。
 */
@Channel
public interface PlayerManager {
    int MODE_PLAYLIST = 0;
    int MODE_RADIO_STATION = 1;

    /**
     * 设置播放器的播放模式。
     *
     * @param mode 播放器的播放模式。该参数的值应该是：{@link #MODE_PLAYLIST}, {@link #MODE_RADIO_STATION} 两者之一。
     */
    void setPlayerMode(int mode);

    /**
     * 注册一个播放器状态监听器。
     *
     * @param token    监听器的 token。注意！不能为 null，且应该保证唯一性。
     * @param listener 监听器的 IBinder 对象。
     */
    void registerPlayerStateListener(String token, IBinder listener);

    /**
     * 取消已注册的播放器状态监听器。
     *
     * @param token 已注册的监听器的 token（不能为 null）。如果监听器没有注册或者已经取消注册，那么将忽略此操作。
     */
    void unregisterPlayerStateListener(String token);
}
