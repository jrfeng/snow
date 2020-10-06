package snow.music.store;

import io.objectbox.annotation.Entity;
import io.objectbox.annotation.Id;
import io.objectbox.annotation.Unique;
import io.objectbox.relation.ToMany;

@Entity
public class MusicListEntity {
    @Id
    long id;
    @Unique
    String name;
    String description;

    byte[] orderBytes;
    ToMany<Music> musicElements;

    public MusicListEntity(long id, String name, String description, byte[] orderBytes) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.orderBytes = orderBytes;
    }
}
