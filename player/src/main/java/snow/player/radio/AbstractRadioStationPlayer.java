package snow.player.radio;

import android.content.Context;

import androidx.annotation.NonNull;

import java.util.HashMap;

import io.reactivex.Single;
import io.reactivex.SingleEmitter;
import io.reactivex.SingleOnSubscribe;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Consumer;
import io.reactivex.schedulers.Schedulers;
import snow.player.AbstractPlayer;
import snow.player.MusicItem;
import snow.player.state.RadioStationState;
import snow.player.state.RadioStationStateListener;

public abstract class AbstractRadioStationPlayer extends AbstractPlayer<RadioStationStateListener>
        implements RadioStationPlayer {
    private RadioStationState mRadioStationState;

    private Disposable mDisposable;

    public AbstractRadioStationPlayer(@NonNull Context context, @NonNull RadioStationState radioStationState) {
        super(context, radioStationState);

        mRadioStationState = radioStationState;
    }

    private void notifyRadioStationChanged(RadioStation radioStation) {
        mRadioStationState.setRadioStation(radioStation);

        HashMap<String, RadioStationStateListener> listenerMap = getAllStateListener();

        for (String key : listenerMap.keySet()) {
            RadioStationStateListener listener = listenerMap.get(key);
            if (listener != null) {
                listener.onRadioStationChanged(radioStation);
            }
        }
    }

    /**
     * 获取 “电台” 的下一首音乐。
     * <p>
     * 该方法会在异步线程中调用。
     *
     * @param radioStation 用于表示电台的 RadioStation 对象
     * @return “电台” 的下一首音乐（不能为 null）
     */
    @NonNull
    protected abstract MusicItem getNextMusicItem(RadioStation radioStation);

    @Override
    protected void onPlayComplete(MusicItem musicItem) {
        super.onPlayComplete(musicItem);
        skipToNext();
    }

    private Single<MusicItem> getNextMusicItemAsync(final RadioStation radioStation) {
        return Single.create(new SingleOnSubscribe<MusicItem>() {
            @Override
            public void subscribe(SingleEmitter<MusicItem> emitter) throws Exception {
                MusicItem musicItem = getNextMusicItem(radioStation);

                if (emitter.isDisposed()) {
                    return;
                }

                emitter.onSuccess(musicItem);
            }
        });
    }

    @Override
    protected void onRelease() {
        disposeLastGetNextMusicItem();
    }

    private void disposeLastGetNextMusicItem() {
        if (mDisposable != null) {
            mDisposable.dispose();
        }
    }

    @Override
    public void skipToNext() {
        disposeLastGetNextMusicItem();

        mDisposable = getNextMusicItemAsync(mRadioStationState.getRadioStation())
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Consumer<MusicItem>() {
                    @Override
                    public void accept(MusicItem musicItem) throws Exception {
                        notifyPlayingMusicItemChanged(musicItem, true);
                    }
                });
    }

    @Override
    public void setRadioStation(RadioStation radioStation) {
        notifyRadioStationChanged(radioStation);
        skipToNext();
    }
}
