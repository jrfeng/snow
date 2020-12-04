package snow.music.fragment.musiclist;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.google.common.base.Preconditions;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import io.reactivex.Single;
import io.reactivex.SingleOnSubscribe;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import snow.music.activity.multichoice.MultiChoiceStateHolder;
import snow.music.store.Music;
import snow.music.store.MusicList;
import snow.player.util.MusicItemUtil;

public abstract class BaseMusicListViewModel extends ViewModel {
    private final MutableLiveData<List<Music>> mMusicListItems;
    private final MutableLiveData<Boolean> mLoadingMusicList;
    private String mMusicListName = "";
    private String mMusicListToken = "";

    private boolean mInitialized;
    private Disposable mLoadMusicListDisposable;

    private boolean mIgnoreDiffUtil;

    public BaseMusicListViewModel() {
        mMusicListItems = new MutableLiveData<>(Collections.emptyList());
        mLoadingMusicList = new MutableLiveData<>(false);
    }

    public void init(@NonNull String musicListName) {
        Preconditions.checkNotNull(musicListName);

        if (mInitialized) {
            return;
        }

        mInitialized = true;
        mMusicListName = musicListName;
        loadMusicList();
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        if (!mInitialized) {
            return;
        }

        cancelLastLoading();

        MultiChoiceStateHolder.getInstance()
                .release();
    }

    @NonNull
    public LiveData<List<Music>> getMusicListItems() {
        if (!mInitialized) {
            throw new IllegalStateException("MusicListViewModel not init yet.");
        }

        return mMusicListItems;
    }

    public LiveData<Boolean> getLoadingMusicList() {
        return mLoadingMusicList;
    }

    public void setMusicListItems(@NonNull List<Music> musicListItems) {
        Preconditions.checkNotNull(musicListItems);
        mMusicListItems.setValue(new ArrayList<>(musicListItems));
    }

    @NonNull
    public String getMusicListName() {
        return mMusicListName;
    }

    @NonNull
    public String getMusicListToken() {
        return mMusicListToken;
    }

    public boolean isInitialized() {
        return mInitialized;
    }

    public void setIgnoreDiffUtil(boolean ignoreDiffUtil) {
        mIgnoreDiffUtil = ignoreDiffUtil;
    }

    public boolean consumeIgnoreDiffUtil() {
        boolean result = mIgnoreDiffUtil;
        mIgnoreDiffUtil = false;

        return result;
    }

    /**
     * 加载歌单中的所有歌曲
     */
    private void loadMusicList() {
        cancelLastLoading();

        mLoadingMusicList.setValue(true);
        mLoadMusicListDisposable = Single.create((SingleOnSubscribe<List<Music>>) emitter -> {
            List<Music> musicList = loadMusicListItems();
            if (emitter.isDisposed()) {
                return;
            }
            emitter.onSuccess(musicList);
        }).subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(musicList -> {
                    mLoadingMusicList.setValue(false);
                    notifyMusicItemsChanged(musicList);
                });
    }

    private void cancelLastLoading() {
        if (mLoadMusicListDisposable != null && mLoadMusicListDisposable.isDisposed()) {
            mLoadMusicListDisposable.dispose();
        }
    }

    protected final void reloadMusicList() {
        loadMusicList();
    }

    protected void notifyMusicItemsChanged(@NonNull List<Music> musicListItems) {
        Preconditions.checkNotNull(musicListItems);
        mMusicListToken = MusicItemUtil.generateToken(musicListItems, item -> {
            String uri = item.getUri();
            return uri == null ? "" : uri;
        });
        mMusicListItems.setValue(new ArrayList<>(musicListItems));
    }

    protected int indexOf(Music music) {
        List<Music> musicList = mMusicListItems.getValue();
        return Objects.requireNonNull(musicList).indexOf(music);
    }

    public void sortMusicList(@NonNull MusicList.SortOrder sortOrder) {
        Preconditions.checkNotNull(sortOrder);
        mIgnoreDiffUtil = true;
        onSortMusicList(sortOrder);
    }

    /**
     * 加载歌单中的所有歌曲。
     * <p>
     * 该方法会在异步线程中调用，因此可以直接在该方法中访问数据库。
     *
     * @return 歌单中的所有歌曲，如果歌单为空，则返回一个空列表。
     */
    @NonNull
    protected abstract List<Music> loadMusicListItems();

    /**
     * 要移除歌单中的一首歌曲时会调用该方法。
     *
     * @param music 要移除的歌曲
     */
    protected abstract void removeMusic(@NonNull Music music);

    /**
     * 对歌单中的歌曲进行排序。
     * <p>
     * <b>注意！由于排序歌单会导致 {@link #getMusicListItems()} 返回的 LiveData 中的数据发生改变，
     * 如果你使用 RecyclerView 来展示歌单，那么在排序歌单后更新数据集时，不应该使用 DiffUtil
     * 来通知数据集发生了改变，而应该使用 RecyclerView.Adapter 的 notifyDataSetChanged() 方法，
     * 因为排序歌单可能会导致列表发生复杂变动，此时如果使用 DiffUtil 则可能会因耗时过长而导致 ANR。</b>
     *
     * @param sortOrder 歌单中歌曲的排列顺序。
     */
    protected abstract void onSortMusicList(@NonNull MusicList.SortOrder sortOrder);

    @NonNull
    protected abstract MusicList.SortOrder getSortOrder();
}
