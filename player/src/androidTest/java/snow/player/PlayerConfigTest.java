package snow.player;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
public class PlayerConfigTest {
    private static PlayerConfig mPlayerConfig;

    @BeforeClass
    public static void initPlayerConfig() {
        mPlayerConfig = new PlayerConfig(
                InstrumentationRegistry.getInstrumentation().getContext(),
                "test_id");
    }

    @Test
    public void setPlayerType() {
        final PlayerManager.PlayerType playerType = PlayerManager.PlayerType.RADIO_STATION;

        mPlayerConfig.setPlayerType(playerType);
        assertEquals(playerType, mPlayerConfig.getPlayerType());
    }

    @Test
    public void setSoundQuality() {
        final Player.SoundQuality soundQuality = Player.SoundQuality.HIGH;

        mPlayerConfig.setSoundQuality(soundQuality);
        assertEquals(soundQuality, mPlayerConfig.getSoundQuality());
    }

    @Test
    public void setAudioEffectEnabled() {
        final boolean enabled = true;

        mPlayerConfig.setAudioEffectEnabled(enabled);
        assertEquals(enabled, mPlayerConfig.isAudioEffectEnabled());
    }

    @Test
    public void setOnlyWifiNetwork() {
        final boolean onlyWifiNetwork = true;

        mPlayerConfig.setOnlyWifiNetwork(onlyWifiNetwork);
        assertEquals(onlyWifiNetwork, mPlayerConfig.isOnlyWifiNetwork());
    }

    @Test
    public void setIgnoreLossAudioFocus() {
        final boolean ignoreLossAudioFocus = true;

        mPlayerConfig.setIgnoreLossAudioFocus(ignoreLossAudioFocus);
        assertEquals(ignoreLossAudioFocus, mPlayerConfig.isIgnoreLossAudioFocus());
    }
}
