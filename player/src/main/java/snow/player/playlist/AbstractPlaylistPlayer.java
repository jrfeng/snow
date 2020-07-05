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

public abstract class AbstractPlaylistPlayer extends AbstractPlayer<PlaylistStateListener> implements PlaylistPlayer {
    private PlaylistState mPlaylistState;
    private PlaylistManager mPlaylistManager;

    private Playlist mPlaylist;
    private volatile boolean mLoadingPlaylist;

    private Handler mMainHandler;
    private volatile Runnable mPlaylistLoadedAction;

    private Random mRandom;

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

    @SuppressWarnings("unused")
    protected final boolean isPlaylistAvailable() {
        return !mLoadingPlaylist;
    }

    protected void onPlaylistAvailable(Playlist playlist) {
    }

    protected final Playlist getPlaylist() {
        return mPlaylist;
    }

    protected final int getPlaylistSize() {
        return mPlaylistManager.getPlaylistSize();
    }

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
        notifyStopped();
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
