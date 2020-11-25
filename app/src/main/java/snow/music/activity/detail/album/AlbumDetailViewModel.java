package snow.music.activity.detail.album;

import androidx.annotation.NonNull;

import java.util.Collections;
import java.util.List;

import pinyin.util.PinyinComparator;
import snow.music.fragment.musiclist.BaseMusicListViewModel;
import snow.music.store.Music;
import snow.music.store.MusicList;
import snow.music.store.MusicStore;

public class AlbumDetailViewModel extends BaseMusicListViewModel {
    @NonNull
    @Override
    protected List<Music> loadMusicListItems() {
        List<Music> musicList = MusicStore.getInstance().getAlbumAllMusic(getAlbumName());

        PinyinComparator pinyinComparator = new PinyinComparator();
        Collections.sort(musicList, (o1, o2) -> pinyinComparator.compare(o1.getTitle(), o2.getTitle()));

        return musicList;
    }

    @Override
    protected void removeMusic(@NonNull Music music) {
        // ignore
    }

    @Override
    protected void onSortMusicList(@NonNull MusicList.SortOrder sortOrder) {
        // ignore
    }

    @NonNull
    @Override
    protected MusicList.SortOrder getSortOrder() {
        return MusicList.SortOrder.BY_TITLE;
    }

    // 去除 album: 前缀
    private String getAlbumName() {
        String album = getMusicListName();
        return album.substring(AlbumDetailActivity.ALBUM_PREFIX.length());
    }
}
