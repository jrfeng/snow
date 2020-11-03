package snow.music.dialog;

import android.app.Dialog;
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
    private int mTitleResId = R.string.dialog;
    private List<MenuItem> mMenuItems = new ArrayList<>();
    @Nullable
    private MenuItemAdapter mMenuItemAdapter;
    @Nullable
    private MenuItemAdapter.OnMenuItemClickListener mMenuItemClickListener;

    public static BottomMenuDialog newInstance(@StringRes int titleResId, @NonNull List<MenuItem> menuItems) {
        Preconditions.checkNotNull(menuItems);

        BottomMenuDialog dialog = new BottomMenuDialog();
        dialog.setDialogTitle(titleResId);
        dialog.setMenuItem(menuItems);
        return dialog;
    }

    private void setDialogTitle(@StringRes int titleResId) {
        mTitleResId = titleResId;
    }

    private void setMenuItem(@NonNull List<MenuItem> menuItems) {
        Preconditions.checkNotNull(menuItems);
        mMenuItems = new ArrayList<>(menuItems);
    }

    public void setOnMenuItemClickListener(MenuItemAdapter.OnMenuItemClickListener listener) {
        mMenuItemClickListener = listener;
        if (mMenuItemAdapter != null) {
            mMenuItemAdapter.setOnMenuItemClickListener(mMenuItemClickListener);
        }
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

        tvDialogTitle.setText(mTitleResId);
        rvMenuItems.setLayoutManager(new LinearLayoutManager(getContext()));
        mMenuItemAdapter = new MenuItemAdapter(mMenuItems);
        mMenuItemAdapter.setOnMenuItemClickListener(mMenuItemClickListener);
        rvMenuItems.setAdapter(mMenuItemAdapter);

        return dialog;
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
        private int mTitleId;

        public MenuItem(@DrawableRes int iconId, @StringRes int titleId) {
            mIconId = iconId;
            mTitleId = titleId;
        }

        @DrawableRes
        public int getIconId() {
            return mIconId;
        }

        @StringRes
        public int getTitleId() {
            return mTitleId;
        }
    }
}
