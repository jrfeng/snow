package snow.player.playlist;

import android.content.Context;

import org.junit.Before;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;

import snow.player.Player;
import snow.player.media.MusicItem;
import snow.player.test.TestMusicPlayer;
import snow.player.test.TestPlaylistManager;
import snow.player.test.TestPlaylistPlayer;

import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
public class AbstractPlaylistPlayerTest {
    private Playlist mPlaylist;
    private TestPlaylistManager mTestPlaylistManager;
    private PlaylistState mPlaylistState;
    private TestPlaylistPlayer mTestPlaylistPlayer;
    private TestMusicPlayer mTestMusicPlayer;

    @Before
    public void initFields() {
        mPlaylist = createPlaylist(10);
        mTestPlaylistManager = new TestPlaylistManager(getContext(), mPlaylist);
        mPlaylistState = createPlaylistState();
        mTestMusicPlayer = new TestMusicPlayer();
        mTestPlaylistPlayer = new TestPlaylistPlayer(getContext(), mPlaylistState, mTestPlaylistManager, mTestMusicPlayer);
    }

    private Context getContext() {
        return InstrumentationRegistry.getInstrumentation().getTargetContext();
    }

    private Playlist createPlaylist(int size) {
        List<MusicItem> musicItems = new ArrayList<>();

        for (int i = 0; i < size; i++) {
            musicItems.add(createMusicItem(i));
        }

        return new Playlist(musicItems);
    }

    private MusicItem createMusicItem(int id) {
        MusicItem musicItem = new MusicItem();

        musicItem.setToken("token_" + id);
        musicItem.setTitle("title_" + id);
        musicItem.setArtist("artist_" + id);
        musicItem.setUri("http://www.test.com/test" + id + ".mp3");

        return musicItem;
    }

    private PlaylistState createPlaylistState() {
        PlaylistState playlistState = new PlaylistState();

        final int position = 0;

        playlistState.setPlayProgress(30_000);
        playlistState.setPlayProgressUpdateTime(System.currentTimeMillis());
        playlistState.setMusicItem(mPlaylist.get(position));
        playlistState.setPosition(position);
        playlistState.setPlayMode(PlaylistPlayer.PlayMode.SEQUENTIAL);

        return playlistState;
    }

    @Test(timeout = 3000)
    public void skipToNext() throws InterruptedException {
        final int position = mPlaylistState.getPosition();

        final CountDownLatch latch = new CountDownLatch(1);
        mTestPlaylistPlayer.tester().setPlayingAction(new Runnable() {
            @Override
            public void run() {
                latch.countDown();
            }
        });
        mTestPlaylistPlayer.skipToNext();
        latch.await();

        assertEquals(position + 1, mPlaylistState.getPosition());

        mTestPlaylistPlayer.setPlayMode(PlaylistPlayer.PlayMode.LOOP);
        mTestPlaylistPlayer.skipToNext();

        assertEquals(position + 2, mPlaylistState.getPosition());

        mTestPlaylistPlayer.playOrPause(mPlaylist.size() - 1);
        mTestPlaylistPlayer.skipToNext();

        assertEquals(0, mPlaylistState.getPosition());
    }

    @Test(timeout = 3000)
    public void skipToPrevious() throws InterruptedException {
        final CountDownLatch latch = new CountDownLatch(1);
        mTestPlaylistPlayer.tester().setPlayingAction(new Runnable() {
            @Override
            public void run() {
                latch.countDown();
            }
        });
        mTestPlaylistPlayer.skipToPrevious();
        latch.await();

        assertEquals(mPlaylist.size() - 1, mPlaylistState.getPosition());

        final int position = mPlaylistState.getPosition();

        mTestPlaylistPlayer.skipToPrevious();
        assertEquals(position - 1, mPlaylistState.getPosition());

        mTestPlaylistPlayer.setPlayMode(PlaylistPlayer.PlayMode.LOOP);
        mTestPlaylistPlayer.skipToPrevious();
        assertEquals(position - 2, mPlaylistState.getPosition());
    }

    @Test(timeout = 3000)
    public void playOrPausePosition() throws InterruptedException {
        final int position = mPlaylistState.getPosition() + 2;

        final CountDownLatch latch = new CountDownLatch(1);
        mTestPlaylistPlayer.tester().setPlayingAction(new Runnable() {
            @Override
            public void run() {
                latch.countDown();
            }
        });
        mTestPlaylistPlayer.playOrPause(position);
        latch.await();

        assertEquals(Player.PlaybackState.PLAYING, mPlaylistState.getPlaybackState());

        mTestPlaylistPlayer.playOrPause(position);
        assertEquals(Player.PlaybackState.PAUSED, mPlaylistState.getPlaybackState());

        mTestPlaylistPlayer.playOrPause(position);
        assertEquals(Player.PlaybackState.PLAYING, mPlaylistState.getPlaybackState());
    }

    @Test(timeout = 3000)
    public void notifyPlaylistSwapped() throws InterruptedException {
        final Playlist playlist = createPlaylist(8);

        mTestPlaylistManager.tester().setPlaylist(playlist);

        final int position = 3;
        final CountDownLatch latch = new CountDownLatch(1);
        mTestPlaylistPlayer.tester().setPlayingAction(new Runnable() {
            @Override
            public void run() {
                assertEquals(position, mPlaylistState.getPosition());
                assertEquals(playlist, mTestPlaylistPlayer.getPlaylist());
                assertEquals(Player.PlaybackState.PLAYING, mPlaylistState.getPlaybackState());

                latch.countDown();
            }
        });
        mTestPlaylistPlayer.notifyPlaylistSwapped(position, true);
        latch.await();
    }

    @Test(timeout = 3000)
    public void notifyMusicItemMoved() throws InterruptedException {
        final int from = mPlaylistState.getPosition();
        final int to = from + 2;

        List<MusicItem> musicItems = mPlaylist.getAllMusicItem();
        MusicItem buff = musicItems.get(from);
        musicItems.set(from, musicItems.get(to));
        musicItems.set(to, buff);

        mTestPlaylistManager.tester().setPlaylist(new Playlist(musicItems));

        final CountDownLatch latch = new CountDownLatch(1);
        mTestPlaylistPlayer.tester().setOnPlaylistAvailableAction(new Runnable() {
            @Override
            public void run() {
                assertEquals(mPlaylist.get(from), mTestPlaylistPlayer.getPlaylist().get(to));
                assertEquals(mPlaylist.get(to), mTestPlaylistPlayer.getPlaylist().get(from));
                assertEquals(to, mPlaylistState.getPosition());

                latch.countDown();
            }
        });
        mTestPlaylistPlayer.notifyMusicItemMoved(from, to);
        latch.await();
    }

    @Test(timeout = 3000)
    public void notifyMusicItemInserted() {
        final int position = mPlaylistState.getPosition();

        List<MusicItem> musicItems = mPlaylist.getAllMusicItem();

        MusicItem musicItem1 = createMusicItem(20);
        MusicItem musicItem2 = createMusicItem(21);

        final List<MusicItem> addItems = new ArrayList<>();
        addItems.add(musicItem1);
        addItems.add(musicItem2);

        final int count = addItems.size();
        musicItems.addAll(position, addItems);

        mTestPlaylistManager.tester().setPlaylist(new Playlist(musicItems));

        final CountDownLatch latch = new CountDownLatch(1);
        mTestPlaylistPlayer.tester().setOnPlaylistAvailableAction(new Runnable() {
            @Override
            public void run() {
                assertEquals(position + count, mPlaylistState.getPosition());

                Playlist playlist = mTestPlaylistPlayer.getPlaylist();
                for(int i = 0; i < count; i++) {
                    assertTrue(addItems.get(i).same(playlist.get(position + i)));
                }

                latch.countDown();
            }
        });

        mTestPlaylistPlayer.notifyMusicItemInserted(position, count);
    }

    @Test(timeout = 3000)
    public void notifyMusicItemRemoved() throws InterruptedException {
        final int position = 4;
        mPlaylistState.setPosition(position);

        List<MusicItem> musicItems = mPlaylist.getAllMusicItem();

        List<Integer> removePositions = new ArrayList<>();
        removePositions.add(6);
        removePositions.add(2);
        removePositions.add(0);

        musicItems.remove(6);
        musicItems.remove(2);
        musicItems.remove(0);

        mTestPlaylistManager.tester().setPlaylist(new Playlist(musicItems));

        final CountDownLatch latch = new CountDownLatch(1);
        mTestPlaylistPlayer.tester().setOnPlaylistAvailableAction(new Runnable() {
            @Override
            public void run() {
                assertEquals(position - 2, mPlaylistState.getPosition());

                latch.countDown();
            }
        });
        mTestPlaylistPlayer.notifyMusicItemRemoved(removePositions);
        latch.await();
    }
}
