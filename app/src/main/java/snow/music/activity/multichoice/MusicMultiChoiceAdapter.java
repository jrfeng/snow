package snow.music.activity.multichoice;

import android.annotation.SuppressLint;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.common.base.Preconditions;

import java.util.ArrayList;
import java.util.List;

import recyclerview.helper.ItemClickHelper;
import recyclerview.helper.PositionHelper;
import recyclerview.helper.SelectableHelper;
import snow.music.R;
import snow.music.store.Music;

public class MusicMultiChoiceAdapter extends RecyclerView.Adapter<MusicMultiChoiceAdapter.ViewHolder> {
    private List<Music> mMusicList;
    private final ItemClickHelper mItemClickHelper;
    private final SelectableHelper mSelectableHelper;
    private final PositionHelper<ViewHolder> mPositionHelper;

    public MusicMultiChoiceAdapter(@NonNull List<Music> musicList, @NonNull List<Integer> selectedPosition) {
        Preconditions.checkNotNull(musicList);
        Preconditions.checkNotNull(selectedPosition);

        mMusicList = new ArrayList<>(musicList);
        mItemClickHelper = new ItemClickHelper();
        mSelectableHelper = new SelectableHelper(this);
        mPositionHelper = new PositionHelper<>(this);

        mSelectableHelper.setSelectMode(SelectableHelper.SelectMode.MULTIPLE);

        mItemClickHelper.setOnItemClickListener((position, viewId, view, holder) ->
                mSelectableHelper.toggle(position)
        );

        for (int position : selectedPosition) {
            mSelectableHelper.setSelect(position, true);
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
        View itemView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_music_multi_choice, parent, false);

        return new ViewHolder(itemView);
    }

    @SuppressLint("SetTextI18n")
    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Music music = mMusicList.get(position);

        holder.tvPosition.setText(String.valueOf(position + 1));
        holder.tvTitle.setText(music.getTitle());
        holder.tvArtistAndAlbum.setText(music.getArtist() + " - " + music.getAlbum());

        mItemClickHelper.bindClickListener(holder.itemView);
        mSelectableHelper.updateSelectState(holder, position);
    }

    @Override
    public int getItemCount() {
        return mMusicList.size();
    }

    @SuppressLint("NotifyDataSetChanged")
    public void setMusicList(List<Music> musicList) {
        mMusicList = new ArrayList<>(musicList);
        notifyDataSetChanged();
    }

    public void setOnSelectCountChangeListener(SelectableHelper.OnSelectCountChangeListener listener) {
        mSelectableHelper.setOnSelectCountChangeListener(listener);
    }

    public int getSelectedCount() {
        return mSelectableHelper.getSelectedCount();
    }

    public List<Integer> getAllSelectedPosition() {
        return mSelectableHelper.getSelectedPositions();
    }

    public List<Music> getAllSelectedMusic() {
        List<Integer> allSelectedPosition = mSelectableHelper.getSelectedPositions();
        List<Music> allSelectedMusic = new ArrayList<>(allSelectedPosition.size());

        for (Integer position : allSelectedPosition) {
            allSelectedMusic.add(mMusicList.get(position));
        }

        return allSelectedMusic;
    }

    public void selectAll() {
        for (int i = 0; i < mMusicList.size(); i++) {
            mSelectableHelper.setSelect(i, true);
        }
    }

    public void clearSelect() {
        mSelectableHelper.clearSelected();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder
            implements SelectableHelper.Selectable, PositionHelper.OnPositionChangeListener {
        TextView tvPosition;
        TextView tvTitle;
        TextView tvArtistAndAlbum;
        ImageView ivCheckBox;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);

            tvPosition = itemView.findViewById(R.id.tvPosition);
            tvTitle = itemView.findViewById(R.id.tvTitle);
            tvArtistAndAlbum = itemView.findViewById(R.id.tvArtistAndAlbum);
            ivCheckBox = itemView.findViewById(R.id.ivCheckBox);
        }

        @Override
        public void onSelected() {
            ivCheckBox.setImageResource(R.drawable.ic_checkbox_checked);
        }

        @Override
        public void onUnselected() {
            ivCheckBox.setImageResource(R.drawable.ic_checkbox_unchecked);
        }

        @Override
        public void onPositionChanged(int oldPosition, int newPosition) {
            tvPosition.setText(String.valueOf(newPosition));
        }
    }
}
