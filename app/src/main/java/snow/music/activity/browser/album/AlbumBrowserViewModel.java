package snow.music.activity.browser.album;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

import io.reactivex.Single;
import io.reactivex.SingleOnSubscribe;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import pinyin.util.PinyinComparator;
import snow.music.store.MusicStore;

public class AlbumBrowserViewModel extends ViewModel {
    private final MutableLiveData<List<String>> mAllAlbum;
    private Disposable mLoadAllAlbumDisposable;

    public AlbumBrowserViewModel() {
        mAllAlbum = new MutableLiveData<>(Collections.emptyList());
        loadAllAlbum();
    }

    @Override
    protected void onCleared() {
        super.onCleared();

        if (mLoadAllAlbumDisposable != null && !mLoadAllAlbumDisposable.isDisposed()) {
            mLoadAllAlbumDisposable.dispose();
        }
    }

    public LiveData<List<String>> getAllAlbum() {
        return mAllAlbum;
    }

    public String getAlbum(int position) {
        return Objects.requireNonNull(mAllAlbum.getValue()).get(position);
    }

    private void loadAllAlbum() {
        mLoadAllAlbumDisposable = Single.create((SingleOnSubscribe<List<String>>) emitter -> {
            List<String> allAlbum = MusicStore.getInstance()
                    .getAllAlbum();

            Collections.sort(allAlbum, new PinyinComparator());

            if (emitter.isDisposed()) {
                return;
            }

            emitter.onSuccess(allAlbum);
        }).subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(mAllAlbum::setValue);
    }
}
