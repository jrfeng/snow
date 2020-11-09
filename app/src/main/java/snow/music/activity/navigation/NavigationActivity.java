package snow.music.activity.navigation;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.databinding.DataBindingUtil;
import androidx.lifecycle.ViewModelProvider;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.CircleCrop;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;

import java.util.ArrayList;
import java.util.List;

import io.reactivex.Single;
import io.reactivex.SingleOnSubscribe;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import snow.music.R;
import snow.music.databinding.ActivityNavigationBinding;
import snow.music.service.AppPlayerService;
import snow.music.store.MusicList;
import snow.music.util.PlayerUtil;
import snow.music.viewmodel.ScannerViewModel;
import snow.music.store.Music;
import snow.music.store.MusicStore;
import snow.music.util.MusicUtil;
import snow.player.PlayerClient;
import snow.player.audio.MusicItem;
import snow.player.lifecycle.PlayerViewModel;
import snow.player.playlist.Playlist;

public class NavigationActivity extends AppCompatActivity {
    private static final String KEY_SCAN_LOCAL_MUSIC = "scan_local_music";
    private static final int PERMISSION_REQUEST_CODE = 1;
    private boolean mScanOnPermissionGranted;

    private ActivityNavigationBinding mBinding;
    private ScannerViewModel mScannerViewModel;
    private PlayerViewModel mPlayerViewModel;
    private NavigationViewModel mNavigationViewModel;

    private DiskAnimManager mDiskAnimManager;
    private Disposable mIconLoadDisposable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mBinding = DataBindingUtil.setContentView(this, R.layout.activity_navigation);

        initAllViewModel();

        mBinding.setNavViewModel(mNavigationViewModel);
        mBinding.setLifecycleOwner(this);

        mDiskAnimManager = new DiskAnimManager(mBinding.ivDisk, this, mPlayerViewModel);
        observerPlayingMusicItem();

        if (shouldScanLocalMusic()) {
            scanLocalMusic();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (!mPlayerViewModel.getPlayerClient().isConnected()) {
            mPlayerViewModel.getPlayerClient().connect();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (isFinishing()) {
            cancelLoadMusicIcon();
        }
    }

    private void initAllViewModel() {
        ViewModelProvider viewModelProvider = new ViewModelProvider(this);

        mPlayerViewModel = viewModelProvider.get(PlayerViewModel.class);
        mNavigationViewModel = viewModelProvider.get(NavigationViewModel.class);
        mScannerViewModel = viewModelProvider.get(ScannerViewModel.class);

        PlayerUtil.initPlayerViewModel(this, mPlayerViewModel, AppPlayerService.class);
        initNavigationViewModel();
    }

    private void initNavigationViewModel() {
        if (mNavigationViewModel.isInitialized()) {
            return;
        }

        mNavigationViewModel.init(mPlayerViewModel);
    }

    private void observerPlayingMusicItem() {
        mPlayerViewModel.getPlayingMusicItem()
                .observe(this, musicItem -> {
                    mDiskAnimManager.reset();

                    if (musicItem == null) {
                        mBinding.ivDisk.setImageResource(0);
                        return;
                    }

                    loadMusicIcon(musicItem.getUri());
                });
    }

    private void loadMusicIcon(String musicUri) {
        cancelLoadMusicIcon();
        mIconLoadDisposable = MusicUtil.getEmbeddedPicture(this, musicUri)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(bytes -> Glide.with(NavigationActivity.this)
                        .load(bytes)
                        .transform(new CircleCrop())
                        .transition(DrawableTransitionOptions.withCrossFade(200))
                        .into(mBinding.ivDisk));
    }

    private void cancelLoadMusicIcon() {
        if (mIconLoadDisposable != null && !mIconLoadDisposable.isDisposed()) {
            mIconLoadDisposable.dispose();
        }
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
            return;
        }

        if (mScanOnPermissionGranted) {
            scanLocalMusicAsync();
        }
    }

    private void scanLocalMusicAsync() {
        getPreferences(MODE_PRIVATE)
                .edit()
                .putBoolean(KEY_SCAN_LOCAL_MUSIC, false)
                .apply();

        mScannerViewModel.scan(30_000, new ScanCompleteListener(mPlayerViewModel.getPlayerClient()));
    }

    private static class ScanCompleteListener implements ScannerViewModel.OnScanCompleteListener {
        private final PlayerClient mPlayerClient;

        ScanCompleteListener(PlayerClient playerClient) {
            mPlayerClient = playerClient;
        }

        @Override
        public void onScanComplete(@NonNull List<Music> musicList) {
            if (musicList.isEmpty()) {
                return;
            }

            saveToMusicStore(musicList);
        }

        @SuppressWarnings("ResultOfMethodCallIgnored")
        @SuppressLint("CheckResult")
        private void saveToMusicStore(@NonNull List<Music> musicList) {
            Single.create((SingleOnSubscribe<Boolean>) emitter -> {
                if (emitter.isDisposed()) {
                    return;
                }

                MusicStore musicStore = MusicStore.getInstance();

                musicStore.putAllMusic(musicList);
                MusicList localMusicList = musicStore.getLocalMusicList();
                localMusicList.getMusicElements().addAll(musicList);
                musicStore.updateMusicList(localMusicList);
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
                    .setName(MusicStore.MUSIC_LIST_LOCAL_MUSIC)
                    .appendAll(itemList)
                    .build();
        }
    }
}