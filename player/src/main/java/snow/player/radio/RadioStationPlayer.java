package snow.player.radio;

import channel.helper.Channel;
import snow.player.Player;

/**
 * 该接口定义了 “电台” 播放器的基本功能。
 */
@Channel
public interface RadioStationPlayer extends Player {
    /**
     * 下一首音乐。
     */
    void skipToNext();

    /**
     * 设置一个新的电台。
     *
     * @param radioStation 要播放的新电台
     */
    void setRadioStation(RadioStation radioStation);

    /**
     * 用于监听电台被替换事件。
     */
    interface OnRadioStationChangeListener {
        /**
         * 当设置了新的电台时会回调该方法。
         *
         * @param radioStation 正在播放的新电台
         */
        void onRadioStationChanged(RadioStation radioStation);
    }
}
