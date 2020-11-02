package snow.music.activity.navigation;

import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentActivity;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;
import androidx.lifecycle.Transformations;
import androidx.lifecycle.ViewModel;

import com.google.common.base.Preconditions;

import snow.music.R;
import snow.music.activity.localmusic.LocalMusicActivity;
import snow.music.dialog.PlaylistDialog;
import snow.music.store.MusicStore;
import snow.music.util.FavoriteObserver;
import snow.music.util.MusicUtil;
import snow.player.PlaybackState;
import snow.player.audio.MusicItem;
import snow.player.lifecycle.PlayerViewModel;

public class NavigationViewModel extends ViewModel {
    private static final String TAG = "NavigationViewModel";
    private final MutableLiveData<Integer> mFavoriteDrawable;

    private final Observer<MusicItem> mPlayingMusicItemObserver;
    private FavoriteObserver mFavoriteObserver;

    private PlayerViewModel mPlayerViewModel;
    private boolean mInitialized;

    public NavigationViewModel() {
        mFavoriteDrawable = new MutableLiveData<>(R.drawable.ic_favorite_false);
        mFavoriteObserver = new FavoriteObserver(favorite ->
                mFavoriteDrawable.setValue(favorite ? R.drawable.ic_favorite_true : R.drawable.ic_favorite_false));
        mPlayingMusicItemObserver = musicItem -> mFavoriteObserver.setMusicItem(musicItem);
    }

    public void init(@NonNull PlayerViewModel playerViewModel) {
        Preconditions.checkNotNull(playerViewModel);

        if (mInitialized) {
            return;
        }

        mInitialized = true;
        mPlayerViewModel = playerViewModel;

        mFavoriteObserver.subscribe();
        mPlayerViewModel.getPlayingMusicItem().observeForever(mPlayingMusicItemObserver);
    }

    public boolean isInitialized() {
        return mInitialized;
    }

    @Override
    protected void onCleared() {
        mFavoriteObserver.unsubscribe();
        if (isInitialized()) {
            mPlayerViewModel.getPlayingMusicItem().removeObserver(mPlayingMusicItemObserver);
            return;
        }

        // 必须放在最后面
        super.onCleared();
    }

    public LiveData<String> getMusicTitle() {
        if (!mInitialized) {
            throw new IllegalStateException("NavigationViewModel not init yet.");
        }

        return mPlayerViewModel.getTitle();
    }

    public LiveData<String> getMusicArtist() {
        if (!mInitialized) {
            throw new IllegalStateException("NavigationViewModel not init yet.");
        }

        return mPlayerViewModel.getArtist();
    }

    @NonNull
    public LiveData<Integer> getFavoriteDrawable() {
        return mFavoriteDrawable;
    }

    @NonNull
    public LiveData<Integer> getPlayPauseDrawable() {
        if (!mInitialized) {
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

    public void skipToPrevious() {
        if (!mInitialized) {
            throw new IllegalStateException("NavigationViewModel not init yet.");
        }

        mPlayerViewModel.skipToPrevious();
    }

    public void playPause() {
        if (!mInitialized) {
            throw new IllegalStateException("NavigationViewModel not init yet.");
        }

        mPlayerViewModel.playPause();
    }

    public void skipToNext() {
        if (!mInitialized) {
            throw new IllegalStateException("NavigationViewModel not init yet.");
        }

        mPlayerViewModel.skipToNext();
    }

    public void showPlaylist(View view) {
        Preconditions.checkNotNull(view);

        if (!mInitialized) {
            throw new IllegalStateException("NavigationViewModel not init yet.");
        }

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

    public void navigateToSearch(View view) {
        // TODO
        Log.d("DEBUG", "navigateToSearch");
    }

    public void navigateToSetting(View view) {
        // TODO
        Log.d("DEBUG", "navigateToSetting");
    }

    public void navigateToLocalMusic(View view) {
        Context context = view.getContext();
        Intent intent = new Intent(context, LocalMusicActivity.class);
        context.startActivity(intent);
    }

    public void navigateToFavorite(View view) {
        // TODO
        Log.d("DEBUG", "navigateToFavorite");
    }

    public void navigateToMusicList(View view) {
        // TODO
        Log.d("DEBUG", "navigateToMusicList");
    }

    public void navigateToArtist(View view) {
        // TODO
        Log.d("DEBUG", "navigateToArtist");
    }

    public void navigateToAlbum(View view) {
        // TODO
        Log.d("DEBUG", "navigateToAlbum");
    }

    public void navigateToHistory(View view) {
        // TODO
        Log.d("DEBUG", "navigateToHistory");
    }
}
