package snow.music.activity.navigation;

import androidx.databinding.DataBindingUtil;
import androidx.lifecycle.ViewModelProvider;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.CircleCrop;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.google.common.base.Preconditions;

import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import snow.music.R;
import snow.music.activity.BaseActivity;
import snow.music.databinding.ActivityNavigationBinding;
import snow.music.dialog.PlaylistDialog;
import snow.music.dialog.ScannerDialog;
import snow.music.service.AppPlayerService;
import snow.music.util.PlayerUtil;
import snow.music.util.MusicUtil;
import snow.player.lifecycle.PlayerViewModel;

public class NavigationActivity extends BaseActivity {
    private static final String KEY_SCAN_LOCAL_MUSIC = "scan_local_music";

    private ActivityNavigationBinding mBinding;
    private PlayerViewModel mPlayerViewModel;
    private NavigationViewModel mNavigationViewModel;

    private DiskAnimManager mDiskAnimManager;
    private Disposable mIconLoadDisposable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mBinding = DataBindingUtil.setContentView(this, R.layout.activity_navigation);

        initAllViewModel();
        setPlayerClient(mPlayerViewModel.getPlayerClient());

        mBinding.setNavViewModel(mNavigationViewModel);
        mBinding.setLifecycleOwner(this);

        mDiskAnimManager = new DiskAnimManager(mBinding.ivDisk, this, mPlayerViewModel);
        observerPlayingMusicItem();

        if (shouldScanLocalMusic()) {
            scanLocalMusic();
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
        getPreferences(MODE_PRIVATE)
                .edit()
                .putBoolean(KEY_SCAN_LOCAL_MUSIC, false)
                .apply();

        ScannerDialog scannerDialog = ScannerDialog.newInstance(true);
        scannerDialog.show(getSupportFragmentManager(), "scannerDialog");
    }

    public void showPlaylist(View view) {
        Preconditions.checkNotNull(view);

        PlaylistDialog.newInstance()
                .show(getSupportFragmentManager(), "Playlist");
    }
}