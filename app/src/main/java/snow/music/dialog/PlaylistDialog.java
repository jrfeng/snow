package snow.music.dialog;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatDialog;
import androidx.appcompat.app.AppCompatDialogFragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.common.base.Preconditions;

import snow.music.R;
import snow.music.util.DialogUtil;
import snow.player.PlayerClient;
import snow.player.PlayerService;
import snow.player.audio.MusicItem;
import snow.player.lifecycle.PlayerViewModel;
import snow.player.playlist.Playlist;

public class PlaylistDialog extends AppCompatDialogFragment {
    private Context mContext;
    private PlayerViewModel mPlayerViewModel;
    private PlaylistAdapter mPlaylistAdapter;

    public static PlaylistDialog newInstance() {
        return new PlaylistDialog();
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        mContext = context;

        ViewModelProvider provider = new ViewModelProvider(this);
        mPlayerViewModel = provider.get(PlayerViewModel.class);
        initPlayerViewModel();
        initPlaylistAdapter();

        mPlayerViewModel.getPlaylist()
                .observe(this, playlist -> {
                    mPlaylistAdapter.setPlaylist(playlist);
                });

        mPlayerViewModel.getPlayPosition()
                .observe(this, playPosition -> {
                    mPlaylistAdapter.setPlayPosition(playPosition);
                });
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
        RecyclerView rvPlaylist = dialog.findViewById(R.id.rvPlaylist);
        if (rvPlaylist == null) {
            throw new IllegalStateException("RecyclerView is null.");
        }

        rvPlaylist.setLayoutManager(new LinearLayoutManager(mContext, LinearLayoutManager.VERTICAL, false));
        rvPlaylist.setAdapter(mPlaylistAdapter);

        return dialog;
    }

    private void initPlayerViewModel() {
        if (mPlayerViewModel.isInitialized()) {
            return;
        }

        PlayerClient playerClient = PlayerClient.newInstance(mContext, PlayerService.class);
        playerClient.setAutoConnect(true);
        playerClient.connect();

        mPlayerViewModel.init(mContext, playerClient);
        mPlayerViewModel.setAutoDisconnect(true);
    }

    private void initPlaylistAdapter() {
        Playlist playlist = mPlayerViewModel.getPlaylist().getValue();
        Integer playPosition = mPlayerViewModel.getPlayPosition().getValue();
        assert playlist != null;
        assert playPosition != null;

        mPlaylistAdapter = new PlaylistAdapter(playlist, playPosition);
    }

    private static class PlaylistAdapter extends RecyclerView.Adapter<PlaylistAdapter.ViewHolder> {
        private static final int TYPE_EMPTY_VIEW = 1;
        private Playlist mPlaylist;
        private int mPlayPosition;

        public PlaylistAdapter(@NonNull Playlist playlist, int playPosition) {
            Preconditions.checkNotNull(playlist);
            mPlaylist = playlist;
            mPlayPosition = playPosition;
            // TODO update play position
        }

        public void setPlaylist(@NonNull Playlist playlist) {
            Preconditions.checkNotNull(playlist);
            mPlaylist = playlist;
            // TODO DiffUtil
            notifyDataSetChanged();
        }

        public void setPlayPosition(int playPosition) {
            mPlayPosition = playPosition;
            // TODO update play position
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

            return super.getItemViewType(position);
        }

        public static class ViewHolder extends RecyclerView.ViewHolder {
            public TextView tvTitle;

            public ViewHolder(@NonNull View itemView, boolean emptyView) {
                super(itemView);

                if (emptyView) {
                    return;
                }

                tvTitle = itemView.findViewById(R.id.tvTitle);
            }
        }
    }
}
