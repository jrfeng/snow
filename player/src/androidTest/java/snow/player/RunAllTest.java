package snow.player;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;

import snow.player.media.MusicItemTest;
import snow.player.playlist.AbstractPlaylistPlayerTest;
import snow.player.playlist.PersistentPlaylistStateTest;
import snow.player.playlist.PlaylistManagerTest;
import snow.player.playlist.PlaylistStateTest;
import snow.player.playlist.PlaylistTest;
import snow.player.radio.PersistentRadioStationStateTest;
import snow.player.radio.RadioStationStateTest;
import snow.player.radio.RadioStationTest;

@Suite.SuiteClasses({
        // snow.player
        AbstractPlayerTest.class,
        PlayerStateTest.class,
        PlayerConfigTest.class,
        // snow.player.media
        MusicItemTest.class,
        // snow.player.playlist
        AbstractPlaylistPlayerTest.class,
        PersistentPlaylistStateTest.class,
        PlaylistManagerTest.class,
        PlaylistStateTest.class,
        PlaylistTest.class,
        // snow.player.radio
        PersistentRadioStationStateTest.class,
        RadioStationStateTest.class,
        RadioStationTest.class
})
@RunWith(Suite.class)
public class RunAllTest {
}
