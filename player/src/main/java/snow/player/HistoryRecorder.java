package snow.player;

import androidx.annotation.NonNull;

import snow.player.media.MusicItem;

/**
 * 历史记录器，用于记录播放器的播放历史。
 */
public interface HistoryRecorder {
    /**
     * 当正在播放的音乐改变时，会调用该方法。
     *
     * @param musicItem 正在播放的音乐
     */
    void onPlayingMusicItemChanged(@NonNull MusicItem musicItem);
}
