package snow.player.media;

import android.os.Bundle;
import android.os.Parcel;

import org.junit.Test;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.runner.RunWith;

import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
public class MusicItemTest {
    @Test
    public void parcelableTest() {
        MusicItem musicItem = new MusicItem();

        musicItem.setMusicId("1024");
        musicItem.setToken("token_test");
        musicItem.setTitle("test");
        musicItem.setArtist("test");
        musicItem.setAlbum("test");
        musicItem.setUri("https://www.test.com/test.mp3");
        musicItem.setUri("https://www.test.com/icon_test.png");
        musicItem.setDuration(1000);

        Parcel parcel = Parcel.obtain();
        musicItem.writeToParcel(parcel, 0);

        parcel.setDataPosition(0);
        MusicItem other = new MusicItem(parcel);

        // assert
        assertEquals(musicItem, other);
        assertTrue(other.same(musicItem));


        Bundle extra = new Bundle();
        extra.putString("key_test", "value_test");

        musicItem.setExtra(extra);

        parcel.setDataPosition(0);
        musicItem.writeToParcel(parcel, 0);

        parcel.setDataPosition(0);
        other = new MusicItem(parcel);

        // assert
        assertEquals(musicItem, other);
        assertTrue(other.same(musicItem));
    }

    @Test
    public void cloneTest() throws CloneNotSupportedException {
        MusicItem musicItem = new MusicItem();

        musicItem.setMusicId("1024");
        musicItem.setToken("token_test");
        musicItem.setTitle("test");
        musicItem.setArtist("test");
        musicItem.setAlbum("test");
        musicItem.setUri("https://www.test.com/test.mp3");
        musicItem.setUri("https://www.test.com/icon_test.png");
        musicItem.setDuration(1000);

        MusicItem other = musicItem.clone();

        // assert
        assertEquals(musicItem, other);
        assertTrue(other.same(musicItem));

        Bundle extra = new Bundle();
        extra.putString("key_test", "value_test");

        musicItem.setExtra(extra);

        other = musicItem.clone();

        // assert
        assertEquals(musicItem, other);
        assertTrue(other.same(musicItem));
    }
}
