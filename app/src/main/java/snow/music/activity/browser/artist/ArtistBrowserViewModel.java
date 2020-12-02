package snow.music.activity.browser.artist;

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

public class ArtistBrowserViewModel extends ViewModel {
    private final MutableLiveData<List<String>> mAllArtist;
    private Disposable mLoadAllArtistDisposable;

    public ArtistBrowserViewModel() {
        mAllArtist = new MutableLiveData<>(Collections.emptyList());
        loadAllArtist();
    }

    @Override
    protected void onCleared() {
        super.onCleared();

        if (mLoadAllArtistDisposable != null && !mLoadAllArtistDisposable.isDisposed()) {
            mLoadAllArtistDisposable.dispose();
        }
    }

    public LiveData<List<String>> getAllArtist() {
        return mAllArtist;
    }

    public String getArtist(int position) {
        return Objects.requireNonNull(mAllArtist.getValue()).get(position);
    }

    private void loadAllArtist() {
        mLoadAllArtistDisposable = Single.create((SingleOnSubscribe<List<String>>) emitter -> {
            List<String> allArtist = MusicStore.getInstance()
                    .getAllArtist();

            Collections.sort(allArtist, new PinyinComparator());

            if (emitter.isDisposed()) {
                return;
            }

            emitter.onSuccess(allArtist);
        }).subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(mAllArtist::setValue);
    }
}
