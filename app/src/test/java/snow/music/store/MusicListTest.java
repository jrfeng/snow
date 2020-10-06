package snow.music.store;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import io.objectbox.BoxStore;

import static org.junit.Assert.*;

public class MusicListTest {
    private static final File TEST_DIRECTORY = new File("objectbox-debug/test-db");
    private static final String TEST_MUSIC_LIST = "music_list_test";

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

        assertEquals(2, musicList.getSize());
        assertEquals(musicA, musicList.getMusicElements().get(0));
        assertEquals(musicB, musicList.getMusicElements().get(1));
    }

    @Test
    public void elements_add_index() {
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
        musicList.getMusicElements().add(musicB);

        musicList.getMusicElements().add(1, musicB);

        assertEquals(2, musicList.getSize());
        assertEquals(musicA, musicList.getMusicElements().get(0));
        assertEquals(musicB, musicList.getMusicElements().get(1));

        musicList.getMusicElements().add(0, musicB);
        assertEquals(musicB, musicList.getMusicElements().get(0));
        assertEquals(musicA, musicList.getMusicElements().get(1));
    }

    @Test
    public void elements_addAll() {
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

        MusicList musicList = mMusicStore.createMusicList(TEST_MUSIC_LIST);
        musicList.getMusicElements().add(musicA);

        List<Music> musics = new ArrayList<>();
        musics.add(musicB);
        musics.add(musicA);
        musics.add(musicC);
        musics.add(musicA);

        musicList.getMusicElements().addAll(musics);

        assertEquals(3, musicList.getSize());
        assertEquals(musicA, musicList.getMusicElements().get(0));
        assertEquals(musicB, musicList.getMusicElements().get(1));
        assertEquals(musicC, musicList.getMusicElements().get(2));
    }

    @Test
    public void elements_addAll_index() {
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

        MusicList musicList = mMusicStore.createMusicList(TEST_MUSIC_LIST);
        musicList.getMusicElements().add(musicA);

        List<Music> musics = new ArrayList<>();
        musics.add(musicB);
        musics.add(musicA);
        musics.add(musicC);
        musics.add(musicB);

        musicList.getMusicElements().addAll(0, musics);

        assertEquals(3, musicList.getSize());
        assertEquals(musicB, musicList.getMusicElements().get(0));
        assertEquals(musicC, musicList.getMusicElements().get(1));
        assertEquals(musicA, musicList.getMusicElements().get(2));
    }

    @Test
    public void elements_remove() {
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
        musicList.getMusicElements().add(musicB);

        musicList.getMusicElements().remove(musicA);

        assertEquals(1, musicList.getSize());
        assertEquals(musicB, musicList.getMusicElements().get(0));
    }

    @Test
    public void elements_remove_index() {
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
        musicList.getMusicElements().add(musicB);

        musicList.getMusicElements().remove(0);

        assertEquals(1, musicList.getSize());
        assertEquals(musicB, musicList.getMusicElements().get(0));
    }

    @Test
    public void elements_removeAll() {
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
        musicList.getMusicElements().add(musicB);

        List<Music> musics = new ArrayList<>();
        musics.add(musicA);
        musics.add(musicA);
        musics.add(musicB);
        musics.add(musicB);

        musicList.getMusicElements().removeAll(musics);

        assertEquals(0, musicList.getSize());
    }

    @Test
    public void elements_set() {
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

        MusicList musicList = mMusicStore.createMusicList(TEST_MUSIC_LIST);
        musicList.getMusicElements().add(musicA);
        musicList.getMusicElements().add(musicB);

        musicList.getMusicElements().set(1, musicC);

        assertEquals(musicC, musicList.getMusicElements().get(1));

        musicList.getMusicElements().add(musicB);
        musicList.getMusicElements().set(2, musicC);

        assertEquals(2, musicList.getSize());
        assertEquals(musicA, musicList.getMusicElements().get(0));
        assertEquals(musicC, musicList.getMusicElements().get(1));
    }

    @Test
    public void elements_retainAll() {
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

        MusicList musicList = mMusicStore.createMusicList(TEST_MUSIC_LIST);
        musicList.getMusicElements().add(musicA);
        musicList.getMusicElements().add(musicB);

        List<Music> retain = new ArrayList<>();
        retain.add(musicB);
        retain.add(musicC);

        musicList.getMusicElements().retainAll(retain);

        assertEquals(1, musicList.getSize());
        assertEquals(musicB, musicList.getMusicElements().get(0));
    }

    @Test
    public void elements_orders() {
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

        List<Music> musics = new ArrayList<>();
        musics.add(musicA);
        musics.add(musicB);
        musics.add(musicC);

        mMusicStore.putAllMusic(musics);

        MusicList musicList = mMusicStore.createMusicList(TEST_MUSIC_LIST);
        musicList.getMusicElements().add(musicB);
        musicList.getMusicElements().add(musicC);
        musicList.getMusicElements().add(musicA);

        mMusicStore.updateMusicList(musicList);

        musicList = mMusicStore.createMusicList(TEST_MUSIC_LIST);

        assertEquals(musicB, musicList.getMusicElements().get(0));
        assertEquals(musicC, musicList.getMusicElements().get(1));
        assertEquals(musicA, musicList.getMusicElements().get(2));
    }
}
