package snow.player.playlist;

import android.content.Context;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.common.base.Preconditions;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Random;

import snow.player.AbstractPlayer;
import snow.player.media.MusicItem;

public abstract class AbstractPlaylistPlayer extends AbstractPlayer<PlaylistStateListener> implements PlaylistPlayer {
    private PlaylistState mPlaylistState;
    private PlaylistManager mPlaylistManager;

    private Playlist mPlaylist;
    private volatile boolean mLoadingPlaylist;

    private volatile Runnable mPlaylistLoadedAction;

    private Random mRandom;

    public AbstractPlaylistPlayer(@NonNull Context context,
                                  @NonNull PlaylistState playlistState,
                                  @NonNull PlaylistManager playlistManager) {
        super(context, playlistState);

        mPlaylistState = playlistState;
        mPlaylistManager = playlistManager;

        loadPlaylistAsync();
    }

    private void loadPlaylistAsync() {
        mLoadingPlaylist = true;
        mPlaylistManager.getPlaylistAsync(new PlaylistManager.Callback() {
            @Override
            public void onFinished(@NonNull Playlist playlist) {
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

    private int getRandomPosition(int exclude) {
        if (mPlaylist == null || mPlaylist.size() < 2) {
            return 0;
        }

        if (mRandom == null) {
            mRandom = new Random();
        }

        int position = mRandom.nextInt(mPlaylist.size());

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

    protected final boolean isPlaylistAvailable() {
        return !mLoadingPlaylist;
    }

    protected void onPlaylistAvailable(Playlist playlist) {
    }

    protected final Playlist getPlaylist() {
        return mPlaylist;
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

        if (mPlaylist.size() < 1) {
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

        if (position >= mPlaylist.size()) {
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

        if (mPlaylist.size() < 1) {
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
            return mPlaylist.size() - 1;
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
        notifyPlaylistChanged(position);
        mPlaylistLoadedAction = new Runnable() {
            @Override
            public void run() {
                MusicItem musicItem = null;
                if (mPlaylist.size() > 0) {
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
            throw new IndexOutOfBoundsException("size: " + mPlaylist.size() + ", positions: " + toString(positions));
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
        return Collections.max(positions) >= mPlaylist.size();
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
