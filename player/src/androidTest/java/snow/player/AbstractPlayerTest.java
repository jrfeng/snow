package snow.player;

import android.content.Context;

import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.concurrent.CountDownLatch;

import snow.player.media.MusicItem;
import snow.player.test.TestMusicPlayer;
import snow.player.test.TestPlayer;

import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
public class AbstractPlayerTest {
    private PlayerConfig mPlayerConfig;
    private PlayerState mPlayerState;
    private TestMusicPlayer mTestMusicPlayer;
    private TestPlayer mTestPlayer;

    private final int mProgress = 1000;

    private int mPlayerConfigId = 0;

    @Before
    public void initFields() {
        MusicItem musicItem = new MusicItem();

        musicItem.setTitle("Title");
        musicItem.setArtist("Artist");
        int duration = 300_000;
        musicItem.setDuration(duration);
        musicItem.setUri("http://www.test.com/test.mp3");

        mPlayerConfig = new PlayerConfig(InstrumentationRegistry.getInstrumentation().getContext(),
                "test_player_config_" + mPlayerConfigId);
        mPlayerConfigId++;
        mPlayerState = new PlayerState();

        mPlayerState.setPlayProgress(mProgress);
        mPlayerState.setPlayProgressUpdateTime(System.currentTimeMillis());
        mPlayerState.setMusicItem(musicItem);

        mTestMusicPlayer = new TestMusicPlayer();
        mTestPlayer = createTestPlayer(mPlayerConfig, mPlayerState, mTestMusicPlayer);
    }

    private Context getContext() {
        return InstrumentationRegistry.getInstrumentation().getTargetContext();
    }

    protected TestPlayer createTestPlayer(PlayerConfig playerConfig, PlayerState playerState, TestMusicPlayer musicPlayer) {
//        TestPlayer player = new TestPlayer(getContext(), playerConfig, playerState);
//        player.tester().setTestMusicPlayer(musicPlayer);
//
//        MusicItem musicItem = playerState.getMusicItem();
//        if (musicItem != null) {
//            musicPlayer.tester().setDuration(musicItem.getDuration());
//        }
//
//        return player;
        // TODO
        return null;
    }

    @Test(timeout = 3_000)
    public void prepareMusicPlayer() throws InterruptedException {
        final boolean looping = true;

        mPlayerConfig.setAudioEffectEnabled(true);
        mTestPlayer.tester().setLooping(looping);

        final CountDownLatch latch = new CountDownLatch(1);
        mTestPlayer.prepareMusicPlayer(new Runnable() {
            @Override
            public void run() {
                assertEquals(looping, mTestMusicPlayer.isLooping());
                assertEquals(mTestMusicPlayer.getAudioSessionId(), mTestPlayer.tester().getEffectAudioSessionId());
                assertEquals(Player.PlaybackState.PREPARED, mPlayerState.getPlaybackState());
                assertEquals(mProgress, mTestMusicPlayer.getProgress());

                latch.countDown();
            }
        });

        latch.await();
    }

    @Test(timeout = 3000)
    public void play() throws InterruptedException {
        final CountDownLatch latch = new CountDownLatch(1);
        mTestPlayer.prepareMusicPlayer(new Runnable() {
            @Override
            public void run() {
                mTestPlayer.play();
                assertEquals(Player.PlaybackState.PLAYING, mPlayerState.getPlaybackState());

                latch.countDown();
            }
        });

        latch.await();
    }

    @Test(timeout = 3000)
    public void pause() throws InterruptedException {
        mTestPlayer.pause();

        assertEquals(Player.PlaybackState.PAUSED, mPlayerState.getPlaybackState());

        final CountDownLatch latch = new CountDownLatch(1);
        mTestPlayer.tester().doOnPlaying(new Runnable() {
            @Override
            public void run() {
                mTestPlayer.pause();
                assertEquals(Player.PlaybackState.PAUSED, mPlayerState.getPlaybackState());

                latch.countDown();
            }
        });

        mTestPlayer.play();
        latch.await();
    }

    @Test(timeout = 3000)
    public void stop() throws InterruptedException {
        mTestPlayer.stop();

        assertEquals(Player.PlaybackState.STOPPED, mPlayerState.getPlaybackState());

        final CountDownLatch latch = new CountDownLatch(1);
        mTestPlayer.tester().doOnPlaying(new Runnable() {
            @Override
            public void run() {
                mTestPlayer.stop();
                assertEquals(Player.PlaybackState.STOPPED, mPlayerState.getPlaybackState());
                assertEquals(0, mPlayerState.getPlayProgress());

                latch.countDown();
            }
        });

        mTestPlayer.play();
        latch.await();
    }

    @Test(timeout = 3000)
    public void errorTest() throws InterruptedException {
        final CountDownLatch latch = new CountDownLatch(1);

        mTestPlayer.tester().doOnPlaying(new Runnable() {
            @Override
            public void run() {
                latch.countDown();
            }
        });

        mTestPlayer.play();
        latch.await();

        mTestPlayer.tester().doOnError(new Runnable() {
            @Override
            public void run() {
                assertFalse(mTestPlayer.isPreparing());
                assertFalse(mTestPlayer.isPrepared());
                assertEquals(Player.Error.PLAYER_ERROR, mPlayerState.getErrorCode());
                assertEquals(Player.PlaybackState.ERROR, mPlayerState.getPlaybackState());
            }
        });

        mTestMusicPlayer.tester().setError(true, 1);
    }

    @Test(timeout = 3000)
    public void playOrPause() throws InterruptedException {
        final CountDownLatch latch = new CountDownLatch(1);
        mTestPlayer.tester().doOnPlaying(new Runnable() {
            @Override
            public void run() {
                assertEquals(Player.PlaybackState.PLAYING, mPlayerState.getPlaybackState());
                mTestPlayer.playOrPause();
                assertEquals(Player.PlaybackState.PAUSED, mPlayerState.getPlaybackState());

                latch.countDown();
            }
        });

        mTestPlayer.playOrPause();
        latch.await();
    }

    @Test(timeout = 3000)
    public void seekTo() throws InterruptedException {
        final int progress = mTestMusicPlayer.getProgress();
        final int seekToProgress = 80_000;

        mTestPlayer.seekTo(seekToProgress);
        assertEquals(progress, mTestMusicPlayer.getProgress());

        final CountDownLatch latch = new CountDownLatch(1);
        mTestPlayer.prepareMusicPlayer(new Runnable() {
            @Override
            public void run() {
                mTestPlayer.seekTo(seekToProgress);
                assertEquals(seekToProgress, mTestMusicPlayer.getProgress());

                latch.countDown();
            }
        });

        latch.await();
    }

    @Test(timeout = 3000)
    public void notifyPlayingMusicItemChanged() throws InterruptedException {
        final MusicItem musicItem = new MusicItem();

        musicItem.setTitle("Title2");
        musicItem.setArtist("Artist2");
        musicItem.setDuration(480_000);
        musicItem.setUri("http://www.test.com/test2.mp3");

        final CountDownLatch latch = new CountDownLatch(1);
        mTestPlayer.tester().doOnPlaying(new Runnable() {
            @Override
            public void run() {
                assertEquals(musicItem,mPlayerState.getMusicItem());
                assertEquals(Player.PlaybackState.PLAYING, mPlayerState.getPlaybackState());
                latch.countDown();
            }
        });

        mTestPlayer.notifyPlayingMusicItemChanged(musicItem, true);
        latch.await();
    }
}
