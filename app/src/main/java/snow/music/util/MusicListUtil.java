package snow.music.util;

import androidx.annotation.NonNull;

import com.google.common.base.Preconditions;

import java.util.List;

import snow.music.store.Music;
import snow.music.store.MusicList;
import snow.player.playlist.Playlist;

public final class MusicListUtil {
    private MusicListUtil() {
        throw new AssertionError();
    }

    public static Playlist asPlaylist(@NonNull MusicList musicList) {
        Preconditions.checkNotNull(musicList);

        Playlist.Builder builder = new Playlist.Builder()
                .setName(musicList.getName());

        for (Music music : musicList.getMusicElements()) {
            builder.append(MusicUtil.asMusicItem(music));
        }

        return builder.build();
    }

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
