package snow.player.playlist;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;

import snow.player.media.MusicItem;

import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
public class PlaylistManagerTest {
    private static PlaylistManager mPlaylistManager;
    private static final String ID_PLAYLIST = "id_test_playlist";

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

    private Playlist generatePlaylist(int size) {
        List<MusicItem> items = new ArrayList<>(size);

        for (int i = 0; i < size; i++) {
            items.add(generateMusicItem(i));
        }

        return new Playlist(items);
    }

    private void await(CountDownLatch countDownLatch) {
        try {
            countDownLatch.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @BeforeClass
    public static void initPlaylistManager() {
        Context context = InstrumentationRegistry.getInstrumentation().getContext();
        mPlaylistManager = new PlaylistManager(context, ID_PLAYLIST) {
        };
        mPlaylistManager.setEditable(true);
    }

    @Before
    public void clearOnModifyPlaylistListener() {
        mPlaylistManager.setOnModifyPlaylistListener(null);
    }

    @Test(timeout = 3000)
    public void getPlaylistSize() {
        final CountDownLatch countDownLatch1 = new CountDownLatch(1);
        final int size1 = mPlaylistManager.getPlaylistSize();

        mPlaylistManager.setOnModifyPlaylistListener(new PlaylistManager.OnModifyPlaylistListener() {
            @Override
            public void onPlaylistSwapped(int position, boolean playOnPrepared) {

            }

            @Override
            public void onMusicItemMoved(int fromPosition, int toPosition) {

            }

            @Override
            public void onMusicItemInserted(int position, int count) {
                assertEquals(size1 + count, mPlaylistManager.getPlaylistSize());
                countDownLatch1.countDown();
            }

            @Override
            public void onMusicItemRemoved(List<Integer> positions) {

            }
        });

        mPlaylistManager.appendMusicItem(generateMusicItem(1));
        await(countDownLatch1);

        final CountDownLatch countDownLatch2 = new CountDownLatch(1);
        final int size2 = mPlaylistManager.getPlaylistSize();

        mPlaylistManager.setOnModifyPlaylistListener(new PlaylistManager.OnModifyPlaylistListener() {
            @Override
            public void onPlaylistSwapped(int position, boolean playOnPrepared) {

            }

            @Override
            public void onMusicItemMoved(int fromPosition, int toPosition) {

            }

            @Override
            public void onMusicItemInserted(int position, int count) {

            }

            @Override
            public void onMusicItemRemoved(List<Integer> positions) {
                assertEquals(size2 - positions.size(), mPlaylistManager.getPlaylistSize());
                countDownLatch2.countDown();
            }
        });

        List<Integer> positions = new ArrayList<>();
        positions.add(0);
        mPlaylistManager.removeMusicItem(positions);
        await(countDownLatch2);
    }

    @Test
    public void getPlaylist() {
        Playlist playlist1 = mPlaylistManager.getPlaylist();
        Playlist playlist2 = mPlaylistManager.getPlaylist();

        assertNotNull(playlist1);
        assertNotNull(playlist2);
        assertEquals(playlist1, playlist2);
    }

    @Test(timeout = 3_000)
    public void getPlaylistAsync() {
        final CountDownLatch countDownLatch = new CountDownLatch(1);

        mPlaylistManager.getPlaylistAsync(new PlaylistManager.Callback() {
            @Override
            public void onFinished(@NonNull Playlist playlist) {
                assertNotNull(playlist);
                assertEquals(playlist, mPlaylistManager.getPlaylist());

                countDownLatch.countDown();
            }
        });

        await(countDownLatch);
    }

    @Test(timeout = 3_000)
    public void setPlaylist() {
        final int size = 100;

        final CountDownLatch countDownLatch = new CountDownLatch(1);

        final Playlist playlist = generatePlaylist(size);

        mPlaylistManager.setOnModifyPlaylistListener(new PlaylistManager.OnModifyPlaylistListener() {
            @Override
            public void onPlaylistSwapped(int position, boolean playOnPrepared) {
                assertEquals(playlist, mPlaylistManager.getPlaylist());
                countDownLatch.countDown();
            }

            @Override
            public void onMusicItemMoved(int fromPosition, int toPosition) {
                // ignore
            }

            @Override
            public void onMusicItemInserted(int position, int count) {
                // ignore
            }

            @Override
            public void onMusicItemRemoved(List<Integer> positions) {
                // ignore
            }
        });

        mPlaylistManager.setPlaylist(playlist);
        await(countDownLatch);
    }

    @Test(timeout = 3_000)
    public void moveMusicItem() {
        final int size = 100;
        final int from = size / 2;
        final int to = 0;
        final Playlist playlist = generatePlaylist(size);

        final CountDownLatch countDownLatch = new CountDownLatch(1);

        mPlaylistManager.setOnModifyPlaylistListener(new PlaylistManager.OnModifyPlaylistListener() {
            @Override
            public void onPlaylistSwapped(int position, boolean playOnPrepared) {
                mPlaylistManager.moveMusicItem(from, to);
            }

            @Override
            public void onMusicItemMoved(int fromPosition, int toPosition) {
                assertEquals(from, fromPosition);
                assertEquals(to, toPosition);

                Playlist pl = mPlaylistManager.getPlaylist();
                assertEquals(playlist.get(from), pl.get(to));

                countDownLatch.countDown();
            }

            @Override
            public void onMusicItemInserted(int position, int count) {
                // ignore
            }

            @Override
            public void onMusicItemRemoved(List<Integer> positions) {
                // ignore
            }
        });

        mPlaylistManager.setPlaylist(playlist);
        await(countDownLatch);
    }

    @Test(timeout = 3_000)
    public void appendMusicItem() {
        final MusicItem musicItem = generateMusicItem(1024);

        final CountDownLatch countDownLatch = new CountDownLatch(1);

        mPlaylistManager.setOnModifyPlaylistListener(new PlaylistManager.OnModifyPlaylistListener() {
            @Override
            public void onPlaylistSwapped(int position, boolean playOnPrepared) {

            }

            @Override
            public void onMusicItemMoved(int fromPosition, int toPosition) {

            }

            @Override
            public void onMusicItemInserted(int position, int count) {
                Playlist playlist = mPlaylistManager.getPlaylist();

                assertEquals(musicItem, playlist.get(playlist.size() - 1));
                countDownLatch.countDown();
            }

            @Override
            public void onMusicItemRemoved(List<Integer> positions) {

            }
        });

        mPlaylistManager.appendMusicItem(musicItem);
        await(countDownLatch);
    }

    @Test(timeout = 3_000)
    public void appendAllMusicItem() {
        final List<MusicItem> musicItems = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            musicItems.add(generateMusicItem(1024 + i));
        }

        final CountDownLatch countDownLatch = new CountDownLatch(1);

        mPlaylistManager.setOnModifyPlaylistListener(new PlaylistManager.OnModifyPlaylistListener() {
            @Override
            public void onPlaylistSwapped(int position, boolean playOnPrepared) {

            }

            @Override
            public void onMusicItemMoved(int fromPosition, int toPosition) {

            }

            @Override
            public void onMusicItemInserted(int position, int count) {
                Playlist playlist = mPlaylistManager.getPlaylist();

                assertEquals(musicItems.size(), count);
                assertEquals(musicItems, playlist.getAllMusicItem().subList(position, playlist.size()));
                countDownLatch.countDown();
            }

            @Override
            public void onMusicItemRemoved(List<Integer> positions) {

            }
        });

        mPlaylistManager.appendAllMusicItem(musicItems);
        await(countDownLatch);
    }

    @Test(timeout = 3_000)
    public void insertMusicItem() {
        final int size = 100;
        final int insertPosition = size / 2;
        final MusicItem musicItem = generateMusicItem(size);
        final Playlist playlist = generatePlaylist(size);

        final CountDownLatch countDownLatch = new CountDownLatch(1);

        mPlaylistManager.setOnModifyPlaylistListener(new PlaylistManager.OnModifyPlaylistListener() {
            @Override
            public void onPlaylistSwapped(int position, boolean playOnPrepared) {
                mPlaylistManager.insertMusicItem(insertPosition, musicItem);
            }

            @Override
            public void onMusicItemMoved(int fromPosition, int toPosition) {
                // ignore
            }

            @Override
            public void onMusicItemInserted(int position, int count) {
                Playlist pl = mPlaylistManager.getPlaylist();

                assertEquals(1, count);
                assertEquals(musicItem, pl.get(insertPosition));
                countDownLatch.countDown();
            }

            @Override
            public void onMusicItemRemoved(List<Integer> positions) {
                // ignore
            }
        });

        mPlaylistManager.setPlaylist(playlist);
        await(countDownLatch);
    }

    @Test(timeout = 3_000)
    public void insertAllMusicItem() {
        final int size = 100;
        final int insertPosition = size / 2;
        final int insertCount = 5;
        final List<MusicItem> items = new ArrayList<>();

        for (int i = size; i < size + insertCount; i++) {
            items.add(generateMusicItem(i));
        }

        final Playlist playlist = generatePlaylist(size);

        final CountDownLatch countDownLatch = new CountDownLatch(1);

        mPlaylistManager.setOnModifyPlaylistListener(new PlaylistManager.OnModifyPlaylistListener() {
            @Override
            public void onPlaylistSwapped(int position, boolean playOnPrepared) {
                mPlaylistManager.insertAllMusicItem(insertPosition, items);
            }

            @Override
            public void onMusicItemMoved(int fromPosition, int toPosition) {
                // ignore
            }

            @Override
            public void onMusicItemInserted(int position, int count) {
                Playlist pl = mPlaylistManager.getPlaylist();

                assertEquals(insertCount, count);
                assertEquals(items, pl.getAllMusicItem().subList(insertPosition, insertPosition + insertCount));
                countDownLatch.countDown();
            }

            @Override
            public void onMusicItemRemoved(List<Integer> positions) {
                // ignore
            }
        });

        mPlaylistManager.setPlaylist(playlist);
        await(countDownLatch);
    }

    @Test(timeout = 3_000)
    public void removeMusicItem() {
        final int size = 100;
        final Playlist playlist = generatePlaylist(size);

        final List<Integer> removePositions = new ArrayList<>();
        removePositions.add(12);
        removePositions.add(13);
        removePositions.add(45);
        removePositions.add(23);

        final CountDownLatch countDownLatch = new CountDownLatch(1);

        mPlaylistManager.setOnModifyPlaylistListener(new PlaylistManager.OnModifyPlaylistListener() {
            @Override
            public void onPlaylistSwapped(int position, boolean playOnPrepared) {
                mPlaylistManager.removeMusicItem(removePositions);
            }

            @Override
            public void onMusicItemMoved(int fromPosition, int toPosition) {
                // ignore
            }

            @Override
            public void onMusicItemInserted(int position, int count) {
                // ignore
            }

            @Override
            public void onMusicItemRemoved(List<Integer> positions) {
                Playlist pl = mPlaylistManager.getPlaylist();

                assertEquals(removePositions, positions);
                assertEquals(size - removePositions.size(), pl.size());
                countDownLatch.countDown();
            }
        });

        mPlaylistManager.setPlaylist(playlist);
        await(countDownLatch);
    }

    @Test(timeout = 3_000)
    public void concurrentTest() {
        final int size = 200;
        final Playlist playlist = generatePlaylist(size);
        final MusicItem musicItem = generateMusicItem(size);

        final CountDownLatch countDownLatch = new CountDownLatch(1);

        mPlaylistManager.setOnModifyPlaylistListener(new PlaylistManager.OnModifyPlaylistListener() {
            @Override
            public void onPlaylistSwapped(int position, boolean playOnPrepared) {
                mPlaylistManager.moveMusicItem(size - 1, 0);
                mPlaylistManager.insertMusicItem(0, musicItem);
            }

            @Override
            public void onMusicItemMoved(int fromPosition, int toPosition) {

            }

            @Override
            public void onMusicItemInserted(int position, int count) {
                Playlist pl = mPlaylistManager.getPlaylist();
                assertEquals(musicItem, pl.get(0));

                countDownLatch.countDown();
            }

            @Override
            public void onMusicItemRemoved(List<Integer> positions) {

            }
        });

        mPlaylistManager.setPlaylist(playlist);
        await(countDownLatch);
    }
}
