package snow.music.store;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.common.base.Preconditions;

import java.io.File;
import java.util.List;

import io.objectbox.Box;
import io.objectbox.BoxStore;

public class MusicStore {
    public static final String MUSIC_LIST_LOCAL_MUSIC = "__local_music";
    public static final String MUSIC_LIST_FAVORITE = "__favorite";
    public static final String MUSIC_LIST_HISTORY = "__history";

    private static final int MAX_HISTORY_SIZE = 500;

    private static MusicStore mInstance;

    private Box<Music> mMusicBox;
    private Box<MusicListEntity> mMusicListEntityBox;

    private MusicList mHistory;

    private MusicStore(BoxStore boxStore) {
        mMusicBox = boxStore.boxFor(Music.class);
        mMusicListEntityBox = boxStore.boxFor(MusicListEntity.class);
    }

    /**
     * 初始化 {@link MusicStore}
     *
     * @param context Context 对象，不能为 null
     */
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

    /**
     * 初始化 {@link MusicStore}
     *
     * @param boxStore BoxStore 对象，不能为 null
     */
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
    public synchronized boolean isMusicListExists(@NonNull String name) {
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
    public synchronized MusicList createMusicList(@NonNull String name, @NonNull String description) throws IllegalArgumentException {
        Preconditions.checkNotNull(name);
        Preconditions.checkNotNull(description);
        Preconditions.checkArgument(name.isEmpty(), "name must not empty");

        if (isBuiltInName(name)) {
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
    public synchronized MusicList getMusicList(@NonNull String name) {
        Preconditions.checkNotNull(name);

        if (isBuiltInName(name)) {
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
    public synchronized void updateMusicList(@NonNull MusicList musicList) {
        Preconditions.checkNotNull(musicList);

        if (!isMusicListExists(musicList.getName())) {
            return;
        }

        musicList.applyChanges();
        mMusicListEntityBox.put(musicList.getMusicListEntity());
    }

    /**
     * 删除歌单。
     * <p>
     * 不允许删除内置歌单。
     */
    public synchronized void deleteMusicList(@NonNull MusicList musicList) {
        Preconditions.checkNotNull(musicList);
        deleteMusicList(musicList.getName());
    }

    /**
     * 删除歌单。
     * <p>
     * 不允许删除内置歌单。
     */
    public synchronized void deleteMusicList(@NonNull String name) {
        Preconditions.checkNotNull(name);

        if (isBuiltInName(name)) {
            return;
        }

        mMusicListEntityBox.query()
                .equal(MusicListEntity_.name, name)
                .build()
                .remove();
    }

    /**
     * 歌曲是否是 “我喜欢”
     */
    public synchronized boolean isFavorite(@NonNull Music music) {
        Preconditions.checkNotNull(music);

        return isFavorite(music.getId());
    }

    /**
     * 指定 musicId 的歌曲是否是 “我喜欢”
     */
    public synchronized boolean isFavorite(long musicId) {
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

    /**
     * 获取 “本地音乐” 歌单。
     */
    public synchronized MusicList getLocalMusicList() {
        return getBuiltInMusicList(MUSIC_LIST_LOCAL_MUSIC);
    }

    /**
     * 获取 “我喜欢” 歌单。
     */
    public synchronized MusicList getFavoriteMusicList() {
        return getBuiltInMusicList(MUSIC_LIST_FAVORITE);
    }

    /**
     * 将歌曲添加到 “我喜欢” 歌单。
     */
    public synchronized void addToFavorite(@NonNull Music music) {
        Preconditions.checkNotNull(music);

        if (isFavorite(music)) {
            return;
        }

        MusicList favorite = getFavoriteMusicList();
        favorite.getMusicElements().add(music);
        updateMusicList(favorite);
    }

    /**
     * 将歌曲从 “我喜欢” 歌单中移除。
     */
    public synchronized void removeFromFavorite(@NonNull Music music) {
        Preconditions.checkNotNull(music);

        if (isFavorite(music)) {
            MusicList favorite = getFavoriteMusicList();
            favorite.getMusicElements().remove(music);
            updateMusicList(favorite);
        }
    }

    /**
     * 指定 name 名称是否是内置歌单名。如果是，则返回 true，否则返回 false。
     */
    public static boolean isBuiltInName(String name) {
        return name.equalsIgnoreCase(MUSIC_LIST_LOCAL_MUSIC) ||
                name.equalsIgnoreCase(MUSIC_LIST_FAVORITE) ||
                name.equalsIgnoreCase(MUSIC_LIST_HISTORY);
    }

    /**
     * 添加一条历史记录。
     */
    public synchronized void addHistory(@NonNull Music music) {
        Preconditions.checkNotNull(music);

        MusicList history = getHistoryMusicList();
        List<Music> elements = history.getMusicElements();

        elements.remove(music);
        elements.add(music);

        if (history.getSize() - MAX_HISTORY_SIZE > 0) {
            elements.remove(0);
        }

        updateMusicList(history);
    }

    /**
     * 移除一条历史记录。
     */
    public synchronized void removeHistory(@NonNull Music music) {
        Preconditions.checkNotNull(music);

        MusicList history = getHistoryMusicList();
        history.getMusicElements().remove(music);

        updateMusicList(history);
    }

    /**
     * 移除多条历史记录。
     */
    public synchronized void removeHistory(@NonNull List<Music> musics) {
        Preconditions.checkNotNull(musics);

        MusicList history = getHistoryMusicList();
        history.getMusicElements().removeAll(musics);

        updateMusicList(history);
    }

    /**
     * 清空历史记录。
     */
    public synchronized void clearHistory() {
        MusicList history = getHistoryMusicList();
        history.getMusicElements().clear();
        updateMusicList(history);
    }

    private synchronized MusicList getHistoryMusicList() {
        if (mHistory == null) {
            mHistory = getBuiltInMusicList(MUSIC_LIST_HISTORY);
        }

        return mHistory;
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
