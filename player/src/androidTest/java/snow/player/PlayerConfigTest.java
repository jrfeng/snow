package snow.player;

import android.os.Parcel;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
public class PlayerConfigTest {

    @Test
    public void defaultConstructorTest() {
        PlayerConfig playerConfig = new PlayerConfig();

        assertEquals(Player.SoundQuality.STANDARD, playerConfig.getSoundQuality());
        assertFalse(playerConfig.isAudioEffectEnabled());
        assertFalse(playerConfig.isOnlyWifiNetwork());
        assertFalse(playerConfig.isIgnoreLossAudioFocus());
    }

    @Test
    public void copyConstructorTest() {
        PlayerConfig source = new PlayerConfig();

        source.setPlayerType(PlayerManager.TYPE_RADIO_STATION);
        source.setSoundQuality(Player.SoundQuality.SUPER);
        source.setAudioEffectEnabled(true);
        source.setOnlyWifiNetwork(true);
        source.setIgnoreLossAudioFocus(true);

        PlayerConfig copy = new PlayerConfig(source);

        assertEquals(source, copy);
    }

    @Test
    public void equals_hashCode() {
        final int playerType = PlayerManager.TYPE_RADIO_STATION;
        final Player.SoundQuality soundQuality = Player.SoundQuality.SUPER;
        final boolean audioEffectEnable = true;
        final boolean onlyWifiNetwork = true;
        final boolean ignoreAudioFocus = false;

        PlayerConfig playerConfigA = new PlayerConfig();

        playerConfigA.setPlayerType(playerType);
        playerConfigA.setSoundQuality(soundQuality);
        playerConfigA.setAudioEffectEnabled(audioEffectEnable);
        playerConfigA.setOnlyWifiNetwork(onlyWifiNetwork);
        playerConfigA.setIgnoreLossAudioFocus(ignoreAudioFocus);

        PlayerConfig playerConfigB = new PlayerConfig();

        playerConfigB.setPlayerType(playerType);
        playerConfigB.setSoundQuality(soundQuality);
        playerConfigB.setAudioEffectEnabled(audioEffectEnable);
        playerConfigB.setOnlyWifiNetwork(onlyWifiNetwork);
        playerConfigB.setIgnoreLossAudioFocus(ignoreAudioFocus);

        assertEquals(playerConfigA, playerConfigB);
        assertEquals(playerConfigA.hashCode(), playerConfigB.hashCode());
    }

    @Test
    public void parcelableTest() {
        PlayerConfig playerConfigA = new PlayerConfig();

        playerConfigA.setPlayerType(PlayerManager.TYPE_RADIO_STATION);
        playerConfigA.setSoundQuality(Player.SoundQuality.HIGH);
        playerConfigA.setAudioEffectEnabled(true);
        playerConfigA.setOnlyWifiNetwork(false);
        playerConfigA.setIgnoreLossAudioFocus(false);

        Parcel parcel = Parcel.obtain();

        playerConfigA.writeToParcel(parcel, 0);

        parcel.setDataPosition(0);
        PlayerConfig playerConfigB = new PlayerConfig(parcel);

        assertEquals(playerConfigA, playerConfigB);
    }
}
