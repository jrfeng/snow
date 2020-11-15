package snow.music.fragment.musiclist;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.common.base.Preconditions;

import java.util.Collections;
import java.util.List;

import io.reactivex.Single;
import io.reactivex.SingleOnSubscribe;
import io.reactivex.schedulers.Schedulers;
import snow.music.store.Music;
import snow.music.store.MusicList;
import snow.music.store.MusicStore;

public class MusicListViewModel extends BaseMusicListViewModel {
    @Nullable
    private MusicList mMusicList;

    @Override
    public void init(@NonNull String musicListName) {
        super.init(musicListName);

        if (musicListName.equals(MusicStore.MUSIC_LIST_LOCAL_MUSIC)) {
            MusicStore.getInstance().setOnScanCompleteListener(() -> {
                setIgnoreDiffUtil(true);
                reloadMusicList();
            });
        }
    }

    @Override
    protected void onCleared() {
        super.onCleared();

        if (getMusicListName().equals(MusicStore.MUSIC_LIST_LOCAL_MUSIC)) {
            MusicStore.getInstance().setOnScanCompleteListener(null);
        }
    }

    @NonNull
    @Override
    protected List<Music> loadMusicListItems() {
        if (getMusicListName().equals(MusicStore.MUSIC_LIST_LOCAL_MUSIC)) {
            mMusicList = MusicStore.getInstance().getLocalMusicList();
            MusicStore.getInstance().setOnScanCompleteListener(() -> {
                setIgnoreDiffUtil(true);
                reloadMusicList();
            });
        } else {
            mMusicList = MusicStore.getInstance().getCustomMusicList(getMusicListName());
        }

        if (mMusicList == null) {
            return Collections.emptyList();
        }

        return mMusicList.getMusicElements();
    }

    @Override
    protected void removeMusic(@NonNull Music music) {
        if (mMusicList == null) {
            return;
        }

        mMusicList.getMusicElements().remove(music);
        notifyMusicItemsChanged(mMusicList.getMusicElements());
        Single.create((SingleOnSubscribe<Boolean>) emitter -> {
            MusicStore.getInstance().updateMusicList(mMusicList);
            emitter.onSuccess(true);
        }).subscribeOn(Schedulers.io()).subscribe();

    }

    @Override
    protected void onSortMusicList(@NonNull MusicList.SortOrder sortOrder) {
        Preconditions.checkNotNull(sortOrder);

        if (mMusicList == null) {
            return;
        }

        MusicStore.getInstance().sort(mMusicList, sortOrder, () -> notifyMusicItemsChanged(mMusicList.getMusicElements()));
    }

    @NonNull
    @Override
    protected MusicList.SortOrder getSortOrder() {
        if (mMusicList == null) {
            return MusicList.SortOrder.BY_ADD_TIME;
        }
        return mMusicList.getSortOrder();
    }
}
