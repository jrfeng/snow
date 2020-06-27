package snow.player.media;

import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.common.base.Objects;
import com.google.common.base.Preconditions;

/**
 * 用于存储于音乐相关的简单数据。
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
    private String token;
    @Nullable
    private Bundle mExtra;

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
        this.token = "";
        this.mExtra = null;
    }

    public MusicItem(MusicItem source) {
        musicId = source.musicId;
        title = source.title;
        artist = source.artist;
        album = source.album;
        uri = source.uri;
        iconUri = source.iconUri;
        duration = source.duration;
        token = source.token;
        if (source.mExtra != null) {
            mExtra = new Bundle(source.mExtra);
        }
    }

    @NonNull
    public String getMusicId() {
        return musicId;
    }

    public void setMusicId(@NonNull String musicId) {
        Preconditions.checkNotNull(musicId);
        this.musicId = musicId;
    }

    @NonNull
    public String getTitle() {
        return title;
    }

    public void setTitle(@NonNull String title) {
        Preconditions.checkNotNull(title);
        this.title = title;
    }

    @NonNull
    public String getArtist() {
        return artist;
    }

    public void setArtist(@NonNull String artist) {
        Preconditions.checkNotNull(artist);
        this.artist = artist;
    }

    @NonNull
    public String getAlbum() {
        return album;
    }

    public void setAlbum(@NonNull String album) {
        Preconditions.checkNotNull(album);
        this.album = album;
    }

    @NonNull
    public String getUri() {
        return uri;
    }

    public void setUri(@NonNull String uri) {
        Preconditions.checkNotNull(uri);
        this.uri = uri;
    }

    @NonNull
    public String getIconUri() {
        return iconUri;
    }

    public void setIconUri(@NonNull String iconUri) {
        Preconditions.checkNotNull(iconUri);
        this.iconUri = iconUri;
    }

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

    @NonNull
    public String getToken() {
        return token;
    }

    public void setToken(@NonNull String token) {
        Preconditions.checkNotNull(token);
        this.token = token;
    }

    @Nullable
    public Bundle getExtra() {
        return mExtra;
    }

    /**
     * 设置 MusicItem 携带的额外数据。
     */
    public void setExtra(@Nullable Bundle extra) {
        mExtra = extra;
    }

    /**
     * 只要 token 相同就会返回 true, 否则返回 false。如果你要判断两个 MusicItem 对象的内容是否相同（忽略
     * token 字段），请使用 {@link #same(MusicItem)} 方法。
     */
    @Override
    public boolean equals(@Nullable Object obj) {
        if (obj instanceof MusicItem) {
            MusicItem other = (MusicItem) obj;
            return Objects.equal(this.getToken(), other.getToken());
        }
        return false;
    }

    /**
     * hashCode 的值只和 token 属性相关。
     */
    @Override
    public int hashCode() {
        return Objects.hashCode(this.token);
    }

    /**
     * 判断两个 MusicItem 对象的内容是否相同（忽略 token 字段与携带的 extra）。
     */
    public boolean same(MusicItem other) {
        if (other == null) {
            return false;
        }

        return Objects.equal(this.musicId, other.musicId)
                && Objects.equal(this.title, other.title)
                && Objects.equal(this.artist, other.artist)
                && Objects.equal(this.album, other.album)
                && Objects.equal(this.uri, other.uri)
                && Objects.equal(this.iconUri, other.iconUri)
                && Objects.equal(this.duration, other.duration);
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
        dest.writeString(this.token);
        dest.writeParcelable(mExtra, 0);
    }

    protected MusicItem(Parcel in) {
        this.musicId = in.readString();
        this.title = in.readString();
        this.artist = in.readString();
        this.album = in.readString();
        this.uri = in.readString();
        this.iconUri = in.readString();
        this.duration = in.readInt();
        this.token = in.readString();
        this.mExtra = in.readParcelable(Thread.currentThread().getContextClassLoader());
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
