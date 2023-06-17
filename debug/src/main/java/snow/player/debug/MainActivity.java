package snow.player.debug;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.databinding.DataBindingUtil;
import androidx.lifecycle.ViewModelProvider;

import android.Manifest;
import android.annotation.SuppressLint;
import android.os.Bundle;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaControllerCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.permissionx.guolindev.PermissionX;
import com.permissionx.guolindev.callback.RequestCallback;

import java.util.List;

import snow.player.Player;
import snow.player.PlayerClient;
import snow.player.SleepTimer;
import snow.player.debug.databinding.ActivityMainBinding;
import snow.player.lifecycle.PlayerViewModel;
import snow.player.audio.MusicItem;
import snow.player.playlist.Playlist;
import snow.player.ui.equalizer.EqualizerActivity;
import snow.player.util.AudioScanner;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";
    private TextView tvMediaSessionState;

    private PlayerClient mPlayerClient;

    private AudioScanner<AudioScanner.AudioItem> mAudioScanner;
    private boolean mStartScannerOnGranted;

    private ProgressBar mScannerProgress;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ActivityMainBinding binding = DataBindingUtil.setContentView(this, R.layout.activity_main);
        binding.setLifecycleOwner(this);

        PlayerViewModel playerViewModel = new ViewModelProvider(this).get(PlayerViewModel.class);
        binding.setViewModel(playerViewModel);

        tvMediaSessionState = binding.tvMediaSessionState;

        mAudioScanner = new AudioScanner<>(this, new AudioScanner.AudioItemConverter());
        mScannerProgress = findViewById(R.id.scanner_progress);

        binding.btnStartAudioEffect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                EqualizerActivity.start(MainActivity.this, MyPlayerService.class);
            }
        });

        binding.sbVolume.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                float volume = seekBar.getProgress() / 100F;
                if (volume == mPlayerClient.getVolume()) {
                    return;
                }

                mPlayerClient.setVolume(volume);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                // ignore
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                // ignore
            }
        });

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

    private void requestPermission() {
        PermissionX.init(this)
                .permissions(Manifest.permission.READ_EXTERNAL_STORAGE)
                .request(new RequestCallback() {
                    @Override
                    public void onResult(boolean allGranted, List<String> grantedList, List<String> deniedList) {
                        if (deniedList.contains(Manifest.permission.READ_EXTERNAL_STORAGE)) {
                            Toast.makeText(MainActivity.this,
                                    "Need permission: READ_EXTERNAL_STORAGE",
                                    Toast.LENGTH_SHORT).show();
                            return;
                        }

                        if (mStartScannerOnGranted) {
                            startAudioScanner();
                        }
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
            return;
        }

        if (id == R.id.btn_start_scanner) {
            if (!PermissionX.isGranted(this, Manifest.permission.READ_EXTERNAL_STORAGE)) {
                mStartScannerOnGranted = true;
                requestPermission();
                return;
            }

            startAudioScanner();
            return;
        }

        if (id == R.id.btn_cancel_scanner) {
            mAudioScanner.cancel();
        }
    }

    private void startAudioScanner() {
        mAudioScanner.scan(new AudioScanner.OnProgressUpdateListener<AudioScanner.AudioItem>() {
            @Override
            public void onStart() {
                mScannerProgress.setProgress(0);
            }

            @Override
            public void onProgressUpdate(int progress) {
                mScannerProgress.setProgress(progress);
            }

            @Override
            public void onEnd(@NonNull List<AudioScanner.AudioItem> audioList, boolean cancelled) {
                // DEBUG
                Log.d("DEBUG", "***************************************************");
                Log.d("DEBUG", "cancelled: " + cancelled);
                Log.d("DEBUG", "size     : " + audioList.size());
                for (AudioScanner.AudioItem audioItem : audioList) {
                    Log.d("DEBUG", "item     : " + audioItem.toString());
                }
                Log.d("DEBUG", "***************************************************");
            }
        });
    }

    private Playlist createPlaylist() {
        MusicItem song1 = new MusicItem.Builder()
                .setTitle("太阳照常升起")
                .setArtist("久石让")
                .setAlbum("太阳照常升起 电影原声大碟")
                .setDuration(224013)
                .setUri("http://music.163.com/song/media/outer/url?id=441722")
                .setIconUri("http://p2.music.126.net/drqGdK7zgW7B7IFl4lWpoQ==/109951163369835547.jpg")
                .build();

        MusicItem song2 = new MusicItem.Builder()
                .setTitle("钢铁洪流进行曲[auto duration]")
                .setArtist("李旭昊")
                .setAlbum("国庆70周年阅兵BGM")
                .autoDuration()
                .setUri("http://music.163.com/song/media/outer/url?id=1394369908")
                .setIconUri("http://p2.music.126.net/KnC_YJjnRTNvCF82_2leCg==/109951164930615683.jpg")
                .build();

        MusicItem song3 = new MusicItem.Builder()
                .setTitle("国际歌-钢琴")
                .setArtist("曹伟健")
                .setAlbum("音迹")
                .setDuration(141369)
                .setUri("http://music.163.com/song/media/outer/url?id=1857796913")
                .build();

        MusicItem song4 = new MusicItem.Builder()
                .setTitle("我爱你中国[Forbid Seek]")
                .setDuration(136000)
                // cross-protocol redirects
                .setUri("https://music.163.com/song/media/outer/url?id=174451")
                .setIconUri("http://p2.music.126.net/x6pVwc6ysKZ9S01jYlYiAw==/97856534887060.jpg")
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

