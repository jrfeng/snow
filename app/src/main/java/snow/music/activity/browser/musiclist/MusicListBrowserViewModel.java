package snow.music.activity.browser.musiclist;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.google.common.base.Preconditions;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

import io.reactivex.Single;
import io.reactivex.SingleOnSubscribe;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import pinyin.util.PinyinComparator;
import snow.music.activity.detail.musiclist.MusicListDetailActivity;
import snow.music.store.MusicList;
import snow.music.store.MusicStore;

public class MusicListBrowserViewModel extends ViewModel {
    private final MutableLiveData<List<MusicList>> mAllMusicList;
    private final Comparator<MusicList> mMusicListComparator;

    private Disposable mLoadMusicListDisposable;
    private Disposable mCreateMusicListDisposable;

    private MusicStore.OnCustomMusicListUpdateListener mOnCustomMusicListUpdateListener;
    private Disposable mReloadMusicListDisposable;

    public MusicListBrowserViewModel() {
        mAllMusicList = new MutableLiveData<>(Collections.emptyList());
        mMusicListComparator = new Comparator<MusicList>() {
            private PinyinComparator pinyinComparator = new PinyinComparator();
            @Override
            public int compare(MusicList o1, MusicList o2) {
                return pinyinComparator.compare(o1.getName(), o2.getName());
            }
        };

        loadAllMusicList();

        mOnCustomMusicListUpdateListener = this::reloadMusicList;
        MusicStore.getInstance().addOnCustomMusicListUpdateListener(mOnCustomMusicListUpdateListener);
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        if (mLoadMusicListDisposable != null && !mLoadMusicListDisposable.isDisposed()) {
            mLoadMusicListDisposable.dispose();
        }

        if (mCreateMusicListDisposable != null && !mCreateMusicListDisposable.isDisposed()) {
            mCreateMusicListDisposable.dispose();
        }

        cancelReloadMusicList();
        MusicStore.getInstance().removeOnCustomMusicListUpdateListener(mOnCustomMusicListUpdateListener);
    }

    public LiveData<List<MusicList>> getAllMusicList() {
        return mAllMusicList;
    }

    public void createMusicList(@NonNull String name) {
        Preconditions.checkNotNull(name);

        if (isNameIllegal(name)) {
            throw new IllegalArgumentException("name is illegal.");
        }

        mCreateMusicListDisposable = Single.create((SingleOnSubscribe<MusicList>) emitter -> {
            MusicList musicList = MusicStore.getInstance().createCustomMusicList(name);

            if (emitter.isDisposed()) {
                return;
            }

            emitter.onSuccess(musicList);
        }).subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(musicList -> {
                    List<MusicList> allMusicList = mAllMusicList.getValue();
                    assert allMusicList != null;
                    allMusicList = new ArrayList<>(allMusicList);   // getValue() 返回的 List 不支持 add 操作

                    allMusicList.add(musicList);
                    Collections.sort(allMusicList, mMusicListComparator);
                    mAllMusicList.setValue(allMusicList);
                });
    }

    public MusicList getMusicList(int position) {
        return Objects.requireNonNull(mAllMusicList.getValue()).get(position);
    }

    public void deleteMusicList(@NonNull MusicList musicList) {
        Preconditions.checkNotNull(musicList);

        Single.create((SingleOnSubscribe<Boolean>) emitter -> MusicStore.getInstance().deleteMusicList(musicList)).subscribeOn(Schedulers.io())
                .subscribe();

        List<MusicList> allMusicList = new ArrayList<>(Objects.requireNonNull(mAllMusicList.getValue()));
        allMusicList.remove(musicList);

        mAllMusicList.setValue(allMusicList);
    }

    public void renameMusicList(@NonNull MusicList musicList, @NonNull String newName) {
        Preconditions.checkNotNull(musicList);
        Preconditions.checkNotNull(newName);

        if (isNameIllegal(newName)) {
            return;
        }

        Single.create((SingleOnSubscribe<Boolean>) emitter ->
                MusicStore.getInstance().renameMusicList(musicList, newName))
                .subscribeOn(Schedulers.io())
                .subscribe();
    }

    public boolean isNameIllegal(@NonNull String name) {
        Preconditions.checkNotNull(name);

        return name.isEmpty() || MusicStore.getInstance().isNameExists(name);
    }

    public void navigateToMusicList(Context context, int position) {
        MusicListDetailActivity.start(context, getMusicList(position).getName());
    }

    private void loadAllMusicList() {
        mLoadMusicListDisposable = Single.create((SingleOnSubscribe<List<MusicList>>) emitter -> {
            List<MusicList> allMusicList = MusicStore.getInstance().getAllCustomMusicList();
            if (emitter.isDisposed()) {
                return;
            }
            Collections.sort(allMusicList, mMusicListComparator);
            emitter.onSuccess(allMusicList);
        }).subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(mAllMusicList::setValue);
    }

    private void reloadMusicList(String name) {
        cancelReloadMusicList();
        mReloadMusicListDisposable = Single.create((SingleOnSubscribe<MusicList>) emitter -> {
            MusicList musicList = MusicStore.getInstance().getCustomMusicList(name);
            if (musicList == null || emitter.isDisposed()) {
                return;
            }
            emitter.onSuccess(musicList);
        }).subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(this::updateMusicList);
    }

    private void cancelReloadMusicList() {
        if (mReloadMusicListDisposable != null && !mReloadMusicListDisposable.isDisposed()) {
            mReloadMusicListDisposable.dispose();
        }
    }

    private void updateMusicList(MusicList musicList) {
        List<MusicList> allMusicList = new ArrayList<>(Objects.requireNonNull(mAllMusicList.getValue()));

        int index = allMusicList.indexOf(musicList);
        allMusicList.remove(index);
        allMusicList.add(index, musicList);

        mAllMusicList.setValue(allMusicList);
    }
}
