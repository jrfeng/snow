package snow.player.playlist;

import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.common.base.Objects;
import com.google.common.base.Preconditions;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import snow.player.media.MusicItem;

/**
 * 用于存储播放队列。
 */
public final class Playlist implements Iterable<MusicItem>, Parcelable {
    private static final String TAG = "Playlist";
    private ArrayList<MusicItem> mMusicItems;
    private Bundle mExtra;

    public Playlist(@NonNull List<MusicItem> items) {
        this(items, null);
    }

    public Playlist(@NonNull List<MusicItem> items, Bundle extra) {
        Preconditions.checkNotNull(items);
        mMusicItems = new ArrayList<>(items);
        mExtra = extra;
    }

    /**
     * 如果当前播放队列包含指定的元素，则返回 true。
     */
    public boolean contains(MusicItem musicItem) {
        return mMusicItems.contains(musicItem);
    }

    /**
     * 返回当前播放队列中指定位置的元素。
     *
     * @param index 要返回的元素的索引。
     * @return 当前播放队列中指定位置的元素。
     * @throws IndexOutOfBoundsException 如果索引超出范围 (index < 0 || index >= size())
     */
    public MusicItem get(int index) throws IndexOutOfBoundsException {
        return mMusicItems.get(index);
    }

    /**
     * 返回当前播放队列中第一次出现的指定元素的索引；如果当前播放队列不包含该元素，则返回 -1。
     *
     * @param musicItem 要搜索的元素（不能为 null）
     * @return 此当前播放队列中第一次出现的指定元素的索引，如果当前播放队列不包含该元素，则返回 -1
     */
    public int indexOf(@NonNull MusicItem musicItem) {
        Preconditions.checkNotNull(musicItem);
        return mMusicItems.indexOf(musicItem);
    }

    /**
     * 如果当前播放队列在没有任何元素，则返回 true。
     *
     * @return 如果列表不包含元素，则返回 true。
     */
    public boolean isEmpty() {
        return mMusicItems.isEmpty();
    }

    /**
     * 返回按适当顺序在当前播放队列的元素上进行迭代的迭代器。
     * <p>
     * <b>注意！由于 Playlist 被设计为是不可变的，因此不允许使用 Iterator#remove() 方法来删除元素。</b>
     *
     * @return 按适当顺序在列表的元素上进行迭代的迭代器。
     */
    @NonNull
    @Override
    public Iterator<MusicItem> iterator() {
        return new Iterator<MusicItem>() {
            private Iterator<MusicItem> iterator = mMusicItems.iterator();

            @Override
            public boolean hasNext() {
                return iterator.hasNext();
            }

            @Override
            public MusicItem next() {
                return iterator.next();
            }

            @Override
            public void remove() {
                Log.e(TAG, "unsupported operation");
            }
        };
    }

    /**
     * 返回当前播放队列中最后出现的指定元素的索引；如果当前播放队列不包含此元素，则返回 -1。
     * <p>
     * 更确切地讲，返回满足 (o==null ? get(i)==null : o.equals(get(i))) 的最高索引 i；如果没有这样的索引，则返回 -1。
     *
     * @param musicItem 要搜索的元素（不能为 null）
     * @return 列表中最后出现的指定元素的索引；如果列表不包含此元素，则返回 -1。
     */
    public int lastIndexOf(@NonNull MusicItem musicItem) {
        Preconditions.checkNotNull(musicItem);
        return mMusicItems.lastIndexOf(musicItem);
    }

    /**
     * 返回当前播放队列中的元素数。
     * <p>
     * 如果当前播放队列包含多于 Integer.MAX_VALUE 个元素，则返回 Integer.MAX_VALUE。
     *
     * @return 当前播放队列中的元素数
     */
    public int size() {
        return mMusicItems.size();
    }

    /**
     * 获取当前播放队列中包含的所有 MusicItem 元素。
     * <p>
     * 如果当前播放队列为空，则返回一个空列表。
     *
     * @return 当前播放队列中包含的所有 MusicItem 元素。如果当前播放队列为空，则返回一个空列表。
     */
    public List<MusicItem> getAllMusicItem() {
        return new ArrayList<>(mMusicItems);
    }

    public Bundle getExtra() {
        return mExtra;
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        if (!(obj instanceof Playlist)) {
            return false;
        }

        return Objects.equal(mMusicItems, ((Playlist) obj).mMusicItems);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(mMusicItems);
    }

    // Parcelable
    protected Playlist(Parcel in) {
        mMusicItems = in.createTypedArrayList(MusicItem.CREATOR);
        mExtra = in.readBundle(Thread.currentThread().getContextClassLoader());
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeTypedList(mMusicItems);
        dest.writeBundle(mExtra);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<Playlist> CREATOR = new Creator<Playlist>() {
        @Override
        public Playlist createFromParcel(Parcel in) {
            return new Playlist(in);
        }

        @Override
        public Playlist[] newArray(int size) {
            return new Playlist[size];
        }
    };
}
