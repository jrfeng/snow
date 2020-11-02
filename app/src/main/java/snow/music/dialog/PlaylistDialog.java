package snow.music.dialog;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.Context;
import android.content.res.Resources;
import android.os.Build;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatDialog;
import androidx.appcompat.app.AppCompatDialogFragment;
import androidx.fragment.app.FragmentActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.common.base.Preconditions;

import recyclerview.helper.ItemClickHelper;
import recyclerview.helper.ScrollToPositionHelper;
import recyclerview.helper.SelectableHelper;
import snow.music.R;
import snow.music.service.AppPlayerService;
import snow.music.util.DialogUtil;
import snow.player.PlayerClient;
import snow.player.audio.MusicItem;
import snow.player.lifecycle.PlayerViewModel;
import snow.player.lifecycle.PlaylistLiveData;
import snow.player.playlist.Playlist;

public class PlaylistDialog extends AppCompatDialogFragment {
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
        initPlayerViewModel(context);
    }

    private void initPlayerViewModel(Context context) {
        if (mPlayerViewModel.isInitialized()) {
            return;
        }

        PlayerClient playerClient = PlayerClient.newInstance(context, AppPlayerService.class);
        playerClient.setAutoConnect(true);
        playerClient.connect();

        mPlayerViewModel.init(context, playerClient);
        mPlayerViewModel.setAutoDisconnect(true);
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

        dialog.setContentView(R.layout.dialog_playlist);

        tvPlaylistTitle = dialog.findViewById(R.id.tvPlaylistTitle);
        rvPlaylist = dialog.findViewById(R.id.rvPlaylist);

        initRecyclerView(rvPlaylist);
        mScrollToPositionHelper = new ScrollToPositionHelper(rvPlaylist, getResources().getColor(R.color.deep_purple_50));

        ImageButton btnLocate = dialog.findViewById(R.id.btnLocate);
        assert btnLocate != null;
        btnLocate.setOnClickListener(v -> mScrollToPositionHelper.smoothScrollToPosition(mPlayerViewModel.getPlayerClient().getPlayPosition()));

        return dialog;
    }

    private void initRecyclerView(RecyclerView rvPlaylist) {
        Context context = getContext();
        if (context == null) {
            throw new IllegalStateException("Context is null.");
        }

        rvPlaylist.setLayoutManager(new LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false));
        initPlaylistAdapter();
    }

    @SuppressLint("SetTextI18n")
    private void initPlaylistAdapter() {
        PlayerClient playerClient = mPlayerViewModel.getPlayerClient();
        playerClient.getPlaylist(playlist -> {
            mPlaylistLiveData = new PlaylistLiveData(playerClient, playlist);
            mPlaylistAdapter = new PlaylistAdapter(playlist, playerClient.getPlayPosition());
            rvPlaylist.setAdapter(mPlaylistAdapter);
            rvPlaylist.scrollToPosition(playerClient.getPlayPosition());

            mPlaylistAdapter.setOnItemClickListener((position, viewId, view, holder) -> {
                switch (viewId) {
                    case R.id.playlistItem:
                        playerClient.playPause(position);
                        break;
                    case R.id.btnRemove:
                        playerClient.removeMusicItem(position);
                        break;
                }
            });

            mPlaylistLiveData.observe(PlaylistDialog.this, newPlaylist -> {
                tvPlaylistTitle.setText(getText(R.string.playlist) + "(" + newPlaylist.size() + ")");
                mPlaylistAdapter.setPlaylist(newPlaylist, playerClient.getPlayPosition());
            });

            mPlayerViewModel.getPlayingMusicItem()
                    .observe(PlaylistDialog.this, musicItem -> mPlaylistAdapter.setPlayPosition(playerClient.getPlayPosition()));
        });
    }

    private static class PlaylistAdapter extends RecyclerView.Adapter<PlaylistAdapter.ViewHolder> {
        private static final int TYPE_EMPTY_VIEW = 1;
        private static final int TYPE_ITEM_VIEW = 2;
        private Playlist mPlaylist;

        private ItemClickHelper mItemClickHelper;
        private SelectableHelper mSelectableHelper;

        public PlaylistAdapter(@NonNull Playlist playlist, int playPosition) {
            Preconditions.checkNotNull(playlist);
            mPlaylist = playlist;

            mItemClickHelper = new ItemClickHelper();
            mSelectableHelper = new SelectableHelper(this);

            if (mPlaylist.isEmpty()) {
                return;
            }

            mSelectableHelper.setSelect(playPosition, true);
        }

        public void setPlaylist(@NonNull Playlist playlist, int playPosition) {
            Preconditions.checkNotNull(playlist);

            if (playlist.isEmpty()) {
                mPlaylist = playlist;
                notifyDataSetChanged();
                mSelectableHelper.clearSelected();
                return;
            }

            if (mPlaylist.isEmpty()) {
                mPlaylist = playlist;
                notifyDataSetChanged();
                mSelectableHelper.setSelect(playPosition, true);
                return;
            }

            DiffUtil.DiffResult diffResult = DiffUtil.calculateDiff(new MusicItemDiffCallback(mPlaylist, playlist));
            diffResult.dispatchUpdatesTo(this);

            mPlaylist = playlist;
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
            boolean emptyView = viewType == TYPE_EMPTY_VIEW;
            int layoutId = R.layout.item_playlist;
            if (emptyView) {
                layoutId = R.layout.empty_playlist;
            }

            View itemView = LayoutInflater.from(parent.getContext())
                    .inflate(layoutId, parent, false);
            return new ViewHolder(itemView, emptyView);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            if (mPlaylist.isEmpty()) {
                return;
            }

            MusicItem musicItem = mPlaylist.get(position);
            holder.tvTitle.setText(musicItem.getTitle());

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
                return TYPE_EMPTY_VIEW;
            }

            return TYPE_ITEM_VIEW;
        }

        private static class ViewHolder extends RecyclerView.ViewHolder implements SelectableHelper.Selectable {
            private boolean mEmptyView;
            private int mColorMark;
            private int mColorText;

            View mark;
            TextView tvTitle;
            ImageButton btnRemove;

            public ViewHolder(@NonNull View itemView, boolean emptyView) {
                super(itemView);

                mEmptyView = emptyView;
                if (emptyView) {
                    return;
                }

                mark = itemView.findViewById(R.id.mark);
                tvTitle = itemView.findViewById(R.id.tvTitle);
                btnRemove = itemView.findViewById(R.id.btnRemove);

                Resources res = itemView.getResources();

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    mColorMark = res.getColor(R.color.colorMark, itemView.getContext().getTheme());
                    mColorText = res.getColor(R.color.colorText, itemView.getContext().getTheme());
                } else {
                    mColorMark = res.getColor(R.color.colorMark);
                    mColorText = res.getColor(R.color.colorText);
                }
            }

            @Override
            public void onSelected() {
                if (mEmptyView) {
                    return;
                }

                mark.setVisibility(View.VISIBLE);
                tvTitle.setTextColor(mColorMark);
            }

            @Override
            public void onUnselected() {
                if (mEmptyView) {
                    return;
                }

                mark.setVisibility(View.GONE);
                tvTitle.setTextColor(mColorText);
            }
        }

        private static class MusicItemDiffCallback extends DiffUtil.Callback {
            private Playlist mOldPlaylist;
            private Playlist mNewPlaylist;

            MusicItemDiffCallback(Playlist oldPlaylist, Playlist newPlaylist) {
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
