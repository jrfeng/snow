package snow.player;

import android.annotation.SuppressLint;
import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.common.base.Preconditions;
import com.tencent.mmkv.MMKV;

import java.util.ArrayList;

import io.reactivex.Single;
import io.reactivex.SingleEmitter;
import io.reactivex.SingleOnSubscribe;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Consumer;
import io.reactivex.schedulers.Schedulers;
import snow.player.media.MusicItem;
import snow.player.playlist.Playlist;
import snow.player.playlist.PlaylistEditor;
import snow.player.playlist.PlaylistManager;

/**
 * 用于获取和设置播放器的播放列表。
 */
class PlaylistManagerImp implements PlaylistManager {
    private static final String TAG = "PlaylistManagerImp";
    private static final String KEY_PLAYLIST = "playlist";
    private static final String KEY_PLAYLIST_SIZE = "playlist_size";

    private PlaylistEditor.OnNewPlaylistListener mNewPlaylistListener;

    private MMKV mMMKV;
    private boolean mEditable;

    private Disposable mSaveDisposable;

    /**
     * 创建一个 PlaylistManagerImp 对象。
     *
     * @param context    {@link Context} 对象，不能为 null
     * @param playlistId 播放列表的 ID，不能为 null。该 ID 会用于持久化保存播放列表，请保证该 ID 的唯一性。
     *                   默认使用 {@link snow.player.PlayerService} 的 {@link Class} 对象的
     *                   {@link Class#getName()} 作为 ID
     */
    PlaylistManagerImp(@NonNull Context context, @NonNull String playlistId, boolean editable) {
        Preconditions.checkNotNull(context);
        Preconditions.checkNotNull(playlistId);

        MMKV.initialize(context);
        mMMKV = MMKV.mmkvWithID("PlaylistManager:" + playlistId, MMKV.MULTI_PROCESS_MODE);
        mEditable = editable;
    }

    public boolean isEditable() {
        return mEditable;
    }

    public void setEditable(boolean editable) {
        mEditable = editable;
    }

    @Override
    public int getPlaylistSize() {
        return mMMKV.decodeInt(KEY_PLAYLIST_SIZE, 0);
    }

    @SuppressLint("CheckResult")
    @Override
    public void getPlaylist(@NonNull final Callback callback) {
        Single.create(new SingleOnSubscribe<Playlist>() {
            @Override
            public void subscribe(SingleEmitter<Playlist> emitter) {
                Playlist playlist = mMMKV.decodeParcelable(KEY_PLAYLIST, Playlist.class);
                if (playlist == null) {
                    playlist = new Playlist(new ArrayList<MusicItem>());
                }
                emitter.onSuccess(playlist);
            }
        }).subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Consumer<Playlist>() {
                    @Override
                    public void accept(Playlist playlist) {
                        callback.onFinished(playlist);
                    }
                });
    }

    public void setPlaylist(@NonNull final Playlist playlist, final int position, final boolean play) {
        Preconditions.checkNotNull(playlist);

        if (!isEditable()) {
            Log.w(TAG, "Playlist not editable.");
            return;
        }

        save(playlist, new Runnable() {
            @Override
            public void run() {
                int p = Math.max(position, 0);
                p = Math.min(p, playlist.size() - 1);
                notifyOnNewPlaylist(playlist.get(p), p, play);
            }
        });
    }

    public void setOnNewPlaylistListener(@Nullable PlaylistEditor.OnNewPlaylistListener listener) {
        mNewPlaylistListener = listener;
    }

    /**
     * 将 Playlist 持久化保存到本地。该方法会异步执行。
     */
    public void save(@NonNull final Playlist playlist, final Runnable doOnSaved) {
        Preconditions.checkNotNull(playlist);

        disposeLastSave();
        mSaveDisposable = Single.create(new SingleOnSubscribe<Boolean>() {
            @Override
            public void subscribe(SingleEmitter<Boolean> emitter) {
                if (emitter.isDisposed()) {
                    return;
                }

                mMMKV.encode(KEY_PLAYLIST, playlist);
                mMMKV.encode(KEY_PLAYLIST_SIZE, playlist.size());

                if (emitter.isDisposed()) {
                    return;
                }
                emitter.onSuccess(true);
            }
        }).subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Consumer<Boolean>() {
                    @Override
                    public void accept(Boolean aBoolean) {
                        if (doOnSaved != null) {
                            doOnSaved.run();
                        }
                    }
                });
    }

    private void disposeLastSave() {
        if (mSaveDisposable != null && !mSaveDisposable.isDisposed()) {
            mSaveDisposable.dispose();
        }
    }

    private void notifyOnNewPlaylist(final MusicItem musicItem, final int position, final boolean play) {
        if (mNewPlaylistListener != null) {
            mNewPlaylistListener.onNewPlaylist(musicItem, position, play);
        }
    }
}
