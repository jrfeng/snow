package snow.music.activity.navigation;

import androidx.databinding.DataBindingUtil;
import androidx.lifecycle.ViewModelProvider;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;

import com.bumptech.glide.load.resource.bitmap.CenterCrop;
import com.bumptech.glide.load.resource.bitmap.RoundedCorners;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.google.common.base.Preconditions;

import snow.music.GlideApp;
import snow.music.R;
import snow.music.activity.BaseActivity;
import snow.music.databinding.ActivityNavigationBinding;
import snow.music.dialog.PlaylistDialog;
import snow.music.dialog.ScannerDialog;
import snow.music.service.AppPlayerService;
import snow.music.util.DimenUtil;
import snow.music.util.PlayerUtil;
import snow.player.lifecycle.PlayerViewModel;

public class NavigationActivity extends BaseActivity {
    private static final String KEY_SCAN_LOCAL_MUSIC = "scan_local_music";

    private ActivityNavigationBinding mBinding;
    private PlayerViewModel mPlayerViewModel;
    private NavigationViewModel mNavigationViewModel;

    private int mIconCornerRadius;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mBinding = DataBindingUtil.setContentView(this, R.layout.activity_navigation);

        initAllViewModel();
        setPlayerClient(mPlayerViewModel.getPlayerClient());

        mBinding.setNavViewModel(mNavigationViewModel);
        mBinding.setLifecycleOwner(this);

        observerPlayingMusicItem();

        if (shouldScanLocalMusic()) {
            scanLocalMusic();
        }

        mIconCornerRadius = DimenUtil.getDimenPx(getResources(), R.dimen.album_icon_corner_radius);
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
                    if (musicItem == null) {
                        mBinding.ivDisk.setImageResource(R.mipmap.ic_album_default_icon_big);
                        return;
                    }

                    loadMusicIcon(musicItem.getUri());
                });
    }

    private void loadMusicIcon(String musicUri) {
        GlideApp.with(this)
                .load(musicUri)
                .placeholder(R.mipmap.ic_album_default_icon_big)
                .transform(new CenterCrop(), new RoundedCorners(mIconCornerRadius))
                .transition(DrawableTransitionOptions.withCrossFade())
                .into(mBinding.ivDisk);
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

        ScannerDialog scannerDialog = ScannerDialog.newInstance(true, true);
        scannerDialog.show(getSupportFragmentManager(), "scannerDialog");
    }

    public void showPlaylist(View view) {
        Preconditions.checkNotNull(view);

        PlaylistDialog.newInstance()
                .show(getSupportFragmentManager(), "Playlist");
    }
}