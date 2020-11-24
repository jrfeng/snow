package snow.music.activity.history;

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
import snow.music.store.HistoryEntity;
import snow.music.store.Music;
import snow.music.store.MusicStore;

public class HistoryViewModel extends ViewModel {
    private final MutableLiveData<List<HistoryEntity>> mHistory;
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

    public LiveData<List<HistoryEntity>> getHistory() {
        return mHistory;
    }

    @NonNull
    public List<Music> getAllHistoryMusic() {
        List<HistoryEntity> history = mHistory.getValue();
        assert history != null;

        List<Music> musicList = new ArrayList<>(history.size());

        for (HistoryEntity entity : history) {
            musicList.add(entity.getMusic());
        }

        return musicList;
    }

    public void removeHistory(@NonNull HistoryEntity historyEntity) {
        Preconditions.checkNotNull(historyEntity);

        List<HistoryEntity> history = Objects.requireNonNull(mHistory.getValue());
        history.remove(historyEntity);
        mHistory.setValue(history);

        Single.create((SingleOnSubscribe<Boolean>) emitter -> MusicStore.getInstance().removeHistory(historyEntity))
                .subscribeOn(Schedulers.io())
                .subscribe();
    }

    public void clearHistory() {
        mHistory.setValue(Collections.emptyList());

        Single.create((SingleOnSubscribe<Boolean>) emitter -> MusicStore.getInstance().clearHistory())
                .subscribeOn(Schedulers.io())
                .subscribe();
    }

    private void loadHistory() {
        mLoadHistoryDisposable = Single.create((SingleOnSubscribe<List<HistoryEntity>>) emitter -> {
            List<HistoryEntity> history = MusicStore.getInstance().getAllHistory();
            if (emitter.isDisposed()) {
                return;
            }
            emitter.onSuccess(history);
        }).subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(mHistory::setValue);
    }
}
