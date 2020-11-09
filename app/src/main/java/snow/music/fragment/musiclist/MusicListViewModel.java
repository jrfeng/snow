package snow.music.fragment.musiclist;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Collections;
import java.util.List;

import io.reactivex.Single;
import io.reactivex.SingleOnSubscribe;
import io.reactivex.schedulers.Schedulers;
import snow.music.store.Music;
import snow.music.store.MusicList;
import snow.music.store.MusicStore;
import snow.music.util.MusicListUtil;

public class MusicListViewModel extends BaseMusicListViewModel {
    @Nullable
    private MusicList mMusicList;

    @NonNull
    @Override
    protected List<Music> loadMusicListItems() {
        if (getMusicListName().equals(MusicStore.MUSIC_LIST_LOCAL_MUSIC)) {
            mMusicList = MusicStore.getInstance().getLocalMusicList();
        } else {
            mMusicList = MusicStore.getInstance().getCustomMusicList(getMusicListName());
        }

        if (mMusicList == null) {
            return Collections.emptyList();
        }

        mMusicList.load();
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
    protected void sortMusicList(@NonNull MusicList.SortOrder sortOrder) {
        if (mMusicList == null) {
            return;
        }

        MusicListUtil.sort(mMusicList, sortOrder);
        notifyMusicItemsChanged(mMusicList.getMusicElements());
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
