package snow.music.fragment.battombar;

import android.content.Context;
import android.util.Log;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.arch.core.util.Function;
import androidx.fragment.app.FragmentActivity;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.Transformations;
import androidx.lifecycle.ViewModel;

import com.google.common.base.Preconditions;

import snow.music.R;
import snow.music.dialog.PlaylistDialog;
import snow.player.PlaybackState;
import snow.player.lifecycle.PlayerViewModel;

public class BottomBarViewModel extends ViewModel {
    private static final String TAG = "PlayerBottomBar";
    private PlayerViewModel mPlayerViewModel;
    private boolean mInitialized;

    public void init(@NonNull PlayerViewModel playerViewModel) {
        Preconditions.checkNotNull(playerViewModel);

        if (mInitialized) {
            return;
        }

        mInitialized = true;
        mPlayerViewModel = playerViewModel;
    }

    public LiveData<String> getMusicTitle() {
        if (!mInitialized) {
            throw new IllegalStateException("PlayerBottomBarViewModel not init yet.");
        }

        return mPlayerViewModel.getTitle();
    }

    public LiveData<String> getMusicArtist() {
        if (!mInitialized) {
            throw new IllegalStateException("PlayerBottomBarViewModel not init yet.");
        }

        return mPlayerViewModel.getArtist();
    }

    public LiveData<Integer> getPlayPauseDrawableRes() {
        if (!mInitialized) {
            throw new IllegalStateException("PlayerBottomBarViewModel not init yet.");
        }

        return Transformations.map(mPlayerViewModel.getPlaybackState(), playbackState -> {
            if (playbackState == PlaybackState.PLAYING) {
                return R.drawable.ic_bottom_bar_pause;
            } else {
                return R.drawable.ic_bottom_bar_play;
            }
        });
    }

    public void playPause() {
        if (!mInitialized) {
            throw new IllegalStateException("PlayerBottomBarViewModel not init yet.");
        }

        mPlayerViewModel.playPause();
    }

    public void showPlaylist(View view) {
        Preconditions.checkNotNull(view);

        Context context = view.getContext();
        if (!(context instanceof FragmentActivity)) {
            Log.e(TAG, "This view not belong a FragmentActivity.");
            return;
        }

        PlaylistDialog.newInstance()
                .show(((FragmentActivity) context).getSupportFragmentManager(), "PlaylistDialog");
    }

    public LiveData<Integer> getDuration() {
        if (!mInitialized) {
            throw new IllegalStateException("NavigationViewModel not init yet.");
        }

        return mPlayerViewModel.getDuration();
    }

    public LiveData<Integer> getPlayProgress() {
        if (!mInitialized) {
            throw new IllegalStateException("NavigationViewModel not init yet.");
        }

        return mPlayerViewModel.getPlayProgress();
    }
}
