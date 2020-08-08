package snow.player;

import androidx.annotation.Nullable;

import snow.player.appwidget.AppWidgetPreferences;
import snow.player.media.MusicItem;

class PlayerStateHelper {
    private PlayerState mPlayerState;
    @Nullable
    private AppWidgetPreferences mAppWidgetPreferences;

    public PlayerStateHelper(PlayerState playerState) {
        this(playerState, null);
    }

    public PlayerStateHelper(PlayerState playerState, @Nullable AppWidgetPreferences preferences) {
        mPlayerState = playerState;
        mAppWidgetPreferences = preferences;
    }

    public void updatePlayProgress(int progress, long updateTime) {
        mPlayerState.setPlayProgress(progress);
        mPlayerState.setPlayProgressUpdateTime(updateTime);
    }

    public void onPreparing() {
        mPlayerState.setPreparing(true);
        mPlayerState.setPrepared(false);

        if (mAppWidgetPreferences != null) {
            mAppWidgetPreferences.edit()
                    .setPreparing(true)
                    .setPlaybackState(mPlayerState.getPlaybackState())
                    .commit();
        }
    }

    public void onPrepared(int audioSessionId) {
        mPlayerState.setPreparing(false);
        mPlayerState.setPrepared(true);
        mPlayerState.setAudioSessionId(audioSessionId);

        if (mAppWidgetPreferences != null) {
            mAppWidgetPreferences.edit()
                    .setPreparing(false)
                    .commit();
        }
    }

    public void clearPrepareState() {
        mPlayerState.setPreparing(false);
        mPlayerState.setPrepared(false);
    }

    public void onPlay(int progress, long updateTime) {
        mPlayerState.setPlaybackState(PlaybackState.PLAYING);
        updatePlayProgress(progress, updateTime);

        if (mAppWidgetPreferences != null) {
            mAppWidgetPreferences.edit()
                    .setPlaybackState(PlaybackState.PLAYING)
                    .setPlayProgress(progress)
                    .setPlayProgressUpdateTime(updateTime)
                    .commit();
        }
    }

    public void onPaused() {
        mPlayerState.setPlaybackState(PlaybackState.PAUSED);

        if (mAppWidgetPreferences != null) {
            mAppWidgetPreferences.edit()
                    .setPlaybackState(PlaybackState.PAUSED)
                    .commit();
        }
    }

    public void onStopped() {
        mPlayerState.setPlaybackState(PlaybackState.STOPPED);
        long updateTime = System.currentTimeMillis();
        updatePlayProgress(0, updateTime);
        clearPrepareState();

        if (mAppWidgetPreferences != null) {
            mAppWidgetPreferences.edit()
                    .setPlaybackState(PlaybackState.STOPPED)
                    .setPlayProgressUpdateTime(0)
                    .setPlayProgressUpdateTime(updateTime)
                    .setPreparing(false)
                    .commit();
        }
    }

    public void onStalled(boolean stalled, int playProgress, long updateTime) {
        mPlayerState.setStalled(stalled);
        mPlayerState.setPlayProgress(playProgress);
        mPlayerState.setPlayProgressUpdateTime(updateTime);

        if (mAppWidgetPreferences != null) {
            mAppWidgetPreferences.edit()
                    .setStalled(stalled)
                    .setPlayProgress(playProgress)
                    .setPlayProgressUpdateTime(updateTime)
                    .commit();
        }
    }

    public void onError(int errorCode, String errorMessage) {
        mPlayerState.setPlaybackState(PlaybackState.ERROR);
        mPlayerState.setErrorCode(errorCode);
        mPlayerState.setErrorMessage(errorMessage);
        long updateTime = System.currentTimeMillis();
        updatePlayProgress(0, updateTime);
        clearPrepareState();

        if (mAppWidgetPreferences != null) {
            mAppWidgetPreferences.edit()
                    .setPlaybackState(PlaybackState.ERROR)
                    .setErrorMessage(errorMessage)
                    .setPlayProgress(0)
                    .setPlayProgressUpdateTime(updateTime)
                    .setPreparing(false)
                    .commit();
        }
    }

    public void onBufferedChanged(int bufferedProgress) {
        mPlayerState.setBufferedProgress(bufferedProgress);
    }

    public void onPlayingMusicItemChanged(@Nullable MusicItem musicItem, int playProgress) {
        mPlayerState.setMusicItem(musicItem);
        long updateTime = System.currentTimeMillis();
        updatePlayProgress(playProgress, updateTime);

        if (mAppWidgetPreferences != null) {
            mAppWidgetPreferences.edit()
                    .setPlayingMusicItem(musicItem)
                    .setPlayProgress(playProgress)
                    .setPlayProgressUpdateTime(updateTime)
                    .commit();
        }
    }

    public void onSeekComplete(int playProgress, long updateTime) {
        updatePlayProgress(playProgress, updateTime);

        if (mAppWidgetPreferences != null) {
            mAppWidgetPreferences.edit()
                    .setPlayProgress(playProgress)
                    .setPlayProgressUpdateTime(updateTime)
                    .commit();
        }
    }

    public void onPlaylistChanged(int position) {
        mPlayerState.setPosition(position);
    }

    public void onPlayModeChanged(PlayMode playMode) {
        mPlayerState.setPlayMode(playMode);

        if (mAppWidgetPreferences != null) {
            mAppWidgetPreferences.edit()
                    .setPlayMode(playMode)
                    .commit();
        }
    }

    public void onPositionChanged(int position) {
        mPlayerState.setPosition(position);
    }
}
