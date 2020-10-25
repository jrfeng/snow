package snow.music.model;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import snow.music.store.MusicStore;
import snow.player.lifecycle.PlayerViewModel;

public class PlayingMusicViewModel extends PlayerViewModel {
    private final MutableLiveData<Boolean> mFavorite;

    public PlayingMusicViewModel() {
        mFavorite = new MutableLiveData<>(false);

        // TODO
    }

    private void addListeners() {
        MusicStore musicStore = MusicStore.getInstance();

        musicStore.addOnFavoriteChangeListener(new MusicStore.OnFavoriteChangeListener() {
            @Override
            public void onFavoriteChanged() {
                updateFavorite();
            }
        });
    }

    private void updateFavorite() {
        // TODO 以异步的方式查询当前歌曲是否是 favorite
    }

    public LiveData<Boolean> getFavorite() {
        return mFavorite;
    }
}
