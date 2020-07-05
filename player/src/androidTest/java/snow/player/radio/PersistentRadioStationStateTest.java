package snow.player.radio;

import android.content.Context;

import org.junit.Test;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.runner.RunWith;

import snow.player.media.MusicItem;

import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
public class PersistentRadioStationStateTest {

    @SuppressWarnings("SpellCheckingInspection")
    @Test
    public void constructorTest() {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        String id = "test_persistent_radio_station_state";

        RadioStation radioStation = new RadioStation("test_id", "test_name", "test_description");

        PersistentRadioStationState prss = new PersistentRadioStationState(context, id);

        prss.setPlayProgress(100);
        prss.setPlayProgressUpdateTime(System.currentTimeMillis());
        prss.setRadioStation(radioStation);

        final MusicItem musicItem = new MusicItem();
        musicItem.setTitle("test_title");
        musicItem.setArtist("test_artist");

        prss.setMusicItem(musicItem);

        PersistentRadioStationState prss2 = new PersistentRadioStationState(context, id);

        assertEquals(prss, prss2);
    }
}
