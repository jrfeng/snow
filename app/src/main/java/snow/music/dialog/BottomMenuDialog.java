package snow.music.dialog;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.appcompat.app.AppCompatDialog;
import androidx.appcompat.app.AppCompatDialogFragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.common.base.Preconditions;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import recyclerview.helper.ItemClickHelper;
import snow.music.R;
import snow.music.util.DialogUtil;

public class BottomMenuDialog extends AppCompatDialogFragment {
    private String mDialogTitle = "";
    private List<MenuItem> mMenuItems = new ArrayList<>();

    @Nullable
    private MenuItemAdapter.OnMenuItemClickListener mMenuItemClickListener;

    public static BottomMenuDialog newInstance() {
        return new BottomMenuDialog();
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        AppCompatDialog dialog = new AppCompatDialog(getContext(), getTheme());

        DialogUtil.setWith(dialog, WindowManager.LayoutParams.MATCH_PARENT);
        DialogUtil.setGravity(dialog, Gravity.BOTTOM);
        DialogUtil.setBackgroundDrawableResource(dialog, R.drawable.bg_playlist);
        DialogUtil.setAnimations(dialog, R.style.PlaylistTransition);
        dialog.setCanceledOnTouchOutside(true);

        dialog.setContentView(R.layout.dialog_bottom_menu);

        TextView tvDialogTitle = dialog.findViewById(R.id.tvDialogTitle);
        RecyclerView rvMenuItems = dialog.findViewById(R.id.rvMenuItems);

        Objects.requireNonNull(tvDialogTitle);
        Objects.requireNonNull(rvMenuItems);

        tvDialogTitle.setText(mDialogTitle);
        rvMenuItems.setLayoutManager(new LinearLayoutManager(getContext()));
        MenuItemAdapter menuItemAdapter = new MenuItemAdapter(mMenuItems);
        menuItemAdapter.setOnMenuItemClickListener(mMenuItemClickListener);
        rvMenuItems.setAdapter(menuItemAdapter);

        return dialog;
    }

    public static class Builder {
        private Context mContext;
        private String mDialogTitle;
        private List<MenuItem> mMenuItems;
        private MenuItemAdapter.OnMenuItemClickListener mMenuItemClickListener;

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

        public Builder setOnMenuItemClickListener(MenuItemAdapter.OnMenuItemClickListener listener) {
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

    public static class MenuItemAdapter extends RecyclerView.Adapter<MenuItemAdapter.ViewHolder> {
        private List<MenuItem> mMenuItems;
        private ItemClickHelper mItemClickHelper;
        @Nullable
        private OnMenuItemClickListener mMenuItemClickListener;
        private ItemClickHelper.OnItemClickListener mItemClickListener;

        public MenuItemAdapter(@NonNull List<MenuItem> menuItems) {
            Preconditions.checkNotNull(menuItems);
            mMenuItems = menuItems;
            mItemClickHelper = new ItemClickHelper();
            mItemClickListener = (position, viewId, view, holder) -> {
                if (mMenuItemClickListener != null) {
                    mMenuItemClickListener.onMenuItemClicked(position);
                }
            };
        }

        public void setOnMenuItemClickListener(@Nullable OnMenuItemClickListener listener) {
            mMenuItemClickListener = listener;
            mItemClickHelper.setOnItemClickListener(listener == null ? null : mItemClickListener);
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
            public ImageView menuIcon;
            public TextView menuTitle;

            public ViewHolder(@NonNull View itemView) {
                super(itemView);

                menuIcon = itemView.findViewById(R.id.ivMenuIcon);
                menuTitle = itemView.findViewById(R.id.tvMenuTitle);
            }
        }

        public interface OnMenuItemClickListener {
            void onMenuItemClicked(int position);
        }
    }

    public static class MenuItem {
        private int mIconId;
        private String mTitle;

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
