package snow.music.activity.search;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.google.common.base.Preconditions;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import io.reactivex.Single;
import io.reactivex.SingleOnSubscribe;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import pinyin.util.PinyinComparator;

import android.app.Application;

import snow.music.R;
import snow.music.activity.detail.album.AlbumDetailActivity;
import snow.music.activity.detail.artist.ArtistDetailActivity;
import snow.music.store.Music;
import snow.music.store.MusicStore;
import snow.music.util.MusicListUtil;
import snow.player.playlist.Playlist;

public class SearchViewModel extends AndroidViewModel {
    private final MutableLiveData<String> mInput;
    private final MutableLiveData<List<Music>> mSearchResult;
    private final MutableLiveData<String> mEmptyMessage;

    private SearchActivity.Type mType;
    private String mTypeName;
    private boolean mInitialized;

    private Disposable mSearchDisposable;
    private final Comparator<Music> mMusicComparator;

    public SearchViewModel(Application application) {
        super(application);

        mInput = new MutableLiveData<>("");
        mSearchResult = new MutableLiveData<>(Collections.emptyList());
        mEmptyMessage = new MutableLiveData<>("");

        mMusicComparator = new Comparator<Music>() {
            private final PinyinComparator pinyinComparator = new PinyinComparator();

            @Override
            public int compare(Music o1, Music o2) {
                return pinyinComparator.compare(o1.getTitle(), o2.getTitle());
            }
        };
    }

    public void init(@NonNull SearchActivity.Type type, @NonNull String typeName) {
        Preconditions.checkNotNull(type);
        Preconditions.checkNotNull(typeName);

        if (mInitialized) {
            return;
        }

        mType = type;
        mTypeName = typeName;
        mInitialized = true;
    }

    @Override
    protected void onCleared() {
        cancelLastSearch();
    }

    @NonNull
    public MutableLiveData<String> getInput() {
        return mInput;
    }

    @NonNull
    public LiveData<List<Music>> getSearchResult() {
        return mSearchResult;
    }

    public LiveData<String> getEmptyMessage() {
        return mEmptyMessage;
    }

    public void search() {
        cancelLastSearch();
        final String key = mInput.getValue();
        assert key != null;

        if (key.isEmpty()) {
            clearSearchResult();
            return;
        }

        mSearchDisposable = Single.create((SingleOnSubscribe<List<Music>>) emitter -> {
            List<Music> result = searchMusic(key);
            if (emitter.isDisposed()) {
                return;
            }
            emitter.onSuccess(result);
        }).subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(result -> {
                    Collections.sort(result, mMusicComparator);

                    mSearchResult.setValue(result);

                    if (result.isEmpty()) {
                        mEmptyMessage.setValue(getApplication().getString(R.string.empty_not_found));
                    }
                });
    }

    public Playlist resultAsPlaylist(int position) {
        List<Music> musicList = mSearchResult.getValue();
        assert musicList != null;
        return MusicListUtil.asPlaylist(getName(), musicList, position);
    }

    private String getName() {
        switch (mType) {
            case MUSIC_LIST:
                return mTypeName;
            case ARTIST:
                return ArtistDetailActivity.ARTIST_PREFIX + mTypeName;
            case ALBUM:
                return AlbumDetailActivity.ALBUM_PREFIX + mTypeName;
        }

        return "";
    }

    private void cancelLastSearch() {
        if (mSearchDisposable != null) {
            mSearchDisposable.dispose();
        }
    }

    @NonNull
    private List<Music> searchMusic(String key) {
        switch (mType) {
            case MUSIC_LIST:
                return MusicStore.getInstance().findMusicListMusic(mTypeName, key);
            case ARTIST:
                return MusicStore.getInstance().findArtistMusic(mTypeName, key);
            case ALBUM:
                return MusicStore.getInstance().findAlbumMusic(mTypeName, key);
        }
        return Collections.emptyList();
    }

    public void clearInput() {
        if (mInput.getValue() == null || mInput.getValue().isEmpty()) {
            return;
        }

        mInput.setValue("");
        clearSearchResult();
    }

    private void clearSearchResult() {
        mSearchResult.setValue(Collections.emptyList());
        mEmptyMessage.setValue("");
    }
}
