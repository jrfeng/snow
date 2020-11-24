package snow.music.activity.navigation;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;
import androidx.lifecycle.Transformations;
import androidx.lifecycle.ViewModel;

import com.google.common.base.Preconditions;

import snow.music.R;
import snow.music.activity.browser.album.AlbumBrowserActivity;
import snow.music.activity.browser.artist.ArtistBrowserActivity;
import snow.music.activity.browser.musiclist.MusicListBrowserActivity;
import snow.music.activity.favorite.FavoriteActivity;
import snow.music.activity.history.HistoryActivity;
import snow.music.activity.localmusic.LocalMusicActivity;
import snow.music.store.MusicStore;
import snow.music.util.FavoriteObserver;
import snow.music.util.MusicUtil;
import snow.player.PlaybackState;
import snow.player.PlayerClient;
import snow.player.audio.MusicItem;
import snow.player.lifecycle.PlayerViewModel;

public class NavigationViewModel extends ViewModel {
    private static final String TAG = "NavigationViewModel";
    private final MutableLiveData<Integer> mFavoriteDrawable;
    private final MutableLiveData<CharSequence> mSecondaryText;

    private final Observer<MusicItem> mPlayingMusicItemObserver;
    private final Observer<String> mArtistObserver;
    private final Observer<Boolean> mErrorObserver;
    private final FavoriteObserver mFavoriteObserver;

    private PlayerViewModel mPlayerViewModel;
    private boolean mInitialized;

    public NavigationViewModel() {
        mFavoriteDrawable = new MutableLiveData<>(R.drawable.ic_favorite_false);
        mSecondaryText = new MutableLiveData<>("");
        mFavoriteObserver = new FavoriteObserver(favorite ->
                mFavoriteDrawable.setValue(favorite ? R.drawable.ic_favorite_true : R.drawable.ic_favorite_false));
        mArtistObserver = artist -> updateSecondaryText();
        mErrorObserver = error -> updateSecondaryText();
        mPlayingMusicItemObserver = mFavoriteObserver::setMusicItem;
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
        mPlayerViewModel.getArtist().observeForever(mArtistObserver);
        mPlayerViewModel.isError().observeForever(mErrorObserver);
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

    public boolean isInitialized() {
        return mInitialized;
    }

    @Override
    protected void onCleared() {
        mFavoriteObserver.unsubscribe();
        if (isInitialized()) {
            mPlayerViewModel.getPlayingMusicItem().removeObserver(mPlayingMusicItemObserver);
            mPlayerViewModel.getArtist().removeObserver(mArtistObserver);
            mPlayerViewModel.isError().removeObserver(mErrorObserver);
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

    public LiveData<CharSequence> getSecondaryText() {
        if (!mInitialized) {
            throw new IllegalStateException("NavigationViewModel not init yet.");
        }

        return mSecondaryText;
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
        Context context = view.getContext();
        Intent intent = new Intent(context, FavoriteActivity.class);
        context.startActivity(intent);
    }

    public void navigateToMusicListBrowser(View view) {
        Context context = view.getContext();
        Intent intent = new Intent(context, MusicListBrowserActivity.class);
        context.startActivity(intent);
    }

    public void navigateToArtistBrowser(View view) {
        Context context = view.getContext();
        Intent intent = new Intent(context, ArtistBrowserActivity.class);
        context.startActivity(intent);
    }

    public void navigateToAlbum(View view) {
        Context context = view.getContext();
        Intent intent = new Intent(context, AlbumBrowserActivity.class);
        context.startActivity(intent);
    }

    public void navigateToHistory(View view) {
        Context context = view.getContext();
        Intent intent = new Intent(context, HistoryActivity.class);
        context.startActivity(intent);
    }
}
