package snow.music.activity.navigation;

import android.animation.ObjectAnimator;
import android.os.Build;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.LinearInterpolator;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.Observer;
import androidx.recyclerview.widget.PagerSnapHelper;
import androidx.recyclerview.widget.RecyclerView;

import com.google.common.base.Preconditions;

import snow.music.R;
import snow.player.PlayerClient;
import snow.player.audio.MusicItem;
import snow.player.playlist.Playlist;

public class NavDiskPanelAdapter extends RecyclerView.Adapter<NavDiskPanelAdapter.ViewHolder> {
    private NavigationViewModel mNavigationViewModel;
    private Playlist mPlaylist;

    private RecyclerView mRecyclerView;
    private PagerSnapHelper mPagerSnapHelper;

    private Observer<Playlist> mPlaylistObserver;
    private Observer<MusicItem> mPlayingMusicItemObserver;
    private Observer<Boolean> mPlayingNoStalledObserver;
    private RecyclerView.OnScrollListener mScrollListener;

    @Nullable
    private ObjectAnimator mDiskRotateAnimator;
    private long mDiskAnimPlayTime;
    private boolean mAnimPaused;

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

        mPlayingMusicItemObserver = musicItem -> {
            if (musicItem == null) {
                cancelDiskRotateAnim();
                return;
            }

            int position = mPlaylist.indexOf(musicItem);
            mRecyclerView.scrollToPosition(position);

            ViewHolder viewHolder = (ViewHolder) mRecyclerView.findViewHolderForAdapterPosition(position);
            if (viewHolder != null) {
                updateDiskRotateAnimator(viewHolder.ivDisk);
            }
        };

        mPlayingNoStalledObserver = playingNoStalled -> {
            if (mDiskRotateAnimator == null) {
                return;
            }

            if (playingNoStalled) {
                resumeDiskRotateAnim();
            } else {
                pauseDiskRotateAnim();
            }
        };

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
        cancelDiskRotateAnim();
    }

    private void addAllObserver() {
        mNavigationViewModel.getPlaylist()
                .observeForever(mPlaylistObserver);

        mNavigationViewModel.getPlayingMusicItem()
                .observeForever(mPlayingMusicItemObserver);

        mNavigationViewModel.getPlayingNoStalled()
                .observeForever(mPlayingNoStalledObserver);

        mRecyclerView.addOnScrollListener(mScrollListener);
    }

    private void removeAllObserver() {
        mNavigationViewModel.getPlaylist()
                .removeObserver(mPlaylistObserver);

        mNavigationViewModel.getPlayingMusicItem()
                .removeObserver(mPlayingMusicItemObserver);

        mNavigationViewModel.getPlayingNoStalled()
                .removeObserver(mPlayingNoStalledObserver);

        mRecyclerView.removeOnScrollListener(mScrollListener);
    }

    private void updateDiskRotateAnimator(View target) {
        cancelDiskRotateAnim();
        mDiskRotateAnimator = ObjectAnimator.ofFloat(target, "rotation", 0, 360);
        mDiskRotateAnimator.setDuration(20_000);
        mDiskRotateAnimator.setRepeatCount(-1);
        mDiskRotateAnimator.setRepeatMode(ObjectAnimator.RESTART);
        mDiskRotateAnimator.setInterpolator(new LinearInterpolator());

        if (shouldStartAnim()) {
            mDiskRotateAnimator.start();
        }
    }

    private boolean shouldStartAnim() {
        PlayerClient playerClient = mNavigationViewModel.getPlayerClient();
        return !mAnimPaused && playerClient.isPlaying() && !playerClient.isPreparing() && !playerClient.isStalled();
    }

    private void cancelDiskRotateAnim() {
        if (mDiskRotateAnimator != null) {
            mDiskRotateAnimator.cancel();
            View view = (View) mDiskRotateAnimator.getTarget();
            assert view != null;
            view.setRotation(0);
        }
    }

    private void pauseDiskRotateAnim() {
        if (mDiskRotateAnimator == null) {
            return;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            mDiskRotateAnimator.pause();
            return;
        }

        mDiskAnimPlayTime = mDiskRotateAnimator.getCurrentPlayTime();
        mDiskRotateAnimator.cancel();
    }

    private void resumeDiskRotateAnim() {
        if (mDiskRotateAnimator == null || !shouldStartAnim()) {
            return;
        }

        if (!mDiskRotateAnimator.isStarted()) {
            mDiskRotateAnimator.start();
            return;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            mDiskRotateAnimator.resume();
            return;
        }

        mDiskRotateAnimator.start();
        mDiskRotateAnimator.setCurrentPlayTime(mDiskAnimPlayTime);
    }

    public void pauseAnim() {
        mAnimPaused = true;
        pauseDiskRotateAnim();
    }

    public void resumeAnim() {
        mAnimPaused = false;
        resumeDiskRotateAnim();
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

        if (position == mNavigationViewModel.getPlayerClient().getPlayPosition()) {
            updateDiskRotateAnimator(holder.ivDisk);
        }
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
