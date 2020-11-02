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

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.RoundedCorners;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;

import java.util.Objects;

import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import snow.music.R;
import snow.music.databinding.FragmentBottomBarBinding;
import snow.music.service.AppPlayerService;
import snow.music.util.DimenUtil;
import snow.music.util.MusicUtil;
import snow.music.util.PlayerUtil;
import snow.player.lifecycle.PlayerViewModel;

public class BottomBarFragment extends Fragment {
    private FragmentBottomBarBinding mBinding;
    private PlayerViewModel mPlayerViewModel;
    private BottomBarViewModel mBottomBarViewModel;

    private Disposable mIconLoadDisposable;

    private int mMinSlideDistance;
    private long mMaxSlideInterval;
    private float mActionDownY;
    private long mActionDownTime;

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);

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
    public void onDestroy() {
        super.onDestroy();
        cancelLoadMusicIcon();
    }

    private void loadMusicIcon(String musicUri) {
        cancelLoadMusicIcon();
        mIconLoadDisposable = MusicUtil.getEmbeddedPicture(Objects.requireNonNull(getContext()), musicUri)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(bytes -> Glide.with(BottomBarFragment.this)
                        .load(bytes)
                        .placeholder(mBinding.ivIcon.getDrawable())
                        .error(R.mipmap.ic_bottom_bar_default_icon)
                        .transform(new RoundedCorners(DimenUtil.getDimenPx(getResources(), R.dimen.player_bottom_bar_icon_corner_size)))
                        .transition(DrawableTransitionOptions.withCrossFade())
                        .into(mBinding.ivIcon));
    }

    private void cancelLoadMusicIcon() {
        if (mIconLoadDisposable != null && !mIconLoadDisposable.isDisposed()) {
            mIconLoadDisposable.dispose();
        }
    }
}
