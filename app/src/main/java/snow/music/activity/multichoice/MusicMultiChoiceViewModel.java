package snow.music.activity.multichoice;

import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModel;

import com.google.common.base.Preconditions;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import io.reactivex.Single;
import io.reactivex.schedulers.Schedulers;
import snow.music.store.Music;
import snow.music.store.MusicStore;

public class MusicMultiChoiceViewModel extends ViewModel {
    private List<Integer> mAllSelectedPosition;

    private boolean mInitialized;

    public MusicMultiChoiceViewModel() {
        mAllSelectedPosition = Collections.emptyList();
    }

    public void init(@NonNull List<Music> musicList, int position) {
        Preconditions.checkNotNull(musicList);

        if (mInitialized) {
            return;
        }

        mInitialized = true;
        mAllSelectedPosition = new ArrayList<>();
        mAllSelectedPosition.add(position);
    }

    public boolean isInitialized() {
        return mInitialized;
    }

    public void setAllSelectedPosition(List<Integer> allSelectedPosition) {
        mAllSelectedPosition = allSelectedPosition;
    }

    public List<Integer> getAllSelectedPosition() {
        return mAllSelectedPosition;
    }

    public void addToMusicList(String musicListName, List<Music> allMusic) {
        Single.create(emitter ->
                MusicStore.getInstance()
                        .addAllMusic(musicListName, allMusic)
        ).subscribeOn(Schedulers.io())
                .subscribe();
    }

    public void addToFavorite(List<Music> allMusic) {
        Single.create(emitter ->
                MusicStore.getInstance()
                        .addAllMusic(MusicStore.MUSIC_LIST_FAVORITE, allMusic)
        ).subscribeOn(Schedulers.io())
                .subscribe();
    }

    public void remove(String musicListName, List<Music> allMusic) {
        Single.create(emitter ->
                MusicStore.getInstance()
                        .removeAllMusic(musicListName, allMusic)
        ).subscribeOn(Schedulers.io())
                .subscribe();
    }
}
