package snow.music.dialog;

import android.content.Context;
import android.content.DialogInterface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.appcompat.app.AppCompatDialog;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.common.base.Preconditions;

import java.util.Objects;

import recyclerview.helper.ItemClickHelper;
import recyclerview.helper.SelectableHelper;
import snow.music.R;

public class SingleChoiceDialog extends BottomDialog {
    private String mTitle;
    private String[] mItems;
    private int mCheckedItem;
    private DialogInterface.OnClickListener mClickListener;

    @Override
    protected void onInitDialog(AppCompatDialog dialog) {
        dialog.setContentView(R.layout.dialog_single_choice);

        TextView tvDialogTitle = dialog.findViewById(R.id.tvDialogTitle);
        RecyclerView rvMenuItems = dialog.findViewById(R.id.rvMenuItems);

        Objects.requireNonNull(tvDialogTitle);
        Objects.requireNonNull(rvMenuItems);

        tvDialogTitle.setText(mTitle);

        SingleChoiceAdapter adapter = new SingleChoiceAdapter(mItems, mCheckedItem);
        adapter.setOnItemClickListener((position, viewId, view, holder) -> {
            mCheckedItem = position;
            adapter.setCheckedItem(mCheckedItem);
            if (mClickListener != null) {
                mClickListener.onClick(dialog, position);
            }
        });

        rvMenuItems.setLayoutManager(new LinearLayoutManager(getContext()));
        rvMenuItems.setAdapter(adapter);
    }

    @Override
    protected boolean keepOnRestarted() {
        return false;
    }

    private static class SingleChoiceAdapter extends RecyclerView.Adapter<SingleChoiceAdapter.ViewHolder> {
        private final String[] mItems;
        private final ItemClickHelper mItemClickHelper;
        private final SelectableHelper mSelectableHelper;

        SingleChoiceAdapter(String[] items, int checkedItem) {
            mItems = items;
            mItemClickHelper = new ItemClickHelper();
            mSelectableHelper = new SelectableHelper(this);

            if (checkedItem >= 0) {
                mSelectableHelper.setSelect(checkedItem, true);
            }
        }

        public void setCheckedItem(int position) {
            mSelectableHelper.setSelect(position, true);
        }

        public void setOnItemClickListener(ItemClickHelper.OnItemClickListener listener) {
            mItemClickHelper.setOnItemClickListener(listener);
        }

        @Override
        public void onAttachedToRecyclerView(@NonNull RecyclerView recyclerView) {
            super.onAttachedToRecyclerView(recyclerView);
            mItemClickHelper.attachToRecyclerView(recyclerView);
            mSelectableHelper.attachToRecyclerView(recyclerView);
        }

        @Override
        public void onDetachedFromRecyclerView(@NonNull RecyclerView recyclerView) {
            super.onDetachedFromRecyclerView(recyclerView);
            mItemClickHelper.detach();
            mSelectableHelper.detach();
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View itemView = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_single_choice, parent, false);
            return new ViewHolder(itemView);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            holder.tvItemTitle.setText(mItems[position]);
            mSelectableHelper.updateSelectState(holder, position);
            mItemClickHelper.bindClickListener(holder.itemView);
        }

        @Override
        public int getItemCount() {
            return mItems.length;
        }

        private static class ViewHolder extends RecyclerView.ViewHolder
                implements SelectableHelper.Selectable {
            final TextView tvItemTitle;

            private final ImageView ivChecked;
            private final int mTextColor;
            private final int mCheckedTextColor;

            public ViewHolder(@NonNull View itemView) {
                super(itemView);

                ivChecked = itemView.findViewById(R.id.ivChecked);
                tvItemTitle = itemView.findViewById(R.id.tvItemTitle);

                mTextColor = tvItemTitle.getCurrentTextColor();
                mCheckedTextColor = itemView.getContext().getResources().getColor(R.color.colorPrimary);
            }

            @Override
            public void onSelected() {
                ivChecked.setVisibility(View.VISIBLE);
                tvItemTitle.setTextColor(mCheckedTextColor);
            }

            @Override
            public void onUnselected() {
                ivChecked.setVisibility(View.GONE);
                tvItemTitle.setTextColor(mTextColor);
            }
        }
    }

    public static class Builder {
        private final Context mContext;
        private String mTitle;
        private String[] mItems;
        private int mCheckedItem;
        private DialogInterface.OnClickListener mClickListener;

        public Builder(@NonNull Context context) {
            mContext = context;
            mTitle = mContext.getString(R.string.title_single_choice);
            mItems = new String[0];
        }

        public Builder setTitle(@StringRes int resId) {
            return setTitle(mContext.getString(resId));
        }

        @NonNull
        public Builder setTitle(@NonNull String title) {
            Preconditions.checkNotNull(title);
            mTitle = title;
            return this;
        }

        public Builder setItems(int[] itemsId, int checkedItem, @Nullable DialogInterface.OnClickListener listener) {
            Preconditions.checkNotNull(itemsId);

            String[] items = new String[itemsId.length];

            for (int i = 0; i < itemsId.length; i++) {
                items[i] = mContext.getString(itemsId[i]);
            }

            return setItems(items, checkedItem, listener);
        }

        @NonNull
        public Builder setItems(@NonNull String[] items, int checkedItem, @Nullable DialogInterface.OnClickListener listener)
                throws IndexOutOfBoundsException {
            Preconditions.checkNotNull(items);
            if (checkedItem >= items.length) {
                throw new IndexOutOfBoundsException("items length: " + items.length + ", checkedItem: " + checkedItem);
            }

            mItems = items;
            mCheckedItem = checkedItem;
            mClickListener = listener;

            return this;
        }

        public SingleChoiceDialog build() {
            SingleChoiceDialog dialog = new SingleChoiceDialog();

            dialog.mTitle = mTitle;
            dialog.mItems = mItems;
            dialog.mCheckedItem = mCheckedItem;
            dialog.mClickListener = mClickListener;

            return dialog;
        }
    }
}
