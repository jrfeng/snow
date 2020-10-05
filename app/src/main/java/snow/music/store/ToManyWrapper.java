package snow.music.store;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.common.base.Preconditions;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

import io.objectbox.relation.ToMany;

/**
 * ToMany 工具类，帮助持久化 ToMany 中元素的顺序。
 * <p>
 * 不要直接使用实体对象的 ToMany 字段来访问/修改列表中的元素，而是通过创建一个 {@link ToManyWrapper}
 * 对象来访问和修改列表中的元素。列表中的元素类型需要实现 {@link OrderedSource} 接口。
 * <p>
 * 使用方法：<br>
 * <ul>
 *     <li>为你的实体类型增加一个 byte[] 类型的字段，用于存储 ID 的顺序；</li>
 *     <li>实体对象的 ToMany 字段与 byte[] 字段初见一个 {@link ToManyWrapper} 对象；</li>
 *     <li>使用 {@link ToManyWrapper} 对象来增加/删除元素；</li>
 *     <li>当修改完列表后，应该调用 {@link #updateOrderBytes()} 方法更新元素顺序数据；</li>
 *     <li>最后，put 实体对象以更新数据库。</li>
 * </ul>
 *
 * @param <TARGET> ToMany 的目标类型。
 */
public class ToManyWrapper<TARGET> implements List<TARGET> {
    private OrderedSource<TARGET> mOrderedSource;
    private ToMany<TARGET> mToMany;
    private List<TARGET> mTargetList;

    /**
     * 创建一个 {@link ToManyWrapper} 对象。
     *
     * @param orderedSource to-many 关系中的源对象
     */
    public ToManyWrapper(@NonNull OrderedSource<TARGET> orderedSource) {
        update(orderedSource);
    }

    /**
     * 更新 ToMany。
     *
     * @param orderedSource to-many 关系中的源对象
     */
    public void update(@NonNull OrderedSource<TARGET> orderedSource) {
        Preconditions.checkNotNull(orderedSource);

        mOrderedSource = orderedSource;
        mToMany = orderedSource.getToMany();
        convertOrderBytes(orderedSource.getOrderBytes());
    }

    private void convertOrderBytes(byte[] orderBytes) {
        if (orderBytes == null) {
            mTargetList = new ArrayList<>(mToMany);
            return;
        }

        try {
            ByteArrayInputStream byteInput = new ByteArrayInputStream(orderBytes);
            ObjectInputStream input = new ObjectInputStream(byteInput);

            mTargetList = new ArrayList<>();
            while (input.available() > 0) {
                mTargetList.add(mToMany.getById(input.readLong()));
            }

            input.close();
        } catch (IOException e) {
            mTargetList = new ArrayList<>(mToMany);
            e.printStackTrace();
        }
    }

    /**
     * 获取字节格式的元素顺序。
     * <p>
     * 可以将该方法的返回值赋值给实体对象的 byte[] 字段以更新元素顺序。
     */
    @NonNull
    public byte[] getOrderBytes() {
        try {
            ByteArrayOutputStream byteOutput = new ByteArrayOutputStream(mTargetList.size() * 4);
            ObjectOutputStream output = new ObjectOutputStream(byteOutput);

            for (TARGET target : mTargetList) {
                output.writeLong(mOrderedSource.getTargetId(target));
            }

            output.flush();
            output.close();
            return byteOutput.toByteArray();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return new byte[0];
    }

    /**
     * 更新元素顺序数据。
     */
    public void updateOrderBytes() {
        mOrderedSource.setOrderBytes(getOrderBytes());
    }

    @Override
    public int size() {
        return mTargetList.size();
    }

    @Override
    public boolean isEmpty() {
        return mTargetList.isEmpty();
    }

    @Override
    public boolean contains(@Nullable Object o) {
        return mTargetList.contains(o);
    }

    @NonNull
    @Override
    public Iterator<TARGET> iterator() {
        return mTargetList.iterator();
    }

    @NonNull
    @Override
    public Object[] toArray() {
        return mTargetList.toArray();
    }

    @NonNull
    @Override
    public <T> T[] toArray(@NonNull T[] a) {
        return mTargetList.toArray(a);
    }

    @Override
    public boolean add(TARGET t) {
        boolean result = mTargetList.add(t);

        if (result) {
            mToMany.add(t);
        }

        return result;
    }

    @Override
    public boolean remove(@Nullable Object o) {
        boolean result = mTargetList.remove(o);

        if (result) {
            mToMany.remove(o);
        }

        return result;
    }

    @Override
    public boolean containsAll(@NonNull Collection<?> c) {
        Preconditions.checkNotNull(c);
        return mTargetList.containsAll(c);
    }

    @Override
    public boolean addAll(@NonNull Collection<? extends TARGET> c) {
        Preconditions.checkNotNull(c);

        boolean result = mTargetList.addAll(c);

        if (result) {
            mToMany.addAll(c);
        }

        return result;
    }

    @Override
    public boolean addAll(int index, @NonNull Collection<? extends TARGET> c) {
        Preconditions.checkNotNull(c);

        boolean result = mTargetList.addAll(c);

        if (result) {
            mToMany.addAll(c);
        }

        return result;
    }

    @Override
    public boolean removeAll(@NonNull Collection<?> c) {
        Preconditions.checkNotNull(c);

        boolean result = mTargetList.removeAll(c);

        if (result) {
            mToMany.removeAll(c);
        }

        return result;
    }

    @Override
    public boolean retainAll(@NonNull Collection<?> c) {
        Preconditions.checkNotNull(c);

        boolean result = mTargetList.retainAll(c);

        if (result) {
            mToMany.retainAll(c);
        }

        return result;
    }

    @Override
    public void clear() {
        mToMany.clear();
        mTargetList.clear();
    }

    @Override
    public TARGET get(int index) {
        return mTargetList.get(index);
    }

    @Override
    public TARGET set(int index, TARGET element) {
        TARGET target = mTargetList.set(index, element);
        mToMany.set(mToMany.indexOf(target), element);
        return target;
    }

    @Override
    public void add(int index, TARGET element) {
        mToMany.add(index, element);
        mTargetList.add(index, element);
    }

    @Override
    public TARGET remove(int index) {
        TARGET target = mTargetList.remove(index);
        mToMany.remove(target);
        return target;
    }

    @Override
    public int indexOf(@Nullable Object o) {
        return mTargetList.indexOf(o);
    }

    @Override
    public int lastIndexOf(@Nullable Object o) {
        return mTargetList.lastIndexOf(o);
    }

    @NonNull
    @Override
    public ListIterator<TARGET> listIterator() {
        return mTargetList.listIterator();
    }

    @NonNull
    @Override
    public ListIterator<TARGET> listIterator(int index) {
        return mTargetList.listIterator(index);
    }

    @NonNull
    @Override
    public List<TARGET> subList(int fromIndex, int toIndex) {
        return mTargetList.subList(fromIndex, toIndex);
    }

    /**
     * 列表元素。
     *
     * @param <TARGET> 元素的实体类型。
     */
    public interface OrderedSource<TARGET> {
        /**
         * 获取源（Source）的 ToMany 对象。
         */
        @NonNull
        ToMany<TARGET> getToMany();

        /**
         * 获取字节格式的元素顺序，
         */
        @Nullable
        byte[] getOrderBytes();

        /**
         * 设置元素顺序数据。
         */
        void setOrderBytes(byte[] orderBytes);

        /**
         * 返回 ObjectBox 实体对象的 ID。
         * <p>
         * 注意！对应新创建的实体对象，必须在添加到 ToMany 中之前先添加到数据库中。因为需要更加 ID 记录元素的顺序，
         * 而新创建的实体对象如果没有添加到数据库中，那么其 ID 是无效的。
         *
         * @return 返回 ObjectBox 实体对象的 ID
         */
        long getTargetId(TARGET target);
    }
}
