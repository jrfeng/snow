package snow.player.debug;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.databinding.DataBindingUtil;
import androidx.lifecycle.ViewModelProvider;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaControllerCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import snow.player.Player;
import snow.player.PlayerClient;
import snow.player.SleepTimer;
import snow.player.debug.databinding.ActivityMainBinding;
import snow.player.lifecycle.PlayerViewModel;
import snow.player.audio.MusicItem;
import snow.player.playlist.Playlist;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";
    private TextView tvMediaSessionState;

    private PlayerClient mPlayerClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ActivityMainBinding binding = DataBindingUtil.setContentView(this, R.layout.activity_main);
        binding.setLifecycleOwner(this);

        PlayerViewModel playerViewModel = new ViewModelProvider(this).get(PlayerViewModel.class);
        binding.setViewModel(playerViewModel);

        tvMediaSessionState = binding.tvMediaSessionState;

        if (playerViewModel.isInitialized()) {
            mPlayerClient = playerViewModel.getPlayerClient();
            return;
        }

        mPlayerClient = PlayerClient.newInstance(this, MyPlayerService.class);
        playerViewModel.init(this, mPlayerClient);
        playerViewModel.setAutoDisconnect(true);

        mPlayerClient.addOnPlayingMusicItemChangeListener(this, new Player.OnPlayingMusicItemChangeListener() {
            @Override
            public void onPlayingMusicItemChanged(@Nullable MusicItem musicItem, int position, int playProgress) {
                // DEBUG
                Log.d(TAG, "onPlayingMusicItemChanged: " + musicItem);
            }
        });

        mPlayerClient.addOnSleepTimerStateChangeListener(this, new SleepTimer.OnStateChangeListener() {
            @Override
            public void onTimerStart(long time, long startTime, SleepTimer.TimeoutAction action) {
                Log.d(TAG, "onStarted {time:" + time + ", startTime:" + startTime + ", timeoutAction:" + action + "}");
                Log.d(TAG, "elapsedTime:" + mPlayerClient.getSleepTimerElapsedTime());
            }

            @Override
            public void onTimerEnd() {
                Log.d(TAG, "onEnd");
            }
        });
    }

    @SuppressLint("SetTextI18n")
    public void onClick(View view) {
        int id = view.getId();
        if (id == R.id.btnConnect) {
            mPlayerClient.connect(new PlayerClient.OnConnectCallback() {
                @Override
                public void onConnected(boolean success) {
                    if (success && mPlayerClient.getMediaController() != null) {
                        testMediaSession(mPlayerClient.getMediaController());
                    }

                    // DEBUG
                    Log.d(TAG, "OnConnectCallback: PlayingMusicItem: " + mPlayerClient.getPlayingMusicItem());
                }
            });
            return;
        }

        if (id == R.id.btnDisconnect) {
            mPlayerClient.disconnect();
            return;
        }

        if (id == R.id.btnShutdown) {
            mPlayerClient.shutdown();
            return;
        }

        if (id == R.id.btnSetPlaylist) {
            mPlayerClient.setPlaylist(createPlaylist());
        }
    }

    private Playlist createPlaylist() {
        MusicItem song1 = new MusicItem.Builder()
                .setTitle("莫失莫忘")
                .setArtist("麦振鸿")
                .setAlbum("仙剑奇侠传")
                .setDuration(199180)
                .setUri("http://music.163.com/song/media/outer/url?id=1427788848")
                .setIconUri("http://p1.music.126.net/4tTN8CnR7wG4E1cauIPCvQ==/109951163240682406.jpg")
                .build();

        MusicItem song2 = new MusicItem.Builder()
                .setTitle("偏爱")
                .setArtist("张芸京")
                .setDuration(213000)
                .setUri("http://music.163.com/song/media/outer/url?id=5238992")
                .build();

        MusicItem song3 = new MusicItem.Builder()
                .setTitle("雪见-落凡尘")
                .setDuration(289973)
                .setUri("http://music.163.com/song/media/outer/url?id=1427193969")
                .setIconUri("http://p1.music.126.net/ADQQb9gmj8j4pv_0HZ9lIA==/109951164755909058.jpg")
                .build();

        MusicItem song4 = new MusicItem.Builder()
                .setTitle("此生不换[Forbid Seek]")
                .setArtist("青鸟飞鱼")
                .setDuration(265000)
                // cross-protocol redirects
                .setUri("https://music.163.com/song/media/outer/url?id=25638340")
                .setIconUri("http://p2.music.126.net/UyDVlWWgOn8p8U8uQ_I1xQ==/7934075907687518.jpg")
                // forbid seek
                .setForbidSeek(true)
                .build();

        return new Playlist.Builder()
                .append(song1)
                .append(song2)
                .append(song3)
                .append(song4)
                .build();
    }

    private void testMediaSession(MediaControllerCompat mediaController) {
        PlaybackStateCompat playbackState = mediaController.getPlaybackState();
        tvMediaSessionState.setText(getStateString(playbackState));

        mediaController.registerCallback(new MediaControllerCompat.Callback() {
            @Override
            public void onPlaybackStateChanged(PlaybackStateCompat state) {
                String stateString = getStateString(state);

                tvMediaSessionState.setText(stateString);
                Log.d(TAG, stateString);
            }

            @Override
            public void onMetadataChanged(MediaMetadataCompat metadata) {
                Log.d(TAG, "Metadata: " + metadata.getString(MediaMetadataCompat.METADATA_KEY_TITLE));
            }
        });
    }

    private String getStateString(PlaybackStateCompat state) {
        switch (state.getState()) {
            case PlaybackStateCompat.STATE_PLAYING:
                return "STATE_PLAYING";
            case PlaybackStateCompat.STATE_PAUSED:
                return "STATE_PAUSED";
            case PlaybackStateCompat.STATE_STOPPED:
                return "STATE_STOPPED";
            case PlaybackStateCompat.STATE_ERROR:
                return "STATE_ERROR";
            case PlaybackStateCompat.STATE_FAST_FORWARDING:
                return "STATE_FAST_FORWARDING";
            case PlaybackStateCompat.STATE_REWINDING:
                return "STATE_REWINDING";
            case PlaybackStateCompat.STATE_BUFFERING:
                return "STATE_BUFFERING";
            case PlaybackStateCompat.STATE_SKIPPING_TO_NEXT:
                return "STATE_SKIPPING_TO_NEXT";
            case PlaybackStateCompat.STATE_SKIPPING_TO_PREVIOUS:
                return "STATE_SKIPPING_TO_PREVIOUS";
            case PlaybackStateCompat.STATE_SKIPPING_TO_QUEUE_ITEM:
                return "STATE_SKIPPING_TO_QUEUE_ITEM";
            case PlaybackStateCompat.STATE_CONNECTING:
                return "STATE_CONNECTING";
            case PlaybackStateCompat.STATE_NONE:
            default:
                return "STATE_NONE";
        }
    }
}

