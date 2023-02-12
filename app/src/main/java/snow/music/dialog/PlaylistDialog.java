package snow.music.dialog;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.Resources;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatDialog;
import androidx.fragment.app.FragmentActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.common.base.Preconditions;

import recyclerview.helper.ItemClickHelper;
import recyclerview.helper.PositionHelper;
import recyclerview.helper.ScrollToPositionHelper;
import recyclerview.helper.SelectableHelper;
import snow.music.R;
import snow.music.service.AppPlayerService;
import snow.music.util.ItemDragCallback;
import snow.music.util.PlayerUtil;
import snow.player.PlayerClient;
import snow.player.audio.MusicItem;
import snow.player.lifecycle.PlayerViewModel;
import snow.player.lifecycle.PlaylistLiveData;
import snow.player.playlist.Playlist;
import snow.player.util.MovablePlaylist;

public class PlaylistDialog extends BottomDialog {
    private PlayerViewModel mPlayerViewModel;
    private PlaylistAdapter mPlaylistAdapter;
    private PlaylistLiveData mPlaylistLiveData;

    private TextView tvPlaylistTitle;
    private RecyclerView rvPlaylist;

    private ScrollToPositionHelper mScrollToPositionHelper;

    public static PlaylistDialog newInstance() {
        return new PlaylistDialog();
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);

        FragmentActivity activity = getActivity();
        assert activity != null;

        ViewModelProvider provider = new ViewModelProvider(activity);
        mPlayerViewModel = provider.get(PlayerViewModel.class);
        PlayerUtil.initPlayerViewModel(context, mPlayerViewModel, AppPlayerService.class);
    }

    @Override
    protected void onInitDialog(AppCompatDialog dialog) {
        dialog.setContentView(R.layout.dialog_playlist);

        tvPlaylistTitle = dialog.findViewById(R.id.tvPlaylistTitle);
        rvPlaylist = dialog.findViewById(R.id.rvPlaylist);

        initRecyclerView(rvPlaylist);
        Context context = getContext();
        assert context != null;
        mScrollToPositionHelper = new ScrollToPositionHelper(rvPlaylist, context.getResources().getColor(R.color.colorPlaylistItemFlash));

        ImageButton btnLocate = dialog.findViewById(R.id.btnLocate);
        assert btnLocate != null;
        btnLocate.setOnClickListener(v -> {
            if (mPlayerViewModel.getPlayerClient().getPlaylistSize() <= 0) {
                return;
            }

            mScrollToPositionHelper.smoothScrollToPosition(mPlayerViewModel.getPlayerClient().getPlayPosition());
        });
    }

    @Override
    protected boolean keepOnRestarted() {
        return false;
    }

    private void initRecyclerView(RecyclerView rvPlaylist) {
        Context context = getContext();
        if (context == null) {
            throw new IllegalStateException("Context is null.");
        }

        rvPlaylist.setLayoutManager(new LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false));
        initPlaylistAdapter();
        initDragHelper();
    }

    @SuppressLint("SetTextI18n")
    private void initPlaylistAdapter() {
        PlayerClient playerClient = mPlayerViewModel.getPlayerClient();

        mPlaylistAdapter = new PlaylistAdapter(new Playlist.Builder().build(), 0);
        mPlaylistAdapter.setLoading(true);

        rvPlaylist.setAdapter(mPlaylistAdapter);

        playerClient.getPlaylist(playlist -> {
            mPlaylistAdapter = new PlaylistAdapter(playlist, playerClient.getPlayPosition());
            mPlaylistAdapter.setLoading(false);
            mPlaylistAdapter.setOnItemClickListener((position, viewId, view, holder) -> {
                if (viewId == R.id.playlistItem) {
                    playerClient.playPause(position);
                } else if (viewId == R.id.btnRemove) {
                    removeMusicItem(position);
                }
            });

            // 必须替换掉原来的 Adapter，否则 scrollToPosition 后的位置不符合需求，该位置的列表项会显示在低端，而不是顶端、
            rvPlaylist.swapAdapter(mPlaylistAdapter, true);
            rvPlaylist.scrollToPosition(playerClient.getPlayPosition());


            mPlaylistLiveData = new PlaylistLiveData(playerClient, playlist);
            mPlaylistLiveData.observe(PlaylistDialog.this, newPlaylist -> {
                tvPlaylistTitle.setText(getText(R.string.playlist) + "(" + newPlaylist.size() + ")");
                mPlaylistAdapter.setPlaylist(newPlaylist, playerClient.getPlayPosition());
            });

            mPlayerViewModel.getPlayingMusicItem()
                    .observe(PlaylistDialog.this, musicItem -> mPlaylistAdapter.setPlayPosition(playerClient.getPlayPosition()));
        });
    }

    private void initDragHelper() {
        ItemDragCallback.OnDragCallback callback = new ItemDragCallback.OnDragCallback() {
            @Override
            public void onDragging(int from, int target) {
                mPlaylistAdapter.move(from, target);
            }

            @Override
            public void onDragComplete(int fromPosition, int toPosition) {
                mPlayerViewModel.getPlayerClient().moveMusicItem(fromPosition, toPosition);
            }
        };

        ItemTouchHelper helper = new ItemTouchHelper(new ItemDragCallback(callback));
        helper.attachToRecyclerView(rvPlaylist);
    }

    private void removeMusicItem(int position) {
        Context context = getContext();
        Playlist playlist = mPlaylistLiveData.getValue();
        if (context == null || playlist == null) {
            return;
        }

        MusicItem musicItem = playlist.get(position);
        MessageDialog messageDialog = new MessageDialog.Builder(context)
                .setTitle(musicItem.getTitle())
                .setMessage(R.string.message_remove_from_playlist)
                .setPositiveTextColor(context.getResources().getColor(R.color.red_500))
                .setPositiveButtonClickListener((dialog, which) -> mPlayerViewModel.getPlayerClient().removeMusicItem(position))
                .setDisableEnterAnim(true)
                .build();

        messageDialog.show(getParentFragmentManager(), "RemoveMusicItem");
    }

    private static class PlaylistAdapter extends RecyclerView.Adapter<PlaylistAdapter.ViewHolder> {
        private static final int TYPE_EMPTY_VIEW = 1;
        private static final int TYPE_ITEM_VIEW = 2;
        private static final int TYPE_EMPTY_LOADING = 3;
        private MovablePlaylist mPlaylist;

        private final ItemClickHelper mItemClickHelper;
        private final SelectableHelper mSelectableHelper;
        private final PositionHelper<PlaylistAdapter.ViewHolder> mPositionHelper;

        private boolean mLoading;

        public PlaylistAdapter(@NonNull Playlist playlist, int playPosition) {
            Preconditions.checkNotNull(playlist);
            mPlaylist = new MovablePlaylist(playlist, playPosition);

            mItemClickHelper = new ItemClickHelper();
            mSelectableHelper = new SelectableHelper(this);
            mPositionHelper = new PositionHelper<>(this);

            if (mPlaylist.isEmpty()) {
                return;
            }

            mSelectableHelper.setSelect(playPosition, true);
        }

        @SuppressLint("NotifyDataSetChanged")
        public void setPlaylist(@NonNull Playlist playlist, int playPosition) {
            Preconditions.checkNotNull(playlist);

            if (playlist.isEmpty()) {
                mPlaylist = new MovablePlaylist(playlist, playPosition);
                notifyDataSetChanged();
                mSelectableHelper.clearSelected();
                return;
            }

            if (mPlaylist.isEmpty()) {
                mPlaylist = new MovablePlaylist(playlist, playPosition);
                notifyDataSetChanged();
                mSelectableHelper.setSelect(playPosition, true);
                return;
            }

            DiffUtil.DiffResult diffResult = DiffUtil.calculateDiff(new MusicItemDiffCallback(mPlaylist, playlist));
            diffResult.dispatchUpdatesTo(this);

            mPlaylist = new MovablePlaylist(playlist, playPosition);
            mSelectableHelper.setSelect(playPosition, true);
        }

        public void setPlayPosition(int playPosition) {
            if (mPlaylist.isEmpty()) {
                mSelectableHelper.clearSelected();
                return;
            }

            mSelectableHelper.setSelect(playPosition, true);
        }

        public void setOnItemClickListener(ItemClickHelper.OnItemClickListener listener) {
            mItemClickHelper.setOnItemClickListener(listener);
        }

        public void setLoading(boolean loading) {
            mLoading = loading;
            if (mPlaylist.isEmpty()) {
                notifyItemChanged(0);
            }
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
            boolean emptyView = (viewType == TYPE_EMPTY_VIEW) || (viewType == TYPE_EMPTY_LOADING);
            int layoutId = R.layout.item_playlist;
            if (emptyView) {
                layoutId = getEmptyLayoutId();
            }

            View itemView = LayoutInflater.from(parent.getContext())
                    .inflate(layoutId, parent, false);
            return new ViewHolder(itemView, emptyView);
        }

        @SuppressLint("SetTextI18n")
        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            if (mPlaylist.isEmpty()) {
                return;
            }

            holder.tvPosition.setText(String.valueOf(position + 1));

            MusicItem musicItem = mPlaylist.get(position);
            holder.tvTitle.setText(musicItem.getTitle());
            holder.tvArtist.setText(" - " + musicItem.getArtist());

            mItemClickHelper.bindClickListener(holder.itemView, holder.btnRemove);
            mSelectableHelper.updateSelectState(holder, position);
        }

        @Override
        public int getItemCount() {
            if (mPlaylist.isEmpty()) {
                return 1;
            }

            return mPlaylist.size();
        }

        @Override
        public int getItemViewType(int position) {
            if (mPlaylist.isEmpty()) {
                return getEmptyType();
            }

            return TYPE_ITEM_VIEW;
        }

        public void move(int from, int target) {
            mPlaylist.move(from, target);
            notifyItemMoved(from, target);
        }

        private int getEmptyType() {
            if (mLoading) {
                return TYPE_EMPTY_LOADING;
            }

            return TYPE_EMPTY_VIEW;
        }

        private int getEmptyLayoutId() {
            if (mLoading) {
                return R.layout.empty_loading;
            }

            return R.layout.empty_playlist;
        }

        private static class ViewHolder extends RecyclerView.ViewHolder implements SelectableHelper.Selectable,
                PositionHelper.OnPositionChangeListener {
            private final boolean mEmptyView;
            private int mColorMark;
            private int mColorTitle;
            private int mColorArtist;

            TextView tvPosition;
            View mark;
            TextView tvTitle;
            TextView tvArtist;
            ImageButton btnRemove;

            public ViewHolder(@NonNull View itemView, boolean emptyView) {
                super(itemView);

                mEmptyView = emptyView;
                if (emptyView) {
                    return;
                }

                tvPosition = itemView.findViewById(R.id.tvPosition);
                mark = itemView.findViewById(R.id.mark);
                tvTitle = itemView.findViewById(R.id.tvTitle);
                tvArtist = itemView.findViewById(R.id.tvArtist);
                btnRemove = itemView.findViewById(R.id.btnRemove);

                Resources res = itemView.getContext().getResources();

                mColorMark = res.getColor(R.color.colorMark);
                mColorTitle = res.getColor(R.color.colorText);
                mColorArtist = res.getColor(R.color.colorSecondaryText);
            }

            @Override
            public void onSelected() {
                if (mEmptyView) {
                    return;
                }

                mark.setVisibility(View.VISIBLE);
                tvTitle.setTextColor(mColorMark);
                tvArtist.setTextColor(mColorMark);
            }

            @Override
            public void onUnselected() {
                if (mEmptyView) {
                    return;
                }

                mark.setVisibility(View.GONE);
                tvTitle.setTextColor(mColorTitle);
                tvArtist.setTextColor(mColorArtist);
            }

            @Override
            public void onPositionChanged(int oldPosition, int newPosition) {
                if (mEmptyView) {
                    return;
                }

                tvPosition.setText(String.valueOf(newPosition + 1));
            }
        }

        private static class MusicItemDiffCallback extends DiffUtil.Callback {
            private final MovablePlaylist mOldPlaylist;
            private final Playlist mNewPlaylist;

            MusicItemDiffCallback(MovablePlaylist oldPlaylist, Playlist newPlaylist) {
                mOldPlaylist = oldPlaylist;
                mNewPlaylist = newPlaylist;
            }

            @Override
            public int getOldListSize() {
                return mOldPlaylist.size();
            }

            @Override
            public int getNewListSize() {
                return mNewPlaylist.size();
            }

            @Override
            public boolean areItemsTheSame(int oldItemPosition, int newItemPosition) {
                return mOldPlaylist.get(oldItemPosition).equals(mNewPlaylist.get(newItemPosition));
            }

            @Override
            public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
                return areItemsTheSame(oldItemPosition, newItemPosition);
            }
        }
    }
}
