package snow.music.fragment.musiclist;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;
import java.util.Objects;

import snow.music.R;
import snow.music.service.AppPlayerService;
import snow.music.store.Music;
import snow.music.util.MusicListUtil;
import snow.music.util.MusicUtil;
import snow.music.util.PlayerUtil;
import snow.player.audio.MusicItem;
import snow.player.lifecycle.PlayerViewModel;

public abstract class BaseMusicListFragment extends Fragment {
    private Context mContext;
    private PlayerViewModel mPlayerViewModel;
    private BaseMusicListViewModel mMusicListViewModel;

    private MusicListAdapter mMusicListAdapter;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mContext = getContext();

        ViewModelProvider viewModelProvider = new ViewModelProvider(Objects.requireNonNull(getActivity()));

        mPlayerViewModel = viewModelProvider.get(PlayerViewModel.class);
        mMusicListViewModel = onCreateMusicListViewModel(viewModelProvider);

        PlayerUtil.initPlayerViewModel(mContext, mPlayerViewModel, AppPlayerService.class);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View contentView = inflater.inflate(R.layout.fragment_music_list, container, false);
        initMusicListAdapter();
        initRecyclerView(contentView);
        return contentView;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        observeMusicListItems();
        observePlayPosition();
    }

    private void observeMusicListItems() {
        mMusicListViewModel.getMusicListItems()
                .observe(getViewLifecycleOwner(), musicList -> {
                    if (mMusicListAdapter == null) {
                        return;
                    }
                    mMusicListAdapter.setMusicList(musicList);
                });
    }

    private void observePlayPosition() {
        mPlayerViewModel.getPlayPosition()
                .observe(getViewLifecycleOwner(), playPosition -> {
                    if (mMusicListAdapter == null) {
                        return;
                    }

                    MusicItem musicItem = mPlayerViewModel.getPlayerClient().getPlayingMusicItem();

                    if (musicItem == null || !matchPlaylistName()) {
                        mMusicListAdapter.clearPlayPosition();
                        return;
                    }

                    if (matchPlaylistToken()) {
                        mMusicListAdapter.setPlayPosition(playPosition);
                        return;
                    }

                    int index = mMusicListViewModel.indexOf(MusicUtil.asMusic(musicItem));
                    mMusicListAdapter.setPlayPosition(index);
                });
    }

    private void initRecyclerView(View contentView) {
        RecyclerView rvMusicList = contentView.findViewById(R.id.rvMusicList);
        rvMusicList.setLayoutManager(new LinearLayoutManager(mContext));
        rvMusicList.setAdapter(mMusicListAdapter);
    }

    private void initMusicListAdapter() {
        mMusicListAdapter = new MusicListAdapter(getMusicListItems());

        mMusicListAdapter.setOnItemClickListener((position, viewId, view, holder) -> {
            switch (viewId) {
                case R.id.musicListItem:
                    onMusicListItemClicked(position);
                    break;
                case R.id.btnOptionMenu:
                    onMusicListItemMenuClicked(position);
                    break;
            }
        });

        mMusicListAdapter.setOnItemLongClickListener((position, viewId, view, holder) -> {
            onMusicListItemLongClicked(position);
            return true;
        });
    }

    private void onMusicListItemClicked(int position) {
        if (matchPlaylistName() && (matchPlaylistToken() || isPlayingMusic(position))) {
            mPlayerViewModel.getPlayerClient().playPause(position);
            return;
        }

        mPlayerViewModel.setPlaylist(
                MusicListUtil.asPlaylist(mMusicListViewModel.getMusicListName(), getMusicListItems()),
                position,
                true);
    }

    private boolean isPlayingMusic(int position) {
        MusicItem musicItem = MusicUtil.asMusicItem(getMusicListItems().get(position));
        return musicItem.equals(mPlayerViewModel.getPlayerClient().getPlayingMusicItem());
    }

    private void onMusicListItemMenuClicked(int position) {
        showItemOptionMenu();
    }

    private void onMusicListItemLongClicked(int position) {
        // TODO
    }

    @NonNull
    private List<Music> getMusicListItems() {
        List<Music> items = mMusicListViewModel.getMusicListItems().getValue();
        return Objects.requireNonNull(items);
    }

    private boolean matchPlaylistName() {
        return mMusicListViewModel.getMusicListName()
                .equals(mPlayerViewModel.getPlayerClient().getPlaylistName());
    }

    private boolean matchPlaylistToken() {
        return mMusicListViewModel.getMusicListToken()
                .equals(mPlayerViewModel.getPlayerClient().getPlaylistToken());
    }

    protected void setNextPlay(@NonNull Music music) {
        mPlayerViewModel.setNextPlay(MusicUtil.asMusicItem(music));
    }

    protected void addToFavorite(@NonNull Music music) {
        // TODO
    }

    protected void removeFavorite(@NonNull Music music) {
        // TODO
    }

    protected void addToMusicList(@NonNull Music music) {
        // TODO
    }

    protected void setAsRingtone(@NonNull Music music) {
        // TODO
    }

    private void removeMusic(@NonNull Music music) {
        // TODO
    }

    protected abstract BaseMusicListViewModel onCreateMusicListViewModel(ViewModelProvider viewModelProvider);

    protected abstract void showItemOptionMenu();
}
