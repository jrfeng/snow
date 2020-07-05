package snow.player;

import android.os.Parcel;

import org.junit.Test;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.runner.RunWith;

import snow.player.media.MusicItem;

import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
public class PlayerStateTest {

    @Test
    public void defaultConstructorTest() {
        PlayerState playerState = new PlayerState();

        assertEquals(0, playerState.getPlayProgress());

        assertEquals(Player.PlaybackState.UNKNOWN, playerState.getPlaybackState());
        assertEquals(0, playerState.getAudioSessionId());
        assertEquals(0, playerState.getBufferingPercent());
        assertFalse(playerState.isStalled());
        assertEquals(Player.Error.NO_ERROR, playerState.getErrorCode());
    }

    @Test
    public void copyConstructorTest() {
        PlayerState source = new PlayerState();

        source.setPlayProgress(1000);
        source.setPlayProgressUpdateTime(System.currentTimeMillis());
        source.setMusicItem(new MusicItem());
        source.setPlaybackState(Player.PlaybackState.PAUSED);
        source.setAudioSessionId(15);
        source.setBufferingPercent(99);
        source.setBufferingPercentUpdateTime(System.currentTimeMillis());
        source.setStalled(true);
        source.setErrorCode(Player.Error.ONLY_WIFI_NETWORK);
        source.setErrorMessage("only wifi network");

        PlayerState copy = new PlayerState(source);

        assertEquals(source, copy);
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
        final int playProgress = 1000;
        final long playProgressUpdateTime = System.currentTimeMillis();

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
    public void parcelableTest() {
        PlayerState playerState = new PlayerState();

        final int playProgress = 2000;
        final long playProgressUpdateTime = System.currentTimeMillis();
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
