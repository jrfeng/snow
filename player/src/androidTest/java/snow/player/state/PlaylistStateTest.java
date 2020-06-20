package snow.player.state;

import android.os.Parcel;

import org.junit.Test;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.runner.RunWith;

import snow.player.Player;
import snow.player.playlist.PlaylistPlayer;

import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
public class PlaylistStateTest {

    @Test
    public void constructor1Test() {
        PlaylistState ps = new PlaylistState();

        assertEquals(0, ps.getPosition());
        assertEquals(PlaylistPlayer.PlayMode.SEQUENTIAL, ps.getPlayMode());
    }

    @Test
    public void constructor2Test() {
        PlaylistState ps = new PlaylistState();
        ps.setPosition(50);
        ps.setPlayMode(PlaylistPlayer.PlayMode.SHUFFLE);

        PlaylistState ps2 = new PlaylistState(ps);

        assertEquals(ps, ps2);
    }

    @Test
    public void setPosition() {
        final int position = -5;

        PlaylistState ps = new PlaylistState();
        ps.setPosition(position);

        assertEquals(0, ps.getPosition());
    }

    @Test
    public void equals_hashCode() {
        final int position = 50;
        final PlaylistPlayer.PlayMode playMode = PlaylistPlayer.PlayMode.LOOP;

        PlaylistState ps1 = new PlaylistState();
        ps1.setPosition(position);
        ps1.setPlayMode(playMode);

        PlaylistState ps2 = new PlaylistState();
        ps2.setPosition(position);
        ps2.setPlayMode(playMode);

        assertEquals(ps1, ps2);
        assertEquals(ps1.hashCode(), ps2.hashCode());
    }

    @Test
    public void copyConstructorTest() {
        final Player.PlaybackState playbackState = Player.PlaybackState.PLAYING;
        final int position = 50;
        final PlaylistPlayer.PlayMode playMode = PlaylistPlayer.PlayMode.LOOP;

        PlaylistState ps1 = new PlaylistState();

        ps1.setPlaybackState(playbackState);
        ps1.setPosition(position);
        ps1.setPlayMode(playMode);

        PlaylistState ps2 = new PlaylistState(ps1);

        assertEquals(ps1, ps2);
    }

    @Test
    public void parcelableTest() {
        Parcel parcel = Parcel.obtain();

        final int position = 50;
        final PlaylistPlayer.PlayMode playMode = PlaylistPlayer.PlayMode.LOOP;

        PlaylistState ps1 = new PlaylistState();
        ps1.setPosition(position);
        ps1.setPlayMode(playMode);

        ps1.writeToParcel(parcel, 0);

        parcel.setDataPosition(0);
        PlaylistState ps2 = new PlaylistState(parcel);

        assertEquals(ps1, ps2);

        parcel.recycle();
    }
}
