package snow.player.playlist;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.common.base.Preconditions;
import com.tencent.mmkv.MMKV;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import snow.player.MusicItem;

/**
 * 用于管理音乐播放器的播放队列。
 */
public class PlaylistManager {
    private static final String KEY_PLAYLIST = "playlist";

    private MMKV mMMKV;
    private Executor mExecutor;

    @Nullable
    private OnModifyPlaylistListener mListener;

    private PlaylistManager(Context context, String playlistId) {
        MMKV.initialize(context);
        mMMKV = MMKV.mmkvWithID(playlistId, MMKV.MULTI_PROCESS_MODE);
        mExecutor = Executors.newCachedThreadPool();
    }

    /**
     * 创建一个新的 PlaylistManager 对象。
     *
     * @param context    Context 对象
     * @param playlistId 播放队列的 ID。请保持该值的唯一性，通常使用的是当前 PlaylistService 的名称，建议使用
     *                   Class&lt;? extends PlayerService&gt;.getCanonicalName()
     */
    public static PlaylistManager newInstance(Context context, String playlistId) {
        return new PlaylistManager(context, playlistId);
    }

    /**
     * 设置一个 {@link OnModifyPlaylistListener} 监听器，该监听器会在使用当前 PlaylistManager 修改播放队列
     * 时被调用。
     *
     * @param listener {@link OnModifyPlaylistListener} 监听器，为 null 时相当于青春已设置的监听器
     */
    public void setOnModifyPlaylistListener(@Nullable OnModifyPlaylistListener listener) {
        mListener = listener;
    }

    /**
     * 获取当前播放队列。
     * <p>
     * 注意！该方法会进行 I/O 操作，因此不建议在 UI 线程中执行。
     *
     * @return 当前播放队列。
     */
    @NonNull
    public Playlist getPlaylist() {
        Playlist playlist = mMMKV.decodeParcelable(KEY_PLAYLIST, Playlist.class);
        if (playlist == null) {
            return new Playlist(new ArrayList<MusicItem>());
        }

        return playlist;
    }

    /**
     * 以异步的方式获取当前播放队列。
     */
    public void getPlaylistAsync(final Callback callback) {
        mExecutor.execute(new Runnable() {
            @Override
            public void run() {
                callback.onFinished(getPlaylist());
            }
        });
    }

    /**
     * 设置新的播放列表。
     *
     * @param playlist 新的播放列表（不能为 null）
     */
    public void setPlaylist(@NonNull Playlist playlist) {
        Preconditions.checkNotNull(playlist);
        setPlaylist(playlist, 0, false);
    }

    /**
     * 设置新的播放列表，并将播放队列的播放位置设为 position 值。
     *
     * @param playlist 新的播放列表（不能为 null）
     * @param position 要设置的播放位置值（小于 0 时，相当于设为 0）
     */
    public void setPlaylist(@NonNull Playlist playlist, int position) {
        Preconditions.checkNotNull(playlist);
        setPlaylist(playlist, position, false);
    }

    /**
     * 设置新的播放列表，并将播放队列的播放位置设为 position 值，同时设置是否在 prepare 完成后自动播放音乐。
     *
     * @param playlist       新的播放列表（不能为 null）
     * @param position       要设置的播放位置值（小于 0 时，相当于设为 0）
     * @param playOnPrepared 否在 prepare 完成后自动播放音乐
     */
    public void setPlaylist(@NonNull final Playlist playlist, final int position, final boolean playOnPrepared) {
        Preconditions.checkNotNull(playlist);
        mExecutor.execute(new Runnable() {
            @Override
            public void run() {
                save(playlist);
                notifyPlaylistSwapped(Math.max(position, 0), playOnPrepared);
            }
        });
    }

    /**
     * 将播放队列中 fromPosition 位置处的 MusicItem 对象移动到 toPosition 位置。
     */
    public void moveMusicItem(final int fromPosition, final int toPosition) {
        if (fromPosition == toPosition) {
            return;
        }

        mExecutor.execute(new Runnable() {
            @Override
            public void run() {
                List<MusicItem> items = getPlaylist().getAllMusicItem();

                MusicItem from = items.remove(fromPosition);
                items.add(toPosition, from);

                save(new Playlist(items));
                notifyMusicItemMoved(fromPosition, toPosition);
            }
        });
    }

    /**
     * 将单个 MusicItem 对象（不能为 null）插入到播放队列的 position 位置处。
     */
    public void insertMusicItem(final int position, @NonNull final MusicItem musicItem) {
        Preconditions.checkNotNull(musicItem);

        mExecutor.execute(new Runnable() {
            @Override
            public void run() {
                List<MusicItem> items = getPlaylist().getAllMusicItem();

                items.add(position, musicItem);

                save(new Playlist(items));
                notifyMusicItemInserted(position, 1);
            }
        });
    }

    /**
     * 将多个 MusicItem 对象（不能为 null）插入到播放队列的 position 位置处。
     */
    public void insertAllMusicItem(final int position, @NonNull final List<MusicItem> musicItems) {
        Preconditions.checkNotNull(musicItems);

        mExecutor.execute(new Runnable() {
            @Override
            public void run() {
                List<MusicItem> items = getPlaylist().getAllMusicItem();

                items.addAll(position, musicItems);

                save(new Playlist(items));
                notifyMusicItemInserted(position, musicItems.size());
            }
        });
    }

    /**
     * 移除播放队列中指定位置处的音乐。
     */
    public void removeMusicItem(final List<Integer> positions) {
        if (positions.size() < 1) {
            return;
        }

        mExecutor.execute(new Runnable() {
            @Override
            public void run() {
                List<MusicItem> items = getPlaylist().getAllMusicItem();

                items.removeAll(getSubList(items, positions));

                save(new Playlist(items));
                notifyMusicItemRemoved(positions);
            }
        });
    }

    private List<MusicItem> getSubList(List<MusicItem> items, List<Integer> positions) {
        List<MusicItem> subList = new ArrayList<>(positions.size());
        for (int position : positions) {
            subList.add(items.get(position));
        }
        return subList;
    }

    private void notifyPlaylistSwapped(int position, boolean playOnPrepared) {
        if (mListener != null) {
            mListener.onPlaylistSwapped(position, playOnPrepared);
        }
    }

    private void notifyMusicItemMoved(int fromPosition, int toPosition) {
        if (mListener != null) {
            mListener.onMusicItemMoved(fromPosition, toPosition);
        }
    }

    private void notifyMusicItemInserted(int position, int count) {
        if (mListener != null) {
            mListener.onMusicItemInserted(position, count);
        }
    }

    private void notifyMusicItemRemoved(List<Integer> positions) {
        if (mListener != null) {
            mListener.onMusicItemRemoved(positions);
        }
    }

    private void save(@NonNull Playlist playlist) {
        Preconditions.checkNotNull(playlist);
        mMMKV.encode(KEY_PLAYLIST, playlist);
    }

    public interface Callback {
        void onFinished(@NonNull Playlist playlist);
    }

    public interface OnModifyPlaylistListener {
        void onPlaylistSwapped(int position, boolean playOnPrepared);

        void onMusicItemMoved(int fromPosition, int toPosition);

        void onMusicItemInserted(int position, int count);

        void onMusicItemRemoved(List<Integer> positions);
    }
}
