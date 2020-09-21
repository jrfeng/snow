package snow.player;

import android.os.SystemClock;

import androidx.annotation.Nullable;

import snow.player.appwidget.AppWidgetPreferences;
import snow.player.audio.ErrorCode;
import snow.player.audio.MusicItem;

class PlayerStateHelper {
    private PlayerState mPlayerState;
    @Nullable
    private AppWidgetPreferences mAppWidgetPreferences;

    public PlayerStateHelper(PlayerState playerState) {
        this(playerState, null);
    }

    // 服务端专用
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

        if (mPlayerState.getPlaybackState() == PlaybackState.ERROR) {
            mPlayerState.setPlaybackState(PlaybackState.NONE);
            mPlayerState.setErrorCode(ErrorCode.NO_ERROR);
            mPlayerState.setErrorMessage("");
        }

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

    public void onPlay(boolean stalled, int progress, long updateTime) {
        mPlayerState.setStalled(stalled);
        mPlayerState.setPlaybackState(PlaybackState.PLAYING);
        updatePlayProgress(progress, updateTime);

        if (mAppWidgetPreferences != null) {
            mAppWidgetPreferences.edit()
                    .setPlaybackState(PlaybackState.PLAYING)
                    .setPlayProgress(progress)
                    .setPlayProgressUpdateTime(updateTime)
                    .setStalled(stalled)
                    .commit();
        }
    }

    public void onPaused(int playProgress, long updateTime) {
        mPlayerState.setPlaybackState(PlaybackState.PAUSED);
        mPlayerState.setPlayProgress(playProgress);
        mPlayerState.setPlayProgressUpdateTime(updateTime);

        if (mAppWidgetPreferences != null) {
            mAppWidgetPreferences.edit()
                    .setPlaybackState(PlaybackState.PAUSED)
                    .commit();
        }
    }

    public void onStopped() {
        mPlayerState.setPlaybackState(PlaybackState.STOPPED);
        long updateTime = SystemClock.elapsedRealtime();
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
        updatePlayProgress(playProgress, updateTime);

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
        clearPrepareState();

        if (mAppWidgetPreferences != null) {
            mAppWidgetPreferences.edit()
                    .setPlaybackState(PlaybackState.ERROR)
                    .setErrorMessage(errorMessage)
                    .setPlayProgress(mPlayerState.getPlayProgress())
                    .setPlayProgressUpdateTime(mPlayerState.getPlayProgressUpdateTime())
                    .setPreparing(false)
                    .commit();
        }
    }

    public void onBufferedChanged(int bufferedProgress) {
        mPlayerState.setBufferedProgress(bufferedProgress);
    }

    public void onPlayingMusicItemChanged(@Nullable MusicItem musicItem, int position, int playProgress) {
        mPlayerState.setMusicItem(musicItem);
        mPlayerState.setPlayPosition(position);
        mPlayerState.setBufferedProgress(0);

        long updateTime = SystemClock.elapsedRealtime();
        updatePlayProgress(playProgress, updateTime);

        if (mPlayerState.getPlaybackState() == PlaybackState.ERROR) {
            mPlayerState.setPlaybackState(PlaybackState.NONE);
            mPlayerState.setErrorCode(ErrorCode.NO_ERROR);
            mPlayerState.setErrorMessage("");
        }

        if (mAppWidgetPreferences != null) {
            mAppWidgetPreferences.edit()
                    .setPlayingMusicItem(musicItem)
                    .setPlayProgress(playProgress)
                    .setPlayProgressUpdateTime(updateTime)
                    .commit();
        }
    }

    public void onSeekComplete(int playProgress, long updateTime, boolean stalled) {
        updatePlayProgress(playProgress, updateTime);
        mPlayerState.setStalled(stalled);

        if (mAppWidgetPreferences != null) {
            mAppWidgetPreferences.edit()
                    .setPlayProgress(playProgress)
                    .setPlayProgressUpdateTime(updateTime)
                    .setStalled(stalled)
                    .commit();
        }
    }

    public void onPlaylistChanged(int position) {
        mPlayerState.setPlayPosition(position);
    }

    public void onPlayModeChanged(PlayMode playMode) {
        mPlayerState.setPlayMode(playMode);

        if (mAppWidgetPreferences != null) {
            mAppWidgetPreferences.edit()
                    .setPlayMode(playMode)
                    .commit();
        }
    }

    public void onStartSleepTimer(long time, long startTime) {
        mPlayerState.setSleepTimerStarted(true);
        mPlayerState.setSleepTimerTime(time);
        mPlayerState.setSleepTimerStartTime(startTime);
    }

    public void onCancelSleepTimer() {
        mPlayerState.setSleepTimerStarted(false);
    }
}
