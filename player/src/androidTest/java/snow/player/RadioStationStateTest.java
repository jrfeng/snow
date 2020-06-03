package snow.player;

import android.os.Parcel;

import org.junit.Test;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.runner.RunWith;

import snow.player.radio.RadioStation;

import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
public class RadioStationStateTest {
    @Test
    public void constructor1Test() {
        RadioStation radioStation = new RadioStation();

        RadioStationState radioStationState = new RadioStationState(radioStation);
        assertNotNull(radioStationState.getRadioStation());
    }

    @Test
    public void constructor2Test() {
        RadioStation radioStation = new RadioStation();

        RadioStationState source = new RadioStationState(radioStation);
        RadioStationState other = new RadioStationState(source);

        assertEquals(source, other);
    }

    @Test
    public void equals_hashCode() {
        RadioStation radioStation = new RadioStation();

        RadioStationState radioStationState1 = new RadioStationState(radioStation);
        RadioStationState radioStationState2 = new RadioStationState(radioStation);

        assertEquals(radioStationState1, radioStationState2);
        assertEquals(radioStationState1.hashCode(), radioStationState2.hashCode());
    }

    @Test
    public void cloneTest() throws CloneNotSupportedException {
        RadioStation radioStation = new RadioStation();

        RadioStationState source = new RadioStationState(radioStation);
        RadioStationState other = source.clone();

        assertEquals(source, other);
    }

    @Test
    public void parcelableTest() {
        Parcel parcel = Parcel.obtain();

        RadioStation radioStation = new RadioStation();

        RadioStationState source = new RadioStationState(radioStation);
        source.writeToParcel(parcel, 0);

        parcel.setDataPosition(0);
        RadioStationState other = new RadioStationState(parcel);

        assertEquals(source, other);

        parcel.recycle();
    }
}
