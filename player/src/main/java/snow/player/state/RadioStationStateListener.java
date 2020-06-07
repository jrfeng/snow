package snow.player.state;

import channel.helper.Channel;
import snow.player.radio.RadioStationPlayer;

@Channel
public interface RadioStationStateListener extends PlayerStateListener,
        RadioStationPlayer.OnRadioStationChangeListener {

}
