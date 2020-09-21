package snow.player.debug;

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

import snow.player.PlayerClient;
import snow.player.SleepTimer;
import snow.player.debug.databinding.ActivityMainBinding;
import snow.player.lifecycle.PlayerViewModel;
import snow.player.audio.MusicItem;
import snow.player.playlist.Playlist;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";

    private TextView tvMessage;
    private PlayerClient mPlayerClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ActivityMainBinding binding = DataBindingUtil.setContentView(this, R.layout.activity_main);
        binding.setLifecycleOwner(this);
        tvMessage = binding.tvMessage;

        PlayerViewModel playerViewModel = new ViewModelProvider(this).get(PlayerViewModel.class);
        binding.setViewModel(playerViewModel);

        if (playerViewModel.isInitialized()) {
            mPlayerClient = playerViewModel.getPlayerClient();
            return;
        }

        mPlayerClient = PlayerClient.newInstance(this, MyPlayerService.class);
        playerViewModel.init(this, mPlayerClient);
        playerViewModel.setAutoDisconnect(true);

        mPlayerClient.addOnSleepTimerStateChangeListener(this, new SleepTimer.OnStateChangeListener() {
            @Override
            public void onStarted(long time, long startTime) {
                Log.d("App", "onStarted {time:" + time + ", startTime:" + startTime + "}");
                Log.d("App", "currentTimeMillis:" + System.currentTimeMillis());
            }

            @Override
            public void onCancelled() {
                Log.d("App", "onCancelled");
            }
        });
    }

    @SuppressLint("SetTextI18n")
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.btnConnect:
                mPlayerClient.connect(new PlayerClient.OnConnectCallback() {
                    @Override
                    public void onConnected(boolean success) {
                        tvMessage.setText("connect: " + success);
                        if (success && mPlayerClient.getMediaController() != null) {
                            testMediaSession(mPlayerClient.getMediaController());
                        }
                    }
                });
                break;
            case R.id.btnDisconnect:
                mPlayerClient.disconnect();
                tvMessage.setText("disconnect");
                break;
            case R.id.btnSetPlaylist:
                mPlayerClient.setPlaylist(createPlaylist());
                break;
            case R.id.btnStartSleepTimer:
                mPlayerClient.startSleepTimer(10_000);
                break;
        }
    }

    private Playlist createPlaylist() {
        MusicItem song1 = new MusicItem.Builder()
                .setTitle("逍遥叹")
                .setArtist("胡歌")
                .setAlbum("仙剑奇侠传")
                .setDuration(313520)
                .setUri("http://music.163.com/song/media/outer/url?id=4875306")
                .setIconUri("http://p1.music.126.net/4tTN8CnR7wG4E1cauIPCvQ==/109951163240682406.jpg")
                .build();

        MusicItem song2 = new MusicItem.Builder()
                .setTitle("终于明白")
                .setArtist("动力火车")
                .setDuration(267786)
                .setUri("http://music.163.com/song/media/outer/url?id=4875305")
                .build();

        MusicItem song3 = new MusicItem.Builder()
                .setTitle("千年泪")
                .setDuration(260946)
                .setUri("http://music.163.com/song/media/outer/url?id=150371")
                .setIconUri("http://p2.music.126.net/0543F-ln2Apdiopez_jbsA==/109951163244853571.jpg")
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
        mediaController.registerCallback(new MediaControllerCompat.Callback() {
            @Override
            public void onPlaybackStateChanged(PlaybackStateCompat state) {
                switch (state.getState()) {
                    case PlaybackStateCompat.STATE_NONE:
                        Log.d(TAG, "STATE_NONE");
                        break;
                    case PlaybackStateCompat.STATE_PLAYING:
                        Log.d(TAG, "STATE_PLAYING");
                        break;
                    case PlaybackStateCompat.STATE_PAUSED:
                        Log.d(TAG, "STATE_PAUSED");
                        break;
                    case PlaybackStateCompat.STATE_STOPPED:
                        Log.d(TAG, "STATE_STOPPED");
                        break;
                    case PlaybackStateCompat.STATE_ERROR:
                        Log.d(TAG, "STATE_ERROR: " + state.getErrorMessage());
                        break;
                    case PlaybackStateCompat.STATE_FAST_FORWARDING:
                        Log.d(TAG, "STATE_FAST_FORWARDING");
                        break;
                    case PlaybackStateCompat.STATE_REWINDING:
                        Log.d(TAG, "STATE_REWINDING");
                        break;
                    case PlaybackStateCompat.STATE_BUFFERING:
                        Log.d(TAG, "STATE_BUFFERING");
                        break;
                    case PlaybackStateCompat.STATE_SKIPPING_TO_NEXT:
                        Log.d(TAG, "STATE_SKIPPING_TO_NEXT");
                        break;
                    case PlaybackStateCompat.STATE_SKIPPING_TO_PREVIOUS:
                        Log.d(TAG, "STATE_SKIPPING_TO_PREVIOUS");
                        break;
                    case PlaybackStateCompat.STATE_SKIPPING_TO_QUEUE_ITEM:
                        Log.d(TAG, "STATE_SKIPPING_TO_QUEUE_ITEM");
                        break;
                    case PlaybackStateCompat.STATE_CONNECTING:
                        Log.d(TAG, "STATE_CONNECTING");
                        break;
                }
            }

            @Override
            public void onMetadataChanged(MediaMetadataCompat metadata) {
                // DEBUG
                Log.d(TAG, "Metadata: " + metadata.getString(MediaMetadataCompat.METADATA_KEY_TITLE));
            }
        });
    }
}

