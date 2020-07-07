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
import snow.player.PlayerConfig;
import snow.player.media.MusicItem;
import snow.player.test.TestPlaylistManager;
import snow.player.test.TestPlaylistPlayer;

import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
public class AbstractPlaylistPlayerTest {
    private Playlist mPlaylist;
    private TestPlaylistManager mTestPlaylistManager;
    private PlaylistState mPlaylistState;
    private TestPlaylistPlayer mTestPlaylistPlayer;

    @Before
    public void initFields() {
        mPlaylist = createPlaylist(10);
        mTestPlaylistManager = new TestPlaylistManager(getContext(), mPlaylist);
        mPlaylistState = createPlaylistState();
        mTestPlaylistPlayer = new TestPlaylistPlayer(getContext(),
                new PlayerConfig(),
                mPlaylistState,
                mTestPlaylistManager,
                mPlaylist);
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
    public void onNewPlaylist() throws InterruptedException {
        final Playlist playlist = createPlaylist(28);

        mTestPlaylistManager.tester().setPlaylist(playlist);

        final CountDownLatch latch = new CountDownLatch(1);
        mTestPlaylistPlayer.tester().setOnPlaylistAvailableAction(new Runnable() {
            @Override
            public void run() {
                assertEquals(playlist, mTestPlaylistPlayer.getPlaylist());
                latch.countDown();
            }
        });
        mTestPlaylistPlayer.onNewPlaylist(0, true);
        latch.await();
    }

    @Test(timeout = 3000)
    public void onMusicItemMoved() throws InterruptedException {
        final int position = 5;

        // position not in region
        mPlaylistState.setPosition(position);
        final int from1 = position - 4;
        final int to1 = position - 2;
        final CountDownLatch latch1 = new CountDownLatch(1);
        mTestPlaylistPlayer.tester().setOnPlaylistAvailableAction(new Runnable() {
            @Override
            public void run() {
                assertEquals(position, mPlaylistState.getPosition());
                latch1.countDown();
            }
        });
        mTestPlaylistPlayer.onMusicItemMoved(from1, to1);
        latch1.await();

        // from < playingPosition
        mPlaylistState.setPosition(position);
        final int from2 = position - 1;
        final int to2 = position + 2;
        final CountDownLatch latch2 = new CountDownLatch(1);
        mTestPlaylistPlayer.tester().setOnPlaylistAvailableAction(new Runnable() {
            @Override
            public void run() {
                assertEquals(position - 1, mPlaylistState.getPosition());
                latch2.countDown();
            }
        });
        mTestPlaylistPlayer.onMusicItemMoved(from2, to2);
        latch2.await();

        // from == playingPosition
        mPlaylistState.setPosition(position);
        final int to3 = position + 2;
        final CountDownLatch latch3 = new CountDownLatch(1);
        mTestPlaylistPlayer.tester().setOnPlaylistAvailableAction(new Runnable() {
            @Override
            public void run() {
                assertEquals(to3, mPlaylistState.getPosition());
                latch3.countDown();
            }
        });
        mTestPlaylistPlayer.onMusicItemMoved(position, to3);
        latch3.await();

        // from > playingPosition
        mPlaylistState.setPosition(position);
        final int from4 = position + 1;
        final int to4 = position - 2;
        final CountDownLatch latch4 = new CountDownLatch(1);
        mTestPlaylistPlayer.tester().setOnPlaylistAvailableAction(new Runnable() {
            @Override
            public void run() {
                assertEquals(position + 1, mPlaylistState.getPosition());
                latch4.countDown();
            }
        });
        mTestPlaylistPlayer.onMusicItemMoved(from4, to4);
        latch4.await();
    }

    @Test(timeout = 3000)
    public void onMusicItemInserted() throws InterruptedException {
        final int position = 5;

        // insertPosition <= playingPosition
        mPlaylistState.setPosition(position);
        final int insertPosition1 = position - 1;
        final CountDownLatch latch1 = new CountDownLatch(1);
        mTestPlaylistPlayer.tester().setOnPlaylistAvailableAction(new Runnable() {
            @Override
            public void run() {
                assertEquals(position + 1, mPlaylistState.getPosition());
                latch1.countDown();
            }
        });
        mTestPlaylistPlayer.onMusicItemInserted(insertPosition1, new MusicItem());
        latch1.await();

        // insertPosition > playingPosition
        mPlaylistState.setPosition(position);
        final int insertPosition2 = position + 1;
        final CountDownLatch latch2 = new CountDownLatch(1);
        mTestPlaylistPlayer.tester().setOnPlaylistAvailableAction(new Runnable() {
            @Override
            public void run() {
                assertEquals(position, mPlaylistState.getPosition());
                latch2.countDown();
            }
        });
        mTestPlaylistPlayer.onMusicItemInserted(insertPosition2, new MusicItem());
        latch2.await();
    }

    @Test(timeout = 3000)
    public void onMusicItemRemoved() throws InterruptedException {
        final int position = 5;

        // removePosition < position
        mPlaylistState.setPosition(position);
        final int removePosition1 = position - 1;
        final MusicItem musicItem1 = mPlaylist.get(removePosition1);
        final CountDownLatch latch1 = new CountDownLatch(1);
        mTestPlaylistPlayer.tester().setOnPlaylistAvailableAction(new Runnable() {
            @Override
            public void run() {
                assertEquals(position - 1, mPlaylistState.getPosition());
                latch1.countDown();
            }
        });
        mTestPlaylistPlayer.onMusicItemRemoved(musicItem1);
        latch1.await();

        // removePosition == position
        mPlaylistState.setPosition(position);
        final MusicItem musicItem2 = mPlaylist.get(position);
        final CountDownLatch latch2 = new CountDownLatch(1);
        final int nextPosition = mTestPlaylistPlayer.getNextPosition(position - 1);
        mTestPlaylistPlayer.tester().setOnPlaylistAvailableAction(new Runnable() {
            @Override
            public void run() {
                assertEquals(nextPosition, mPlaylistState.getPosition());
                latch2.countDown();
            }
        });
        mTestPlaylistPlayer.onMusicItemRemoved(musicItem2);
        latch2.await();

        // removePosition > position
        mPlaylistState.setPosition(position);
        final int removePosition3 = position + 1;
        final MusicItem musicItem3 = mPlaylist.get(removePosition3);
        final CountDownLatch latch3 = new CountDownLatch(1);
        mTestPlaylistPlayer.tester().setOnPlaylistAvailableAction(new Runnable() {
            @Override
            public void run() {
                assertEquals(position, mPlaylistState.getPosition());
                latch3.countDown();
            }
        });
        mTestPlaylistPlayer.onMusicItemRemoved(musicItem3);
        latch3.await();
    }
}
