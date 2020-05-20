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

    void setSoundQuality(int soundQuality);

    void setAudioEffectEnabled(boolean enabled);

    void setOnlyWifiNetwork(boolean onlyWifiNetwork);

    void setIgnoreLossAudioFocus(boolean ignoreLossAudioFocus);

    class SoundQuality {
        public static final int STANDARD = 0;
        public static final int LOW = 1;
        public static final int HIGH = 2;
        public static final int LOSSLESS = 3;
    }

    class PlaybackState {
        public static final int UNKNOWN = 0;
        public static final int PREPARING = 1;
        public static final int PREPARED = 2;
        public static final int PLAYING = 3;
        public static final int PAUSED = 4;
        public static final int STOPPED = 5;
        public static final int STALLED = 6;
        public static final int ERROR = 7;
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
        void onPlayingMusicItemChanged(@Nullable MusicItem musicItem);
    }
}
