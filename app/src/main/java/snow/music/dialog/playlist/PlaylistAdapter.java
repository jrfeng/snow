package snow.music.dialog.playlist;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.lifecycle.LifecycleOwner;
import androidx.recyclerview.widget.RecyclerView;

import snow.music.R;
import snow.player.audio.MusicItem;
import snow.player.lifecycle.PlayerViewModel;
import snow.player.playlist.Playlist;

public class PlaylistAdapter extends RecyclerView.Adapter<PlaylistAdapter.ViewHolder> {
    private LifecycleOwner mLifecycleOwner;
    private PlayerViewModel mPlayerViewModel;
    private Playlist mPlaylist;

    public PlaylistAdapter(LifecycleOwner lifecycleOwner, PlayerViewModel playerViewModel) {
        mLifecycleOwner = lifecycleOwner;
        mPlayerViewModel = playerViewModel;

        mPlaylist = mPlayerViewModel.getPlaylist().getValue();
        observePlaylist();
        // TODO observe play position
    }

    private void observePlaylist() {
        mPlayerViewModel.getPlaylist()
                .observe(mLifecycleOwner, playlist -> {
                    mPlaylist = playlist;
                    // TODO DiffUtil
                    PlaylistAdapter.this.notifyDataSetChanged();
                });
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_playlist, parent, false);
        return new ViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        MusicItem musicItem = mPlaylist.get(position);

        holder.tvTitle.setText(musicItem.getTitle());
    }

    @Override
    public int getItemCount() {
        // TODO empty view
        return mPlaylist.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        public TextView tvTitle;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);

            tvTitle = itemView.findViewById(R.id.tvTitle);
        }
    }
}
