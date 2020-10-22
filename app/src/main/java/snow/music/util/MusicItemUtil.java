package snow.music.util;

import android.os.Bundle;

import androidx.annotation.NonNull;

import com.google.common.base.Preconditions;

import snow.music.store.Music;
import snow.player.audio.MusicItem;

/**
 * 用于在 {@link Music} 与 {@link MusicItem} 之间进行类型转换。
 */
public final class MusicItemUtil {
    private static final String KEY_ADD_TIME = "add_time";

    private MusicItemUtil() {
        throw new AssertionError();
    }

    public static Music asMusic(@NonNull MusicItem musicItem) {
        Preconditions.checkNotNull(musicItem);

        return new Music(
                Long.parseLong(musicItem.getMusicId()),
                musicItem.getTitle(),
                musicItem.getArtist(),
                musicItem.getAlbum(),
                musicItem.getUri(),
                musicItem.getIconUri(),
                musicItem.getDuration(),
                getAddTime(musicItem)
        );
    }

    public static MusicItem asMusicItem(@NonNull Music music) {
        Preconditions.checkNotNull(music);

        MusicItem musicItem = new MusicItem.Builder()
                .setMusicId(String.valueOf(music.getId()))
                .setTitle(music.getTitle())
                .setArtist(music.getArtist())
                .setAlbum(music.getAlbum())
                .setUri(music.getUri())
                .setIconUri(music.getIconUri())
                .setDuration(music.getDuration())
                .build();

        putAddTime(musicItem, music);

        return musicItem;
    }

    private static long getAddTime(MusicItem musicItem) {
        Bundle extra = musicItem.getExtra();
        if (extra == null) {
            return System.currentTimeMillis();
        }

        return extra.getLong(KEY_ADD_TIME, System.currentTimeMillis());
    }

    private static void putAddTime(MusicItem musicItem, Music music) {
        Bundle extra = new Bundle();
        extra.putLong(KEY_ADD_TIME, music.getAddTime());
        musicItem.setExtra(extra);
    }
}
