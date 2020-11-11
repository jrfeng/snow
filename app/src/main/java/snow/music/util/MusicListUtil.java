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
     * 将 {@link MusicList} 对象转换成一个 {@link Playlist} 对象。
     * <p>
     * 注意！如果 {@link MusicList} 的尺寸大于 {@link Playlist#MAX_SIZE}，则超出部分会被丢弃。
     *
     * @param musicList {@link MusicList} 对象，不能为 null。
     * @return {@link Playlist} 对象，不为 null
     */
    @NonNull
    public static Playlist asPlaylist(@NonNull MusicList musicList) {
        Preconditions.checkNotNull(musicList);

        Playlist.Builder builder = new Playlist.Builder()
                .setName(musicList.getName());

        for (Music music : musicList.getMusicElements()) {
            builder.append(MusicUtil.asMusicItem(music));
        }

        return builder.build();
    }

    /**
     * 将一个 {@code List<Music>} 列表转换成一个 {@link Playlist} 对象。
     * <p>
     * 注意！如果 {@code List<Music>} 列表的尺寸大于 {@link Playlist#MAX_SIZE}，则超出部分会被丢弃。
     *
     * @param name       String 对象，该 String 对象将作为 {@link Playlist} 的名称，不能为 null。
     * @param musicItems {@code List<Music>} 列表，不能为 null。
     * @return {@link Playlist} 对象，不为 null
     */
    @NonNull
    public static Playlist asPlaylist(@NonNull String name, @NonNull List<Music> musicItems) {
        Preconditions.checkNotNull(name);
        Preconditions.checkNotNull(musicItems);

        Playlist.Builder builder = new Playlist.Builder()
                .setName(name);

        for (Music music : musicItems) {
            builder.append(MusicUtil.asMusicItem(music));
        }

        return builder.build();
    }
}
