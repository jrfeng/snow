package snow.player.util;

import android.content.Context;

import androidx.annotation.NonNull;

import com.google.common.base.Preconditions;

import snow.player.R;
import snow.player.audio.MusicItem;

/**
 * 用于帮助获取 {@link MusicItem} 的 title, artist, album 值。
 */
public final class MusicItemUtil {
    private MusicItemUtil() {
        throw new AssertionError();
    }

    /**
     * 获取歌曲的标题，如果标题为空，则返回默认值。
     */
    public static String getTitle(@NonNull MusicItem musicItem, @NonNull String defaultTitle) {
        Preconditions.checkNotNull(musicItem);
        Preconditions.checkNotNull(defaultTitle);

        return getStringOrDefault(musicItem.getTitle(), defaultTitle);
    }

    /**
     * 获取歌曲的艺术家（歌手），如果艺术家为空，则返回默认值。
     */
    public static String getArtist(@NonNull MusicItem musicItem, @NonNull String defaultArtist) {
        Preconditions.checkNotNull(musicItem);
        Preconditions.checkNotNull(defaultArtist);

        return getStringOrDefault(musicItem.getArtist(), defaultArtist);
    }

    /**
     * 获取歌曲的专辑，如果专辑为空，则返回默认值。
     */
    public static String getAlbum(@NonNull MusicItem musicItem, @NonNull String defaultAlbum) {
        Preconditions.checkNotNull(musicItem);
        Preconditions.checkNotNull(defaultAlbum);

        return getStringOrDefault(musicItem.getAlbum(), defaultAlbum);
    }

    /**
     * 获取歌曲的标题，如果标题为空，则返回字符串 {@code "未知标题"}。
     */
    public static String getTitle(@NonNull Context context, @NonNull MusicItem musicItem) {
        return getTitle(musicItem, context.getString(R.string.snow_music_item_unknown_title));
    }

    /**
     * 获取歌曲的艺术家（歌手），如果艺术家为空，则返回字符串 {@code "未知歌手"}。
     */
    public static String getArtist(@NonNull Context context, @NonNull MusicItem musicItem) {
        return getArtist(musicItem, context.getString(R.string.snow_music_item_unknown_artist));
    }

    /**
     * 获取歌曲的专辑，如果专辑为空，则返回字符串 {@code "未知专辑"}。
     */
    public static String getAlbum(@NonNull Context context, @NonNull MusicItem musicItem) {
        return getAlbum(musicItem, context.getString(R.string.snow_music_item_unknown_album));
    }

    // value 参数为空时返回 defaultValue 参数
    private static String getStringOrDefault(String value, String defaultValue) {
        return value.isEmpty() ? defaultValue : value;
    }
}
