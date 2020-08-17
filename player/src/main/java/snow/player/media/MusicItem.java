package snow.player.media;

import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.common.base.Objects;
import com.google.common.base.Preconditions;

/**
 * 用于存储与音乐相关的简单数据。
 * <p>
 * 如果需要存储额外的数据，可以使用 {@link #setExtra(Bundle)} 方法与 {@link #getExtra()} 方法。
 */
public final class MusicItem implements Parcelable {
    private String musicId;
    private String title;
    private String artist;
    private String album;
    private String uri;
    private String iconUri;
    private int duration;
    @Nullable
    private Bundle extra;

    /**
     * 构造一个 MusicItem 对象。
     */
    public MusicItem() {
        this.musicId = "";
        this.title = "";
        this.artist = "";
        this.album = "";
        this.uri = "";
        this.iconUri = "";
        this.duration = 0;
        this.extra = null;
    }

    /**
     * 对 {@code source} 进行拷贝。
     * <p>
     * 注意！不会对 {@code source} 携带的 {@code Extra} 进行深拷贝。
     *
     * @param source 要拷贝的 {@link MusicItem} 对象
     */
    public MusicItem(MusicItem source) {
        musicId = source.musicId;
        title = source.title;
        artist = source.artist;
        album = source.album;
        uri = source.uri;
        iconUri = source.iconUri;
        duration = source.duration;
        if (source.extra != null) {
            extra = new Bundle(source.extra);
        }
    }

    /**
     * 获取歌曲的 music id（NonNull）。
     *
     * @return 歌曲的 music id（NonNull）
     */
    @NonNull
    public String getMusicId() {
        return musicId;
    }

    /**
     * 设置歌曲的 music id（NonNull）。
     *
     * @param musicId 要设置的值（NonNull）
     */
    public void setMusicId(@NonNull String musicId) {
        Preconditions.checkNotNull(musicId);
        this.musicId = musicId;
    }

    /**
     * 获取歌曲的标题（NonNull）。
     *
     * @return 歌曲的标题（NonNull）
     */
    @NonNull
    public String getTitle() {
        return title;
    }

    /**
     * 设置歌曲的标题。
     *
     * @param title 要设置的标题（NonNull）
     */
    public void setTitle(@NonNull String title) {
        Preconditions.checkNotNull(title);
        this.title = title;
    }

    /**
     * 获取歌曲的艺术家（NonNull）。
     *
     * @return 歌曲的艺术家（NonNull）
     */
    @NonNull
    public String getArtist() {
        return artist;
    }

    /**
     * 设置歌曲的艺术家（NonNull）。
     *
     * @param artist 要设置的艺术家（NonNull）
     */
    public void setArtist(@NonNull String artist) {
        Preconditions.checkNotNull(artist);
        this.artist = artist;
    }

    /**
     * 获取歌曲的专辑（NonNull）。
     *
     * @return 歌曲的专辑（NonNull）
     */
    @NonNull
    public String getAlbum() {
        return album;
    }

    /**
     * 设置歌曲的专辑（NonNull）。
     *
     * @param album 要设置的专辑（NonNull）
     */
    public void setAlbum(@NonNull String album) {
        Preconditions.checkNotNull(album);
        this.album = album;
    }

    /**
     * 获取歌曲的 Uri（NonNull）
     *
     * @return 歌曲的 Uri（NonNull）
     */
    @NonNull
    public String getUri() {
        return uri;
    }

    /**
     * 设置歌曲的 Uri（NonNull）。
     *
     * @param uri 要设置的 Uri（NonNull）
     */
    public void setUri(@NonNull String uri) {
        Preconditions.checkNotNull(uri);
        this.uri = uri;
    }

    /**
     * 获取歌曲图标的 Uri（NonNull）。
     *
     * @return 歌曲图标的 Uri（NonNull）
     */
    @NonNull
    public String getIconUri() {
        return iconUri;
    }

    /**
     * 设置歌曲图标的 Uri（NonNull）。
     *
     * @param iconUri 要设置的图标 Uri（NonNull）
     */
    public void setIconUri(@NonNull String iconUri) {
        Preconditions.checkNotNull(iconUri);
        this.iconUri = iconUri;
    }

    /**
     * 获取歌曲的持续时间。
     *
     * @return 歌曲的持续时间
     */
    public int getDuration() {
        return duration;
    }

    /**
     * 设置歌曲的持续时间（播放时长）。
     *
     * @param duration 歌曲的持续时间，小于 0 时，duration 的值将被设置为 0
     */
    public void setDuration(int duration) {
        if (duration < 0) {
            this.duration = 0;
            return;
        }

        this.duration = duration;
    }

    /**
     * 获取携带的额外数据（Nullable）。
     *
     * @return 额外携带的数据（Nullable）
     */
    @Nullable
    public Bundle getExtra() {
        return extra;
    }

    /**
     * 设置 MusicItem 携带的额外数据。
     */
    public void setExtra(@Nullable Bundle extra) {
        this.extra = extra;
    }

    /**
     * 忽略携带的 {@code extra} 数据。
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof MusicItem)) return false;
        MusicItem other = (MusicItem) o;
        return Objects.equal(musicId, other.musicId) &&
                Objects.equal(title, other.title) &&
                Objects.equal(artist, other.artist) &&
                Objects.equal(album, other.album) &&
                Objects.equal(uri, other.uri) &&
                Objects.equal(iconUri, other.iconUri) &&
                Objects.equal(duration, other.duration);
    }

    /**
     * 忽略携带的 {@code extra} 数据。
     */
    @Override
    public int hashCode() {
        return Objects.hashCode(musicId,
                title,
                artist,
                album,
                uri,
                iconUri,
                duration);
    }

    @Override
    public String toString() {
        return "MusicItem{" +
                "musicId='" + musicId + '\'' +
                ", title='" + title + '\'' +
                ", artist='" + artist + '\'' +
                ", album='" + album + '\'' +
                ", uri='" + uri + '\'' +
                ", iconUri='" + iconUri + '\'' +
                ", duration=" + duration +
                '}';
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(this.musicId);
        dest.writeString(this.title);
        dest.writeString(this.artist);
        dest.writeString(this.album);
        dest.writeString(this.uri);
        dest.writeString(this.iconUri);
        dest.writeInt(this.duration);
        dest.writeParcelable(extra, 0);
    }

    /**
     * Parcelable 专用。
     */
    protected MusicItem(Parcel in) {
        this.musicId = in.readString();
        this.title = in.readString();
        this.artist = in.readString();
        this.album = in.readString();
        this.uri = in.readString();
        this.iconUri = in.readString();
        this.duration = in.readInt();
        this.extra = in.readParcelable(Thread.currentThread().getContextClassLoader());
    }

    public static final Creator<MusicItem> CREATOR = new Creator<MusicItem>() {
        @Override
        public MusicItem createFromParcel(Parcel source) {
            return new MusicItem(source);
        }

        @Override
        public MusicItem[] newArray(int size) {
            return new MusicItem[size];
        }
    };
}
