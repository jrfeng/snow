package snow.player.playlist;

import android.os.Parcel;

import org.junit.BeforeClass;
import org.junit.Test;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.List;

import snow.player.MusicItem;

import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
public class PlaylistTest {
    private static List<MusicItem> mItems;
    private static Playlist mPlaylist;
    private static final int mSize = 100;

    @BeforeClass
    public static void initPlaylist() {
        mItems = new ArrayList<>(mSize);
        for (int i = 0; i < mSize; i++) {
            mItems.add(generateMusicItem(i));
        }

        mPlaylist = new Playlist(mItems);
    }

    private static MusicItem generateMusicItem(int id) {
        MusicItem musicItem = new MusicItem();

        musicItem.setMusicId("au" + id);
        musicItem.setToken("token_test_" + id);
        musicItem.setTitle("test_" + id);
        musicItem.setArtist("artist_" + id);
        musicItem.setAlbum("album_" + id);
        musicItem.setUri("https://www.test.com/test_" + id + ".mp3");
        musicItem.setUri("https://www.test.com/icon_test" + id + ".png");
        musicItem.setDuration(1000 * id);

        return musicItem;
    }

    @Test
    public void constructorTest() {
        mItems.remove(0);
        assertEquals(mSize, mPlaylist.size());

        mItems.add(generateMusicItem(mSize));
        assertEquals(mSize, mPlaylist.size());
    }

    @Test
    public void contains() {
        MusicItem musicItem1 = generateMusicItem(1);
        MusicItem musicItem2 = generateMusicItem(mSize);

        assertTrue(mPlaylist.contains(musicItem1));
        assertFalse(mPlaylist.contains(musicItem2));
    }

    @Test
    public void get() {
        final int index = 1;
        MusicItem musicItem = generateMusicItem(index);
        assertEquals(musicItem, mPlaylist.get(index));
    }

    @Test
    public void indexOf() {
        final int index = 1;

        MusicItem musicItem1 = generateMusicItem(index);
        MusicItem musicItem2 = generateMusicItem(mSize);

        assertEquals(index, mPlaylist.indexOf(musicItem1));
        assertEquals(-1, mPlaylist.indexOf(musicItem2));
    }

    @Test
    public void isEmpty() {
        assertFalse(mPlaylist.isEmpty());

        Playlist playlist = new Playlist(new ArrayList<MusicItem>());

        assertTrue(playlist.isEmpty());
    }

    @Test
    public void iterator() {
        assertNotNull(mPlaylist.iterator());
    }

    @Test
    public void lastIndexOf() {
        MusicItem musicItem1 = generateMusicItem(1);
        MusicItem musicItem2 = generateMusicItem(2);
        MusicItem musicItem3 = generateMusicItem(3);

        List<MusicItem> items = new ArrayList<>();

        items.add(musicItem1);
        items.add(musicItem2);
        items.add(musicItem1);
        items.add(musicItem3);

        Playlist playlist = new Playlist(items);

        assertEquals(2, playlist.lastIndexOf(musicItem1));
    }

    @Test
    public void size() {
        assertEquals(mSize, mPlaylist.size());
    }

    @Test
    public void getAllMusicItem() {
        List<MusicItem> items = mPlaylist.getAllMusicItem();
        assertEquals(mPlaylist.size(), items.size());

        items.remove(0);
        assertEquals(mSize, mPlaylist.size());

        items.add(generateMusicItem(mSize));
        assertEquals(mSize, mPlaylist.size());
    }

    @Test
    public void equalsTest() {
        Playlist playlist1 = new Playlist(mItems);
        Playlist playlist2 = new Playlist(mItems);

        mItems.remove(0);
        Playlist playlist3 = new Playlist(mItems);

        assertEquals(playlist1, playlist2);
        assertNotEquals(playlist1, playlist3);
    }

    @Test
    public void hashCodeTest() {
        Playlist playlist1 = new Playlist(mItems);
        Playlist playlist2 = new Playlist(mItems);

        mItems.remove(0);
        Playlist playlist3 = new Playlist(mItems);

        assertEquals(playlist1.hashCode(), playlist2.hashCode());
        assertNotEquals(playlist1.hashCode(), playlist3.hashCode());
    }

    @Test
    public void parcelableTest() {
        Parcel parcel = Parcel.obtain();

        mPlaylist.writeToParcel(parcel, 0);

        parcel.setDataPosition(0);

        Playlist other = new Playlist(parcel);

        assertEquals(mPlaylist, other);
        parcel.recycle();
    }
}
