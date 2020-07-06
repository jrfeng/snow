package snow.player.playlist;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.common.base.Preconditions;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Random;

import snow.player.AbstractPlayer;
import snow.player.PlayerConfig;
import snow.player.media.MusicItem;

/**
 * 该类提供了基本的 “列表” 播放器实现。
 */
public abstract class AbstractPlaylistPlayer extends AbstractPlayer<PlaylistStateListener> implements PlaylistPlayer {
    private PlaylistState mPlaylistState;
    private PlaylistManager mPlaylistManager;

    private Playlist mPlaylist;
    private volatile boolean mLoadingPlaylist;

    private Handler mMainHandler;
    private volatile Runnable mPlaylistLoadedAction;

    private Random mRandom;

    /**
     * 构造方法。
     *
     * @param context         {@link Context} 对象，不能为 null
     * @param playerConfig    {@link PlayerConfig} 对象，保存了播放器的初始配置信息，不能为 null
     * @param playlistState   {@link PlaylistState} 对象，保存了列表播放器的初始状态，不能为 null
     * @param enable          是否启用当前播放器，如果为 {@code false}，则当前播放器不会响应任何操作
     * @param playlistManager {@link PlaylistManager} 对象，用于管理当前播放队列，不能为 null
     */
    public AbstractPlaylistPlayer(@NonNull Context context,
                                  @NonNull PlayerConfig playerConfig,
                                  @NonNull PlaylistState playlistState,
                                  boolean enable,
                                  @NonNull PlaylistManager playlistManager) {
        super(context, playerConfig, playlistState, enable);

        mPlaylistState = playlistState;
        mPlaylistManager = playlistManager;
        mMainHandler = new Handler(Looper.getMainLooper());

        loadPlaylistAsync();
    }

    /**
     * 构造方法。
     *
     * @param context         {@link Context} 对象，不能为 null
     * @param playerConfig    {@link PlayerConfig} 对象，保存了播放器的初始配置信息，不能为 null
     * @param playlistState   {@link PlaylistState} 对象，保存了列表播放器的初始状态，不能为 null
     * @param enable          是否启用当前播放器，如果为 {@code false}，则当前播放器不会响应任何操作
     * @param playlistManager {@link PlaylistManager} 对象，用于管理当前播放队列，不能为 null
     * @param playlist        当前的播放列表，不能为 null
     */
    public AbstractPlaylistPlayer(@NonNull Context context,
                                  @NonNull PlayerConfig playerConfig,
                                  @NonNull PlaylistState playlistState,
                                  boolean enable,
                                  @NonNull PlaylistManager playlistManager,
                                  @NonNull Playlist playlist) {
        super(context, playerConfig, playlistState, enable);

        mPlaylistState = playlistState;
        mPlaylistManager = playlistManager;
        mPlaylist = playlist;
        mMainHandler = new Handler(Looper.getMainLooper());
    }

    private void loadPlaylistAsync() {
        mLoadingPlaylist = true;
        mPlaylistManager.getPlaylistAsync(new PlaylistManager.Callback() {
            @Override
            public void onFinished(@NonNull final Playlist playlist) {
                mMainHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        mPlaylist = playlist;
                        mLoadingPlaylist = false;

                        if (mPlaylistLoadedAction != null) {
                            mPlaylistLoadedAction.run();
                            mPlaylistLoadedAction = null;
                        }

                        onPlaylistAvailable(mPlaylist);
                    }
                });
            }
        });
    }

    private int getRandomPosition(int exclude) {
        if (mPlaylist == null || getPlaylistSize() < 2) {
            return 0;
        }

        if (mRandom == null) {
            mRandom = new Random();
        }

        int position = mRandom.nextInt(getPlaylistSize());

        if (position != exclude) {
            return position;
        }

        return getRandomPosition(exclude);
    }

    private void notifyPlayingMusicItemPositionChanged(int position) {
        mPlaylistState.setPosition(position);
        HashMap<String, PlaylistStateListener> listenerMap = getAllStateListener();

        for (String key : listenerMap.keySet()) {
            PlaylistStateListener listener = listenerMap.get(key);
            if (listener != null) {
                listener.onPositionChanged(position);
            }
        }
    }

    private void notifyPlayModeChanged(PlayMode playMode) {
        mPlaylistState.setPlayMode(playMode);
        HashMap<String, PlaylistStateListener> listenerMap = getAllStateListener();

        for (String key : listenerMap.keySet()) {
            PlaylistStateListener listener = listenerMap.get(key);
            if (listener != null) {
                listener.onPlayModeChanged(playMode);
            }
        }
    }

    private void notifyPlaylistChanged(int position) {
        mPlaylistState.setPosition(position);

        HashMap<String, PlaylistStateListener> listenerMap = getAllStateListener();

        for (String key : listenerMap.keySet()) {
            PlaylistStateListener listener = listenerMap.get(key);
            if (listener != null) {
                // 注意！playlistManager 参数为 null，客户端接收到该事件后，应该将其替换为自己的 PlaylistManager 对象
                listener.onPlaylistChanged(null, position);
            }
        }
    }

    /**
     * 查询播放列表当前是否可用。
     * <p>
     * 当播放列表被修改时，播放器会重新加载播放列表，在加载完成前，该方法会返回 {@code false}，加
     * 载完成后，该方法会返回 {@code true}。在播放列表加载完成前，不应访问播放列表。
     * <p>
     * 如果你需要在播放列表加载完成后访问它，可以重写 {@link #onPlaylistAvailable(Playlist)} 方法。
     *
     * @return 播放队列当前是否可用
     * @see #onPlaylistAvailable(Playlist)
     */
    @SuppressWarnings("unused")
    protected final boolean isPlaylistAvailable() {
        return !mLoadingPlaylist;
    }

    /**
     * 该方法会在播放列表变得可用时调用。
     *
     * @param playlist 当前的播放列表
     */
    protected void onPlaylistAvailable(Playlist playlist) {
    }

    /**
     * 获取当前的播放列表。
     *
     * @return 当前播放列表
     */
    protected final Playlist getPlaylist() {
        return mPlaylist;
    }

    /**
     * 获取播放列表的大小。
     *
     * @return 播放列表的大小
     */
    protected final int getPlaylistSize() {
        return mPlaylistManager.getPlaylistSize();
    }

    /**
     * 获取播放列表携带的额外参数。
     *
     * @return 播放列表携带的额外参数，可能为 null
     */
    @Nullable
    public final Bundle getPlaylistExtra() {
        if (mPlaylist == null) {
            return null;
        }

        return mPlaylist.getExtra();
    }

    @Override
    public boolean isLooping() {
        return mPlaylistState.getPlayMode() == PlayMode.LOOP;
    }

    @Override
    protected void onRelease() {
        mPlaylistLoadedAction = null;
    }

    @Override
    protected void onPlayComplete(MusicItem musicItem) {
        if (mPlaylistState.getPlayMode() == PlayMode.LOOP) {
            return;
        }

        skipToNext();
    }

    @Override
    public void skipToNext() {
        if (mLoadingPlaylist) {
            mPlaylistLoadedAction = new Runnable() {
                @Override
                public void run() {
                    skipToNext();
                }
            };
            return;
        }

        if (getPlaylistSize() < 1) {
            return;
        }

        int position = mPlaylistState.getPosition();

        switch (mPlaylistState.getPlayMode()) {
            case SEQUENTIAL:   // 注意！case 穿透
            case LOOP:
                position = getNextPosition(position);
                break;
            case SHUFFLE:
                position = getRandomPosition(position);
                break;
        }

        notifyPlayingMusicItemChanged(mPlaylist.get(position), true);
        notifyPlayingMusicItemPositionChanged(position);
    }

    protected int getNextPosition(int currentPosition) {
        int position = currentPosition + 1;

        if (position >= getPlaylistSize()) {
            return 0;
        }

        return position;
    }

    @Override
    public void skipToPrevious() {
        if (mLoadingPlaylist) {
            mPlaylistLoadedAction = new Runnable() {
                @Override
                public void run() {
                    skipToPrevious();
                }
            };
            return;
        }

        if (getPlaylistSize() < 1) {
            return;
        }

        int position = mPlaylistState.getPosition();

        switch (mPlaylistState.getPlayMode()) {
            case SEQUENTIAL:   // 注意！case 穿透
            case LOOP:
                position = getPreviousPosition(position);
                break;
            case SHUFFLE:
                position = getRandomPosition(position);
                break;
        }

        notifyPlayingMusicItemChanged(mPlaylist.get(position), true);
        notifyPlayingMusicItemPositionChanged(position);
    }

    protected int getPreviousPosition(int currentPosition) {
        int position = currentPosition - 1;

        if (position < 0) {
            return getPlaylistSize() - 1;
        }

        return position;
    }

    @Override
    public void playOrPause(final int position) {
        if (mLoadingPlaylist) {
            mPlaylistLoadedAction = new Runnable() {
                @Override
                public void run() {
                    playOrPause(position);
                }
            };
            return;
        }

        if (position == mPlaylistState.getPosition()) {
            playOrPause();
            return;
        }

        notifyPlayingMusicItemChanged(mPlaylist.get(position), true);
        notifyPlayingMusicItemPositionChanged(position);
    }

    @Override
    public void setPlayMode(@NonNull PlayMode playMode) {
        Preconditions.checkNotNull(playMode);
        if (playMode == mPlaylistState.getPlayMode()) {
            return;
        }

        notifyPlayModeChanged(playMode);
    }

    @Override
    public void notifyPlaylistSwapped(final int position, final boolean playOnPrepared) {
        stop();
        notifyPlaylistChanged(position);
        mPlaylistLoadedAction = new Runnable() {
            @Override
            public void run() {
                MusicItem musicItem = null;
                if (getPlaylistSize() > 0) {
                    musicItem = mPlaylist.get(position);
                }

                notifyPlayingMusicItemChanged(musicItem, playOnPrepared);
            }
        };
        loadPlaylistAsync();
    }

    @Override
    public void notifyMusicItemMoved(int fromPosition, final int toPosition) {
        if (fromPosition == toPosition) {
            return;
        }

        mPlaylistLoadedAction = new Runnable() {
            @Override
            public void run() {
                notifyPlayingMusicItemPositionChanged(toPosition);
            }
        };

        loadPlaylistAsync();
    }

    @Override
    public void notifyMusicItemInserted(final int position, final int count) {
        if (position < 0) {
            throw new IllegalArgumentException("'position' must >= 0, position=" + position);
        }

        if (count <= 0) {
            throw new IllegalArgumentException("'count' must > 0, count=" + count);
        }

        notifyPlaylistChanged(position);

        if (position > mPlaylistState.getPosition()) {
            loadPlaylistAsync();
            return;
        }

        mPlaylistLoadedAction = new Runnable() {
            @Override
            public void run() {
                notifyPlayingMusicItemPositionChanged(position + count);
            }
        };

        loadPlaylistAsync();
    }

    @Override
    public void notifyMusicItemRemoved(List<Integer> positions) {
        if (isPositionsIllegal(positions)) {
            throw new IndexOutOfBoundsException("size: " + getPlaylistSize() + ", positions: " + toString(positions));
        }

        if (positions.contains(mPlaylistState.getPosition())) {
            adjustPlayingPosition(positions);
            return;
        }

        adjustPlayingPosition2(positions);
    }

    private String toString(List<Integer> integers) {
        StringBuilder buff = new StringBuilder();

        buff.append("[");

        for (int i = 0; i < integers.size() - 1; i++) {
            buff.append(i)
                    .append(", ");
        }

        buff.append(integers.get(integers.size() - 1))
                .append("]");

        return buff.toString();
    }

    private boolean isPositionsIllegal(List<Integer> positions) {
        return Collections.max(positions) >= getPlaylistSize();
    }

    private void adjustPlayingPosition(final List<Integer> positions) {
        final boolean playing = isPlaying();
        mPlaylistLoadedAction = new Runnable() {
            @Override
            public void run() {
                int p = Collections.min(positions);

                MusicItem musicItem = null;
                if (!mPlaylist.isEmpty()) {
                    musicItem = mPlaylist.get(p);
                }

                notifyPlayingMusicItemChanged(musicItem, playing);
                notifyPlayingMusicItemPositionChanged(mPlaylist.isEmpty() ? -1 : p);
            }
        };

        loadPlaylistAsync();
    }

    private void adjustPlayingPosition2(List<Integer> positions) {
        final int position = mPlaylistState.getPosition();

        int count = 0;
        for (int p : positions) {
            if (p < position) {
                count += 1;
            }
        }

        if (count > 0) {
            final int i = Math.max(0, position - count);
            mPlaylistLoadedAction = new Runnable() {
                @Override
                public void run() {
                    notifyPlayingMusicItemPositionChanged(mPlaylist.isEmpty() ? -1 : i);
                }
            };
        }

        loadPlaylistAsync();
    }
}
