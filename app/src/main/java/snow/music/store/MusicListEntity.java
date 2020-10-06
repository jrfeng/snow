package snow.music.store;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.common.base.Objects;

import io.objectbox.annotation.Entity;
import io.objectbox.annotation.Id;
import io.objectbox.annotation.Unique;
import io.objectbox.relation.ToMany;

@Entity
public class MusicListEntity implements ToManyWrapper.OrderedSource<Music> {
    @Id
    long id;
    @Unique
    private String name;
    private String description;

    byte[] orderBytes;
    ToMany<Music> musicElements;

    public MusicListEntity(long id, String name, String description, byte[] orderBytes) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.orderBytes = orderBytes;
    }

    public long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MusicListEntity musicList1 = (MusicListEntity) o;
        return id == musicList1.id &&
                Objects.equal(name, musicList1.name);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id, name);
    }

    @NonNull
    @Override
    public String toString() {
        return "MusicList{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", description='" + description + '\'' +
                '}';
    }

    @NonNull
    @Override
    public ToMany<Music> getToMany() {
        return this.musicElements;
    }

    @Nullable
    @Override
    public byte[] getOrderBytes() {
        return this.orderBytes;
    }

    @Override
    public void setOrderBytes(byte[] orderBytes) {
        this.orderBytes = orderBytes;
    }

    @Override
    public long getTargetId(Music music) {
        return music.getId();
    }
}
