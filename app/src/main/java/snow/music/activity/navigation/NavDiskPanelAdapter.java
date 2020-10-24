package snow.music.activity.navigation;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.lifecycle.Observer;
import androidx.recyclerview.widget.PagerSnapHelper;
import androidx.recyclerview.widget.RecyclerView;

import com.google.common.base.Preconditions;

import snow.music.R;
import snow.player.audio.MusicItem;
import snow.player.playlist.Playlist;

public class NavDiskPanelAdapter extends RecyclerView.Adapter<NavDiskPanelAdapter.ViewHolder> {
    private NavigationViewModel mNavigationViewModel;
    private Playlist mPlaylist;

    private RecyclerView mRecyclerView;
    private PagerSnapHelper mPagerSnapHelper;

    private Observer<Playlist> mPlaylistObserver;
    private Observer<Integer> mPlayPositionObserver;
    private RecyclerView.OnScrollListener mScrollListener;

    public NavDiskPanelAdapter(@NonNull NavigationViewModel navigationViewModel) {
        Preconditions.checkNotNull(navigationViewModel);

        mNavigationViewModel = navigationViewModel;
        mPlaylist = navigationViewModel.getPlaylist().getValue();
        initAllObserver();
    }

    private void initAllObserver() {
        mPlaylistObserver = playlist -> {
            mPlaylist = playlist;
            notifyDataSetChanged();
        };

        mPlayPositionObserver = playPosition -> mRecyclerView.scrollToPosition(playPosition);

        mScrollListener = new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(@NonNull RecyclerView recyclerView, int newState) {
                if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                    RecyclerView.LayoutManager layoutManager = recyclerView.getLayoutManager();
                    assert layoutManager != null;

                    View snapView = mPagerSnapHelper.findSnapView(layoutManager);
                    assert snapView != null;

                    mNavigationViewModel.getPlayerClient()
                            .skipToPosition(recyclerView.getChildAdapterPosition(snapView));
                }
            }
        };
    }

    @Override
    public void onAttachedToRecyclerView(@NonNull RecyclerView recyclerView) {
        super.onAttachedToRecyclerView(recyclerView);
        mRecyclerView = recyclerView;

        mPagerSnapHelper = new PagerSnapHelper();
        mPagerSnapHelper.attachToRecyclerView(recyclerView);

        addAllObserver();
    }

    @Override
    public void onDetachedFromRecyclerView(@NonNull RecyclerView recyclerView) {
        super.onDetachedFromRecyclerView(recyclerView);
        removeAllObserver();
    }

    private void addAllObserver() {
        mNavigationViewModel.getPlaylist()
                .observeForever(mPlaylistObserver);

        mNavigationViewModel.getPlayPosition()
                .observeForever(mPlayPositionObserver);

        mRecyclerView.addOnScrollListener(mScrollListener);
    }

    private void removeAllObserver() {
        mNavigationViewModel.getPlaylist()
                .removeObserver(mPlaylistObserver);

        mNavigationViewModel.getPlayPosition()
                .removeObserver(mPlayPositionObserver);

        mRecyclerView.removeOnScrollListener(mScrollListener);
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_disk_panel, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        if (mPlaylist.isEmpty()) {
            holder.tvTitle.setText(R.string.empty_playlist);
            holder.tvArtist.setText(R.string.playlist_is_empty);
            return;
        }

        MusicItem musicItem = mPlaylist.get(position);

        holder.tvTitle.setText(musicItem.getTitle());
        holder.tvArtist.setText(musicItem.getArtist());
    }

    @Override
    public int getItemCount() {
        if (mPlaylist.isEmpty()) {
            return 1;
        }

        return mPlaylist.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView ivDisk;
        TextView tvTitle;
        TextView tvArtist;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);

            ivDisk = itemView.findViewById(R.id.ivDisk);
            tvTitle = itemView.findViewById(R.id.tvTitle);
            tvArtist = itemView.findViewById(R.id.tvArtist);
        }
    }
}
