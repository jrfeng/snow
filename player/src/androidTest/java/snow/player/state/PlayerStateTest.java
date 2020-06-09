package snow.player.state;

import android.os.Parcel;

import org.junit.Test;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.runner.RunWith;

import snow.player.MusicItem;
import snow.player.Player;

import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
public class PlayerStateTest {

    @Test
    public void constructorTest() {
        PlayerState playerState = new PlayerState();

        assertEquals(0, playerState.getPlayProgress());
        assertFalse(playerState.isLooping());
        assertEquals(Player.PlaybackState.UNKNOWN, playerState.getPlaybackState());
        assertEquals(Player.SoundQuality.STANDARD, playerState.getSoundQuality());
        assertFalse(playerState.isAudioEffectEnabled());
        assertTrue(playerState.isOnlyWifiNetwork());
        assertFalse(playerState.isIgnoreLossAudioFocus());
    }

    public void constructor2Test() {
        PlayerState playerState = new PlayerState();
        playerState.setPlayProgress(1000);

        PlayerState other = new PlayerState(playerState);

        assertEquals(playerState, other);
    }

    @Test
    public void equals_hashCode() {
        final long playProgress = 1000;
        final long playProgressUpdateTime = System.currentTimeMillis();
        final boolean looping = true;
        final int playbackState = Player.PlaybackState.PLAYING;
        final int soundQuality = Player.SoundQuality.SUPER;
        final boolean audioEffectEnable = false;
        final boolean onlyWifiNetwork = true;
        final boolean ignoreAudioFocus = false;
        final MusicItem musicItem = new MusicItem();
        musicItem.setTitle("test_title");
        musicItem.setArtist("test_artist");

        PlayerState playerState = new PlayerState();
        playerState.setPlayProgress(playProgress);
        playerState.setPlayProgressUpdateTime(playProgressUpdateTime);
        playerState.setLooping(looping);
        playerState.setPlaybackState(playbackState);
        playerState.setSoundQuality(soundQuality);
        playerState.setAudioEffectEnabled(audioEffectEnable);
        playerState.setOnlyWifiNetwork(onlyWifiNetwork);
        playerState.setIgnoreLossAudioFocus(ignoreAudioFocus);
        playerState.setMusicItem(musicItem);

        PlayerState other = new PlayerState();
        other.setPlayProgress(playProgress);
        other.setPlayProgressUpdateTime(playProgressUpdateTime);
        other.setLooping(looping);
        other.setPlaybackState(playbackState);
        other.setSoundQuality(soundQuality);
        other.setAudioEffectEnabled(audioEffectEnable);
        other.setOnlyWifiNetwork(onlyWifiNetwork);
        other.setIgnoreLossAudioFocus(ignoreAudioFocus);
        other.setMusicItem(musicItem);

        assertEquals(playerState, other);
        assertEquals(playerState.hashCode(), other.hashCode());
    }

    @Test
    public void cloneTest() throws CloneNotSupportedException {
        final long playProgress = 1000;
        final long playProgressUpdateTime = System.currentTimeMillis();
        final boolean looping = true;
        final int playbackState = Player.PlaybackState.PLAYING;
        final int soundQuality = Player.SoundQuality.SUPER;
        final boolean audioEffectEnable = false;
        final boolean onlyWifiNetwork = true;
        final boolean ignoreAudioFocus = false;
        final MusicItem musicItem = new MusicItem();
        musicItem.setTitle("test_title");
        musicItem.setArtist("test_artist");

        PlayerState playerState = new PlayerState();
        playerState.setPlayProgress(playProgress);
        playerState.setPlayProgressUpdateTime(playProgressUpdateTime);
        playerState.setLooping(looping);
        playerState.setPlaybackState(playbackState);
        playerState.setSoundQuality(soundQuality);
        playerState.setAudioEffectEnabled(audioEffectEnable);
        playerState.setOnlyWifiNetwork(onlyWifiNetwork);
        playerState.setIgnoreLossAudioFocus(ignoreAudioFocus);
        playerState.setMusicItem(musicItem);

        PlayerState other = playerState.clone();

        assertEquals(playerState, other);
    }

    @Test
    public void parcelableTest() {
        PlayerState playerState = new PlayerState();

        final long playProgress = 2000;
        final long playProgressUpdateTime = System.currentTimeMillis();
        final boolean looping = true;
        final int playbackState = Player.PlaybackState.PAUSED;
        final int soundQuality = Player.SoundQuality.HIGH;
        final boolean audioEffectEnable = true;
        final boolean onlyWifiNetwork = false;
        final boolean ignoreAudioFocus = true;
        final MusicItem musicItem = new MusicItem();
        musicItem.setTitle("test_title");
        musicItem.setArtist("test_artist");

        playerState.setPlayProgress(playProgress);
        playerState.setPlayProgressUpdateTime(playProgressUpdateTime);
        playerState.setLooping(looping);
        playerState.setPlaybackState(playbackState);
        playerState.setSoundQuality(soundQuality);
        playerState.setAudioEffectEnabled(audioEffectEnable);
        playerState.setOnlyWifiNetwork(onlyWifiNetwork);
        playerState.setIgnoreLossAudioFocus(ignoreAudioFocus);
        playerState.setMusicItem(musicItem);

        Parcel parcel = Parcel.obtain();

        playerState.writeToParcel(parcel, 0);

        parcel.setDataPosition(0);

        PlayerState other = new PlayerState(parcel);

        assertEquals(playerState, other);
    }
}
