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
import snow.player.PlayerConfig;
import snow.player.media.MusicItem;

/**
 * 该类实现了 “电台” 播放器的基本功能。
 */
public abstract class AbstractRadioStationPlayer extends AbstractPlayer<RadioStationStateListener>
        implements RadioStationPlayer {
    private RadioStationState mRadioStationState;

    private Disposable mDisposable;

    /**
     * @param context           {@link Context} 对象，不能为 null
     * @param playerConfig      {@link PlayerConfig} 对象，保存了播放器的初始配置信息，不能为 null
     * @param radioStationState {@link RadioStationState} 对象，保存了 “电台” 播放器初始状态，不能为 null
     * @param enabled           是否启用当前播放器，如果为 {@code false}，则当前播放器不会响应任何操作
     */
    public AbstractRadioStationPlayer(@NonNull Context context,
                                      @NonNull PlayerConfig playerConfig,
                                      @NonNull RadioStationState radioStationState,
                                      boolean enabled) {
        super(context, playerConfig, radioStationState, enabled);

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

    /**
     * 获取当前电台携带的额外参数。
     *
     * @return 当前电台携带的额外参数，可能为 null
     */
    @Nullable
    public final Bundle getRadioStationExtra() {
        return mRadioStationState.getRadioStation().getExtra();
    }

    @Override
    public boolean isLooping() {
        return false;
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
