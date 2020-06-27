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
    private PlayerState mPlayerState;
    private TestMusicPlayer mTestMusicPlayer;
    private TestPlayer mTestPlayer;

    private final int mProgress = 1000;

    @Before
    public void initFields() {
        MusicItem musicItem = new MusicItem();

        musicItem.setTitle("Title");
        musicItem.setArtist("Artist");
        int duration = 300_000;
        musicItem.setDuration(duration);
        musicItem.setUri("http://www.test.com/test.mp3");

        mPlayerState = new PlayerState();

        mPlayerState.setPlayProgress(mProgress);
        mPlayerState.setPlayProgressUpdateTime(System.currentTimeMillis());
        mPlayerState.setMusicItem(musicItem);

        mTestMusicPlayer = new TestMusicPlayer();
        mTestPlayer = createTestPlayer(mPlayerState, mTestMusicPlayer);
    }

    private Context getContext() {
        return InstrumentationRegistry.getInstrumentation().getTargetContext();
    }

    protected TestPlayer createTestPlayer(PlayerState playerState, TestMusicPlayer musicPlayer) {
        TestPlayer player = new TestPlayer(getContext(), playerState);
        player.tester().setTestMusicPlayer(musicPlayer);

        MusicItem musicItem = playerState.getMusicItem();
        if (musicItem != null) {
            musicPlayer.tester().setDuration(musicItem.getDuration());
        }

        return player;
    }

    @Test(timeout = 3_000)
    public void prepareMusicPlayer() throws InterruptedException {
        final boolean looping = true;

        mPlayerState.setAudioEffectEnabled(true);
        mTestPlayer.tester().setLooping(looping);

        final CountDownLatch latch = new CountDownLatch(1);
        mTestPlayer.prepareMusicPlayer(new Runnable() {
            @Override
            public void run() {
                assertEquals(looping, mTestMusicPlayer.isLooping());
                assertEquals(mTestMusicPlayer.getAudioSessionId(), mTestPlayer.tester().getEffectAudioSessionId());
                assertEquals(Player.PlaybackState.PREPARED, mPlayerState.getPlaybackState());
                assertEquals(mProgress, mTestMusicPlayer.getCurrentPosition());

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
        final int progress = mTestMusicPlayer.getCurrentPosition();
        final int seekToProgress = 80_000;

        mTestPlayer.seekTo(seekToProgress);
        assertEquals(progress, mTestMusicPlayer.getCurrentPosition());

        final CountDownLatch latch = new CountDownLatch(1);
        mTestPlayer.prepareMusicPlayer(new Runnable() {
            @Override
            public void run() {
                mTestPlayer.seekTo(seekToProgress);
                assertEquals(seekToProgress, mTestMusicPlayer.getCurrentPosition());

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
                assertTrue(musicItem.same(mPlayerState.getMusicItem()));
                assertEquals(Player.PlaybackState.PLAYING, mPlayerState.getPlaybackState());
                latch.countDown();
            }
        });

        mTestPlayer.notifyPlayingMusicItemChanged(musicItem, true);
        latch.await();
    }

    @Test
    public void setSoundQuality() {
        final Player.SoundQuality soundQuality = Player.SoundQuality.SUPER;

        mTestPlayer.setSoundQuality(soundQuality);
        assertEquals(soundQuality, mPlayerState.getSoundQuality());
    }

    @Test
    public void setAudioEffectEnabled() {
        final boolean enabled = true;

        mTestPlayer.setAudioEffectEnabled(enabled);
        assertEquals(enabled, mPlayerState.isAudioEffectEnabled());
    }

    @Test
    public void setOnlyWifiNetwork() {
        final boolean onlyWifiNetwork = false;

        mTestPlayer.setOnlyWifiNetwork(onlyWifiNetwork);
        assertEquals(onlyWifiNetwork, mPlayerState.isOnlyWifiNetwork());
    }

    @Test
    public void setIgnoreLossAudioFocus() {
        final boolean ignoreLossAudioFocus = true;

        mTestPlayer.setIgnoreLossAudioFocus(ignoreLossAudioFocus);
        assertEquals(ignoreLossAudioFocus, mPlayerState.isIgnoreLossAudioFocus());
    }
}
