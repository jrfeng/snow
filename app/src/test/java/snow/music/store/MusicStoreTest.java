package snow.music.store;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import io.objectbox.BoxStore;

import static org.junit.Assert.*;

public class MusicStoreTest {
    private static final File TEST_DIRECTORY = new File("objectbox-debug/test-db");
    private BoxStore store;
    private MusicStore mMusicStore;

    @Before
    public void setUp() {
        BoxStore.deleteAllFiles(TEST_DIRECTORY);
        store = MyObjectBox.builder()
                .directory(TEST_DIRECTORY)
                .build();
        MusicStore.init(store);
        mMusicStore = MusicStore.getInstance();
    }

    @After
    public void tearDown() {
        if (store != null) {
            store.close();
            store = null;
        }
        BoxStore.deleteAllFiles(TEST_DIRECTORY);
    }

    @Test
    public void createMusicList() {
        final String name = "TestMusicList";
        final String description = "test description";

        MusicList musicList = mMusicStore.createMusicList(name, description);

        assertEquals(name, musicList.getName());
        assertEquals(description, musicList.getDescription());

        boolean exception = false;
        try {
            mMusicStore.createMusicList(MusicStore.MUSIC_LIST_FAVORITE);
        } catch (IllegalArgumentException e) {
            exception = true;
        }

        assertTrue(exception);
    }

    @Test
    public void getMusicList() {
        final String name = "TestMusicList";
        final String description = "test description";

        mMusicStore.createMusicList(name, description);
        MusicList musicList = mMusicStore.getMusicList(name);

        assertNotNull(musicList);
        assertEquals(name, musicList.getName());
        assertEquals(description, musicList.getDescription());
    }

    @Test
    public void updateMusicList() {
        final String name = "TestMusicList";
        final String description = "test description";

        final String newName = "NewTestMusicList";
        final String newDescription = "new test description";
        final Music music = new Music(
                0,
                "title1",
                "artist1",
                "album1",
                "https://www.test.com/test1.mp3",
                "https://www.test.com/test1.png",
                60_000,
                System.currentTimeMillis());
        mMusicStore.putMusic(music);

        MusicList musicList = mMusicStore.createMusicList(name, description);

        musicList.setName(newName);
        musicList.setDescription(newDescription);
        musicList.getMusicElements().add(music);

        mMusicStore.updateMusicList(musicList);

        musicList = mMusicStore.getMusicList(newName);

        assertNotNull(musicList);
        assertEquals(newName, musicList.getName());
        assertEquals(newDescription, musicList.getDescription());
        assertEquals(music, musicList.getMusicElements().get(0));
    }

    @Test
    public void deleteMusicList() {
        final String name = "TestMusicList";
        final String description = "test description";
        mMusicStore.createMusicList(name, description);

        MusicList musicList = mMusicStore.getMusicList(name);

        assertNotNull(musicList);

        mMusicStore.deleteMusicList(musicList);
        musicList = mMusicStore.getMusicList(name);

        assertNull(musicList);
    }

    @Test
    public void deleteMusicList_name() {
        final String name = "TestMusicList";
        final String description = "test description";
        mMusicStore.createMusicList(name, description);

        MusicList musicList = mMusicStore.getMusicList(name);

        assertNotNull(musicList);

        mMusicStore.deleteMusicList(name);
        musicList = mMusicStore.getMusicList(name);

        assertNull(musicList);
    }

    @Test
    public void isFavorite() {
        final Music music = new Music(
                0,
                "title1",
                "artist1",
                "album1",
                "https://www.test.com/test1.mp3",
                "https://www.test.com/test1.png",
                60_000,
                System.currentTimeMillis());
        mMusicStore.putMusic(music);

        MusicList favorite = mMusicStore.getFavoriteMusicList();
        favorite.getMusicElements().add(music);

        assertFalse(mMusicStore.isFavorite(music));

        mMusicStore.updateMusicList(favorite);

        assertTrue(mMusicStore.isFavorite(music));
    }

    @Test
    public void addToFavorite() {
        final Music music = new Music(
                0,
                "title1",
                "artist1",
                "album1",
                "https://www.test.com/test1.mp3",
                "https://www.test.com/test1.png",
                60_000,
                System.currentTimeMillis());
        mMusicStore.putMusic(music);

        mMusicStore.addToFavorite(music);
        assertTrue(mMusicStore.isFavorite(music));
    }

    @Test
    public void removeFromFavorite() {
        final Music music = new Music(
                0,
                "title1",
                "artist1",
                "album1",
                "https://www.test.com/test1.mp3",
                "https://www.test.com/test1.png",
                60_000,
                System.currentTimeMillis());
        mMusicStore.putMusic(music);

        mMusicStore.addToFavorite(music);
        assertTrue(mMusicStore.isFavorite(music));

        mMusicStore.removeFromFavorite(music);
        assertFalse(mMusicStore.isFavorite(music));
    }

    @Test
    public void addHistory() {
        final Music musicA = new Music(
                0,
                "title1",
                "artist1",
                "album1",
                "https://www.test.com/test1.mp3",
                "https://www.test.com/test1.png",
                60_000,
                System.currentTimeMillis());

        final Music musicB = new Music(
                0,
                "title2",
                "artist2",
                "album2",
                "https://www.test.com/test2.mp3",
                "https://www.test.com/test2.png",
                60_000,
                System.currentTimeMillis());
        mMusicStore.putMusic(musicA);
        mMusicStore.putMusic(musicB);

        mMusicStore.addHistory(musicA);
        mMusicStore.addHistory(musicB);

        assertEquals(musicA, mMusicStore.getAllHistory().get(0));
        assertEquals(musicB, mMusicStore.getAllHistory().get(1));

        mMusicStore.addHistory(musicA);

        assertEquals(musicB, mMusicStore.getAllHistory().get(0));
        assertEquals(musicA, mMusicStore.getAllHistory().get(1));
    }

    @Test
    public void removeHistory() {
        final Music music = new Music(
                0,
                "title1",
                "artist1",
                "album1",
                "https://www.test.com/test1.mp3",
                "https://www.test.com/test1.png",
                60_000,
                System.currentTimeMillis());
        mMusicStore.putMusic(music);

        mMusicStore.addHistory(music);

        assertEquals(music, mMusicStore.getAllHistory().get(0));

        mMusicStore.removeHistory(music);

        assertEquals(0, mMusicStore.getAllHistory().size());
    }

    @Test
    public void removeHistory_collection() {
        final Music musicA = new Music(
                0,
                "title1",
                "artist1",
                "album1",
                "https://www.test.com/test1.mp3",
                "https://www.test.com/test1.png",
                60_000,
                System.currentTimeMillis());

        final Music musicB = new Music(
                0,
                "title2",
                "artist2",
                "album2",
                "https://www.test.com/test2.mp3",
                "https://www.test.com/test2.png",
                60_000,
                System.currentTimeMillis());

        final Music musicC = new Music(
                0,
                "title3",
                "artist3",
                "album3",
                "https://www.test.com/test3.mp3",
                "https://www.test.com/test3.png",
                60_000,
                System.currentTimeMillis());

        mMusicStore.putMusic(musicA);
        mMusicStore.putMusic(musicB);
        mMusicStore.putMusic(musicC);

        mMusicStore.addHistory(musicA);
        mMusicStore.addHistory(musicB);
        mMusicStore.addHistory(musicC);

        List<Music> removeItems = new ArrayList<>();
        removeItems.add(musicA);
        removeItems.add(musicC);

        mMusicStore.removeHistory(removeItems);

        assertEquals(musicB, mMusicStore.getAllHistory().get(0));
    }

    @Test
    public void clearHistory() {
        final Music musicA = new Music(
                0,
                "title1",
                "artist1",
                "album1",
                "https://www.test.com/test1.mp3",
                "https://www.test.com/test1.png",
                60_000,
                System.currentTimeMillis());

        final Music musicB = new Music(
                0,
                "title2",
                "artist2",
                "album2",
                "https://www.test.com/test2.mp3",
                "https://www.test.com/test2.png",
                60_000,
                System.currentTimeMillis());

        final Music musicC = new Music(
                0,
                "title3",
                "artist3",
                "album3",
                "https://www.test.com/test3.mp3",
                "https://www.test.com/test3.png",
                60_000,
                System.currentTimeMillis());

        mMusicStore.putMusic(musicA);
        mMusicStore.putMusic(musicB);
        mMusicStore.putMusic(musicC);

        mMusicStore.addHistory(musicA);
        mMusicStore.addHistory(musicB);
        mMusicStore.addHistory(musicC);

        mMusicStore.clearHistory();

        assertEquals(0, mMusicStore.getAllHistory().size());
    }
}
