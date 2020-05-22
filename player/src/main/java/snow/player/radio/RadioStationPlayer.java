package snow.player.radio;

import snow.player.Player;

public interface RadioStationPlayer extends Player {
    void setRadioStation(RadioStation radioStation);

    interface OnRadioStationChangeListener {
        void onRadioStationChanged(RadioStation radioStation);
    }
}
