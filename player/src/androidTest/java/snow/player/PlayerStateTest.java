package snow.player;

import android.os.Parcel;

import org.junit.Test;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.runner.RunWith;

import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
public class PlayerStateTest {

    @Test
    public void constructorTest() {
        PlayerState playerState = new PlayerState();

        assertEquals(Player.PlaybackState.UNKNOWN, playerState.getPlaybackState());
        assertEquals(Player.SoundQuality.STANDARD, playerState.getSoundQuality());
        assertFalse(playerState.isAudioEffectEnabled());
        assertTrue(playerState.isOnlyWifiNetwork());
        assertFalse(playerState.isIgnoreLossAudioFocus());
    }

    @Test
    public void equals_hashCode() {
        PlayerState playerState = new PlayerState();

        playerState.setPlaybackState(Player.PlaybackState.PLAYING);
        playerState.setSoundQuality(Player.SoundQuality.SUPER);
        playerState.setAudioEffectEnabled(false);
        playerState.setOnlyWifiNetwork(true);
        playerState.setIgnoreLossAudioFocus(false);

        PlayerState other = new PlayerState(playerState);

        assertEquals(playerState, other);
        assertEquals(playerState.hashCode(), other.hashCode());
    }

    @Test
    public void parcelableTest() {
        PlayerState playerState = new PlayerState();

        final int playbackState = Player.PlaybackState.PLAYING;
        final int soundQuality = Player.SoundQuality.SUPER;
        final boolean audioEffectEnable = true;
        final boolean onlyWifiNetwork = false;
        final boolean ignoreAudioFocus = true;

        playerState.setPlaybackState(playbackState);
        playerState.setSoundQuality(soundQuality);
        playerState.setAudioEffectEnabled(audioEffectEnable);
        playerState.setOnlyWifiNetwork(onlyWifiNetwork);
        playerState.setIgnoreLossAudioFocus(ignoreAudioFocus);

        Parcel parcel = Parcel.obtain();

        playerState.writeToParcel(parcel, 0);

        parcel.setDataPosition(0);

        PlayerState other = new PlayerState(parcel);

        assertEquals(playerState, other);
    }
}
