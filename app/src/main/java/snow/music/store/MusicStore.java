package snow.music.store;

import android.content.Context;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.common.base.Preconditions;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import android.os.Handler;
import android.util.Log;

import io.objectbox.Box;
import io.objectbox.BoxStore;
import io.objectbox.query.QueryBuilder;
import io.reactivex.Single;
import io.reactivex.schedulers.Schedulers;

/**
 * 歌曲数据库，用于存储本地音乐与本地歌单。
 * <p>
 * 注意！除以下方法外，{@link MusicStore} 的其他方法都会访问数据库，因此不建议在 UI 线程上调用，否则可能会导致 ANR。
 * <ul>
 *     <li>{@link #getInstance()}</li>
 *     <li>{@link #init(Context)}</li>
 *     <li>{@link #init(BoxStore)}</li>
 *     <li>{@link #isBuiltInName(String)}</li>
 *     <li>{@link #getBoxStore()}</li>
 * </ul>
 * <p>
 * 还有就是 {@link #sort(MusicList, MusicList.SortOrder, SortCallback)} 方法，该方法虽然会访问数据库，
 * 但是会在异步线程中执行。
 */
public class MusicStore {
    private static final String TAG = "MusicStore";
    public static final String MUSIC_LIST_LOCAL_MUSIC = "__local_music";
    public static final String MUSIC_LIST_FAVORITE = "__favorite";
    public static final String MUSIC_LIST_HISTORY = "__history";

    private static final int MAX_HISTORY_SIZE = 500;

    private static MusicStore mInstance;

    private final BoxStore mBoxStore;
    private final Box<Music> mMusicBox;
    private final Box<MusicListEntity> mMusicListEntityBox;

    private final Handler mMainHandler;

    private final List<OnFavoriteChangeListener> mAllFavoriteChangeListener;
    private OnScanCompleteListener mOnScanCompleteListener;

    private Set<String> mAllCustomMusicListName;

    private MusicStore(BoxStore boxStore) {
        mBoxStore = boxStore;
        mMusicBox = boxStore.boxFor(Music.class);
        mMusicListEntityBox = boxStore.boxFor(MusicListEntity.class);
        mMainHandler = new Handler(Looper.getMainLooper());
        mAllFavoriteChangeListener = new LinkedList<>();
        mAllCustomMusicListName = new HashSet<>();

        loadAllMusicListName();
    }

    private void loadAllMusicListName() {
        Single.create(emitter -> {
            String[] allName = mMusicListEntityBox.query()
                    .build()
                    .property(MusicListEntity_.name)
                    .findStrings();

            if (allName == null) {
                return;
            }

            mAllCustomMusicListName.addAll(Arrays.asList(allName));

        }).subscribeOn(Schedulers.io())
                .subscribe();
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

        init(boxStore);
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

    public synchronized void sort(@NonNull MusicList musicList, @NonNull MusicList.SortOrder sortOrder, @Nullable SortCallback callback) {
        Preconditions.checkNotNull(musicList);
        Preconditions.checkNotNull(sortOrder);

        BoxStore boxStore = getInstance().getBoxStore();
        boxStore.runInTxAsync(() -> {
            ArrayList<Music> items = new ArrayList<>(musicList.getMusicElements());
            Collections.sort(items, sortOrder.comparator());

            musicList.setSortOrder(sortOrder);
            musicList.getMusicElements().clear();
            musicList.getMusicElements().addAll(items);
            updateMusicList(musicList);
        }, (result, error) -> mMainHandler.post(() -> {
            if (callback != null) {
                callback.onSortFinished();
            }
        }));
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

    private void checkThread() {
        if (Thread.currentThread() == Looper.getMainLooper().getThread()) {
            Log.e(TAG, "Please do not access the database on the main thread.");
        }
    }

    /**
     * 歌单名是否已存在。
     * <p>
     * 注意！所有自定义歌单名会在初始化时在异步线程进行预加载，如果你在所有自定义歌单名加载完成前调用该方法，
     * 那么该方法的返回结果可能是不准确的。如果你需要准确的判断歌单名是否已存在，请使用
     * {@link #isMusicListExists(String)} 方法，不过该方法会访问数据库，因此不建议在 UI 线程调用。
     *
     * @param name 歌单名，不能为 null
     * @return 如果歌单名已存在，则返回 true，否则返回 false
     */
    public boolean isNameExists(@NonNull String name) {
        Preconditions.checkNotNull(name);
        return isBuiltInName(name) || mAllCustomMusicListName.contains(name);
    }

    /**
     * 歌单是否已存在。
     * <p>
     * 该方法会访问数据库，不建议在 UI 线程调用。
     *
     * @param name 歌单名，不能为 null
     * @return 如果歌单已存在，则返回 true，否则返回 false
     */
    public synchronized boolean isMusicListExists(@NonNull String name) {
        Preconditions.checkNotNull(name);
        checkThread();

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
    public synchronized MusicList createCustomMusicList(@NonNull String name) throws IllegalArgumentException {
        return createCustomMusicList(name, "");
    }

    /**
     * 创建一个新的歌单，如果歌单已存在，则直接返回它，不会创建新歌单。
     *
     * @throws IllegalArgumentException 如果 name 参数是个空字符串或者内置名称，则抛出该异常。
     */
    @NonNull
    public synchronized MusicList createCustomMusicList(@NonNull String name, @NonNull String description) throws IllegalArgumentException {
        Preconditions.checkNotNull(name);
        Preconditions.checkNotNull(description);
        Preconditions.checkArgument(!name.isEmpty(), "name must not empty");
        checkThread();

        if (isBuiltInName(name)) {
            throw new IllegalArgumentException("Illegal music list name, conflicts with built-in name.");
        }

        if (isMusicListExists(name)) {
            MusicList musicList = getCustomMusicList(name);
            assert musicList != null;
            return musicList;
        }

        mAllCustomMusicListName.add(name);
        MusicListEntity entity = new MusicListEntity(0, name, description, 0, MusicList.SortOrder.BY_ADD_TIME, new byte[0]);
        mMusicListEntityBox.put(entity);
        return new MusicList(entity);
    }

    /**
     * 获取自建歌单，如果自建歌单不存在，则返回 null。
     */
    @Nullable
    public synchronized MusicList getCustomMusicList(@NonNull String name) {
        Preconditions.checkNotNull(name);
        checkThread();

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
        checkThread();

        if (!isMusicListExists(musicList.getId())) {
            return;
        }

        String name = musicList.getName();
        if (!isBuiltInName(name)) {
            mAllCustomMusicListName.add(name);
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
        checkThread();

        if (isBuiltInMusicList(musicList)) {
            return;
        }

        mAllCustomMusicListName.remove(musicList.getName());
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
        checkThread();

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
    public synchronized List<MusicList> getAllCustomMusicList() {
        checkThread();

        List<MusicListEntity> allEntity = mMusicListEntityBox.query()
                .notEqual(MusicListEntity_.name, MUSIC_LIST_LOCAL_MUSIC)
                .and()
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
     * 歌曲是否是 “我喜欢”
     */
    public synchronized boolean isFavorite(@NonNull Music music) {
        Preconditions.checkNotNull(music);
        checkThread();

        return isFavorite(music.getId());
    }

    /**
     * 指定 musicId 的歌曲是否是 “我喜欢”
     */
    public synchronized boolean isFavorite(long musicId) {
        checkThread();
        if (musicId <= 0) {
            return false;
        }

        QueryBuilder<Music> builder = mMusicBox.query().equal(Music_.id, musicId);
        builder.backlink(MusicListEntity_.musicElements)
                .equal(MusicListEntity_.name, MUSIC_LIST_FAVORITE);

        return builder.build().count() > 0;
    }

    /**
     * 获取 “本地音乐” 歌单。
     */
    public synchronized MusicList getLocalMusicList() {
        checkThread();
        return getBuiltInMusicList(MUSIC_LIST_LOCAL_MUSIC);
    }

    /**
     * 获取 “我喜欢” 歌单。
     */
    @NonNull
    public synchronized MusicList getFavoriteMusicList() {
        checkThread();
        return getBuiltInMusicList(MUSIC_LIST_FAVORITE);
    }

    /**
     * 将歌曲添加到 “我喜欢” 歌单。
     */
    public synchronized void addToFavorite(@NonNull Music music) {
        Preconditions.checkNotNull(music);
        checkThread();

        if (isFavorite(music)) {
            return;
        }

        MusicList favorite = getFavoriteMusicList();
        favorite.getMusicElements().add(music);
        updateMusicList(favorite);
        notifyFavoriteChanged();
    }

    /**
     * 将歌曲从 “我喜欢” 歌单中移除。
     */
    public synchronized void removeFromFavorite(@NonNull Music music) {
        Preconditions.checkNotNull(music);
        checkThread();

        if (isFavorite(music)) {
            MusicList favorite = getFavoriteMusicList();
            favorite.getMusicElements().remove(music);
            updateMusicList(favorite);
            notifyFavoriteChanged();
        }
    }

    /**
     * 切换歌曲的 “我喜欢” 状态。
     * <p>
     * 如果歌曲已经添加到 “我喜欢” 歌单中，则移除它，否则将其添加到 “我喜欢” 歌单中。
     *
     * @param music {@link Music} 对象，不能为 null
     */
    public synchronized void toggleFavorite(@NonNull Music music) {
        Objects.requireNonNull(music);
        checkThread();

        if (isFavorite(music)) {
            removeFromFavorite(music);
        } else {
            addToFavorite(music);
        }
    }

    private void notifyFavoriteChanged() {
        mMainHandler.post(() -> {
            for (OnFavoriteChangeListener listener : mAllFavoriteChangeListener) {
                listener.onFavoriteChanged();
            }
        });
    }

    /**
     * 添加一个 {@link OnFavoriteChangeListener} 监听器，如果已添加，则忽略本次调用。
     *
     * @param listener {@link OnFavoriteChangeListener} 监听器对象，不能为 null
     */
    public synchronized void addOnFavoriteChangeListener(@NonNull OnFavoriteChangeListener listener) {
        Preconditions.checkNotNull(listener);

        if (mAllFavoriteChangeListener.contains(listener)) {
            return;
        }

        mAllFavoriteChangeListener.add(listener);
    }

    /**
     * 移除一个已添加的 {@link OnFavoriteChangeListener} 监听器，如果未添加或者已经移除，则忽略本次调用。
     *
     * @param listener {@link OnFavoriteChangeListener} 监听器对象，为 null 时将忽略本次调用。
     */
    public synchronized void removeOnFavoriteChangeListener(OnFavoriteChangeListener listener) {
        if (listener == null) {
            return;
        }

        mAllFavoriteChangeListener.remove(listener);
    }

    /**
     * 是否是内置歌单。
     *
     * @param musicList {@link MusicList} 对象，不能为 null
     * @return 如果是内置歌单，则返回 true，否则返回 false
     */
    public boolean isBuiltInMusicList(@NonNull MusicList musicList) {
        String name = mMusicListEntityBox.query()
                .equal(MusicListEntity_.id, musicList.getId())
                .build()
                .property(MusicListEntity_.name)
                .unique()
                .findString();

        return isBuiltInName(name);
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
        checkThread();

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
        checkThread();

        MusicList history = getHistoryMusicList();
        history.getMusicElements().remove(music);

        updateMusicList(history);
    }

    /**
     * 移除多条历史记录。
     */
    public synchronized void removeHistory(@NonNull Collection<Music> musics) {
        Preconditions.checkNotNull(musics);
        checkThread();

        MusicList history = getHistoryMusicList();
        history.getMusicElements().removeAll(musics);

        updateMusicList(history);
    }

    /**
     * 清空历史记录。
     */
    public synchronized void clearHistory() {
        checkThread();
        MusicList history = getHistoryMusicList();
        history.getMusicElements().clear();

        updateMusicList(history);
    }

    /**
     * 获取所有的历史记录。
     */
    public synchronized List<Music> getAllHistory() {
        checkThread();
        return new ArrayList<>(getHistoryMusicList().getMusicElements());
    }

    /**
     * 存储/更新一个 {@link Music} 对象到数据库中。
     * <p>
     * <b>注意！必须先将 {@link Music} 对象存储到数据库中，然后才能添加到歌单中，否则无法保证歌单中元素的顺序</b>
     */
    public synchronized void putMusic(@NonNull Music music) {
        checkThread();
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
        checkThread();
        return mMusicBox.get(id);
    }

    /**
     * 获取所有本地音乐。
     */
    @NonNull
    public synchronized List<Music> getAllMusic() {
        checkThread();
        return mMusicBox.getAll();
    }

    /**
     * 获取在给定的 offset 偏移量和 limit 限制之间的所有音乐。
     */
    @NonNull
    public synchronized List<Music> getAllMusic(long offset, long limit) {
        checkThread();
        return mMusicBox.query()
                .build()
                .find(offset, limit);
    }

    /**
     * 获取数据库中包含的 {@link Music} 对象的数量。
     */
    public synchronized long getMusicCount() {
        checkThread();
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
        checkThread();
        return mMusicBox.remove(music.getId());
    }

    /**
     * 从数据中移除指定集合中的所有歌曲。
     *
     * @param musics 所有要移除的歌曲。
     */
    public synchronized void removeMusic(Collection<Music> musics) {
        checkThread();
        mMusicBox.remove(musics);
    }

    /**
     * 存储/更新多个 {@link Music} 对象到数据库中。
     * <p>
     * <b>注意！必须先将 {@link Music} 对象存储到数据库中，然后才能添加到歌单中，否则无法保证歌单中元素的顺序</b>
     */
    public synchronized void putAllMusic(@NonNull Collection<Music> musics) {
        Preconditions.checkNotNull(musics);
        checkThread();
        mMusicBox.put(musics);
    }

    /**
     * 获取具有指定 uri 的 {@link Music} 的 id 值。
     *
     * @param uri uri 字符串，不能为 null
     * @return 如果 {@link Music} 已存在，则返回其 id 值，否则返回 0
     */
    public synchronized long getId(@NonNull String uri) {
        Preconditions.checkNotNull(uri);

        Long id = mMusicBox.query()
                .equal(Music_.uri, uri)
                .build()
                .property(Music_.id)
                .findLong();

        if (id == null) {
            return 0;
        }

        return id;
    }

    /**
     * 查询具有指定 uri 的 {@link Music} 是否已添加到 “本地音乐” 歌单中。
     *
     * @param uri uri 字符串，不能为 null
     * @return 如果歌曲已添加到本地歌单，则返回 true
     */
    public synchronized boolean isLocalMusic(@NonNull String uri) {
        Preconditions.checkNotNull(uri);

        QueryBuilder<Music> builder = mMusicBox.query()
                .equal(Music_.uri, uri);

        builder.backlink(MusicListEntity_.musicElements)
                .equal(MusicListEntity_.name, MUSIC_LIST_LOCAL_MUSIC);

        return builder.build().count() > 0;
    }

    /**
     * 获取所有的歌手名。
     */
    @NonNull
    public synchronized List<String> getAllArtist() {
        checkThread();
        return new ArrayList<>(Arrays.asList(mMusicBox.query()
                .build()
                .property(Music_.artist)
                .distinct()
                .findStrings()));
    }

    /**
     * 获取所有的专辑名。
     */
    @NonNull
    public synchronized List<String> getAllAlbum() {
        checkThread();
        return new ArrayList<>(Arrays.asList(mMusicBox.query()
                .build()
                .property(Music_.album)
                .distinct()
                .findStrings()));
    }

    /**
     * 获取指定歌手的全部音乐。
     *
     * @param artist 歌手名，不能为 null
     * @return 歌手的全部音乐，不为 null
     */
    @NonNull
    public synchronized List<Music> getArtistAllMusic(@NonNull String artist) {
        Preconditions.checkNotNull(artist);
        checkThread();

        return mMusicBox.query()
                .equal(Music_.artist, artist)
                .build()
                .find();
    }

    /**
     * 获取指定歌手在给定的 offset 偏移量和 limit 限制间的全部音乐。
     *
     * @param artist 歌手名，不能为 null
     * @return 在给定的 offset 偏移量和 limit 限制间的全部音乐，不为 null
     */
    public synchronized List<Music> getArtistAllMusic(@NonNull String artist, long offset, long limit) {
        Preconditions.checkNotNull(artist);
        checkThread();

        return mMusicBox.query()
                .equal(Music_.artist, artist)
                .build()
                .find(offset, limit);
    }

    /**
     * 获取指定专辑的全部音乐。
     *
     * @param album 专辑名，不能为 null
     * @return 专辑中的全部音乐，不为 null
     */
    @NonNull
    public synchronized List<Music> getAlbumAllMusic(@NonNull String album) {
        Preconditions.checkNotNull(album);
        checkThread();

        return mMusicBox.query()
                .equal(Music_.album, album)
                .build()
                .find();
    }

    /**
     * 获取指定专辑在给定的 offset 偏移量和 limit 限制间的全部音乐。
     *
     * @param album 专辑名，不能为 null
     * @return 在给定的 offset 偏移量和 limit 限制间的全部音乐，不为 null
     */
    public synchronized List<Music> getAlbumAllMusic(@NonNull String album, long offset, long limit) {
        Preconditions.checkNotNull(album);
        checkThread();

        return mMusicBox.query()
                .equal(Music_.album, album)
                .build()
                .find(offset, limit);
    }

    private synchronized MusicList getHistoryMusicList() {
        return getBuiltInMusicList(MUSIC_LIST_HISTORY);
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
        MusicListEntity entity = new MusicListEntity(0, name, "", 0, MusicList.SortOrder.BY_ADD_TIME, new byte[0]);
        mMusicListEntityBox.put(entity);
        return entity;
    }

    public void setOnScanCompleteListener(@Nullable OnScanCompleteListener listener) {
        mOnScanCompleteListener = listener;
    }

    /**
     * 通知本地音乐已扫描完成。
     */
    public void notifyScanComplete() {
        if (mOnScanCompleteListener != null) {
            mOnScanCompleteListener.onScanComplete();
        }
    }

    /**
     * 用于监听 “我喜欢” 歌单的修改事件。
     * <p>
     * 当往 “我喜欢” 歌单中添加或移除一首歌曲时，该监听器会被调用。
     */
    public interface OnFavoriteChangeListener {
        /**
         * 当 “我喜欢” 歌单被修改时，会调用该方法。
         * <p>
         * 该回调方法会在应用程序主线程调用。
         */
        void onFavoriteChanged();
    }

    /**
     * 监听 “本地音乐扫描完成” 事件。
     */
    public interface OnScanCompleteListener {
        /**
         * 本地音乐扫描完成时会调用该方法。
         * <p>
         * 该方法会在主线程中调用，请不要直接在该方法中访问数据库。
         *
         * @see #notifyFavoriteChanged()
         */
        void onScanComplete();
    }

    /**
     * 监听 “排序歌单” 完成事件。
     */
    public interface SortCallback {
        /**
         * 歌单排序完成后会调用该方法，且会在主线程中调用该方法。
         */
        void onSortFinished();
    }
}
