package snow.player.util;

import android.content.Context;

import androidx.annotation.NonNull;

import com.google.common.base.Preconditions;

import snow.player.R;
import snow.player.media.MusicItem;

public final class MusicItemUtil {
    private MusicItemUtil() {
        throw new AssertionError();
    }

    /**
     * 获取歌曲的标题，如果标题为空，则返回字符串 {@code "未知标题"}。
     */
    public static String getTitle(@NonNull Context context, @NonNull MusicItem musicItem) {
        Preconditions.checkNotNull(context);
        Preconditions.checkNotNull(musicItem);

        String title = musicItem.getTitle();

        if (title.isEmpty()) {
            return context.getString(R.string.snow_music_item_unknown_title);
        }

        return title;
    }

    /**
     * 获取歌曲的艺术家（歌手），如果艺术家为空，则返回字符串 {@code "未知歌手"}。
     */
    public static String getArtist(@NonNull Context context, @NonNull MusicItem musicItem) {
        Preconditions.checkNotNull(context);
        Preconditions.checkNotNull(musicItem);

        String artist = musicItem.getArtist();

        if (artist.isEmpty()) {
            return context.getString(R.string.snow_music_item_unknown_artist);
        }

        return artist;
    }

    /**
     * 获取歌曲的专辑，如果专辑为空，则返回字符串 {@code "未知专辑"}。
     */
    public static String getAlbum(@NonNull Context context, @NonNull MusicItem musicItem) {
        Preconditions.checkNotNull(context);
        Preconditions.checkNotNull(musicItem);

        String album = musicItem.getAlbum();

        if (album.isEmpty()) {
            return context.getString(R.string.snow_music_item_unknown_album);
        }

        return album;
    }
}
