package snow.player.radio;

import snow.player.Player;

public interface RadioStationPlayer extends Player {
    void swapRadioStation();

    interface OnRadioStationChangeListener {
        void onRadioStationChanged(RadioStation radioStation);
    }
}
