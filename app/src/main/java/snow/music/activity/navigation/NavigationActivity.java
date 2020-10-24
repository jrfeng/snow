package snow.music.activity.navigation;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.databinding.DataBindingUtil;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;

import java.util.ArrayList;
import java.util.List;

import io.reactivex.Single;
import io.reactivex.SingleOnSubscribe;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;
import snow.music.R;
import snow.music.databinding.ActivityNavigationBinding;
import snow.music.databinding.AppBinderAdapter;
import snow.music.model.ScannerViewModel;
import snow.music.store.Music;
import snow.music.store.MusicStore;
import snow.music.util.MusicUtil;
import snow.player.PlayerClient;
import snow.player.PlayerService;
import snow.player.audio.MusicItem;
import snow.player.playlist.Playlist;

public class NavigationActivity extends AppCompatActivity {
    private static final String KEY_SCAN_LOCAL_MUSIC = "scan_local_music";
    private static final int PERMISSION_REQUEST_CODE = 1;
    private boolean mScanOnPermissionGranted;
    private boolean mRepeatedRequestStoragePermission;

    private ScannerViewModel mScannerViewModel;
    private PlayerClient mPlayerClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ActivityNavigationBinding binding = DataBindingUtil.setContentView(this, R.layout.activity_navigation);

        ViewModelProvider viewModelProvider = new ViewModelProvider(this);
        NavigationViewModel navigationViewModel = viewModelProvider.get(NavigationViewModel.class);
        initNavigationViewModel(navigationViewModel);

        mScannerViewModel = viewModelProvider.get(ScannerViewModel.class);

        initDiskPanel(binding.rvDiskPanel, navigationViewModel);

        binding.setNavViewModel(navigationViewModel);
        fixDataBindingBUG(binding, navigationViewModel);

        if (shouldScanLocalMusic()) {
            scanLocalMusic();
        }
    }

    private void fixDataBindingBUG(ActivityNavigationBinding binding, NavigationViewModel navigationViewModel) {
        // BUG: DataBinding 框架不会自动更新绑定到 getFavoriteDrawable() 与 getPlayPauseDrawable() 的属性
        navigationViewModel.getFavoriteDrawable()
                .observe(this, integer -> AppBinderAdapter.setSrcCompat(binding.btnFavorite, integer));
        navigationViewModel.getPlayPauseDrawable()
                .observe(this, integer -> AppBinderAdapter.setSrcCompat(binding.btnPlayPause, integer));
    }

    private void initDiskPanel(RecyclerView diskPanel, NavigationViewModel navigationViewModel) {
        diskPanel.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        diskPanel.setAdapter(new NavDiskPanelAdapter(navigationViewModel));
    }

    private void initNavigationViewModel(NavigationViewModel navigationViewModel) {
        if (navigationViewModel.isInitialized()) {
            mPlayerClient = navigationViewModel.getPlayerClient();
            return;
        }

        mPlayerClient = PlayerClient.newInstance(this, PlayerService.class);
        navigationViewModel.init(this, mPlayerClient);
        mPlayerClient.connect();

        navigationViewModel.setAutoDisconnect(true);
    }

    private boolean shouldScanLocalMusic() {
        SharedPreferences preferences = getPreferences(MODE_PRIVATE);
        return preferences.getBoolean(KEY_SCAN_LOCAL_MUSIC, true);
    }

    private void scanLocalMusic() {
        if (hasStoragePermission()) {
            scanLocalMusicAsync();
            return;
        }

        mScanOnPermissionGranted = true;
        requestStoragePermission();
    }

    private boolean hasStoragePermission() {
        int result = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE);
        return result == PackageManager.PERMISSION_GRANTED;
    }

    private void requestStoragePermission() {
        if (hasStoragePermission()) {
            return;
        }

        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, PERMISSION_REQUEST_CODE);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode != PERMISSION_REQUEST_CODE) {
            return;
        }

        if (grantResults.length <= 0 || grantResults[0] == PackageManager.PERMISSION_DENIED) {
            showRequestPermissionRationale();
            return;
        }

        if (mScanOnPermissionGranted) {
            scanLocalMusicAsync();
        }
    }

    private void showRequestPermissionRationale() {
        if (!ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.READ_EXTERNAL_STORAGE)) {
            return;
        }

        if (mRepeatedRequestStoragePermission) {
            return;
        }

        mRepeatedRequestStoragePermission = true;
        requestStoragePermission();
    }

    private void scanLocalMusicAsync() {
        getPreferences(MODE_PRIVATE)
                .edit()
                .putBoolean(KEY_SCAN_LOCAL_MUSIC, false)
                .apply();

        mScannerViewModel.scan(30_000, musicList -> {
            if (musicList.isEmpty()) {
                return;
            }

            saveToMusicStore(musicList);
        });
    }

    @SuppressLint("CheckResult")
    private void saveToMusicStore(@NonNull List<Music> musicList) {
        Single.create((SingleOnSubscribe<Boolean>) emitter -> {
            if (emitter.isDisposed()) {
                return;
            }

            MusicStore.getInstance().putAllMusic(musicList);
            emitter.onSuccess(true);
        }).subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(aBoolean -> mPlayerClient.setPlaylist(createPlaylist(musicList)));
    }

    private Playlist createPlaylist(List<Music> musicList) {
        List<MusicItem> itemList = new ArrayList<>(musicList.size());

        for (Music music : musicList) {
            itemList.add(MusicUtil.asMusicItem(music));
        }

        return new Playlist.Builder()
                .appendAll(itemList)
                .build();
    }
}