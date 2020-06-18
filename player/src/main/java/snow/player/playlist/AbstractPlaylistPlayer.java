package snow.player.playlist;

import android.content.Context;

import androidx.annotation.NonNull;

import com.google.common.base.Preconditions;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import snow.player.AbstractPlayer;
import snow.player.media.MusicItem;
import snow.player.state.PlaylistState;
import snow.player.state.PlaylistStateListener;

public abstract class AbstractPlaylistPlayer extends AbstractPlayer<PlaylistStateListener> implements PlaylistPlayer {
    private PlaylistState mPlaylistState;
    private PlaylistManager mPlaylistManager;

    private Playlist mPlaylist;
    private volatile boolean mPlaylistAvailable;

    private ExecutorService mExecutor;
    private Runnable mPlaylistAvailableAction;

    private Random mRandom;

    public AbstractPlaylistPlayer(@NonNull Context context,
                                  @NonNull PlaylistState playlistState,
                                  @NonNull PlaylistManager playlistManager) {
        super(context, playlistState);

        mPlaylistState = playlistState;
        mPlaylistManager = playlistManager;

        mExecutor = Executors.newSingleThreadExecutor();
        loadPlaylistAsync();
    }

    private void loadPlaylistAsync() {
        mPlaylistAvailable = false;
        mExecutor.execute(new Runnable() {
            @Override
            public void run() {
                mPlaylist = mPlaylistManager.getPlaylist();
                mPlaylistAvailable = true;

                if (mPlaylistAvailableAction != null) {
                    mPlaylistAvailableAction.run();
                    mPlaylistAvailableAction = null;
                }
            }
        });
    }

    private int getRandomPosition() {
        if (mPlaylist == null) {
            return 0;
        }

        if (mRandom == null) {
            mRandom = new Random();
        }

        return mRandom.nextInt(mPlaylist.size());
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

    @Override
    protected void onRelease() {
        mPlaylistAvailableAction = null;
        mExecutor.shutdown();
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
        if (!mPlaylistAvailable) {
            mPlaylistAvailableAction = new Runnable() {
                @Override
                public void run() {
                    skipToNext();
                }
            };
            return;
        }

        int position = mPlaylistState.getPosition();

        switch (mPlaylistState.getPlayMode()) {
            case SEQUENTIAL:   // 注意！case 穿透
            case LOOP:
                position = Math.min(position + 1, mPlaylist.size());
                break;
            case SHUFFLE:
                position = getRandomPosition();
                break;
        }

        notifyPlayingMusicItemChanged(mPlaylist.get(position), true);
        notifyPlayingMusicItemPositionChanged(position);
    }

    @Override
    public void skipToPrevious() {
        if (!mPlaylistAvailable) {
            mPlaylistAvailableAction = new Runnable() {
                @Override
                public void run() {
                    skipToPrevious();
                }
            };
            return;
        }

        int position = mPlaylistState.getPosition();

        switch (mPlaylistState.getPlayMode()) {
            case SEQUENTIAL:   // 注意！case 穿透
            case LOOP:
                position = Math.max(position - 1, 0);
                break;
            case SHUFFLE:
                position = getRandomPosition();
                break;
        }

        notifyPlayingMusicItemChanged(mPlaylist.get(position), true);
        notifyPlayingMusicItemPositionChanged(position);
    }

    @Override
    public void playOrPause(final int position) {
        if (!mPlaylistAvailable) {
            mPlaylistAvailableAction = new Runnable() {
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
        mPlaylistAvailableAction = new Runnable() {
            @Override
            public void run() {
                notifyPlayingMusicItemChanged(mPlaylist.get(position), playOnPrepared);
            }
        };
        loadPlaylistAsync();
    }

    @Override
    public void notifyMusicItemMoved(int fromPosition, int toPosition) {
        if (fromPosition == toPosition) {
            return;
        }

        loadPlaylistAsync();
        notifyPlayingMusicItemPositionChanged(toPosition);
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

        mPlaylistAvailableAction = new Runnable() {
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
        mPlaylistAvailableAction = new Runnable() {
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
            final boolean playing = isPlaying();
            mPlaylistAvailableAction = new Runnable() {
                @Override
                public void run() {
                    MusicItem musicItem = null;
                    if (!mPlaylist.isEmpty()) {
                        musicItem = mPlaylist.get(i);
                    }

                    notifyPlayingMusicItemChanged(musicItem, playing);
                    notifyPlayingMusicItemPositionChanged(mPlaylist.isEmpty() ? -1 : i);
                }
            };
        }

        loadPlaylistAsync();
    }
}
