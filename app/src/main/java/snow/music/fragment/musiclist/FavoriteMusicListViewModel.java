package snow.music.fragment.musiclist;

import androidx.annotation.NonNull;

import com.google.common.base.Preconditions;

import java.util.List;

import io.reactivex.Single;
import io.reactivex.SingleOnSubscribe;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import snow.music.store.Music;
import snow.music.store.MusicList;
import snow.music.store.MusicStore;

public class FavoriteMusicListViewModel extends BaseMusicListViewModel {
    private MusicList mFavoriteMusicList;
    private MusicStore.OnFavoriteChangeListener mFavoriteChangeListener;
    private Disposable mReloadFavoriteDisposable;

    @Override
    public void init(@NonNull String musicListName) {
        super.init(musicListName);

        mFavoriteChangeListener = this::reloadFavoriteMusicList;
        MusicStore.getInstance().addOnFavoriteChangeListener(mFavoriteChangeListener);
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        if (!isInitialized()) {
            return;
        }

        MusicStore.getInstance().removeOnFavoriteChangeListener(mFavoriteChangeListener);
        if (mReloadFavoriteDisposable != null && !mReloadFavoriteDisposable.isDisposed()) {
            mReloadFavoriteDisposable.dispose();
        }
    }

    @NonNull
    @Override
    protected List<Music> loadMusicListItems() {
        mFavoriteMusicList = MusicStore.getInstance().getFavoriteMusicList();
        mFavoriteMusicList.load();
        return mFavoriteMusicList.getMusicElements();
    }

    @Override
    protected void removeMusic(@NonNull Music music) {
        MusicStore.getInstance().removeFromFavorite(music);
    }

    @Override
    protected void onSortMusicList(@NonNull MusicList.SortOrder sortOrder) {
        Preconditions.checkNotNull(sortOrder);

        MusicStore.getInstance().sort(mFavoriteMusicList, sortOrder, () -> notifyMusicItemsChanged(mFavoriteMusicList.getMusicElements()));
    }

    @NonNull
    @Override
    protected MusicList.SortOrder getSortOrder() {
        return mFavoriteMusicList.getSortOrder();
    }

    private void reloadFavoriteMusicList() {
        mReloadFavoriteDisposable = Single.create((SingleOnSubscribe<MusicList>) emitter -> {
            MusicList favoriteMusicList = MusicStore.getInstance().getFavoriteMusicList();
            favoriteMusicList.load();
            if (emitter.isDisposed()) {
                return;
            }
            emitter.onSuccess(favoriteMusicList);
        }).subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(musicList -> {
                    mFavoriteMusicList = musicList;
                    notifyMusicItemsChanged(mFavoriteMusicList.getMusicElements());
                });
    }
}
