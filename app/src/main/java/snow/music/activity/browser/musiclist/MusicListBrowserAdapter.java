package snow.music.activity.browser.musiclist;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;

import com.google.common.base.Preconditions;

import java.util.ArrayList;
import java.util.List;

import recyclerview.helper.ItemClickHelper;
import snow.music.R;
import snow.music.store.MusicList;

public class MusicListBrowserAdapter extends RecyclerView.Adapter<MusicListBrowserAdapter.ViewHolder> {
    private static final int TYPE_EMPTY = 1;
    private static final int TYPE_ITEM_VIEW = 2;

    private List<MusicList> mMusicLists;
    private final ItemClickHelper mItemClickHelper;

    @Nullable
    private OnItemClickListener mOnItemClickListener;

    public MusicListBrowserAdapter(@NonNull List<MusicList> musicLists) {
        Preconditions.checkNotNull(musicLists);

        mMusicLists = new ArrayList<>(musicLists);
        mItemClickHelper = new ItemClickHelper();

        mItemClickHelper.setOnItemClickListener((position, viewId, view, holder) -> {
            if (viewId == R.id.musicListBrowserItem) {
                notifyItemClicked(position, OnItemClickListener.ITEM_VIEW);
            } else if (viewId == R.id.btnOptionMenu) {
                notifyItemClicked(position, OnItemClickListener.OPTION_MENU);
            }
        });
    }

    private void notifyItemClicked(int position, int which) {
        if (mOnItemClickListener != null) {
            mOnItemClickListener.onItemClicked(position, which);
        }
    }

    public void setOnItemClickListener(@Nullable OnItemClickListener onItemClickListener) {
        mOnItemClickListener = onItemClickListener;
    }

    public void setMusicLists(@NonNull List<MusicList> musicLists) {
        Preconditions.checkNotNull(musicLists);

        if (mMusicLists.isEmpty() || musicLists.isEmpty()) {
            mMusicLists = new ArrayList<>(musicLists);
            notifyDataSetChanged();
            return;
        }

        DiffUtil.DiffResult result = DiffUtil.calculateDiff(new DiffCallback(mMusicLists, musicLists));
        result.dispatchUpdatesTo(this);

        mMusicLists = new ArrayList<>(musicLists);
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
        int layoutId = R.layout.item_music_list_browser;
        boolean empty = mMusicLists.isEmpty();
        if (empty) {
            layoutId = R.layout.empty_no_music_list;
        }

        View itemView = LayoutInflater.from(parent.getContext())
                .inflate(layoutId, parent, false);

        return new ViewHolder(itemView, empty);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        if (mMusicLists.isEmpty()) {
            return;
        }

        MusicList musicList = mMusicLists.get(position);
        holder.tvMusicListName.setText(musicList.getName());

        switch (musicList.getSize()) {
            case 0:
                holder.tvMusicListSize.setText(R.string.music_list_size_0);
                break;
            case 1:
                holder.tvMusicListSize.setText(R.string.music_list_size_1);
                break;
            default:
                String s = holder.itemView.getContext().getString(R.string.music_list_size_n);
                holder.tvMusicListSize.setText(s.replaceFirst("n", String.valueOf(musicList.getSize())));
                break;
        }

        mItemClickHelper.bindClickListener(holder.itemView, holder.btnOptionMenu);
    }

    @Override
    public int getItemCount() {
        if (mMusicLists.isEmpty()) {
            return 1;
        }

        return mMusicLists.size();
    }

    @Override
    public int getItemViewType(int position) {
        if (mMusicLists.isEmpty()) {
            return TYPE_EMPTY;
        }

        return TYPE_ITEM_VIEW;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        public TextView tvMusicListName;
        public TextView tvMusicListSize;
        public ImageButton btnOptionMenu;

        public ViewHolder(@NonNull View itemView, boolean emptyView) {
            super(itemView);

            if (emptyView) {
                return;
            }

            tvMusicListName = itemView.findViewById(R.id.tvMusicListName);
            tvMusicListSize = itemView.findViewById(R.id.tvMusicListSize);
            btnOptionMenu = itemView.findViewById(R.id.btnOptionMenu);
        }
    }

    /**
     * 监听列表项的点击事件。
     */
    public interface OnItemClickListener {
        int ITEM_VIEW = 0;
        int OPTION_MENU = 1;

        /**
         * 当列表项被点击时会回调该方法。
         *
         * @param position 被点击的列表项位置
         * @param which    列表中的哪个 View 被点击。共有 2 个值：
         *                 <ul>
         *                 <li>{@link #ITEM_VIEW}：列表项自身被点击</li>
         *                 <li>{@link #OPTION_MENU：列表项的菜单被点击}</li>
         */
        void onItemClicked(int position, int which);
    }

    private static class DiffCallback extends DiffUtil.Callback {
        private final List<MusicList> mOldMusicLists;
        private final List<MusicList> mNewMusicLists;

        public DiffCallback(List<MusicList> oldMusicLists, List<MusicList> newMusicLists) {
            mOldMusicLists = oldMusicLists;
            mNewMusicLists = newMusicLists;
        }

        @Override
        public int getOldListSize() {
            return mOldMusicLists.size();
        }

        @Override
        public int getNewListSize() {
            return mNewMusicLists.size();
        }

        @Override
        public boolean areItemsTheSame(int oldItemPosition, int newItemPosition) {
            String oldItemName = mOldMusicLists.get(oldItemPosition).getName();
            String newItemName = mNewMusicLists.get(newItemPosition).getName();
            return oldItemName.equals(newItemName);
        }

        @Override
        public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
            return areItemsTheSame(oldItemPosition, newItemPosition);
        }
    }
}
