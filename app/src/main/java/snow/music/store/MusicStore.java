package snow.music.store;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.common.base.Preconditions;

import java.io.File;

import io.objectbox.Box;
import io.objectbox.BoxStore;

public class MusicStore {
    private static final String MUSIC_LIST_LOCAL_MUSIC = "__local_music";
    private static final String MUSIC_LIST_FAVORITE = "__favorite";
    private static final String MUSIC_LIST_HISTORY = "__history";

    private static MusicStore mInstance;

    private Box<Music> mMusicBox;
    private Box<MusicListEntity> mMusicListEntityBox;

    private MusicStore(BoxStore boxStore) {
        mMusicBox = boxStore.boxFor(Music.class);
        mMusicListEntityBox = boxStore.boxFor(MusicListEntity.class);
    }

    public synchronized static void init(@NonNull Context context) {
        Preconditions.checkNotNull(context);

        if (mInstance != null) {
            return;
        }

        BoxStore boxStore = MyObjectBox.builder()
                .directory(new File(context.getFilesDir(), "music_store"))
                .build();

        mInstance = new MusicStore(boxStore);
    }

    public synchronized static void init(@NonNull BoxStore boxStore) {
        Preconditions.checkNotNull(boxStore);

        if (mInstance != null) {
            return;
        }

        mInstance = new MusicStore(boxStore);
    }

    public static MusicStore getInstance() throws IllegalStateException {
        if (mInstance == null) {
            throw new IllegalStateException("music store not init yet.");
        }

        return mInstance;
    }

    /**
     * 歌单是否已存在。
     */
    public boolean isMusicListExists(@NonNull String name) {
        Preconditions.checkNotNull(name);

        long count = mMusicListEntityBox.query()
                .equal(MusicListEntity_.name, name)
                .build()
                .count();

        return count > 0;
    }

    /**
     * 创建一个新的歌单，如果歌单已存在，则直接返回它，不会创建新歌单。
     *
     * @throws IllegalArgumentException 如果 name 参数是个空字符串，则抛出该异常。
     */
    @NonNull
    public MusicList createMusicList(@NonNull String name, @NonNull String description) throws IllegalArgumentException {
        Preconditions.checkNotNull(name);
        Preconditions.checkNotNull(description);
        Preconditions.checkArgument(name.isEmpty(), "name must not empty");

        if (isBuiltIn(name)) {
            throw new IllegalArgumentException("Illegal music list name, conflicts with built-in name.");
        }

        if (isMusicListExists(name)) {
            MusicList musicList = getMusicList(name);
            assert musicList != null;
            return musicList;
        }

        MusicListEntity entity = new MusicListEntity(0, name, description, null);
        mMusicListEntityBox.put(entity);
        return new MusicList(entity);
    }

    /**
     * 获取自建歌单，如果自建歌单不存在，则返回 null。
     */
    @Nullable
    public MusicList getMusicList(@NonNull String name) {
        Preconditions.checkNotNull(name);

        if (isBuiltIn(name)) {
            return null;
        }

        MusicListEntity entity = mMusicListEntityBox.query()
                .equal(MusicListEntity_.name, name)
                .build()
                .findUnique();

        if (entity == null) {
            return null;
        }

        return new MusicList(entity);
    }

    /**
     * 更新歌单。
     */
    public void updateMusicList(@NonNull MusicList musicList) {
        Preconditions.checkNotNull(musicList);

        if (isBuiltIn(musicList.getName())) {
            return;
        }

        if (!isMusicListExists(musicList.getName())) {
            return;
        }

        musicList.applyChanges();
        mMusicListEntityBox.put(musicList.getMusicListEntity());
    }

    /**
     * 删除歌单。
     */
    public void deleteMusicList(@NonNull MusicList musicList) {
        Preconditions.checkNotNull(musicList);
        deleteMusicList(musicList.getName());
    }

    /**
     * 删除歌单。
     */
    public void deleteMusicList(@NonNull String name) {
        Preconditions.checkNotNull(name);

        if (isBuiltIn(name)) {
            return;
        }

        mMusicListEntityBox.query()
                .equal(MusicListEntity_.name, name)
                .build()
                .remove();
    }

    public boolean isFavorite(@NonNull Music music) {
        Preconditions.checkNotNull(music);

        return isFavorite(music.getId());
    }

    public boolean isFavorite(long musicId) {
        if (musicId <= 0) {
            return false;
        }

        long count = mMusicBox.query()
                .equal(Music_.id, musicId)
                .backlink(MusicListEntity_.musicElements)
                .equal(MusicListEntity_.name, MUSIC_LIST_FAVORITE)
                .build()
                .count();

        return count > 0;
    }

    private boolean isBuiltIn(String name) {
        return name.equalsIgnoreCase(MUSIC_LIST_LOCAL_MUSIC) ||
                name.equalsIgnoreCase(MUSIC_LIST_FAVORITE) ||
                name.equalsIgnoreCase(MUSIC_LIST_HISTORY);
    }

    @NonNull
    private synchronized MusicList getBuiltInMusicList(String name) {
        MusicList musicList = getMusicList(name);
        if (musicList != null) {
            return musicList;
        }

        MusicListEntity entity = new MusicListEntity(0, name, "", new byte[0]);
        mMusicListEntityBox.put(entity);

        return new MusicList(entity);
    }
}
