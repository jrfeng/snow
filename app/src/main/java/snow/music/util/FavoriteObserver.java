package snow.music.util;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.common.base.Objects;
import com.google.common.base.Preconditions;

import io.reactivex.Single;
import io.reactivex.SingleOnSubscribe;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import snow.music.store.MusicStore;
import snow.player.audio.MusicItem;

/**
 * 帮助实时监听指定歌曲的 “我喜欢” 状态。
 * <p>
 * 当不在需要 {@link FavoriteObserver} 对象时，请务必调用 {@link #unsubscribe()} 取消订阅，否则会导致内存泄漏。
 */
public class FavoriteObserver {
    private boolean mFavorite;
    @Nullable
    private MusicItem mMusicItem;
    private final OnFavoriteStateChangeListener mListener;

    private boolean mSubscribed;
    private final MusicStore.OnFavoriteChangeListener mFavoriteChangeListener;
    private Disposable mCheckFavoriteDisposable;

    /**
     * 创建一个 {@link FavoriteObserver} 对象。
     *
     * @param listener 歌曲的 “我喜欢” 状态监听器，不能为 null
     */
    public FavoriteObserver(@NonNull OnFavoriteStateChangeListener listener) {
        Preconditions.checkNotNull(listener);
        mListener = listener;

        mFavoriteChangeListener = this::checkMusicFavoriteState;
    }

    /**
     * 设置要监听的歌曲。
     *
     * @param musicItem 要监听的歌曲，为 null 时不监听任何歌曲。
     */
    public synchronized void setMusicItem(@Nullable MusicItem musicItem) {
        if (Objects.equal(musicItem, mMusicItem)) {
            return;
        }

        mMusicItem = musicItem;
        checkMusicFavoriteState();
    }

    /**
     * 获取当前监听的歌曲。
     *
     * @return 当前监听的歌曲，可能为 null
     */
    @Nullable
    public synchronized MusicItem getMusicItem() {
        return mMusicItem;
    }

    /**
     * 歌曲是否处于 “我喜欢” 状态。
     *
     * @return 如果歌曲是 “我喜欢”，则返回 true，否则返回 false
     */
    public synchronized boolean isFavorite() {
        return mFavorite;
    }

    /**
     * 开始订阅歌曲的 “我喜欢” 状态。
     */
    public void subscribe() {
        if (mSubscribed) {
            return;
        }

        mSubscribed = true;
        MusicStore.getInstance().addOnFavoriteChangeListener(mFavoriteChangeListener);
    }

    /**
     * 取消订阅歌曲的 “我喜欢” 状态。
     */
    public void unsubscribe() {
        mSubscribed = false;
        disposeCheckFavorite();
        MusicStore.getInstance().removeOnFavoriteChangeListener(mFavoriteChangeListener);
    }

    private synchronized void setFavorite(boolean favorite) {
        mFavorite = favorite;
        mListener.onFavoriteStateChanged(favorite);
    }

    private void checkMusicFavoriteState() {
        if (mMusicItem == null) {
            setFavorite(false);
            return;
        }

        disposeCheckFavorite();

        mCheckFavoriteDisposable = Single.create((SingleOnSubscribe<Boolean>) emitter -> {
            MusicItem musicItem = getMusicItem();

            boolean result;
            if (musicItem == null) {
                result = false;
            } else {
                result = MusicStore.getInstance().isFavorite(MusicUtil.getId(mMusicItem));
            }

            if (emitter.isDisposed()) {
                return;
            }

            emitter.onSuccess(result);
        }).subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(this::setFavorite);
    }

    private void disposeCheckFavorite() {
        if (mCheckFavoriteDisposable != null) {
            mCheckFavoriteDisposable.dispose();
        }
    }

    /**
     * 歌曲的 “我喜欢” 状态监听器。
     */
    public interface OnFavoriteStateChangeListener {
        /**
         * 该方法会在歌曲的 “我喜欢” 状态改变时调用。
         * <p>
         * 该回调方法会在应用程序主线程调用。
         *
         * @param favorite 如果歌曲是 “我喜欢”，则该参数为 true，否则为 false
         */
        void onFavoriteStateChanged(boolean favorite);
    }
}
