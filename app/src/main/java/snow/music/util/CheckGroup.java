package snow.music.util;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.HashSet;
import java.util.Set;

public class CheckGroup {
    private Set<CheckItem> mCheckItems;
    private int mCheckedItemId;

    @Nullable
    private OnCheckedItemChangeListener mOnCheckedItemChangeListener;

    public CheckGroup() {
        mCheckItems = new HashSet<>();
        mCheckedItemId = -1;
    }

    public void addItem(CheckItem item) {
        item.setCheckGroup(this);
        mCheckItems.add(item);
    }

    public void setChecked(int itemId) {
        if (itemId == mCheckedItemId) {
            return;
        }

        mCheckedItemId = itemId;

        for (CheckItem item : mCheckItems) {
            if (item.getId() == mCheckedItemId) {
                item.onChecked();
            } else {
                item.onUnchecked();
            }
        }

        if (mOnCheckedItemChangeListener != null) {
            mOnCheckedItemChangeListener.onCheckedItemChanged(mCheckedItemId);
        }
    }

    public int getCheckedItemId() {
        return mCheckedItemId;
    }

    public void setOnCheckedItemChangeListener(@Nullable OnCheckedItemChangeListener onCheckedItemChangeListener) {
        mOnCheckedItemChangeListener = onCheckedItemChangeListener;
    }

    public static abstract class CheckItem {
        private int mItemId;

        @Nullable
        private CheckGroup mCheckGroup;

        public CheckItem(int id) {
            mItemId = id;
        }

        public int getId() {
            return mItemId;
        }

        public void requestChecked() {
            if (mCheckGroup != null) {
                mCheckGroup.setChecked(mItemId);
            }
        }

        void setCheckGroup(@Nullable CheckGroup checkGroup) {
            mCheckGroup = checkGroup;
        }

        public abstract void onChecked();

        public abstract void onUnchecked();
    }

    public interface OnCheckedItemChangeListener {
        void onCheckedItemChanged(int checkedItemId);
    }
}
