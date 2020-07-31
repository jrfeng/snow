package snow.demo;

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
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.TextView;

import java.util.ArrayList;

import snow.demo.databinding.ActivityMainBinding;
import snow.player.PlayerClient;
import snow.player.PlayerService;
import snow.player.lifecycle.PlayerViewModel;
import snow.player.lifecycle.ProgressClock;
import snow.player.media.MusicItem;
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

        mPlayerClient = PlayerClient.newInstance(this, PlayerService.class);
        playerViewModel.init(mPlayerClient, "Unknown", "Unknown");
        playerViewModel.setDisconnectOnCleared(true);
    }

    @SuppressLint("SetTextI18n")
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.btnConnect:
                mPlayerClient.connect(new PlayerClient.OnConnectCallback() {
                    @Override
                    public void onConnected(boolean success) {
                        tvMessage.setText("connect: " + success);
                        if (success) {
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
        }
    }

    private Playlist createPlaylist() {
        ArrayList<MusicItem> items = new ArrayList<>();

        MusicItem song1 = new MusicItem();
        song1.setTitle("Song 1");
        song1.setArtist("artist 1");
        song1.setUri("https://music.163.com/song/media/outer/url?id=33894312");
        song1.setIconUri("http://p2.music.126.net/ZDUo6vF_5ykD6E_08HE1kw==/3385396303317256.jpg");
        song1.setDuration(267232);

        MusicItem song2 = new MusicItem();
        song2.setTitle("Song 2");
        song2.setArtist("artist 2");
        song2.setUri("https://music.163.com/song/media/outer/url?id=1420218751");
        song2.setDuration(218027);

        MusicItem song3 = new MusicItem();
        song3.setTitle("Song 3");
        song3.setArtist("artist 3");
        song3.setUri("https://music.163.com/song/media/outer/url?id=1452046251");
        song3.setIconUri("http://p1.music.126.net/o3G7lWrGBQAvSRt3UuApTw==/2002210674180201.jpg");
        song3.setDuration(341787);

        items.add(song1);
        items.add(song2);
        items.add(song3);

        return new Playlist(items);
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
