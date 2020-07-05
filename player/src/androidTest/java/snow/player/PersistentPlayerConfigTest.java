package snow.player;

import android.content.Context;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
public class PersistentPlayerConfigTest {
    @Test
    public void persistentTest() {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        final String persistentId = "persistent_player_config_test";

        PersistentPlayerConfig playerConfigA = new PersistentPlayerConfig(context, persistentId);

        playerConfigA.setPlayerType(PlayerManager.TYPE_RADIO_STATION);
        playerConfigA.setSoundQuality(Player.SoundQuality.SUPER);
        playerConfigA.setAudioEffectEnabled(true);
        playerConfigA.setOnlyWifiNetwork(true);
        playerConfigA.setIgnoreLossAudioFocus(true);

        PersistentPlayerConfig playerConfigB = new PersistentPlayerConfig(context, persistentId);

        assertEquals(playerConfigA, playerConfigB);
    }
}
