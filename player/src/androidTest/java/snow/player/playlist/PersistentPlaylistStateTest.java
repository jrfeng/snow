package snow.player.playlist;

import android.content.Context;

import org.junit.Test;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.runner.RunWith;

import snow.player.media.MusicItem;

import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
public class PersistentPlaylistStateTest {

    @Test
    public void constructorTest() {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        String id = "test_persistent_playlist";

        PersistentPlaylistState pps = new PersistentPlaylistState(context, id);

        pps.setPlayProgress(100);
        pps.setPlayProgressUpdateTime(System.currentTimeMillis());
        pps.setPosition(50);
        pps.setPlayMode(PlaylistPlayer.PlayMode.SHUFFLE);

        final MusicItem musicItem = new MusicItem();
        musicItem.setTitle("test_title");
        musicItem.setArtist("test_artist");

        pps.setMusicItem(musicItem);

        PersistentPlaylistState pps2 = new PersistentPlaylistState(context, id);

        assertEquals(pps, pps2);
    }
}
