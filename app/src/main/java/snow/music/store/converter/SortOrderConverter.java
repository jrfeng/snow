package snow.music.store.converter;

import io.objectbox.converter.PropertyConverter;
import snow.music.store.MusicList;

public class SortOrderConverter implements PropertyConverter<MusicList.SortOrder, Integer> {
    @Override
    public MusicList.SortOrder convertToEntityProperty(Integer databaseValue) {
        if (databaseValue == null) {
            return MusicList.SortOrder.BY_ADD_TIME;
        }

        return MusicList.SortOrder.getValueById(databaseValue);
    }

    @Override
    public Integer convertToDatabaseValue(MusicList.SortOrder entityProperty) {
        return entityProperty == null ? MusicList.SortOrder.BY_ADD_TIME.id : entityProperty.id;
    }
}
