package snow.music.store;

import androidx.annotation.NonNull;

import com.google.common.base.Objects;

import io.objectbox.annotation.Entity;
import io.objectbox.annotation.Id;
import io.objectbox.annotation.Unique;

@Entity
public class Music {
    @Id
    long id;

    private String title;
    private String artist;
    private String album;
    @Unique
    private String uri;
    private String iconUri;
    private int duration;
    private long addTime;

    public Music(long id, String title, String artist, String album, String uri, String iconUri, int duration, long addTime) {
        this.id = id;
        this.title = title;
        this.artist = artist;
        this.album = album;
        this.uri = uri;
        this.iconUri = iconUri;
        this.duration = duration;
        this.addTime = addTime;
    }

    public long getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getArtist() {
        return artist;
    }

    public void setArtist(String artist) {
        this.artist = artist;
    }

    public String getAlbum() {
        return album;
    }

    public void setAlbum(String album) {
        this.album = album;
    }

    public String getUri() {
        return uri;
    }

    public void setUri(String uri) {
        this.uri = uri;
    }

    public String getIconUri() {
        return iconUri;
    }

    public void setIconUri(String iconUri) {
        this.iconUri = iconUri;
    }

    public int getDuration() {
        return duration;
    }

    public void setDuration(int duration) {
        this.duration = duration;
    }

    public long getAddTime() {
        return addTime;
    }

    public void setAddTime(long addTime) {
        this.addTime = addTime;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Music music = (Music) o;
        return id == music.id &&
                duration == music.duration &&
                addTime == music.addTime &&
                Objects.equal(title, music.title) &&
                Objects.equal(artist, music.artist) &&
                Objects.equal(album, music.album) &&
                Objects.equal(uri, music.uri) &&
                Objects.equal(iconUri, music.iconUri);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id, title, artist, album, uri, iconUri, duration, addTime);
    }

    @NonNull
    @Override
    public String toString() {
        return "Music{" +
                "id=" + id +
                ", title='" + title + '\'' +
                ", artist='" + artist + '\'' +
                ", album='" + album + '\'' +
                ", uri='" + uri + '\'' +
                ", iconUri='" + iconUri + '\'' +
                ", duration=" + duration +
                ", addTime=" + addTime +
                '}';
    }
}
