package snow.music.fragment.battombar;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
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
import snow.music.util.DimenUtil;
import snow.music.util.MusicUtil;
import snow.player.lifecycle.PlayerViewModel;

public class BottomBarFragment extends Fragment {
    private FragmentBottomBarBinding mBinding;
    private BottomBarViewModel mBottomBarViewModel;

    private Disposable mIconLoadDisposable;

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);

        FragmentActivity activity = getActivity();
        assert activity != null;
        ViewModelProvider viewModelProvider = new ViewModelProvider(activity);

        PlayerViewModel playerViewModel = viewModelProvider.get(PlayerViewModel.class);
        mBottomBarViewModel = viewModelProvider.get(BottomBarViewModel.class);
        mBottomBarViewModel.init(playerViewModel);

        playerViewModel.getPlayingMusicItem()
                .observe(this, musicItem -> loadMusicIcon(musicItem.getUri()));
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        mBinding = FragmentBottomBarBinding.inflate(inflater);
        mBinding.setBottomBarViewModel(mBottomBarViewModel);
        mBinding.setLifecycleOwner(this);
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
                        .error(R.mipmap.ic_bottom_bar_default_icon)
                        .transform(new RoundedCorners(DimenUtil.getDimenPx(getResources(), R.dimen.player_bottom_bar_icon_corner_size)))
                        .transition(DrawableTransitionOptions.withCrossFade(200))
                        .into(mBinding.ivIcon));
    }

    private void cancelLoadMusicIcon() {
        if (mIconLoadDisposable != null && !mIconLoadDisposable.isDisposed()) {
            mIconLoadDisposable.dispose();
        }
    }
}
