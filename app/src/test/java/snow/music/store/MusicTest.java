package snow.music.store;

import org.junit.Test;

import static org.junit.Assert.*;

public class MusicTest {
    @Test
    public void equalsAndHashCode() {
        final long id = 1024;
        final String title = "TestTitle";
        final String artist = "TestArtist";
        final String album = "TestAlbum";
        final String uri = "https://www.test.com/test.mp3";
        final String iconUri = "https://www.test.com/test.png";
        final int duration = 60_000;
        final long addTime = System.currentTimeMillis();

        Music musicA = new Music(id, title, artist, album, uri, iconUri, duration, addTime);
        Music musicB = new Music(id, title, artist, album, uri, iconUri, duration, addTime);

        assertEquals(musicA, musicB);
        assertEquals(musicA.hashCode(), musicB.hashCode());

        assertEquals(id, musicA.getId());
        assertEquals(title, musicA.getTitle());
        assertEquals(artist, musicA.getArtist());
        assertEquals(album, musicA.getAlbum());
        assertEquals(uri, musicA.getUri());
        assertEquals(iconUri, musicA.getIconUri());
        assertEquals(duration, musicA.getDuration());
        assertEquals(addTime, musicA.getAddTime());
    }
}
