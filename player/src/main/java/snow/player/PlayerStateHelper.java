package snow.player;

import androidx.annotation.Nullable;

import snow.player.media.MusicItem;

class PlayerStateHelper {
    private PlayerState mPlayerState;

    public PlayerStateHelper(PlayerState playerState) {
        mPlayerState = playerState;
    }

    public void updatePlayProgress(int progress, long updateTime) {
        mPlayerState.setPlayProgress(progress);
        mPlayerState.setPlayProgressUpdateTime(updateTime);
    }

    public void onPreparing() {
        mPlayerState.setPlaybackState(PlaybackState.PREPARING);
    }

    public void onPrepared(int audioSessionId) {
        mPlayerState.setPlaybackState(PlaybackState.PREPARED);
        mPlayerState.setAudioSessionId(audioSessionId);
    }

    public void onPlay(int progress, long updateTime) {
        mPlayerState.setPlaybackState(PlaybackState.PLAYING);
        updatePlayProgress(progress, updateTime);
    }

    public void onPaused() {
        mPlayerState.setPlaybackState(PlaybackState.PAUSED);
    }

    public void onStopped() {
        mPlayerState.setPlaybackState(PlaybackState.STOPPED);
        updatePlayProgress(0, System.currentTimeMillis());
    }

    public void onStalled(boolean stalled, int playProgress, long updateTime) {
        mPlayerState.setStalled(stalled);
        mPlayerState.setPlayProgress(playProgress);
        mPlayerState.setPlayProgressUpdateTime(updateTime);
    }

    public void onError(int errorCode, String errorMessage) {
        mPlayerState.setPlaybackState(PlaybackState.ERROR);
        mPlayerState.setErrorCode(errorCode);
        mPlayerState.setErrorMessage(errorMessage);
        updatePlayProgress(0, System.currentTimeMillis());
    }

    public void onBufferedChanged(int bufferedProgress) {
        mPlayerState.setBufferedProgress(bufferedProgress);
    }

    public void onPlayingMusicItemChanged(@Nullable MusicItem musicItem, int playProgress) {
        mPlayerState.setMusicItem(musicItem);
        updatePlayProgress(playProgress, System.currentTimeMillis());
    }

    public void onSeekComplete(int playProgress, long updateTime) {
        updatePlayProgress(playProgress, updateTime);
    }

    public void onPlaylistChanged(int position) {
        mPlayerState.setPosition(position);
    }

    public void onPlayModeChanged(PlayMode playMode) {
        mPlayerState.setPlayMode(playMode);
    }

    public void onPositionChanged(int position) {
        mPlayerState.setPosition(position);
    }
}
