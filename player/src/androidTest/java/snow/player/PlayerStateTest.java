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
        assertEquals(0,playerState.getPlayProgressUpdateTime());
        assertEquals(0, playerState.getPosition());
        assertEquals(Player.PlayMode.SEQUENTIAL, playerState.getPlayMode());

        assertEquals(Player.PlaybackState.UNKNOWN, playerState.getPlaybackState());
        assertEquals(0, playerState.getAudioSessionId());
        assertEquals(0, playerState.getBufferingPercent());
        assertFalse(playerState.isStalled());
        assertEquals(Player.Error.NO_ERROR, playerState.getErrorCode());
        assertNotNull(playerState.getErrorMessage());
    }

    @Test
    public void copyConstructorTest() {
        PlayerState source = new PlayerState();
        source.setPlayProgress(1000);
        source.setPlayProgressUpdateTime(System.currentTimeMillis());
        source.setMusicItem(new MusicItem());
        source.setPosition(15);
        source.setPlayMode(Player.PlayMode.LOOP);
        source.setPlaybackState(Player.PlaybackState.ERROR);
        source.setAudioSessionId(12);
        source.setBufferingPercent(100);
        source.setBufferingPercentUpdateTime(System.currentTimeMillis());
        source.setStalled(true);
        source.setErrorCode(Player.Error.PLAYER_ERROR);
        source.setErrorMessage("player error");

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
        musicItem.setTitle("Test Title");
        musicItem.setArtist("Test Artist");
        musicItem.setUri("https://www.test.com/test.mp3");
        final int position = 5;
        final Player.PlayMode playMode = Player.PlayMode.SHUFFLE;
        final Player.PlaybackState playbackState = Player.PlaybackState.PLAYING;
        final int audioSessionId = 12;
        final int bufferingPercent = 100;
        final long bufferingPercentUpdateTime = System.currentTimeMillis();
        final boolean stalled = true;
        final int errorCode = Player.Error.PLAYER_ERROR;
        final String errorMessage = "player error";

        PlayerState playerState = new PlayerState();
        playerState.setPlayProgress(playProgress);
        playerState.setPlayProgressUpdateTime(playProgressUpdateTime);
        playerState.setMusicItem(musicItem);
        playerState.setPosition(position);
        playerState.setPlayMode(playMode);
        playerState.setPlaybackState(playbackState);
        playerState.setAudioSessionId(audioSessionId);
        playerState.setBufferingPercent(bufferingPercent);
        playerState.setBufferingPercentUpdateTime(bufferingPercentUpdateTime);
        playerState.setStalled(stalled);
        playerState.setErrorCode(errorCode);
        playerState.setErrorMessage(errorMessage);

        PlayerState other1 = new PlayerState();
        other1.setPlayProgress(playProgress);
        other1.setPlayProgressUpdateTime(playProgressUpdateTime);
        other1.setMusicItem(musicItem);
        other1.setPosition(position);
        other1.setPlayMode(playMode);
        other1.setPlaybackState(playbackState);
        other1.setAudioSessionId(audioSessionId);
        other1.setBufferingPercent(bufferingPercent);
        other1.setBufferingPercentUpdateTime(bufferingPercentUpdateTime);
        other1.setStalled(stalled);
        other1.setErrorCode(errorCode);
        other1.setErrorMessage(errorMessage);

        assertEquals(playerState, other1);
        assertEquals(playerState.hashCode(), other1.hashCode());

        PlayerState other2 = new PlayerState(playerState);
        other2.setPlayProgress(other2.getPlayProgress() + 1000);

        assertNotEquals(playerState, other2);
        assertNotEquals(playerState.hashCode(), other2.hashCode());
    }

    @Test
    public void parcelableTest() {
        final int playProgress = 1000;
        final long playProgressUpdateTime = System.currentTimeMillis();
        final MusicItem musicItem = new MusicItem();
        musicItem.setTitle("Test Title");
        musicItem.setArtist("Test Artist");
        musicItem.setUri("https://www.test.com/test.mp3");
        final int position = 5;
        final Player.PlayMode playMode = Player.PlayMode.SHUFFLE;
        final Player.PlaybackState playbackState = Player.PlaybackState.PLAYING;
        final int audioSessionId = 12;
        final int bufferingPercent = 100;
        final long bufferingPercentUpdateTime = System.currentTimeMillis();
        final boolean stalled = true;
        final int errorCode = Player.Error.PLAYER_ERROR;
        final String errorMessage = "player error";

        PlayerState playerState = new PlayerState();
        playerState.setPlayProgress(playProgress);
        playerState.setPlayProgressUpdateTime(playProgressUpdateTime);
        playerState.setMusicItem(musicItem);
        playerState.setPosition(position);
        playerState.setPlayMode(playMode);
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
