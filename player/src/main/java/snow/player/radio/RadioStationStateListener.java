package snow.player.radio;

import channel.helper.Channel;
import snow.player.PlayerStateListener;

@Channel
public interface RadioStationStateListener extends PlayerStateListener,
        RadioStationPlayer.OnRadioStationChangeListener {

}
