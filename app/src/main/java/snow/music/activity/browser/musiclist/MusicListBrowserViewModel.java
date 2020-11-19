package snow.music.activity.browser.musiclist;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.google.common.base.Preconditions;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

import io.reactivex.Single;
import io.reactivex.SingleOnSubscribe;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import snow.music.store.MusicList;
import snow.music.store.MusicStore;

public class MusicListBrowserViewModel extends ViewModel {
    private MutableLiveData<List<MusicList>> mAllMusicList;

    private Disposable mLoadMusicListDisposable;

    public MusicListBrowserViewModel() {
        mAllMusicList = new MutableLiveData<>(Collections.emptyList());
        loadAllMusicList();
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        if (mLoadMusicListDisposable != null && !mLoadMusicListDisposable.isDisposed()) {
            mLoadMusicListDisposable.dispose();
        }
    }

    public LiveData<List<MusicList>> getAllMusicList() {
        return mAllMusicList;
    }

    public Single<MusicList> createMusicList(@NonNull String name) {
        Preconditions.checkNotNull(name);

        if (isNameIllegal(name)) {
            throw new IllegalArgumentException("name is illegal.");
        }

        return Single.create(emitter -> {
            MusicList musicList = MusicStore.getInstance().createCustomMusicList(name);

            if (emitter.isDisposed()) {
                return;
            }

            emitter.onSuccess(musicList);
        });
    }

    public MusicList getMusicList(int position) {
        return Objects.requireNonNull(mAllMusicList.getValue()).get(position);
    }

    public void deleteMusicList(@NonNull MusicList musicList) {
        Preconditions.checkNotNull(musicList);

        Single.create((SingleOnSubscribe<Boolean>) emitter -> MusicStore.getInstance().deleteMusicList(musicList)).subscribeOn(Schedulers.io())
                .subscribe();
    }

    public boolean renameMusicList(@NonNull MusicList musicList, @NonNull String newName) {
        Preconditions.checkNotNull(musicList);
        Preconditions.checkNotNull(newName);

        if (isNameIllegal(newName)) {
            return false;
        }

        musicList.setName(newName);
        Single.create((SingleOnSubscribe<Boolean>) emitter -> MusicStore.getInstance().updateMusicList(musicList)).subscribeOn(Schedulers.io())
                .subscribe();

        return true;
    }

    public boolean isNameIllegal(@NonNull String name) {
        Preconditions.checkNotNull(name);

        return name.isEmpty() || MusicStore.getInstance().isMusicListExists(name);
    }

    public void navigateToMusicList(Context context, int position) {
        // TODO
    }

    private void loadAllMusicList() {
        mLoadMusicListDisposable = Single.create((SingleOnSubscribe<List<MusicList>>) emitter -> {
            List<MusicList> allMusicList = MusicStore.getInstance().getAllCustomMusicList();
            if (emitter.isDisposed()) {
                return;
            }
            emitter.onSuccess(allMusicList);
        }).subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(allMusicList -> mAllMusicList.setValue(allMusicList));
    }
}
