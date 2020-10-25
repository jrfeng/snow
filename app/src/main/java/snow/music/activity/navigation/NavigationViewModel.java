package snow.music.activity.navigation;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;
import androidx.lifecycle.Transformations;

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

public class NavigationViewModel extends PlayerViewModel {
    private final MutableLiveData<Integer> mFavoriteDrawable;

    private final MusicStore.OnFavoriteChangeListener mFavoriteChangeListener;
    private final Observer<MusicItem> mPlayingMusicItemObserver;

    private Disposable mCheckFavoriteDisposable;

    public NavigationViewModel() {
        mFavoriteDrawable = new MutableLiveData<>(R.drawable.ic_favorite_false);
        mFavoriteChangeListener = this::checkPlayingMusicFavoriteState;
        mPlayingMusicItemObserver = musicItem -> checkPlayingMusicFavoriteState();
    }

    @Override
    protected void onInitialized() {
        MusicStore.getInstance().addOnFavoriteChangeListener(mFavoriteChangeListener);
        getPlayingMusicItem().observeForever(mPlayingMusicItemObserver);
    }

    @Override
    protected void onCleared() {
        if (!isInitialized()) {
            return;
        }

        MusicStore.getInstance().removeOnFavoriteChangeListener(mFavoriteChangeListener);
        getPlayingMusicItem().removeObserver(mPlayingMusicItemObserver);

        disposeCheckFavorite();

        // 必须放在最后面
        super.onCleared();
    }

    @NonNull
    public LiveData<Integer> getFavoriteDrawable() {
        return mFavoriteDrawable;
    }

    @NonNull
    public LiveData<Integer> getPlayPauseDrawable() {
        if (!isInitialized()) {
            throw new IllegalStateException("NavigationViewModel not init yet.");
        }

        return Transformations.map(getPlaybackState(), playbackState -> {
            if (playbackState == PlaybackState.PLAYING) {
                return R.drawable.ic_pause;
            } else {
                return R.drawable.ic_play;
            }
        });
    }

    private void checkPlayingMusicFavoriteState() {
        disposeCheckFavorite();

        mCheckFavoriteDisposable = Single.create((SingleOnSubscribe<Boolean>) emitter -> {
            MusicItem playingMusicItem = getPlayingMusicItem().getValue();

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
        if (!isInitialized()) {
            return;
        }

        MusicItem playingMusicItem = getPlayingMusicItem().getValue();
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
