package snow.player.playlist;

import androidx.annotation.NonNull;

import channel.helper.Channel;
import snow.player.media.MusicItem;

/**
 * 该接口定义了播放列表编辑器的基本功能。
 *
 * @see snow.player.PlayerClient
 */
@Channel
public interface PlaylistEditor {
    /**
     * 往列表中插入了一首新的歌曲。
     * <p>
     * 如果播放列表中已包含指定歌曲，则会将它移动到 position 位置，如果不存在，则会将歌曲插入到 position 位置。
     *
     * @param position  歌曲插入的位置
     * @param musicItem 要插入的歌曲，不能为 null
     */
    @SuppressWarnings("NullableProblems")
    void insertMusicItem(int position, @NonNull MusicItem musicItem);

    /**
     * 移动播放列表中某首歌曲的位置。
     *
     * @param fromPosition 歌曲在列表中的位置
     * @param toPosition   歌曲要移动到的位置。如果 {@code toPosition == fromPosition}，则会忽略本次调用
     */
    void moveMusicItem(int fromPosition, int toPosition);

    /**
     * 从播放列表中移除了指定歌曲。
     *
     * @param musicItem 要移除的歌曲。如果播放列表中不包含该歌曲，则忽略本次调用
     */
    @SuppressWarnings("NullableProblems")
    void removeMusicItem(@NonNull MusicItem musicItem);

    /**
     * 设置 “下一次播放” 的歌曲。
     *
     * @param musicItem 要设定为 “下一次播放” 的歌曲，如果歌曲已存在播放列表中，则会移动到 “下一曲播放” 的位
     *                  置，如果歌曲不存在，则插入到 “下一曲播放” 位置
     */
    @SuppressWarnings("NullableProblems")
    void setNextPlay(@NonNull MusicItem musicItem);
}
