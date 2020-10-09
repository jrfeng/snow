package snow.music.store;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;

import com.google.common.base.Preconditions;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import io.objectbox.Box;
import io.objectbox.BoxStore;
import io.objectbox.android.ObjectBoxLiveData;
import io.objectbox.query.Query;
import io.objectbox.query.QueryBuilder;

public class MusicStore {
    public static final String MUSIC_LIST_FAVORITE = "__favorite";
    public static final String MUSIC_LIST_HISTORY = "__history";

    private static final int MAX_HISTORY_SIZE = 500;

    private static MusicStore mInstance;

    private BoxStore mBoxStore;
    private Box<Music> mMusicBox;
    private Box<MusicListEntity> mMusicListEntityBox;

    private MusicList mHistory;
    private MutableLiveData<List<Music>> mHistoryLiveData;

    private MusicStore(BoxStore boxStore) {
        mBoxStore = boxStore;
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
        mInstance = new MusicStore(boxStore);
    }

    public static MusicStore getInstance() throws IllegalStateException {
        if (mInstance == null) {
            throw new IllegalStateException("music store not init yet.");
        }

        return mInstance;
    }

    /**
     * 获取当前数据库的 BoxStore 对象，如果数据库还没有初始化，则会抛出 {@link IllegalStateException}
     *
     * @return 当前数据库的 BoxStore 对象
     */
    public synchronized BoxStore getBoxStore() throws IllegalStateException {
        if (mBoxStore == null) {
            throw new IllegalStateException("MusicStore not init yet.");
        }

        return mBoxStore;
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

    private boolean isMusicListExists(long id) {
        long count = mMusicListEntityBox.query()
                .equal(MusicListEntity_.id, id)
                .build()
                .count();

        return count > 0;
    }

    /**
     * 创建一个新的歌单，如果歌单已存在，则直接返回它，不会创建新歌单。
     *
     * @throws IllegalArgumentException 如果 name 参数是个空字符串或者内置名称，则抛出该异常。
     */
    @NonNull
    public synchronized MusicList createMusicList(@NonNull String name) throws IllegalArgumentException {
        return createMusicList(name, "");
    }

    /**
     * 创建一个新的歌单，如果歌单已存在，则直接返回它，不会创建新歌单。
     *
     * @throws IllegalArgumentException 如果 name 参数是个空字符串或者内置名称，则抛出该异常。
     */
    @NonNull
    public synchronized MusicList createMusicList(@NonNull String name, @NonNull String description) throws IllegalArgumentException {
        Preconditions.checkNotNull(name);
        Preconditions.checkNotNull(description);
        Preconditions.checkArgument(!name.isEmpty(), "name must not empty");

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

        if (!isMusicListExists(musicList.getId())) {
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

        mMusicListEntityBox.query()
                .equal(MusicListEntity_.id, musicList.getId())
                .build()
                .remove();
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
     * 获取所有自建歌单（不包括内置歌单）。
     */
    @NonNull
    public synchronized List<MusicList> getAllMusicList() {
        List<MusicListEntity> allEntity = mMusicListEntityBox.query()
                .notEqual(MusicListEntity_.name, MUSIC_LIST_FAVORITE)
                .and()
                .notEqual(MusicListEntity_.name, MUSIC_LIST_HISTORY)
                .build()
                .find();

        if (allEntity.isEmpty()) {
            return Collections.emptyList();
        }

        List<MusicList> allMusicList = new ArrayList<>(allEntity.size());

        for (MusicListEntity entity : allEntity) {
            allMusicList.add(new MusicList(entity));
        }

        return allMusicList;
    }

    /**
     * 监听 “我喜欢” 歌单。
     */
    @NonNull
    public synchronized LiveData<MusicList> observeFavorite() {
        Query<MusicListEntity> favoriteQuery = mMusicListEntityBox.query()
                .equal(MusicListEntity_.name, MUSIC_LIST_FAVORITE)
                .build();

        if (favoriteQuery.count() < 0) {
            createBuiltInMusicList(MUSIC_LIST_FAVORITE);
        }

        ObjectBoxLiveData<MusicListEntity> favoriteLiveData = new ObjectBoxLiveData<>(favoriteQuery);
        return Transformations.map(favoriteLiveData, input -> new MusicList(input.get(0)));
    }

    /**
     * 监听历史记录。
     */
    public synchronized LiveData<List<Music>> observeHistory() {
        if (mHistoryLiveData == null) {
            mHistoryLiveData = new MutableLiveData<>(getHistoryMusicList().getMusicElements());
        }
        return mHistoryLiveData;
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

        QueryBuilder<Music> builder = mMusicBox.query().equal(Music_.id, musicId);
        builder.backlink(MusicListEntity_.musicElements)
                .equal(MusicListEntity_.name, MUSIC_LIST_FAVORITE);

        return builder.build().count() > 0;
    }

    /**
     * 获取 “我喜欢” 歌单。
     */
    @NonNull
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
        return name.equalsIgnoreCase(MUSIC_LIST_FAVORITE) ||
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

        mHistoryLiveData.setValue(new ArrayList<>(history.getMusicElements()));
        updateMusicList(history);
    }

    /**
     * 移除一条历史记录。
     */
    public synchronized void removeHistory(@NonNull Music music) {
        Preconditions.checkNotNull(music);

        MusicList history = getHistoryMusicList();
        history.getMusicElements().remove(music);

        mHistoryLiveData.setValue(new ArrayList<>(history.getMusicElements()));
        updateMusicList(history);
    }

    /**
     * 移除多条历史记录。
     */
    public synchronized void removeHistory(@NonNull Collection<Music> musics) {
        Preconditions.checkNotNull(musics);

        MusicList history = getHistoryMusicList();
        history.getMusicElements().removeAll(musics);

        mHistoryLiveData.setValue(new ArrayList<>(history.getMusicElements()));
        updateMusicList(history);
    }

    /**
     * 清空历史记录。
     */
    public synchronized void clearHistory() {
        MusicList history = getHistoryMusicList();
        history.getMusicElements().clear();

        mHistoryLiveData.setValue(new ArrayList<>(history.getMusicElements()));
        updateMusicList(history);
    }

    /**
     * 获取所有的历史记录。
     */
    public synchronized List<Music> getAllHistory() {
        return new ArrayList<>(getHistoryMusicList().getMusicElements());
    }

    /**
     * 存储/更新一个 {@link Music} 对象到数据库中。
     * <p>
     * <b>注意！必须先将 {@link Music} 对象存储到数据库中，然后才能添加到歌单中，否则无法保证歌单中元素的顺序</b>
     */
    public synchronized void putMusic(@NonNull Music music) {
        Preconditions.checkNotNull(music);
        mMusicBox.put(music);
    }

    /**
     * 获取指定 ID 的歌曲，如果歌曲不存在，则返回 null。
     *
     * @param id 歌曲 ID
     */
    @Nullable
    public synchronized Music getMusic(long id) {
        return mMusicBox.get(id);
    }

    /**
     * 获取所有本地音乐。
     */
    @NonNull
    public synchronized List<Music> getAllMusic() {
        return mMusicBox.getAll();
    }

    /**
     * 获取在给定的 offset 偏移量和 limit 限制之间的所有音乐。
     */
    @NonNull
    public synchronized List<Music> getAllMusic(long offset, long limit) {
        return mMusicBox.query()
                .build()
                .find(offset, limit);
    }

    /**
     * 获取数据库中包含的 {@link Music} 对象的数量。
     */
    public synchronized long getMusicCount() {
        return mMusicBox.count();
    }

    /**
     * 从数据中移除指定歌曲。
     * <p>
     * 注意！如果歌曲已添加到歌单中，则会把歌曲同时从所有歌单中移除。
     *
     * @return 如果歌曲已添加到数据库中，并且移除成功则返回 true；如果歌曲没有添加到数据库中，则返回 false
     */
    public synchronized boolean removeMusic(@NonNull Music music) {
        return mMusicBox.remove(music.getId());
    }

    /**
     * 从数据中移除指定集合中的所有歌曲。
     *
     * @param musics 所有要移除的歌曲。
     */
    public synchronized void removeMusic(Collection<Music> musics) {
        mMusicBox.remove(musics);
    }

    /**
     * 存储/更新多个 {@link Music} 对象到数据库中。
     * <p>
     * <b>注意！必须先将 {@link Music} 对象存储到数据库中，然后才能添加到歌单中，否则无法保证歌单中元素的顺序</b>
     */
    public synchronized void putAllMusic(@NonNull Collection<Music> musics) {
        Preconditions.checkNotNull(musics);
        mMusicBox.put(musics);
    }

    private synchronized MusicList getHistoryMusicList() {
        if (mHistory == null) {
            mHistory = getBuiltInMusicList(MUSIC_LIST_HISTORY);
        }

        return mHistory;
    }

    @NonNull
    private synchronized MusicList getBuiltInMusicList(String name) {
        if (!isBuiltInName(name)) {
            throw new IllegalArgumentException("not built-in name:" + name);
        }

        MusicListEntity entity = mMusicListEntityBox.query()
                .equal(MusicListEntity_.name, name)
                .build()
                .findUnique();

        if (entity != null) {
            return new MusicList(entity);
        }

        entity = createBuiltInMusicList(name);

        return new MusicList(entity);
    }

    private MusicListEntity createBuiltInMusicList(String name) {
        MusicListEntity entity = new MusicListEntity(0, name, "", new byte[0]);
        mMusicListEntityBox.put(entity);
        return entity;
    }
}
