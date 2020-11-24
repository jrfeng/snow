package snow.music.dialog;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.appcompat.app.AppCompatDialog;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.common.base.Preconditions;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import recyclerview.helper.ItemClickHelper;
import snow.music.R;

public class BottomMenuDialog extends BottomDialog {
    private String mDialogTitle = "";
    private List<MenuItem> mMenuItems = new ArrayList<>();

    @Nullable
    private OnMenuItemClickListener mMenuItemClickListener;

    public static BottomMenuDialog newInstance() {
        return new BottomMenuDialog();
    }

    @Override
    protected void onInitDialog(AppCompatDialog dialog) {
        dialog.setContentView(R.layout.dialog_bottom_menu);

        TextView tvDialogTitle = dialog.findViewById(R.id.tvDialogTitle);
        RecyclerView rvMenuItems = dialog.findViewById(R.id.rvMenuItems);

        Objects.requireNonNull(tvDialogTitle);
        Objects.requireNonNull(rvMenuItems);

        tvDialogTitle.setText(mDialogTitle);
        rvMenuItems.setLayoutManager(new LinearLayoutManager(getContext()));
        MenuItemAdapter menuItemAdapter = new MenuItemAdapter(mMenuItems);
        menuItemAdapter.setOnMenuItemClickListener((position, viewId, view, holder) -> {
            if (mMenuItemClickListener != null) {
                mMenuItemClickListener.onMenuItemClicked(dialog, position);
            }
        });
        rvMenuItems.setAdapter(menuItemAdapter);
    }

    @Override
    protected boolean keepOnRestarted() {
        return false;
    }

    public static class Builder {
        private final Context mContext;
        private String mDialogTitle;
        private final List<MenuItem> mMenuItems;
        private OnMenuItemClickListener mMenuItemClickListener;

        public Builder(@NonNull Context context) {
            mContext = context;
            mDialogTitle = mContext.getString(R.string.dialog);
            mMenuItems = new ArrayList<>();
        }

        public Builder setTitle(@NonNull String title) {
            mDialogTitle = title;
            return this;
        }

        public Builder setTitle(@StringRes int titleId) {
            mDialogTitle = mContext.getString(titleId);
            return this;
        }

        public Builder addMenuItem(@DrawableRes int iconId, @NonNull String menuTitle) {
            Preconditions.checkNotNull(menuTitle);
            mMenuItems.add(new MenuItem(iconId, menuTitle));
            return this;
        }

        public Builder addMenuItem(@DrawableRes int iconId, @StringRes int meueTitleId) {
            mMenuItems.add(new MenuItem(iconId, mContext.getString(meueTitleId)));
            return this;
        }

        public Builder setOnMenuItemClickListener(OnMenuItemClickListener listener) {
            mMenuItemClickListener = listener;
            return this;
        }

        public BottomMenuDialog build() {
            BottomMenuDialog dialog = BottomMenuDialog.newInstance();

            dialog.mDialogTitle = mDialogTitle;
            dialog.mMenuItems = new ArrayList<>(mMenuItems);
            dialog.mMenuItemClickListener = mMenuItemClickListener;

            return dialog;
        }
    }

    public interface OnMenuItemClickListener {
        void onMenuItemClicked(AppCompatDialog dialog, int position);
    }

    private static class MenuItemAdapter extends RecyclerView.Adapter<MenuItemAdapter.ViewHolder> {
        private final List<MenuItem> mMenuItems;
        private final ItemClickHelper mItemClickHelper;

        public MenuItemAdapter(@NonNull List<MenuItem> menuItems) {
            Preconditions.checkNotNull(menuItems);
            mMenuItems = menuItems;
            mItemClickHelper = new ItemClickHelper();
        }

        public void setOnMenuItemClickListener(@Nullable ItemClickHelper.OnItemClickListener listener) {
            mItemClickHelper.setOnItemClickListener(listener);
        }

        @Override
        public void onAttachedToRecyclerView(@NonNull RecyclerView recyclerView) {
            super.onAttachedToRecyclerView(recyclerView);
            mItemClickHelper.attachToRecyclerView(recyclerView);
        }

        @Override
        public void onDetachedFromRecyclerView(@NonNull RecyclerView recyclerView) {
            super.onDetachedFromRecyclerView(recyclerView);
            mItemClickHelper.detach();
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View itemView = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_bottom_menu, parent, false);
            return new ViewHolder(itemView);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            MenuItem menuItem = mMenuItems.get(position);

            holder.menuIcon.setImageResource(menuItem.getIconId());
            holder.menuTitle.setText(menuItem.getTitleId());

            mItemClickHelper.bindClickListener(holder.itemView);
        }

        @Override
        public int getItemCount() {
            return mMenuItems.size();
        }

        public static class ViewHolder extends RecyclerView.ViewHolder {
            public final ImageView menuIcon;
            public final TextView menuTitle;

            public ViewHolder(@NonNull View itemView) {
                super(itemView);

                menuIcon = itemView.findViewById(R.id.ivMenuIcon);
                menuTitle = itemView.findViewById(R.id.tvMenuTitle);
            }
        }
    }

    private static class MenuItem {
        private final int mIconId;
        private final String mTitle;

        public MenuItem(@DrawableRes int iconId, @NonNull String title) {
            Preconditions.checkNotNull(title);
            mIconId = iconId;
            mTitle = title;
        }

        @DrawableRes
        public int getIconId() {
            return mIconId;
        }

        @NonNull
        public String getTitleId() {
            return mTitle;
        }
    }
}
