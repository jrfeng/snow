package snow.music.util;

import androidx.annotation.NonNull;

import com.google.common.base.Preconditions;

import java.util.List;

import snow.music.store.Music;
import snow.music.store.MusicList;
import snow.player.playlist.Playlist;

/**
 * {@link MusicList} 工具，用于将 {@link MusicList} 转换成一个 {@link Playlist} 对象。
 */
public final class MusicListUtil {
    private MusicListUtil() {
        throw new AssertionError();
    }

    /**
     * 将一个 {@code List<Music>} 列表转换成一个 {@link Playlist} 对象。
     * <p>
     * 注意！如果 {@code List<Music>} 列表的尺寸大于 {@link Playlist#MAX_SIZE} 时，则会以 position
     * 参数作为基准索引，先向后提取 {@link Music} 元素，当 position 后面的元素数不足以凑足
     * {@link Playlist#MAX_SIZE} 时，再从 position 处向前提取，直至凑足 {@link Playlist#MAX_SIZE} 个
     * {@link Music} 元素。然后再使用这些 {@link Music} 元素来创建 {@link Playlist} 对象。
     *
     * @param name       String 对象，该 String 对象将作为 {@link Playlist} 的名称，不能为 null。
     * @param musicItems {@code List<Music>} 列表，不能为 null。
     * @param position   基准索引，仅在 {@code List<Music>} 列表的尺寸大于 {@link Playlist#MAX_SIZE} 时有用
     * @return {@link Playlist} 对象，不为 null
     */
    @NonNull
    public static Playlist asPlaylist(@NonNull String name, @NonNull List<Music> musicItems, int position) {
        Preconditions.checkNotNull(name);
        Preconditions.checkNotNull(musicItems);

        Playlist.Builder builder = new Playlist.Builder()
                .setName(name)
                .setPosition(position);

        for (Music music : musicItems) {
            builder.append(MusicUtil.asMusicItem(music));
        }

        return builder.build();
    }
}
