package snow.music.adapter;

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
import recyclerview.helper.SelectableHelper;
import snow.music.R;
import snow.music.store.Music;

public class MusicListAdapter extends RecyclerView.Adapter<MusicListAdapter.ViewHolder> {
    private static final int TYPE_EMPTY_VIEW = 1;
    private static final int TYPE_ITEM_VIEW = 2;
    private List<OrderMusic> mOrderMusicList;

    private ItemClickHelper mItemClickHelper;
    private SelectableHelper mSelectableHelper;

    public MusicListAdapter(@NonNull List<Music> musicList, int playPosition) {
        Preconditions.checkNotNull(musicList);

        mOrderMusicList = asOrderMusicList(musicList);

        mItemClickHelper = new ItemClickHelper();
        mSelectableHelper = new SelectableHelper(this);

        if (mOrderMusicList.isEmpty() || playPosition < 0) {
            return;
        }

        mSelectableHelper.setSelect(playPosition, true);
    }

    public void setMusicList(@NonNull List<Music> musicList, int playPosition) {
        Preconditions.checkNotNull(musicList);

        if (mOrderMusicList.isEmpty() || musicList.isEmpty()) {
            mOrderMusicList = asOrderMusicList(musicList);
            notifyDataSetChanged();
        } else {
            List<OrderMusic> newMusicList = asOrderMusicList(musicList);
            DiffUtil.DiffResult diffResult = DiffUtil.calculateDiff(new OrderMusicDiffCallback(mOrderMusicList, newMusicList));
            diffResult.dispatchUpdatesTo(this);
            mOrderMusicList = newMusicList;
        }

        if (mOrderMusicList.isEmpty() || playPosition < 0) {
            mSelectableHelper.clearSelected();
        } else {
            mSelectableHelper.setSelect(playPosition, true);
        }
    }

    public void setOnItemClickListener(ItemClickHelper.OnItemClickListener listener) {
        mItemClickHelper.setOnItemClickListener(listener);
    }

    public void setOnItemLongClickListener(ItemClickHelper.OnItemLongClickListener listener) {
        mItemClickHelper.setOnItemLongClickListener(listener);
    }

    public void setPlayPosition(int position) {
        if (mOrderMusicList.isEmpty()) {
            mSelectableHelper.clearSelected();
            return;
        }

        mSelectableHelper.setSelect(position, true);
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
        int layoutId = R.layout.item_music_list;
        boolean emptyView = viewType == TYPE_EMPTY_VIEW;
        if (emptyView) {
            layoutId = R.layout.empty_music_list;
        }

        return new ViewHolder(LayoutInflater.from(parent.getContext()).inflate(layoutId, parent, false), emptyView);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        if (mOrderMusicList.isEmpty()) {
            return;
        }

        OrderMusic orderMusic = mOrderMusicList.get(position);

        holder.tvOrder.setText(String.valueOf(orderMusic.order));
        holder.tvTitle.setText(orderMusic.music.getTitle());
        holder.tvArtist.setText(orderMusic.music.getArtist());

        mItemClickHelper.bindClickListener(holder.musicListItem, holder.btnOptionMenu);
        mItemClickHelper.bindLongClickListener(holder.musicListItem);
        mSelectableHelper.updateSelectState(holder, position);
    }

    @Override
    public int getItemCount() {
        if (mOrderMusicList.isEmpty()) {
            return 1;
        }

        return mOrderMusicList.size();
    }

    @Override
    public int getItemViewType(int position) {
        if (mOrderMusicList.isEmpty()) {
            return TYPE_EMPTY_VIEW;
        }

        return TYPE_ITEM_VIEW;
    }

    private List<OrderMusic> asOrderMusicList(@NonNull List<Music> musicList) {
        Preconditions.checkNotNull(musicList);

        List<OrderMusic> orderMusicList = new ArrayList<>(musicList.size());

        for (int i = 0; i < musicList.size(); i++) {
            orderMusicList.add(new OrderMusic(i + 1, musicList.get(i)));
        }

        return orderMusicList;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder implements SelectableHelper.Selectable {
        View musicListItem;
        TextView tvOrder;
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
            tvOrder = itemView.findViewById(R.id.tvOrder);
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
    }

    private static class OrderMusic {
        int order;
        Music music;

        OrderMusic(int order, Music music) {
            this.order = order;
            this.music = music;
        }
    }

    private static class OrderMusicDiffCallback extends DiffUtil.Callback {
        private List<OrderMusic> mOldMusicList;
        private List<OrderMusic> mNewMusicList;

        OrderMusicDiffCallback(@NonNull List<OrderMusic> oldMusicList, @NonNull List<OrderMusic> newMusicList) {
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
            OrderMusic oldMusic = mOldMusicList.get(oldItemPosition);
            OrderMusic newMusic = mNewMusicList.get(newItemPosition);

            return oldMusic.order == newMusic.order && oldMusic.music.equals(newMusic.music);
        }

        @Override
        public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
            return areItemsTheSame(oldItemPosition, newItemPosition);
        }
    }
}
