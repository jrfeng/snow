package snow.player.media;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.common.base.Preconditions;

/**
 * 用于构建 {@link MusicItem} 对象。
 */
public class MusicItemBuilder {
    private String musicId = "";
    private String title = "Unknown";
    private String artist = "Unknown";
    private String album = "Unknown";
    private String uri;
    private String iconUri = "";
    private int duration;
    private boolean forbidSeek = false;
    private Bundle extra;

    /**
     * 创建一个 {@link MusicItemBuilder} 构建器。
     *
     * @param duration 歌曲的持续时长（单位：毫秒），不能小于 0
     * @param uri      歌曲的播放链接，不能为 null
     * @throws IllegalArgumentException 如果歌曲的持续时长小于 0，则会抛出该异常
     */
    public MusicItemBuilder(int duration, @NonNull String uri) throws IllegalArgumentException {
        Preconditions.checkNotNull(uri);
        if (duration < 0) {
            throw new IllegalArgumentException("duration must >= 0.");
        }

        this.uri = uri;
        this.duration = duration;
    }

    /**
     * 设置歌曲的 music id（NonNull）。
     *
     * @param musicId 要设置的值（NonNull）
     */
    public MusicItemBuilder setMusicId(@NonNull String musicId) {
        Preconditions.checkNotNull(musicId);
        this.musicId = musicId;
        return this;
    }

    /**
     * 设置歌曲的标题。
     *
     * @param title 要设置的标题（NonNull）
     */
    public MusicItemBuilder setTitle(@NonNull String title) {
        Preconditions.checkNotNull(title);
        this.title = title;
        return this;
    }

    /**
     * 设置歌曲的艺术家（NonNull）。
     *
     * @param artist 要设置的艺术家（NonNull）
     */
    public MusicItemBuilder setArtist(@NonNull String artist) {
        Preconditions.checkNotNull(artist);
        this.artist = artist;
        return this;
    }

    /**
     * 设置歌曲的专辑（NonNull）。
     *
     * @param album 要设置的专辑（NonNull）
     */
    public MusicItemBuilder setAlbum(@NonNull String album) {
        Preconditions.checkNotNull(album);
        this.album = album;
        return this;
    }

    /**
     * 设置歌曲的 Uri（NonNull）。
     *
     * @param uri 要设置的 Uri（NonNull）
     */
    public MusicItemBuilder setUri(@NonNull String uri) {
        Preconditions.checkNotNull(uri);
        this.uri = uri;
        return this;
    }

    /**
     * 设置歌曲图标的 Uri（NonNull）。
     *
     * @param iconUri 要设置的图标 Uri（NonNull）
     */
    public MusicItemBuilder setIconUri(@NonNull String iconUri) {
        Preconditions.checkNotNull(iconUri);
        this.iconUri = iconUri;
        return this;
    }

    /**
     * 设置歌曲的持续时间（播放时长）。
     *
     * @param duration 歌曲的持续时间，小于 0 时，duration 的值将被设置为 0
     * @throws IllegalArgumentException 如果歌曲的持续时长小于 0，则会抛出该异常
     */
    public MusicItemBuilder setDuration(int duration) throws IllegalArgumentException {
        if (duration < 0) {
            throw new IllegalArgumentException("duration music >= 0.");
        }

        this.duration = duration;
        return this;
    }

    /**
     * 设置是否禁用 seekTo 操作。
     *
     * @param forbidSeek 如果为 true，则会同时禁用 seekTo、fastForward、rewind 操作。
     */
    public void setForbidSeek(boolean forbidSeek) {
        this.forbidSeek = forbidSeek;
    }

    /**
     * 设置 MusicItem 携带的额外数据。
     */
    public MusicItemBuilder setExtra(@Nullable Bundle extra) {
        this.extra = extra;
        return this;
    }

    public MusicItem build() {
        MusicItem musicItem = new MusicItem();

        musicItem.setMusicId(musicId);
        musicItem.setTitle(title);
        musicItem.setArtist(artist);
        musicItem.setAlbum(album);
        musicItem.setUri(uri);
        musicItem.setIconUri(iconUri);
        musicItem.setDuration(duration);
        musicItem.setForbidSeek(forbidSeek);
        musicItem.setExtra(extra);

        return musicItem;
    }
}
