package snow.music.activity.navigation;

import android.util.Log;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;
import androidx.lifecycle.Transformations;
import androidx.lifecycle.ViewModel;

import com.google.common.base.Preconditions;

import io.reactivex.Single;
import io.reactivex.SingleOnSubscribe;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import snow.music.R;
import snow.music.dialog.PlaylistDialog;
import snow.music.store.MusicStore;
import snow.music.util.MusicUtil;
import snow.player.PlaybackState;
import snow.player.audio.MusicItem;
import snow.player.lifecycle.PlayerViewModel;

public class NavigationViewModel extends ViewModel {
    private final MutableLiveData<Integer> mFavoriteDrawable;

    private final MusicStore.OnFavoriteChangeListener mFavoriteChangeListener;
    private final Observer<MusicItem> mPlayingMusicItemObserver;

    private PlayerViewModel mPlayerViewModel;
    private boolean mInitialized;

    private Disposable mCheckFavoriteDisposable;

    public NavigationViewModel() {
        mFavoriteDrawable = new MutableLiveData<>(R.drawable.ic_favorite_false);
        mFavoriteChangeListener = this::checkPlayingMusicFavoriteState;
        mPlayingMusicItemObserver = musicItem -> checkPlayingMusicFavoriteState();
    }

    public void init(@NonNull PlayerViewModel playerViewModel) {
        Preconditions.checkNotNull(playerViewModel);

        mInitialized = true;
        mPlayerViewModel = playerViewModel;

        MusicStore.getInstance().addOnFavoriteChangeListener(mFavoriteChangeListener);
        mPlayerViewModel.getPlayingMusicItem().observeForever(mPlayingMusicItemObserver);
    }

    public boolean isInitialized() {
        return mInitialized;
    }

    @Override
    protected void onCleared() {
        if (!isInitialized()) {
            return;
        }

        MusicStore.getInstance().removeOnFavoriteChangeListener(mFavoriteChangeListener);
        mPlayerViewModel.getPlayingMusicItem().removeObserver(mPlayingMusicItemObserver);

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

        return Transformations.map(mPlayerViewModel.getPlaybackState(), playbackState -> {
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
        if (!isInitialized()) {
            return;
        }

        MusicItem playingMusicItem = mPlayerViewModel.getPlayingMusicItem().getValue();
        if (playingMusicItem == null) {
            return;
        }

        MusicStore.getInstance().toggleFavorite(MusicUtil.asMusic(playingMusicItem));
    }

    public void showPlaylist(View view) {
        Preconditions.checkNotNull(view);

        PlaylistDialog playlistDialog = PlaylistDialog.newInstance();
        playlistDialog.show(((AppCompatActivity) view.getContext()).getSupportFragmentManager(), "PlaylistDialog");
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
