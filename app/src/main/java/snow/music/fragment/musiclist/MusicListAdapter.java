package snow.music.fragment.musiclist;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;

import com.google.common.base.Preconditions;

import java.util.ArrayList;
import java.util.List;

import recyclerview.helper.ItemClickHelper;
import recyclerview.helper.PositionHelper;
import recyclerview.helper.SelectableHelper;
import snow.music.R;
import snow.music.store.Music;

public class MusicListAdapter extends RecyclerView.Adapter<MusicListAdapter.ViewHolder> {
    private static final int TYPE_EMPTY_VIEW = 1;
    private static final int TYPE_ITEM_VIEW = 2;
    private List<Music> mMusicList;

    private ItemClickHelper mItemClickHelper;
    private SelectableHelper mSelectableHelper;
    private PositionHelper<MusicListAdapter.ViewHolder> mPositionHelper;

    private boolean mIgnoreDiffUtilOnce;

    public MusicListAdapter(@NonNull List<Music> musicList) {
        Preconditions.checkNotNull(musicList);

        mMusicList = new ArrayList<>(musicList);

        mItemClickHelper = new ItemClickHelper();
        mSelectableHelper = new SelectableHelper(this);
        mPositionHelper = new PositionHelper<>(this);
    }

    public void setMusicList(@NonNull List<Music> musicList) {
        Preconditions.checkNotNull(musicList);

        if (mIgnoreDiffUtilOnce || mMusicList.isEmpty() || musicList.isEmpty()) {
            mIgnoreDiffUtilOnce = false;
            mMusicList = new ArrayList<>(musicList);
            notifyDataSetChanged();
        } else {
            List<Music> newMusicList = new ArrayList<>(musicList);
            DiffUtil.DiffResult diffResult = DiffUtil.calculateDiff(new OrderMusicDiffCallback(mMusicList, newMusicList));
            diffResult.dispatchUpdatesTo(this);
            mMusicList = newMusicList;
        }
    }

    /**
     * 设置是否忽略 DiffUtil。如果忽略的话，则没有动画效果。
     * <p>
     * 该方法仅对下次调用 {@link #setMusicList(List)} 方法时有效。
     */
    public void setIgnoreDiffUtilOnce(boolean ignoreDiffUtil) {
        mIgnoreDiffUtilOnce = ignoreDiffUtil;
    }

    public void setOnItemClickListener(ItemClickHelper.OnItemClickListener listener) {
        mItemClickHelper.setOnItemClickListener(listener);
    }

    public void setOnItemLongClickListener(ItemClickHelper.OnItemLongClickListener listener) {
        mItemClickHelper.setOnItemLongClickListener(listener);
    }

    public void setPlayPosition(int position) {
        if (mMusicList.isEmpty()) {
            return;
        }

        if (position < 0) {
            mSelectableHelper.clearSelected();
            return;
        }

        mSelectableHelper.setSelect(position, true);
    }

    public void clearPlayPosition() {
        mSelectableHelper.clearSelected();
    }

    @Override
    public void onAttachedToRecyclerView(@NonNull RecyclerView recyclerView) {
        super.onAttachedToRecyclerView(recyclerView);
        mItemClickHelper.attachToRecyclerView(recyclerView);
        mSelectableHelper.attachToRecyclerView(recyclerView);
        mPositionHelper.attachToRecyclerView(recyclerView);
    }

    @Override
    public void onDetachedFromRecyclerView(@NonNull RecyclerView recyclerView) {
        super.onDetachedFromRecyclerView(recyclerView);
        mItemClickHelper.detach();
        mSelectableHelper.detach();
        mPositionHelper.detach();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        int layoutId = R.layout.item_music_list;
        boolean emptyView = (viewType == TYPE_EMPTY_VIEW);
        if (emptyView) {
            layoutId = R.layout.empty_music_list;
        }

        return new ViewHolder(LayoutInflater.from(parent.getContext()).inflate(layoutId, parent, false), emptyView);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        if (mMusicList.isEmpty()) {
            return;
        }

        Music music = mMusicList.get(position);

        holder.tvPosition.setText(String.valueOf(position + 1));
        holder.tvTitle.setText(music.getTitle());
        holder.tvArtist.setText(music.getArtist());

        mItemClickHelper.bindClickListener(holder.musicListItem, holder.btnOptionMenu);
        mItemClickHelper.bindLongClickListener(holder.musicListItem);
        mSelectableHelper.updateSelectState(holder, position);
    }

    @Override
    public int getItemCount() {
        if (mMusicList.isEmpty()) {
            return 1;
        }

        return mMusicList.size();
    }

    @Override
    public int getItemViewType(int position) {
        if (mMusicList.isEmpty()) {
            return TYPE_EMPTY_VIEW;
        }

        return TYPE_ITEM_VIEW;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder implements SelectableHelper.Selectable,
            PositionHelper.OnPositionChangeListener {
        View musicListItem;
        TextView tvPosition;
        TextView tvTitle;
        TextView tvArtist;
        ImageButton btnOptionMenu;

        private View markView;
        private boolean mEmptyView;
        private int mTextColor;
        private int mSecondaryTextColor;

        private int mSelectedTextColor;

        public ViewHolder(@NonNull View itemView, boolean empty) {
            super(itemView);

            mEmptyView = empty;
            if (empty) {
                return;
            }

            musicListItem = itemView.findViewById(R.id.musicListItem);
            tvPosition = itemView.findViewById(R.id.tvPosition);
            tvTitle = itemView.findViewById(R.id.tvTitle);
            tvArtist = itemView.findViewById(R.id.tvArtist);
            btnOptionMenu = itemView.findViewById(R.id.btnOptionMenu);
            markView = itemView.findViewById(R.id.mark);

            mTextColor = tvTitle.getCurrentTextColor();
            mSecondaryTextColor = tvArtist.getCurrentTextColor();

            mSelectedTextColor = itemView.getResources().getColor(R.color.deep_purple_400);
        }

        @Override
        public void onSelected() {
            if (mEmptyView) {
                return;
            }

            tvTitle.setTextColor(mSelectedTextColor);
            tvArtist.setTextColor(mSelectedTextColor);
            markView.setVisibility(View.VISIBLE);
        }

        @Override
        public void onUnselected() {
            if (mEmptyView) {
                return;
            }

            tvTitle.setTextColor(mTextColor);
            tvArtist.setTextColor(mSecondaryTextColor);
            markView.setVisibility(View.GONE);
        }

        @Override
        public void onPositionChanged(int oldPosition, int newPosition) {
            if (mEmptyView) {
                return;
            }

            tvPosition.setText(String.valueOf(newPosition + 1));
        }
    }

    private static class OrderMusicDiffCallback extends DiffUtil.Callback {
        private List<Music> mOldMusicList;
        private List<Music> mNewMusicList;

        OrderMusicDiffCallback(@NonNull List<Music> oldMusicList, @NonNull List<Music> newMusicList) {
            Preconditions.checkNotNull(oldMusicList);
            Preconditions.checkNotNull(newMusicList);

            mOldMusicList = oldMusicList;
            mNewMusicList = newMusicList;
        }

        @Override
        public int getOldListSize() {
            return mOldMusicList.size();
        }

        @Override
        public int getNewListSize() {
            return mNewMusicList.size();
        }

        @Override
        public boolean areItemsTheSame(int oldItemPosition, int newItemPosition) {
            Music oldMusic = mOldMusicList.get(oldItemPosition);
            Music newMusic = mNewMusicList.get(newItemPosition);

            return oldMusic.equals(newMusic);
        }

        @Override
        public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
            return areItemsTheSame(oldItemPosition, newItemPosition);
        }
    }
}
