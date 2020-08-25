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
 *
 * @see snow.player.PlayerClient#setPlaylist(Playlist, int, boolean)
 */
public final class Playlist implements Iterable<MusicItem>, Parcelable {
    private static final String TAG = "Playlist";
    private ArrayList<MusicItem> mMusicItems;
    private Bundle mExtra;

    /**
     * 创建一个 {@link Playlist} 对象。
     * <p>
     * 建议优先使用 {@link Builder} 来创建 {@link Playlist} 对象。
     *
     * @param items 要添加到播放队列中的 {@link MusicItem} 对象，重复的 {@link MusicItem} 对象会被排除
     * @see Builder
     */
    public Playlist(@NonNull List<MusicItem> items) {
        this(items, null);
    }

    /**
     * 创建一个 {@link Playlist} 对象。
     *
     * @param items 要添加到播放队列中的 {@link MusicItem} 对象，重复的 {@link MusicItem} 对象会被排除
     * @param extra 要携带的额外参数
     */
    public Playlist(@NonNull List<MusicItem> items, Bundle extra) {
        Preconditions.checkNotNull(items);
        mMusicItems = excludeRepeatItem(items);
        mExtra = extra;
    }

    private ArrayList<MusicItem> excludeRepeatItem(List<MusicItem> items) {
        ArrayList<MusicItem> musicItems = new ArrayList<>();

        for (MusicItem item : items) {
            if (musicItems.contains(item)) {
                continue;
            }

            musicItems.add(item);
        }

        return musicItems;
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
        return new Bundle(mExtra);
    }

    /**
     * 不包含携带的 {@code extra} 数据
     */
    @Override
    public boolean equals(@Nullable Object obj) {
        if (this == obj) {
            return true;
        }

        if (!(obj instanceof Playlist)) {
            return false;
        }

        return Objects.equal(mMusicItems, ((Playlist) obj).mMusicItems);
    }

    /**
     * 不包含携带的 {@code extra} 数据
     */
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

    /**
     * {@link Playlist} 构建器。
     */
    public static final class Builder {
        private List<MusicItem> mMusicItems;
        private Bundle mExtra;

        /**
         * 创建一个 {@link Builder} 构建器对象。
         */
        public Builder() {
            mMusicItems = new ArrayList<>();
        }

        /**
         * 添加一首音乐。
         */
        public Builder append(@NonNull MusicItem musicItem) {
            Preconditions.checkNotNull(musicItem);
            mMusicItems.add(musicItem);
            return this;
        }

        /**
         * 添加多首音乐。
         */
        public Builder appendAll(@NonNull List<MusicItem> musicItems) {
            Preconditions.checkNotNull(musicItems);
            mMusicItems.addAll(musicItems);
            return this;
        }

        /**
         * 移除一首音乐。
         */
        public Builder remove(@NonNull MusicItem musicItem) {
            Preconditions.checkNotNull(musicItem);
            mMusicItems.remove(musicItem);
            return this;
        }

        /**
         * 移除多首音乐。
         */
        public Builder removeAll(@NonNull List<MusicItem> musicItems) {
            Preconditions.checkNotNull(musicItems);
            mMusicItems.removeAll(musicItems);
            return this;
        }

        /**
         * 设置要携带的额外参数，
         *
         * @param extra 要携带的额外参数，可为 null
         */
        public Builder setExtra(@Nullable Bundle extra) {
            mExtra = extra;
            return this;
        }

        /**
         * 构造一个 {@link Playlist} 对象。
         * <p>
         * 重复的 {@link MusicItem} 项会被在构造 {@link Playlist} 对象时被排除。
         */
        public Playlist build() {
            return new Playlist(mMusicItems, mExtra);
        }
    }
}
