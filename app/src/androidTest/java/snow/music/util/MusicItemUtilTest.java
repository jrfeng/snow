package snow.music.util;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import static org.junit.Assert.*;

import org.junit.Test;
import org.junit.runner.RunWith;

import snow.music.store.Music;
import snow.player.audio.MusicItem;

@RunWith(AndroidJUnit4.class)
public class MusicItemUtilTest {

    @Test
    public void asMusic_asMusicItem() {
        Music music1 = new Music(1024,
                "TestTitle",
                "TestArtist",
                "TestAlbum",
                "https://www.test.com/test.mp3",
                "https://www.test.com/test.png",
                60_000,
                System.currentTimeMillis());

        MusicItem musicItem1 = MusicItemUtil.asMusicItem(music1);

        Music music2 = MusicItemUtil.asMusic(musicItem1);
        MusicItem musicItem2 = MusicItemUtil.asMusicItem(music2);

        // assert
        assertEquals(music1, music2);
        assertEquals(musicItem1, musicItem2);
    }
}
