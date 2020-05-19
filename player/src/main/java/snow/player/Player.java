package snow.player;

import androidx.annotation.Nullable;

public interface Player {
    void play();

    void pause();

    void stop();

    void playOrPause();

    void skipToNext();

    void seekTo(long progress);

    void fastForward();

    void rewind();

    enum PlaybackState {
        UNKNOWN,
        PREPARING,
        PREPARED,
        PLAYING,
        PAUSED,
        STOPPED,
        STALLED,
        ERROR
    }

    interface OnPlaybackStateChangeListener {
        void onPreparing();

        void onPrepared(int audioSessionId);

        void onPlay(long playProgress, long playProgressUpdateTime);

        void onPause();

        void onStop();

        void onStalled();

        void onError(int errorCode, String errorMessage);
    }

    interface OnBufferingPercentChangeListener {
        void onBufferingPercentChanged(int percent, long updateTime);
    }

    interface OnPlayingMusicItemChangeListener {
        void onPlayingMusicItemChanged(@Nullable MusicItem musicItem, int position);
    }
}
