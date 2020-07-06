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
     * 忽略携带的 {@code extra} 数据。
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MusicItem musicItem = (MusicItem) o;
        return duration == musicItem.duration &&
                Objects.equal(musicId, musicItem.musicId) &&
                Objects.equal(title, musicItem.title) &&
                Objects.equal(artist, musicItem.artist) &&
                Objects.equal(album, musicItem.album) &&
                Objects.equal(uri, musicItem.uri) &&
                Objects.equal(iconUri, musicItem.iconUri);
    }

    /**
     * 忽略携带的 {@code extra} 数据。
     */
    @Override
    public int hashCode() {
        return Objects.hashCode(musicId, title, artist, album, uri, iconUri, duration);
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
