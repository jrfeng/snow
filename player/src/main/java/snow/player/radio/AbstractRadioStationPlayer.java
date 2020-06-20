package snow.player.radio;

import android.content.Context;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.HashMap;

import io.reactivex.Single;
import io.reactivex.SingleEmitter;
import io.reactivex.SingleOnSubscribe;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Consumer;
import io.reactivex.schedulers.Schedulers;
import snow.player.AbstractPlayer;
import snow.player.media.MusicItem;
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
     * @return “电台” 的下一首音乐（返回 null 时会停止播放）
     */
    @Nullable
    protected abstract MusicItem getNextMusicItem(RadioStation radioStation);

    @Nullable
    public final Bundle getRadioStationExtra() {
        return mRadioStationState.getRadioStation().getExtra();
    }

    @Override
    protected void onPlayComplete(MusicItem musicItem) {
        super.onPlayComplete(musicItem);
        skipToNext();
    }

    private Single<MusicItem> getNextMusicItemAsync(final RadioStation radioStation) {
        return Single.create(new SingleOnSubscribe<MusicItem>() {
            @Override
            public void subscribe(SingleEmitter<MusicItem> emitter) {
                MusicItem musicItem = getNextMusicItem(radioStation);

                if (emitter.isDisposed()) {
                    return;
                }

                if (musicItem == null) {
                    emitter.onError(new UnsupportedOperationException("Music Item is null."));
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
                    public void accept(MusicItem musicItem) {
                        notifyPlayingMusicItemChanged(musicItem, true);
                    }
                }, new Consumer<Throwable>() {
                    @Override
                    public void accept(Throwable throwable) {
                        throwable.printStackTrace();
                        notifyPlayingMusicItemChanged(null, false);
                    }
                });
    }

    @Override
    public void setRadioStation(RadioStation radioStation) {
        notifyRadioStationChanged(radioStation);
        skipToNext();
    }
}
