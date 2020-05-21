package snow.player.playlist;

import java.util.List;

import snow.player.Player;

public interface PlaylistPlayer extends Player {
    void skipToPrevious();

    void skipToPosition(int position);

    void setPlayMode(int playMode);

    void notifyPlaylistSwapped(int position, boolean playOnPrepared);

    void notifyMusicItemMoved(int fromPosition, int toPosition);

    void notifyMusicItemInserted(int position, int count);

    void notifyMusicItemRemoved(List<Integer> positions);

    class PlayMode {
        public static final int SEQUENTIAL = 0;
        public static final int LOOP = 1;
        public static final int SHUFFLE = 2;
    }

    interface OnPlaylistChangeListener {
        void onPlaylistChanged(PlaylistManager playlistManager, int position);
    }

    interface OnPlayModeChangeListener {
        void onPlayModeChanged(int playMode);
    }

    interface OnPlayingMusicItemPositionChangeListener {
        void onPlayingMusicItemPositionChanged(int position);
    }
}
