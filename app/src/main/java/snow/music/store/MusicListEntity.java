package snow.music.store;

import io.objectbox.annotation.Convert;
import io.objectbox.annotation.Entity;
import io.objectbox.annotation.Id;
import io.objectbox.annotation.Unique;
import io.objectbox.relation.ToMany;
import snow.music.store.converter.SortOrderConverter;

/**
 * 这是一个 ObjectBox Entity, 请不要直接用于该类。
 */
@Entity
public class MusicListEntity {
    @Id
    long id;
    @Unique
    String name;
    String description;
    int size;
    @Convert(converter = SortOrderConverter.class, dbType = Integer.class)
    MusicList.SortOrder sortOrder;

    byte[] orderBytes;
    ToMany<Music> musicElements;

    public MusicListEntity() {
        this.name = "";
        this.description = "";
    }

    public MusicListEntity(long id, String name, String description, byte[] orderBytes) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.orderBytes = orderBytes;
    }
}
