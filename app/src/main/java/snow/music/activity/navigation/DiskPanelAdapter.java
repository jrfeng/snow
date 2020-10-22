package snow.music.activity.navigation;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.recyclerview.widget.RecyclerView;

import snow.music.R;
import snow.player.audio.MusicItem;
import snow.player.lifecycle.PlayerViewModel;
import snow.player.playlist.Playlist;

public class DiskPanelAdapter extends RecyclerView.Adapter<DiskPanelAdapter.ViewHolder> {
    private LiveData<Playlist> mPlaylist;

    public DiskPanelAdapter(PlayerViewModel playerViewModel) {
        mPlaylist = playerViewModel.getPlaylist();

        // TODO 1. observe playlist
        // TODO 2. observe scroll to position
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_disk_panel, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        if (isEmpty()) {
            return;
        }

        MusicItem musicItem = getMusicItem(position);

        holder.tvTitle.setText(musicItem.getTitle());
        holder.tvArtist.setText(musicItem.getArtist());
    }

    private boolean isEmpty() {
        assert mPlaylist.getValue() != null;
        return mPlaylist.getValue().isEmpty();
    }

    private int getPlaylistSize() {
        assert mPlaylist.getValue() != null;
        return mPlaylist.getValue().size();
    }

    private MusicItem getMusicItem(int position) {
        assert mPlaylist.getValue() != null;
        return mPlaylist.getValue().get(position);
    }

    @Override
    public int getItemCount() {
        if (isEmpty()) {
            return 1;
        }

        return getPlaylistSize();
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
