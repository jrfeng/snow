package snow.music.fragment.battombar;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Bundle;
import android.os.SystemClock;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.lifecycle.ViewModelProvider;

import com.bumptech.glide.load.resource.bitmap.CenterCrop;
import com.bumptech.glide.load.resource.bitmap.RoundedCorners;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.google.common.base.Preconditions;

import snow.music.GlideApp;
import snow.music.R;
import snow.music.databinding.FragmentBottomBarBinding;
import snow.music.dialog.PlaylistDialog;
import snow.music.service.AppPlayerService;
import snow.music.util.DimenUtil;
import snow.music.util.PlayerUtil;
import snow.player.PlayerClient;
import snow.player.lifecycle.PlayerViewModel;

public class BottomBarFragment extends Fragment {
    private FragmentBottomBarBinding mBinding;
    private PlayerViewModel mPlayerViewModel;
    private BottomBarViewModel mBottomBarViewModel;

    private int mMinSlideDistance;
    private long mMaxSlideInterval;
    private float mActionDownY;
    private long mActionDownTime;

    private int mIconCornerRadius;

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);

        mIconCornerRadius = DimenUtil.getDimenPx(context.getResources(), R.dimen.player_bottom_bar_icon_corner_size);

        FragmentActivity activity = getActivity();
        assert activity != null;
        ViewModelProvider viewModelProvider = new ViewModelProvider(activity);

        mPlayerViewModel = viewModelProvider.get(PlayerViewModel.class);
        PlayerUtil.initPlayerViewModel(context, mPlayerViewModel, AppPlayerService.class);
        mBottomBarViewModel = viewModelProvider.get(BottomBarViewModel.class);
        mBottomBarViewModel.init(mPlayerViewModel);

        mPlayerViewModel.getPlayingMusicItem()
                .observe(this, musicItem -> {
                    if (musicItem == null) {
                        mBinding.ivIcon.setImageResource(R.mipmap.ic_bottom_bar_default_icon);
                        return;
                    }

                    loadMusicIcon(musicItem.getUri());
                });

        mMinSlideDistance = DimenUtil.getDimenPx(getResources(), R.dimen.min_slide_distance);
        mMaxSlideInterval = 500;
    }

    @SuppressLint("ClickableViewAccessibility")
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        mBinding = FragmentBottomBarBinding.inflate(inflater);
        mBinding.setBottomBarViewModel(mBottomBarViewModel);
        mBinding.setLifecycleOwner(this);
        mBinding.btnShowPlaylist.setOnClickListener(this::showPlaylist);

        mBinding.messagePanel.setOnTouchListener((v, event) -> {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    mActionDownY = event.getX();
                    mActionDownTime = SystemClock.elapsedRealtime();
                    break;
                case MotionEvent.ACTION_UP:
                    float slideDistance = event.getX() - mActionDownY;
                    long interval = SystemClock.elapsedRealtime() - mActionDownTime;
                    if (Math.abs(slideDistance) >= mMinSlideDistance && interval <= mMaxSlideInterval) {
                        if (slideDistance > 0) {
                            mPlayerViewModel.skipToPrevious();
                        } else {
                            mPlayerViewModel.skipToNext();
                        }
                    }
                    break;
            }

            return true;
        });

        return mBinding.getRoot();
    }

    @Override
    public void onResume() {
        super.onResume();

        PlayerClient playerClient = mPlayerViewModel.getPlayerClient();
        if (!playerClient.isConnected()) {
            playerClient.connect();
        }
    }

    private void loadMusicIcon(String musicUri) {
        GlideApp.with(this)
                .load(musicUri)
                .placeholder(R.mipmap.ic_bottom_bar_default_icon)
                .transform(new CenterCrop(), new RoundedCorners(mIconCornerRadius))
                .transition(DrawableTransitionOptions.withCrossFade())
                .into(mBinding.ivIcon);
    }

    public void showPlaylist(View view) {
        PlaylistDialog.newInstance()
                .show(getParentFragmentManager(), "PlaylistDialog");
    }
}
