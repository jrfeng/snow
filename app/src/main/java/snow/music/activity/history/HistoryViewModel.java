package snow.music.activity.history;

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
import snow.music.store.Music;
import snow.music.store.MusicStore;

public class HistoryViewModel extends ViewModel {
    private MutableLiveData<List<Music>> mHistory;
    private Disposable mLoadHistoryDisposable;

    public HistoryViewModel() {
        mHistory = new MutableLiveData<>(Collections.emptyList());
        loadHistory();
    }

    @Override
    protected void onCleared() {
        super.onCleared();

        if (mLoadHistoryDisposable != null && !mLoadHistoryDisposable.isDisposed()) {
            mLoadHistoryDisposable.dispose();
        }
    }

    public LiveData<List<Music>> getHistory() {
        return mHistory;
    }

    public void removeHistory(@NonNull Music music) {
        Preconditions.checkNotNull(music);
        List<Music> musics = Objects.requireNonNull(mHistory.getValue());
        musics.remove(music);
        mHistory.setValue(musics);
        MusicStore.getInstance().removeHistory(music);
    }

    public void clearHistory() {
        mHistory.setValue(Collections.emptyList());
        MusicStore.getInstance().clearHistory();
    }

    private void loadHistory() {
        mLoadHistoryDisposable = Single.create((SingleOnSubscribe<List<Music>>) emitter -> {
            List<Music> history = MusicStore.getInstance().getAllHistory();
            if (emitter.isDisposed()) {
                return;
            }
            Collections.reverse(history);
            emitter.onSuccess(history);
        }).subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(history -> mHistory.setValue(history));
    }
}
