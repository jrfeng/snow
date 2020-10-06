package snow.music.store;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;

import io.objectbox.BoxStore;

import static org.junit.Assert.*;

public class MusicListTest {
    private static final File TEST_DIRECTORY = new File("objectbox-debug/test-db");
    private static final String TEST_MUSIC_LIST = "music_list_test";

    private BoxStore store;
    private MusicStore mMusicStore;

    @Before
    public void setUp() throws Exception {
        BoxStore.deleteAllFiles(TEST_DIRECTORY);
        store = MyObjectBox.builder()
                .directory(TEST_DIRECTORY)
                .build();
        MusicStore.init(store);
        mMusicStore = MusicStore.getInstance();
    }

    @After
    public void tearDown() throws Exception {
        if (store != null) {
            store.close();
            store = null;
        }
        BoxStore.deleteAllFiles(TEST_DIRECTORY);
    }

    @Test
    public void setName() {
        final String newName = "NewTestName";

        MusicList musicList = mMusicStore.createMusicList(TEST_MUSIC_LIST);
        musicList.setName(newName);

        assertEquals(newName, musicList.getName());

        MusicList localMusicList = mMusicStore.getLocalMusicList();
        localMusicList.setName(newName);

        assertNotEquals(newName, localMusicList.getName());
        assertEquals(MusicStore.MUSIC_LIST_LOCAL_MUSIC, localMusicList.getName());

        MusicList favorite = mMusicStore.getFavoriteMusicList();
        favorite.setName(newName);

        assertNotEquals(newName, favorite.getName());
        assertEquals(MusicStore.MUSIC_LIST_FAVORITE, favorite.getName());
    }

    @Test
    public void setDescription() {
        final String description = "new description";

        MusicList musicList = mMusicStore.createMusicList(TEST_MUSIC_LIST);
        musicList.setDescription(description);

        assertEquals(description, musicList.getDescription());
    }

    @Test
    public void getSize() {
        MusicList musicList = mMusicStore.createMusicList(TEST_MUSIC_LIST);

        musicList.getMusicElements()
                .add(new Music(
                        0,
                        "title1",
                        "artist1",
                        "album1",
                        "https://www.test.com/test1.mp3",
                        "https://www.test.com/test1.png",
                        60_000,
                        System.currentTimeMillis()));

        musicList.getMusicElements()
                .add(new Music(
                        0,
                        "title2",
                        "artist2",
                        "album2",
                        "https://www.test.com/test2.mp3",
                        "https://www.test.com/test2.png",
                        60_000,
                        System.currentTimeMillis()));

        assertEquals(2, musicList.getSize());
    }

    @Test
    public void elements_add() {
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

        MusicList musicList = mMusicStore.createMusicList(TEST_MUSIC_LIST);

        musicList.getMusicElements().add(musicA);
        musicList.getMusicElements().add(musicA);
        musicList.getMusicElements().add(musicB);
        musicList.getMusicElements().add(musicB);

        mMusicStore.updateMusicList(musicList);

        assertEquals(2, musicList.getSize());
        assertEquals(musicA, musicList.getMusicElements().get(0));
        assertEquals(musicB, musicList.getMusicElements().get(1));
    }


}
