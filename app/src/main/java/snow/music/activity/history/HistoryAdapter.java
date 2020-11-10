package snow.music.activity.history;

import android.annotation.SuppressLint;
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
import snow.music.R;
import snow.music.store.Music;

public class HistoryAdapter extends RecyclerView.Adapter<HistoryAdapter.ViewHolder> {
    private static final int TYPE_EMPTY_VIEW = 1;
    private static final int TYPE_ITEM_VIEW = 2;

    private List<Music> mHistory;
    private ItemClickHelper mItemClickHelper;
    private OnItemClickListener mOnItemClickListener;

    public HistoryAdapter(@NonNull List<Music> history) {
        mHistory = new ArrayList<>(history);
        mItemClickHelper = new ItemClickHelper();
        mItemClickHelper.setOnItemClickListener((position, viewId, view, holder) -> {
            if (mOnItemClickListener == null) {
                return;
            }

            switch (viewId) {
                case R.id.historyItem:
                    mOnItemClickListener.onItemClicked(position, mHistory.get(position));
                    break;
                case R.id.btnRemove:
                    mOnItemClickListener.onRemoveClicked(position, mHistory.get(position));
                    break;
            }
        });
    }

    public void setHistory(@NonNull List<Music> history) {
        Preconditions.checkNotNull(history);

        if (mHistory.isEmpty() || history.isEmpty()) {
            mHistory = new ArrayList<>(history);
            notifyDataSetChanged();
            return;
        }

        DiffUtil.DiffResult result = DiffUtil.calculateDiff(new DiffUtil.Callback() {
            @Override
            public int getOldListSize() {
                return mHistory.size();
            }

            @Override
            public int getNewListSize() {
                return history.size();
            }

            @Override
            public boolean areItemsTheSame(int oldItemPosition, int newItemPosition) {
                return mHistory.get(oldItemPosition).equals(history.get(newItemPosition));
            }

            @Override
            public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
                return areItemsTheSame(oldItemPosition, newItemPosition);
            }
        });

        result.dispatchUpdatesTo(this);
        mHistory = new ArrayList<>(history);
    }

    public void setOnItemClickListener(OnItemClickListener itemClickListener) {
        mOnItemClickListener = itemClickListener;
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
        int layoutId = R.layout.item_history;
        boolean emptyView = (viewType == TYPE_EMPTY_VIEW);
        if (emptyView) {
            layoutId = R.layout.empty_history;
        }

        return new ViewHolder(LayoutInflater.from(parent.getContext()).inflate(layoutId, parent, false), emptyView);
    }

    @SuppressLint("SetTextI18n")
    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        if (holder.emptyView) {
            return;
        }

        Music music = mHistory.get(position);

        holder.tvTitle.setText(music.getTitle());
        holder.tvArtistAndAlbum.setText(music.getArtist() + " - " + music.getAlbum());

        mItemClickHelper.bindClickListener(holder.itemView, holder.btnRemove);
    }

    @Override
    public int getItemCount() {
        if (mHistory.isEmpty()) {
            return 1;
        }

        return mHistory.size();
    }

    @Override
    public int getItemViewType(int position) {
        if (mHistory.isEmpty()) {
            return TYPE_EMPTY_VIEW;
        }

        return TYPE_ITEM_VIEW;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        boolean emptyView;

        TextView tvTitle;
        TextView tvArtistAndAlbum;
        ImageButton btnRemove;

        public ViewHolder(@NonNull View itemView, boolean emptyView) {
            super(itemView);

            this.emptyView = emptyView;
            if (emptyView) {
                return;
            }

            tvTitle = itemView.findViewById(R.id.tvTitle);
            tvArtistAndAlbum = itemView.findViewById(R.id.tvArtistAndAlbum);
            btnRemove = itemView.findViewById(R.id.btnRemove);
        }
    }

    public interface OnItemClickListener {
        void onItemClicked(int position, @NonNull Music music);

        void onRemoveClicked(int position, @NonNull Music music);
    }
}
