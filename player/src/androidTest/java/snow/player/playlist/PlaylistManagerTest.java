package snow.player.playlist;

import android.content.Context;

import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;

import snow.player.media.MusicItem;

import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
public class PlaylistManagerTest {
    private static int persistentId = 0;

    private PlaylistManager createPlaylistManager() {
        Context context = InstrumentationRegistry.getInstrumentation().getContext();
        PlaylistManager playlistManager = new PlaylistManager(context, "test" + persistentId) {
            @Override
            protected void setEditable(boolean editable) {
                super.setEditable(editable);
            }
        };
        playlistManager.setEditable(true);

        persistentId++;

        return playlistManager;
    }

    private MusicItem createMusicItem(int id) {
        MusicItem musicItem = new MusicItem();

        musicItem.setMusicId("au" + id);
        musicItem.setTitle("title_" + id);
        musicItem.setArtist("artist_" + id);
        musicItem.setAlbum("album_" + id);
        musicItem.setUri("https://www.test.com/test" + id + ".mp3");
        musicItem.setUri("https://www.test.com/icon" + id + ".png");
        musicItem.setDuration(120_000);

        return musicItem;
    }

    private Playlist createPlaylist(int size) {
        List<MusicItem> items = new ArrayList<>(size);

        for (int i = 0; i < size; i++) {
            items.add(createMusicItem(i));
        }

        return new Playlist(items);
    }

    @Test(timeout = 3000)
    public void setPlaylistSize() throws InterruptedException {
        final PlaylistManager playlistManager = createPlaylistManager();

        final int size = 20;
        final Playlist playlist = createPlaylist(size);
        final CountDownLatch latch = new CountDownLatch(1);
        playlistManager.setOnModifyPlaylistListener(new OnModifyPlaylistAdapter() {
            @Override
            public void onNewPlaylist(int position, boolean play) {
                assertEquals(size, playlistManager.getPlaylistSize());
                assertEquals(playlist, playlistManager.getPlaylist());
                latch.countDown();
            }
        });

        playlistManager.setPlaylist(playlist, 0, false);
        latch.await();
    }

    @Test(timeout = 3_000)
    public void insertMusicItem() throws InterruptedException {
        final PlaylistManager playlistManager = createPlaylistManager();

        // init playlist
        final int size = 10;
        final Playlist playlist = createPlaylist(size);
        final CountDownLatch latch1 = new CountDownLatch(1);
        playlistManager.setOnModifyPlaylistListener(new OnModifyPlaylistAdapter() {
            @Override
            public void onNewPlaylist(int position, boolean play) {
                latch1.countDown();
            }
        });
        playlistManager.setPlaylist(playlist, 0, false);
        latch1.await();

        // insert musicItem: not exist
        final int insertPosition = size / 2;
        final MusicItem notExistMusicItem = createMusicItem(size + 5);
        final CountDownLatch latch2 = new CountDownLatch(1);
        playlistManager.setOnModifyPlaylistListener(new OnModifyPlaylistAdapter() {
            @Override
            public void onMusicItemInserted(int position, MusicItem musicItem) {
                assertEquals(size + 1, playlistManager.getPlaylistSize());
                assertEquals(musicItem, playlistManager.getPlaylist().get(insertPosition));
                latch2.countDown();
            }
        });
        playlistManager.insertMusicItem(insertPosition, notExistMusicItem);
        latch2.await();

        // insert musicItem: exist
        final int existMusicItemIndex = 0;
        final MusicItem existMusicItem = playlist.get(existMusicItemIndex);
        final CountDownLatch latch3 = new CountDownLatch(1);
        playlistManager.setOnModifyPlaylistListener(new OnModifyPlaylistAdapter() {
            @Override
            public void onMusicItemMoved(int fromPosition, int toPosition) {
                assertEquals(existMusicItemIndex, fromPosition);
                assertEquals(insertPosition, toPosition);
                assertEquals(existMusicItem, playlistManager.getPlaylist().get(insertPosition));
                latch3.countDown();
            }
        });
        playlistManager.insertMusicItem(insertPosition, existMusicItem);
        latch3.await();
    }

    @Test(timeout = 3_000)
    public void moveMusicItem() throws InterruptedException {
        final PlaylistManager playlistManager = createPlaylistManager();

        // init playlist
        final int size = 10;
        final Playlist playlist = createPlaylist(size);
        final CountDownLatch latch1 = new CountDownLatch(1);
        playlistManager.setOnModifyPlaylistListener(new OnModifyPlaylistAdapter() {
            @Override
            public void onNewPlaylist(int position, boolean play) {
                latch1.countDown();
            }
        });
        playlistManager.setPlaylist(playlist, 0, false);
        latch1.await();

        // move music item
        final int from = 2;
        final int to = 8;
        final MusicItem fromMusicItem = playlist.get(from);
        final CountDownLatch latch2 = new CountDownLatch(1);
        playlistManager.setOnModifyPlaylistListener(new OnModifyPlaylistAdapter() {
            @Override
            public void onMusicItemMoved(int fromPosition, int toPosition) {
                assertEquals(from, fromPosition);
                assertEquals(to, toPosition);
                assertEquals(fromMusicItem, playlistManager.getPlaylist().get(toPosition));
                latch2.countDown();
            }
        });
        playlistManager.moveMusicItem(from, to);
        latch2.await();
    }

    @Test(timeout = 3_000)
    public void removeMusicItem() throws InterruptedException {
        final PlaylistManager playlistManager = createPlaylistManager();

        // init playlist
        final int size = 10;
        final Playlist playlist = createPlaylist(size);
        final CountDownLatch latch1 = new CountDownLatch(1);
        playlistManager.setOnModifyPlaylistListener(new OnModifyPlaylistAdapter() {
            @Override
            public void onNewPlaylist(int position, boolean play) {
                latch1.countDown();
            }
        });
        playlistManager.setPlaylist(playlist, 0, false);
        latch1.await();

        // remove music item
        final int removePosition = 0;
        final MusicItem removeMusicItem = playlist.get(removePosition);
        final CountDownLatch latch2 = new CountDownLatch(1);
        playlistManager.setOnModifyPlaylistListener(new OnModifyPlaylistAdapter(){
            @Override
            public void onMusicItemRemoved(MusicItem musicItem) {
                assertEquals(removeMusicItem, musicItem);
                assertNotEquals(removeMusicItem, playlistManager.getPlaylist().get(removePosition));
                latch2.countDown();
            }
        });
        playlistManager.removeMusicItem(removeMusicItem);
        latch2.await();
    }

    private static class OnModifyPlaylistAdapter implements PlaylistManager.OnModifyPlaylistListener {
        @Override
        public void onNewPlaylist(int position, boolean play) {

        }

        @Override
        public void onMusicItemMoved(int fromPosition, int toPosition) {

        }

        @Override
        public void onMusicItemInserted(int position, MusicItem musicItem) {

        }

        @Override
        public void onMusicItemRemoved(MusicItem musicItem) {

        }
    }
}
