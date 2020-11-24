package snow.music.store;

import io.objectbox.annotation.Entity;
import io.objectbox.annotation.Id;
import io.objectbox.annotation.Index;
import io.objectbox.relation.ToOne;

@Entity
public class HistoryEntity {
    @Id
    long id;
    ToOne<Music> music;
    @Index
    long timestamp;

    public HistoryEntity() {
    }

    public HistoryEntity(long id, long musicId, long timestamp) {
        this.id = id;
        this.music.setTargetId(musicId);
        this.timestamp = timestamp;
    }

    public Music getMusic() {
        return music.getTarget();
    }
}
