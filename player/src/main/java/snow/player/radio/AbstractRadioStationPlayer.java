package snow.player.radio;

import android.content.Context;

import androidx.annotation.NonNull;

import java.util.HashMap;

import snow.player.AbstractPlayer;
import snow.player.MusicItem;
import snow.player.state.RadioStationState;
import snow.player.state.RadioStationStateListener;

public abstract class AbstractRadioStationPlayer extends AbstractPlayer<RadioStationStateListener>
        implements RadioStationPlayer {
    private RadioStationState mRadioStationState;

    public AbstractRadioStationPlayer(@NonNull Context context, @NonNull RadioStationState radioStationState) {
        super(context, radioStationState);

        mRadioStationState = radioStationState;
    }

    private void notifyRadioStationChanged(RadioStation radioStation) {
        mRadioStationState.setRadioStation(radioStation);

        HashMap<String, RadioStationStateListener> listenerMap = getAllStateListener();

        for (String key : listenerMap.keySet()) {
            RadioStationStateListener listener = listenerMap.get(key);
            if (listener != null) {
                listener.onRadioStationChanged(radioStation);
            }
        }
    }

    @NonNull
    protected abstract MusicItem getNextMusicItem(RadioStation radioStation);

    @Override
    public void skipToNext() {
        notifyPlayingMusicItemChanged(getNextMusicItem(mRadioStationState.getRadioStation()), true);
    }

    @Override
    public void setRadioStation(RadioStation radioStation) {
        notifyRadioStationChanged(radioStation);
        notifyPlayingMusicItemChanged(getNextMusicItem(mRadioStationState.getRadioStation()), true);
    }
}
