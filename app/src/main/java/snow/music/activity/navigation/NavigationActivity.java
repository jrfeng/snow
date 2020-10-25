package snow.music.activity.navigation;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.databinding.DataBindingUtil;
import androidx.lifecycle.ViewModelProvider;

import android.Manifest;
import android.animation.ObjectAnimator;
import android.annotation.SuppressLint;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.animation.LinearInterpolator;

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
import snow.music.model.ScannerViewModel;
import snow.music.store.Music;
import snow.music.store.MusicStore;
import snow.music.util.EmbeddedPictureUtil;
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

    private ActivityNavigationBinding mBinding;
    private ScannerViewModel mScannerViewModel;
    private NavigationViewModel mNavigationViewModel;

    private ObjectAnimator mDiskRotateAnimator;
    private long mDiskAnimPlayTime;
    private boolean mAnimPaused;

    private Disposable mIconLoadDisposable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mBinding = DataBindingUtil.setContentView(this, R.layout.activity_navigation);

        ViewModelProvider viewModelProvider = new ViewModelProvider(this);
        mNavigationViewModel = viewModelProvider.get(NavigationViewModel.class);
        initNavigationViewModel(mNavigationViewModel);

        mScannerViewModel = viewModelProvider.get(ScannerViewModel.class);
        initDiskRotateAnim(mBinding.ivDiskIcon);

        mBinding.setNavViewModel(mNavigationViewModel);
        mBinding.setLifecycleOwner(this);

        observerMusicIconUri();

        if (shouldScanLocalMusic()) {
            scanLocalMusic();
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        mAnimPaused = false;
        resumeDiskRotateAnim();
    }

    @Override
    protected void onStop() {
        super.onStop();
        mAnimPaused = true;
        pauseDiskRotateAnim();

        if (isFinishing() && mIconLoadDisposable != null) {
            mIconLoadDisposable.dispose();
        }
    }

    private void initNavigationViewModel(NavigationViewModel navigationViewModel) {
        if (navigationViewModel.isInitialized()) {
            return;
        }

        PlayerClient playerClient = PlayerClient.newInstance(this, PlayerService.class);
        navigationViewModel.init(this, playerClient);
        playerClient.connect();

        navigationViewModel.setAutoDisconnect(true);
    }

    private void initDiskRotateAnim(View diskView) {
        mDiskRotateAnimator = ObjectAnimator.ofFloat(diskView, "rotation", 0, 360);
        mDiskRotateAnimator.setDuration(20_000);
        mDiskRotateAnimator.setRepeatCount(-1);
        mDiskRotateAnimator.setRepeatMode(ObjectAnimator.RESTART);
        mDiskRotateAnimator.setInterpolator(new LinearInterpolator());

        mNavigationViewModel.getPlayingNoStalled()
                .observe(this, playingNoStalled -> {
                    if (playingNoStalled) {
                        resumeDiskRotateAnim();
                    } else {
                        pauseDiskRotateAnim();
                    }
                });
    }

    private void pauseDiskRotateAnim() {
        if (mDiskRotateAnimator == null) {
            return;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            mDiskRotateAnimator.pause();
            return;
        }

        mDiskAnimPlayTime = mDiskRotateAnimator.getCurrentPlayTime();
        mDiskRotateAnimator.cancel();
    }

    private void resumeDiskRotateAnim() {
        if (mDiskRotateAnimator == null || !shouldStartAnim()) {
            return;
        }

        if (!mDiskRotateAnimator.isStarted()) {
            mDiskRotateAnimator.start();
            return;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            mDiskRotateAnimator.resume();
            return;
        }

        mDiskRotateAnimator.start();
        mDiskRotateAnimator.setCurrentPlayTime(mDiskAnimPlayTime);
    }

    private boolean shouldStartAnim() {
        PlayerClient playerClient = mNavigationViewModel.getPlayerClient();
        return !mAnimPaused && playerClient.isPlaying() && !playerClient.isPreparing() && !playerClient.isStalled();
    }

    private void observerMusicIconUri() {
        mNavigationViewModel.getPlayingMusicItem()
                .observe(this, musicItem -> {
                    if (musicItem == null) {
                        mBinding.ivDiskIcon.setImageResource(0);
                        return;
                    }

                    loadMusicIcon(musicItem.getUri());
                });
    }

    private void loadMusicIcon(String musicUri) {
        mIconLoadDisposable = EmbeddedPictureUtil.getEmbeddedPicture(this, musicUri)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(bytes -> Glide.with(NavigationActivity.this)
                        .load(bytes)
                        .transform(new CircleCrop())
                        .transition(DrawableTransitionOptions.withCrossFade(200))
                        .into(mBinding.ivDiskIcon));
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

        mScannerViewModel.scan(30_000, new ScanCompleteListener(mNavigationViewModel.getPlayerClient()));
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
}