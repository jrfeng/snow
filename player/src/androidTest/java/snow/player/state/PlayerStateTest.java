package snow.player.state;

import android.os.Parcel;

import org.junit.Test;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.runner.RunWith;

import snow.player.media.MusicItem;
import snow.player.Player;

import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
public class PlayerStateTest {

    @Test
    public void constructorTest() {
        PlayerState playerState = new PlayerState();

        assertEquals(0, playerState.getPlayProgress());
        assertFalse(playerState.isLooping());
        assertEquals(Player.SoundQuality.STANDARD, playerState.getSoundQuality());
        assertFalse(playerState.isAudioEffectEnabled());
        assertTrue(playerState.isOnlyWifiNetwork());
        assertFalse(playerState.isIgnoreLossAudioFocus());

        assertEquals(Player.PlaybackState.UNKNOWN, playerState.getPlaybackState());
        assertEquals(0, playerState.getAudioSessionId());
        assertEquals(0, playerState.getBufferingPercent());
        assertFalse(playerState.isStalled());
        assertEquals(Player.Error.NO_ERROR, playerState.getErrorCode());
    }

    public void constructor2Test() {
        PlayerState playerState = new PlayerState();

        playerState.setPlayProgress(1000);
        playerState.setPlayProgressUpdateTime(System.currentTimeMillis());
        playerState.setLooping(true);
        playerState.setSoundQuality(Player.SoundQuality.HIGH);
        playerState.setAudioEffectEnabled(true);
        playerState.setOnlyWifiNetwork(false);
        playerState.setIgnoreLossAudioFocus(true);
        playerState.setMusicItem(new MusicItem());
        playerState.setPlaybackState(Player.PlaybackState.PAUSED);
        playerState.setAudioSessionId(15);
        playerState.setBufferingPercent(99);
        playerState.setBufferingPercentUpdateTime(System.currentTimeMillis());
        playerState.setStalled(true);
        playerState.setErrorCode(Player.Error.ONLY_WIFI_NETWORK);
        playerState.setErrorMessage("only wifi network");

        PlayerState other = new PlayerState(playerState);

        assertEquals(playerState, other);
    }

    @Test
    public void setPlaybackState() {
        PlayerState playerState = new PlayerState();

        final int errorCode = Player.Error.PLAYER_ERROR;

        playerState.setErrorCode(errorCode);

        assertEquals(errorCode, playerState.getErrorCode());

        playerState.setPlaybackState(Player.PlaybackState.PLAYING);
        assertEquals(Player.Error.NO_ERROR, playerState.getErrorCode());
    }

    @Test
    public void equals_hashCode() {
        final long playProgress = 1000;
        final long playProgressUpdateTime = System.currentTimeMillis();
        final boolean looping = true;
        final Player.SoundQuality soundQuality = Player.SoundQuality.SUPER;
        final boolean audioEffectEnable = false;
        final boolean onlyWifiNetwork = true;
        final boolean ignoreAudioFocus = false;
        final MusicItem musicItem = new MusicItem();
        musicItem.setTitle("test_title");
        musicItem.setArtist("test_artist");

        final Player.PlaybackState playbackState = Player.PlaybackState.PLAYING;
        final int audioSessionId = 15;
        final int bufferingPercent = 100;
        final long bufferingPercentUpdateTime = System.currentTimeMillis();
        final boolean stalled = true;
        final int errorCode = Player.Error.PLAYER_ERROR;
        final String errorMessage = "player error";

        PlayerState playerState = new PlayerState();
        playerState.setPlayProgress(playProgress);
        playerState.setPlayProgressUpdateTime(playProgressUpdateTime);
        playerState.setLooping(looping);
        playerState.setSoundQuality(soundQuality);
        playerState.setAudioEffectEnabled(audioEffectEnable);
        playerState.setOnlyWifiNetwork(onlyWifiNetwork);
        playerState.setIgnoreLossAudioFocus(ignoreAudioFocus);
        playerState.setMusicItem(musicItem);
        playerState.setPlaybackState(playbackState);
        playerState.setAudioSessionId(audioSessionId);
        playerState.setBufferingPercent(bufferingPercent);
        playerState.setBufferingPercentUpdateTime(bufferingPercentUpdateTime);
        playerState.setStalled(stalled);
        playerState.setErrorCode(errorCode);
        playerState.setErrorMessage(errorMessage);

        PlayerState other = new PlayerState();
        other.setPlayProgress(playProgress);
        other.setPlayProgressUpdateTime(playProgressUpdateTime);
        other.setLooping(looping);
        other.setSoundQuality(soundQuality);
        other.setAudioEffectEnabled(audioEffectEnable);
        other.setOnlyWifiNetwork(onlyWifiNetwork);
        other.setIgnoreLossAudioFocus(ignoreAudioFocus);
        other.setMusicItem(musicItem);
        other.setPlaybackState(playbackState);
        other.setAudioSessionId(audioSessionId);
        other.setBufferingPercent(bufferingPercent);
        other.setBufferingPercentUpdateTime(bufferingPercentUpdateTime);
        other.setStalled(stalled);
        other.setErrorCode(errorCode);
        other.setErrorMessage(errorMessage);

        assertEquals(playerState, other);
        assertEquals(playerState.hashCode(), other.hashCode());
    }

    @Test
    public void copyConstructorTest() {
        final long playProgress = 1000;
        final long playProgressUpdateTime = System.currentTimeMillis();
        final boolean looping = true;
        final Player.SoundQuality soundQuality = Player.SoundQuality.SUPER;
        final boolean audioEffectEnable = false;
        final boolean onlyWifiNetwork = true;
        final boolean ignoreAudioFocus = false;
        final MusicItem musicItem = new MusicItem();
        musicItem.setTitle("test_title");
        musicItem.setArtist("test_artist");

        final Player.PlaybackState playbackState = Player.PlaybackState.PLAYING;
        final int audioSessionId = 8;
        final int bufferingPercent = 95;
        final long bufferingPercentUpdateTime = System.currentTimeMillis();
        final boolean stalled = true;
        final int errorCode = Player.Error.ONLY_WIFI_NETWORK;
        final String errorMessage = "only wifi network";

        PlayerState playerState = new PlayerState();
        playerState.setPlayProgress(playProgress);
        playerState.setPlayProgressUpdateTime(playProgressUpdateTime);
        playerState.setLooping(looping);
        playerState.setSoundQuality(soundQuality);
        playerState.setAudioEffectEnabled(audioEffectEnable);
        playerState.setOnlyWifiNetwork(onlyWifiNetwork);
        playerState.setIgnoreLossAudioFocus(ignoreAudioFocus);
        playerState.setMusicItem(musicItem);
        playerState.setPlaybackState(playbackState);
        playerState.setAudioSessionId(audioSessionId);
        playerState.setBufferingPercent(bufferingPercent);
        playerState.setBufferingPercentUpdateTime(bufferingPercentUpdateTime);
        playerState.setStalled(stalled);
        playerState.setErrorCode(errorCode);
        playerState.setErrorMessage(errorMessage);

        PlayerState other = new PlayerState(playerState);

        assertEquals(playerState, other);
    }

    @Test
    public void parcelableTest() {
        PlayerState playerState = new PlayerState();

        final long playProgress = 2000;
        final long playProgressUpdateTime = System.currentTimeMillis();
        final boolean looping = true;
        final Player.SoundQuality soundQuality = Player.SoundQuality.HIGH;
        final boolean audioEffectEnable = true;
        final boolean onlyWifiNetwork = false;
        final boolean ignoreAudioFocus = true;
        final MusicItem musicItem = new MusicItem();
        musicItem.setTitle("test_title");
        musicItem.setArtist("test_artist");

        final Player.PlaybackState playbackState = Player.PlaybackState.PAUSED;
        final int audioSessionId = 4;
        final int bufferingPercent = 50;
        final long bufferingPercentUpdateTime = System.currentTimeMillis();
        final boolean stalled = true;
        final int errorCode = Player.Error.NETWORK_UNAVAILABLE;
        final String errorMessage = "network unavailable";

        playerState.setPlayProgress(playProgress);
        playerState.setPlayProgressUpdateTime(playProgressUpdateTime);
        playerState.setLooping(looping);
        playerState.setSoundQuality(soundQuality);
        playerState.setAudioEffectEnabled(audioEffectEnable);
        playerState.setOnlyWifiNetwork(onlyWifiNetwork);
        playerState.setIgnoreLossAudioFocus(ignoreAudioFocus);
        playerState.setMusicItem(musicItem);
        playerState.setPlaybackState(playbackState);
        playerState.setAudioSessionId(audioSessionId);
        playerState.setBufferingPercent(bufferingPercent);
        playerState.setBufferingPercentUpdateTime(bufferingPercentUpdateTime);
        playerState.setStalled(stalled);
        playerState.setErrorCode(errorCode);
        playerState.setErrorMessage(errorMessage);

        Parcel parcel = Parcel.obtain();

        playerState.writeToParcel(parcel, 0);

        parcel.setDataPosition(0);

        PlayerState other = new PlayerState(parcel);

        assertEquals(playerState, other);
    }
}
