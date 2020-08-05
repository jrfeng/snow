package snow.player.playlist;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.common.base.Preconditions;
import com.google.common.hash.Hashing;
import com.tencent.mmkv.MMKV;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import snow.player.media.MusicItem;

/**
 * 用于管理音乐播放器的播放队列。
 */
public abstract class PlaylistManager {
    private static final String KEY_PLAYLIST = "playlist";
    private static final String KEY_PLAYLIST_SIZE = "playlist_size";
    private static final String KEY_PLAYLIST_TAG = "playlist_tag";

    private MMKV mMMKV;
    private Executor mExecutor;
    private boolean mEditable;

    private Handler mMainHandler;

    private String mPlaylistTag;
    private Playlist mPlaylist;

    @Nullable
    private OnModifyPlaylistListener mModifyPlaylistListener;

    /**
     * 创建一个 {@link PlaylistManager} 对象。
     *
     * @param context    {@link Context} 对象，不能为 null
     * @param playlistId 播放列表的 ID，不能为 null。该 ID 会用于持久化保存播放列表，请保证该 ID 的唯一性。
     *                   默认使用 {@link snow.player.PlayerService} 的 {@link Class} 对象的
     *                   {@link Class#getName()} 作为 ID
     */
    public PlaylistManager(@NonNull Context context, @NonNull String playlistId) {
        Preconditions.checkNotNull(context);
        Preconditions.checkNotNull(playlistId);

        MMKV.initialize(context);
        mMMKV = MMKV.mmkvWithID(playlistId, MMKV.MULTI_PROCESS_MODE);
        mExecutor = Executors.newSingleThreadExecutor();
        mEditable = false;
        mMainHandler = new Handler(Looper.getMainLooper());

        mPlaylistTag = "";
    }

    /**
     * 创建一个 PlaylistManager 对象。该 PlaylistManager 对象无法修改 Playlist，仅能用于获取 Playlist。
     * <p>
     * 也就是说，无法调用 {@link #setPlaylist(Playlist, int, boolean)}、
     * {@link #insertMusicItem(int, MusicItem)}、{@link #removeMusicItem(MusicItem)}、
     * {@link #moveMusicItem(int, int)} 方法修改 Playlist。即使调用了这些方法也无效。
     *
     * @param context    Context 对象
     * @param playlistId 播放列表的 ID，不能为 null。该 ID 会用于持久化保存播放列表，请保证该 ID 的唯一性。
     *                   默认使用 {@link snow.player.PlayerService} 的 {@link Class} 对象的
     *                   {@link Class#getName()} 作为 ID
     * @return PlaylistManager 对象
     */
    public static PlaylistManager newInstance(@NonNull Context context, @NonNull String playlistId) {
        return new PlaylistManager(context, playlistId) {
        };
    }

    /**
     * 判断当前 PlaylistManager 是否是可编辑的。
     *
     * @return 如果当前 PlaylistManager 是可编辑的，则返回 true，否则返回 false。当返回 false 时，
     * 对 Playlist 的一切修改操作都会被忽略（可以正常访问）。
     */
    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    public final boolean isEditable() {
        return mEditable;
    }

    /**
     * 设置当前播放列表是否是可编辑的。
     * <p>
     * 这是一个 {@code protected} 方法，不可直接访问，需要继承 {@link PlaylistManager} 类才能访问到该方法。
     */
    protected void setEditable(boolean editable) {
        mEditable = editable;
    }

    /**
     * 设置一个 {@link OnModifyPlaylistListener} 监听器，该监听器会在使用当前 PlaylistManager 修改播放队列
     * 时被调用。
     *
     * @param listener {@link OnModifyPlaylistListener} 监听器，为 null 时相当于青春已设置的监听器
     */
    public void setOnModifyPlaylistListener(@Nullable OnModifyPlaylistListener listener) {
        mModifyPlaylistListener = listener;
    }

    /**
     * 获取当前播放队列的大小。
     * <p>
     * 这是个轻量级操作，可在 UI 线程上直接运行。
     *
     * @return 当前播放队列的大小。
     */
    public int getPlaylistSize() {
        return mMMKV.decodeInt(KEY_PLAYLIST_SIZE, 0);
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
        if (tagNotChanged()) {
            return mPlaylist;
        }

        Playlist playlist = mMMKV.decodeParcelable(KEY_PLAYLIST, Playlist.class);
        if (playlist == null) {
            playlist = new Playlist(new ArrayList<MusicItem>());
        }

        mPlaylistTag = mMMKV.decodeString(KEY_PLAYLIST_TAG, "");
        mPlaylist = playlist;

        return playlist;
    }

    private boolean tagNotChanged() {
        if (mPlaylistTag.isEmpty()) {
            return false;
        }

        return mPlaylistTag.equals(mMMKV.decodeString(KEY_PLAYLIST_TAG, ""));
    }

    /**
     * 以异步的方式获取当前播放队列。
     * <p>
     * 回调接口会在主线程上调用。
     */
    public void getPlaylistAsync(final Callback callback) {
        mExecutor.execute(new Runnable() {
            @Override
            public void run() {
                mMainHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        callback.onFinished(getPlaylist());
                    }
                });
            }
        });
    }

    /**
     * 设置新的播放列表，并将播放队列的播放位置设为 position 值，同时设置是否在 prepare 完成后自动播放音乐。
     *
     * @param playlist 新的播放列表（不能为 null）
     * @param position 要设置的播放位置值（小于 0 时，相当于设为 0）
     * @param play     否在自动播放 position 位置处的音乐
     */
    public void setPlaylist(@NonNull final Playlist playlist,
                            final int position,
                            final boolean play) {
        Preconditions.checkNotNull(playlist);

        if (!isEditable()) {
            return;
        }

        mExecutor.execute(new Runnable() {
            @Override
            public void run() {
                save(playlist);
                notifyOnSetNewPlaylist(Math.max(position, 0), play);
            }
        });
    }

    /**
     * 往列表中插入了一首新的歌曲。
     * <p>
     * 如果播放列表中已包含指定歌曲，则会将它移动到 position 位置，如果不存在，则会将歌曲插入到 position 位置。
     *
     * @param position  歌曲插入的位置
     * @param musicItem 要插入的歌曲，不能为 null
     */
    public void insertMusicItem(final int position, @NonNull final MusicItem musicItem) {
        Preconditions.checkNotNull(musicItem);

        if (!isEditable()) {
            return;
        }

        mExecutor.execute(new Runnable() {
            @Override
            public void run() {
                List<MusicItem> musicItems = getPlaylist().getAllMusicItem();
                if (musicItems.contains(musicItem)) {
                    moveMusicItem(musicItems.indexOf(musicItem), position);
                    return;
                }

                musicItems.add(position, musicItem);

                save(new Playlist(musicItems));
                notifyMusicItemInserted(position, musicItem);
            }
        });
    }

    /**
     * 移动播放列表中某首歌曲的位置。
     *
     * @param fromPosition 歌曲在列表中的位置
     * @param toPosition   歌曲要移动到的位置。如果 {@code toPosition == fromPosition}，则会忽略本次调用
     */
    public void moveMusicItem(final int fromPosition, final int toPosition) {
        if (fromPosition == toPosition) {
            return;
        }

        if (!isEditable()) {
            return;
        }

        mExecutor.execute(new Runnable() {
            @Override
            public void run() {
                List<MusicItem> musicItems = getPlaylist().getAllMusicItem();

                MusicItem from = musicItems.remove(fromPosition);
                musicItems.add(toPosition, from);

                save(new Playlist(musicItems));
                notifyMusicItemMoved(fromPosition, toPosition);
            }
        });
    }

    /**
     * 从播放列表中移除了指定歌曲。
     *
     * @param musicItem 要移除的歌曲。如果播放列表中不包含该歌曲，则忽略本次调用
     */
    public void removeMusicItem(@NonNull final MusicItem musicItem) {
        Preconditions.checkNotNull(musicItem);

        if (!isEditable()) {
            return;
        }

        mExecutor.execute(new Runnable() {
            @Override
            public void run() {
                List<MusicItem> musicItems = getPlaylist().getAllMusicItem();
                if (!musicItems.contains(musicItem)) {
                    return;
                }

                musicItems.remove(musicItem);

                save(new Playlist(musicItems));
                notifyMusicItemRemoved(musicItem);
            }
        });
    }

    @SuppressWarnings("UnstableApiUsage")
    private void save(@NonNull Playlist playlist) {
        Preconditions.checkNotNull(playlist);

        mPlaylist = playlist;
        mPlaylistTag = Hashing.sha256()
                .newHasher()
                .putLong(System.currentTimeMillis())
                .putLong(playlist.hashCode())
                .hash()
                .toString();

        mMMKV.encode(KEY_PLAYLIST, playlist);
        mMMKV.encode(KEY_PLAYLIST_SIZE, playlist.size());
        mMMKV.encode(KEY_PLAYLIST_TAG, mPlaylistTag);
    }

    private void notifyOnSetNewPlaylist(final int position, final boolean play) {
        mMainHandler.post(new Runnable() {
            @Override
            public void run() {
                if (mModifyPlaylistListener != null) {
                    mModifyPlaylistListener.onNewPlaylist(position, play);
                }
            }
        });
    }

    private void notifyMusicItemMoved(final int fromPosition, final int toPosition) {
        mMainHandler.post(new Runnable() {
            @Override
            public void run() {
                if (mModifyPlaylistListener != null) {
                    mModifyPlaylistListener.onMusicItemMoved(fromPosition, toPosition);
                }
            }
        });
    }

    private void notifyMusicItemInserted(final int position, final MusicItem musicItem) {
        mMainHandler.post(new Runnable() {
            @Override
            public void run() {
                if (mModifyPlaylistListener != null) {
                    mModifyPlaylistListener.onMusicItemInserted(position, musicItem);
                }
            }
        });
    }

    private void notifyMusicItemRemoved(final MusicItem musicItem) {
        mMainHandler.post(new Runnable() {
            @Override
            public void run() {
                if (mModifyPlaylistListener != null) {
                    mModifyPlaylistListener.onMusicItemRemoved(musicItem);
                }
            }
        });
    }

    public interface Callback {
        void onFinished(@NonNull Playlist playlist);
    }

    /**
     * 用于监听播放列表的修改事件。
     */
    public interface OnModifyPlaylistListener {

        /**
         * 设置了一个新的播放列表。
         *
         * @param position 将播放列表的播放位置设置为 position，不能小于 0
         * @param play     是否立即播放 position 处的音乐
         */
        void onNewPlaylist(int position, boolean play);

        /**
         * 列表中某首歌曲的位置被移动了。
         *
         * @param fromPosition 歌曲原来在列表中的位置
         * @param toPosition   歌曲移动后在列表中的位置
         */
        void onMusicItemMoved(int fromPosition, int toPosition);

        /**
         * 往列表中插入了一首新的歌曲。
         *
         * @param position  歌曲插入的位置
         * @param musicItem 插入的歌曲
         */
        void onMusicItemInserted(int position, MusicItem musicItem);

        /**
         * 从播放列表中移除了某首歌曲。
         *
         * @param musicItem 被移除的歌曲
         */
        void onMusicItemRemoved(MusicItem musicItem);
    }
}
