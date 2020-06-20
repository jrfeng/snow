package snow.player.state;

import android.os.Parcel;

import org.junit.Test;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.runner.RunWith;

import snow.player.Player;
import snow.player.radio.RadioStation;

import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
public class RadioStationStateTest {
    @Test
    public void constructor1Test() {
        RadioStationState radioStationState = new RadioStationState();
        assertNotNull(radioStationState.getRadioStation());
    }

    @Test
    public void copyConstructorTest() {
        final Player.PlaybackState playbackState = Player.PlaybackState.PLAYING;
        final RadioStation radioStation = new RadioStation();

        RadioStationState source = new RadioStationState();
        source.setPlaybackState(playbackState);
        source.setRadioStation(radioStation);

        RadioStationState other = new RadioStationState(source);

        assertEquals(source, other);
    }

    @Test
    public void equals_hashCode() {
        final RadioStation radioStation = new RadioStation();

        RadioStationState radioStationState1 = new RadioStationState();
        radioStationState1.setRadioStation(radioStation);

        RadioStationState radioStationState2 = new RadioStationState();
        radioStationState2.setRadioStation(radioStation);

        assertEquals(radioStationState1, radioStationState2);
        assertEquals(radioStationState1.hashCode(), radioStationState2.hashCode());
    }

    @Test
    public void parcelableTest() {
        Parcel parcel = Parcel.obtain();

        RadioStation radioStation = new RadioStation();

        RadioStationState source = new RadioStationState();
        source.setRadioStation(radioStation);
        source.writeToParcel(parcel, 0);

        parcel.setDataPosition(0);
        RadioStationState other = new RadioStationState(parcel);

        assertEquals(source, other);

        parcel.recycle();
    }
}
