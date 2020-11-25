package snow.music.activity.search;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.common.base.Preconditions;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

import recyclerview.helper.ItemClickHelper;
import snow.music.R;
import snow.music.store.Music;

public class SearchResultAdapter extends RecyclerView.Adapter<SearchResultAdapter.ViewHolder> {
    private static final int TYPE_EMPTY = 1;
    private static final int TYPE_ITEM = 2;

    private List<Music> mSearchResult;
    private ItemClickHelper mItemClickHelper;

    private String mEmptyMessage;
    private WeakReference<TextView> mEmptyViewWeakReference;

    public SearchResultAdapter(@NonNull List<Music> searchResult) {
        mSearchResult = new ArrayList<>(searchResult);
        mItemClickHelper = new ItemClickHelper();
        mEmptyViewWeakReference = new WeakReference<>(null);
    }

    public void setSearchResult(List<Music> searchResult) {
        mSearchResult = new ArrayList<>(searchResult);
        notifyDataSetChanged();
    }

    public void setOnItemClickListener(ItemClickHelper.OnItemClickListener listener) {
        mItemClickHelper.setOnItemClickListener(listener);
    }

    public void setEmptyMessage(@NonNull String message) {
        Preconditions.checkNotNull(message);

        mEmptyMessage = message;
        TextView emptyView = mEmptyViewWeakReference.get();
        if (emptyView != null) {
            emptyView.setText(mEmptyMessage);
        }
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
        int layoutId = R.layout.item_search_result;
        boolean empty = (viewType == TYPE_EMPTY);

        if (empty) {
            layoutId = R.layout.empty_search_result;
        }

        View itemView = LayoutInflater.from(parent.getContext())
                .inflate(layoutId, parent, false);

        return new ViewHolder(itemView, empty);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        if (holder.empty) {
            holder.emptyView.setText(mEmptyMessage);
            mEmptyViewWeakReference = new WeakReference<>(holder.emptyView);
            return;
        }

        Music music = mSearchResult.get(position);

        holder.tvTitle.setText(music.getTitle());

        mItemClickHelper.bindClickListener(holder.btnPlay);
    }

    @Override
    public int getItemCount() {
        if (mSearchResult.isEmpty()) {
            return 1;
        }

        return mSearchResult.size();
    }

    @Override
    public int getItemViewType(int position) {
        if (mSearchResult.isEmpty()) {
            return TYPE_EMPTY;
        }

        return TYPE_ITEM;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        public boolean empty;
        public TextView emptyView;

        public TextView tvTitle;
        public Button btnPlay;

        public ViewHolder(@NonNull View itemView, boolean empty) {
            super(itemView);

            this.empty = empty;
            if (empty) {
                emptyView = (TextView) itemView;
                return;
            }

            tvTitle = itemView.findViewById(R.id.tvTitle);
            btnPlay = itemView.findViewById(R.id.btnPlay);
        }
    }
}
