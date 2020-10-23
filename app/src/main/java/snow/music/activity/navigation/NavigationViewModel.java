package snow.music.activity.navigation;

import android.app.Application;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;

import com.google.common.base.Preconditions;

import io.reactivex.Single;
import io.reactivex.SingleOnSubscribe;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import snow.music.R;
import snow.music.store.MusicStore;
import snow.music.util.MusicUtil;
import snow.player.PlaybackState;
import snow.player.audio.MusicItem;
import snow.player.lifecycle.PlayerViewModel;

public class NavigationViewModel extends AndroidViewModel {
    private MutableLiveData<Integer> mFavoriteDrawable;
    private MutableLiveData<Integer> mPlayPauseDrawable;

    private MusicStore.OnFavoriteChangeListener mFavoriteChangeListener;
    private Observer<MusicItem> mPlayingMusicItemObserver;
    private Observer<PlaybackState> mPlaybackStateObserver;

    private boolean mInitialized;
    private PlayerViewModel mPlayerViewModel;

    private Disposable mCheckFavoriteDisposable;

    public NavigationViewModel(@NonNull Application application) {
        super(application);

        mFavoriteDrawable = new MutableLiveData<>(R.drawable.ic_favorite_false);
        mPlayPauseDrawable = new MutableLiveData<>(R.drawable.ic_play);

        mFavoriteChangeListener = this::checkPlayingMusicFavoriteState;
        mPlayingMusicItemObserver = musicItem -> checkPlayingMusicFavoriteState();
        mPlaybackStateObserver = playbackState -> {
            if (playbackState == PlaybackState.PLAYING) {
                mPlayPauseDrawable.setValue(R.drawable.ic_pause);
            } else {
                mPlayPauseDrawable.setValue(R.drawable.ic_play);
            }
        };
    }

    public void init(@NonNull PlayerViewModel playerViewModel) {
        Preconditions.checkNotNull(playerViewModel);
        mInitialized = true;
        mPlayerViewModel = playerViewModel;

        MusicStore.getInstance().addOnFavoriteChangeListener(mFavoriteChangeListener);
        mPlayerViewModel.getPlayingMusicItem().observeForever(mPlayingMusicItemObserver);
        mPlayerViewModel.getPlaybackState().observeForever(mPlaybackStateObserver);
    }

    @Override
    protected void onCleared() {
        super.onCleared();

        if (!mInitialized) {
            return;
        }

        MusicStore.getInstance().removeOnFavoriteChangeListener(mFavoriteChangeListener);
        mPlayerViewModel.getPlayingMusicItem().removeObserver(mPlayingMusicItemObserver);
        mPlayerViewModel.getPlaybackState().removeObserver(mPlaybackStateObserver);

        mInitialized = false;
        mPlayerViewModel = null;
        disposeCheckFavorite();
    }

    @NonNull
    public LiveData<Integer> getFavoriteDrawable() {
        return mFavoriteDrawable;
    }

    @NonNull
    public LiveData<Integer> getPlayPauseDrawable() {
        return mPlayPauseDrawable;
    }

    public boolean isInitialized() {
        return mInitialized;
    }

    private void checkPlayingMusicFavoriteState() {
        disposeCheckFavorite();

        mCheckFavoriteDisposable = Single.create((SingleOnSubscribe<Boolean>) emitter -> {
            MusicItem playingMusicItem = mPlayerViewModel.getPlayingMusicItem().getValue();

            boolean result;
            if (playingMusicItem == null) {
                result = false;
            } else {
                result = MusicStore.getInstance().isFavorite(MusicUtil.getId(playingMusicItem));
            }

            if (emitter.isDisposed()) {
                return;
            }

            emitter.onSuccess(result);
        }).subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(aBoolean -> mFavoriteDrawable.setValue(aBoolean ? R.drawable.ic_favorite_true : R.drawable.ic_favorite_false));
    }

    private void disposeCheckFavorite() {
        if (mCheckFavoriteDisposable != null) {
            mCheckFavoriteDisposable.dispose();
        }
    }

    public void togglePlayingMusicFavorite() {
        if (!mInitialized) {
            return;
        }

        MusicItem playingMusicItem = mPlayerViewModel.getPlayingMusicItem().getValue();
        if (playingMusicItem == null) {
            return;
        }

        MusicStore.getInstance().toggleFavorite(MusicUtil.asMusic(playingMusicItem));
    }

    public void showPlaylist() {
        // TODO
        Log.d("DEBUG", "showPlaylist");
    }

    public void navigateToSearch() {
        // TODO
        Log.d("DEBUG", "navigateToSearch");
    }

    public void navigateToSetting() {
        // TODO
        Log.d("DEBUG", "navigateToSetting");
    }

    public void navigateToLocalMusic() {
        // TODO
        Log.d("DEBUG", "navigateToLocalMusic");
    }

    public void navigateToFavorite() {
        // TODO
        Log.d("DEBUG", "navigateToFavorite");
    }

    public void navigateToMusicList() {
        // TODO
        Log.d("DEBUG", "navigateToMusicList");
    }

    public void navigateToArtist() {
        // TODO
        Log.d("DEBUG", "navigateToArtist");
    }

    public void navigateToAlbum() {
        // TODO
        Log.d("DEBUG", "navigateToAlbum");
    }

    public void navigateToHistory() {
        // TODO
        Log.d("DEBUG", "navigateToHistory");
    }
}
