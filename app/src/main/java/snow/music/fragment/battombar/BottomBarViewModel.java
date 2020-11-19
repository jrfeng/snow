package snow.music.fragment.battombar;

import android.graphics.Color;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;
import androidx.lifecycle.Transformations;
import androidx.lifecycle.ViewModel;

import com.google.common.base.Preconditions;

import snow.music.R;
import snow.player.PlaybackState;
import snow.player.PlayerClient;
import snow.player.lifecycle.PlayerViewModel;

public class BottomBarViewModel extends ViewModel {
    private static final String TAG = "PlayerBottomBar";
    private PlayerViewModel mPlayerViewModel;
    private boolean mInitialized;

    private MutableLiveData<CharSequence> mSecondaryText;
    private Observer<String> mArtistObserver;
    private Observer<Boolean> mErrorObserver;

    public void init(@NonNull PlayerViewModel playerViewModel) {
        Preconditions.checkNotNull(playerViewModel);

        if (mInitialized) {
            return;
        }

        mInitialized = true;
        mPlayerViewModel = playerViewModel;

        mSecondaryText = new MutableLiveData<>("");
        mArtistObserver = artist -> updateSecondaryText();
        mErrorObserver = error -> updateSecondaryText();

        mPlayerViewModel.getArtist().observeForever(mArtistObserver);
        mPlayerViewModel.isError().observeForever(mErrorObserver);
    }

    @Override
    protected void onCleared() {
        super.onCleared();

        if (mInitialized) {
            mPlayerViewModel.getArtist().removeObserver(mArtistObserver);
            mPlayerViewModel.isError().removeObserver(mErrorObserver);
        }
    }

    private void updateSecondaryText() {
        PlayerClient playerClient = mPlayerViewModel.getPlayerClient();

        CharSequence text = mPlayerViewModel.getArtist().getValue();

        if (playerClient.isError()) {
            text = playerClient.getErrorMessage();
            SpannableString colorText = new SpannableString(text);
            colorText.setSpan(new ForegroundColorSpan(Color.parseColor("#F44336")), 0, text.length(), Spanned.SPAN_INCLUSIVE_EXCLUSIVE);
            text = colorText;
        }

        mSecondaryText.setValue(text);
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

    public LiveData<CharSequence> getSecondaryText() {
        if (!mInitialized) {
            throw new IllegalStateException("PlayerBottomBarViewModel not init yet.");
        }

        return mSecondaryText;
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
