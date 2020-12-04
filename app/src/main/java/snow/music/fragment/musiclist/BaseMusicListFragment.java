package snow.music.fragment.musiclist;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.common.base.Preconditions;

import java.util.List;
import java.util.Objects;

import snow.music.R;
import snow.music.activity.multichoice.MusicMultiChoiceActivity;
import snow.music.activity.multichoice.MultiChoiceStateHolder;
import snow.music.dialog.AddToMusicListDialog;
import snow.music.dialog.MessageDialog;
import snow.music.dialog.SingleChoiceDialog;
import snow.music.service.AppPlayerService;
import snow.music.store.Music;
import snow.music.store.MusicList;
import snow.music.store.MusicStore;
import snow.music.util.MusicListUtil;
import snow.music.util.MusicUtil;
import snow.music.util.PlayerUtil;
import snow.player.PlayerClient;
import snow.player.audio.MusicItem;
import snow.player.lifecycle.PlayerViewModel;

public abstract class BaseMusicListFragment extends Fragment {
    private static final int REQUEST_CODE_MULTI_CHOICE = 1;

    private Context mContext;
    private PlayerViewModel mPlayerViewModel;
    private BaseMusicListViewModel mMusicListViewModel;

    private MusicListAdapter mMusicListAdapter;
    private LinearLayoutManager mLinearLayoutManager;

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

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_CODE_MULTI_CHOICE) {
            checkResultCode(resultCode);
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        PlayerClient playerClient = mPlayerViewModel.getPlayerClient();
        if (!playerClient.isConnected()) {
            playerClient.connect();
        }
    }

    private void checkResultCode(int resultCode) {
        MultiChoiceStateHolder stateHolder = MultiChoiceStateHolder.getInstance();
        mLinearLayoutManager.onRestoreInstanceState(stateHolder.consumeLayoutManagerState());

        if (resultCode == MusicMultiChoiceActivity.RESULT_CODE_MODIFIED) {
            mMusicListViewModel.setIgnoreDiffUtil(true);
            mMusicListViewModel.setMusicListItems(stateHolder.getMusicList());
        }
    }

    private void observeMusicListItems() {
        mMusicListViewModel.getMusicListItems()
                .observe(getViewLifecycleOwner(), musicList -> {
                    if (mMusicListAdapter == null) {
                        return;
                    }

                    mMusicListAdapter.setMusicList(musicList, mMusicListViewModel.consumeIgnoreDiffUtil());

                    MusicItem musicItem = mPlayerViewModel.getPlayerClient().getPlayingMusicItem();

                    if (musicItem == null || !matchPlaylistName()) {
                        mMusicListAdapter.clearPlayPosition();
                        return;
                    }

                    int index = mMusicListViewModel.indexOf(MusicUtil.asMusic(musicItem));
                    mMusicListAdapter.setPlayPosition(index);
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
        mLinearLayoutManager = new LinearLayoutManager(mContext);
        rvMusicList.setLayoutManager(mLinearLayoutManager);
        rvMusicList.setAdapter(mMusicListAdapter);
    }

    private void initMusicListAdapter() {
        mMusicListAdapter = new MusicListAdapter(getMusicListItems());

        mMusicListViewModel.getLoadingMusicList()
                .observe(getViewLifecycleOwner(), mMusicListAdapter::setLoading);

        mMusicListAdapter.setOnItemClickListener((position, viewId, view, holder) -> {
            if (viewId == R.id.musicListItem) {
                onMusicListItemClicked(position);
            } else if (viewId == R.id.btnOptionMenu) {
                onMusicListItemMenuClicked(position);
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
                MusicListUtil.asPlaylist(mMusicListViewModel.getMusicListName(), getMusicListItems(), position),
                position,
                true);
    }

    private boolean isPlayingMusic(int position) {
        MusicItem musicItem = MusicUtil.asMusicItem(getMusicListItems().get(position));
        return musicItem.equals(mPlayerViewModel.getPlayerClient().getPlayingMusicItem());
    }

    private void onMusicListItemMenuClicked(int position) {
        showItemOptionMenu(getMusicListItems().get(position));
    }

    private void onMusicListItemLongClicked(int position) {
        MultiChoiceStateHolder stateHolder = MultiChoiceStateHolder.getInstance();

        stateHolder.setMusicList(Objects.requireNonNull(mMusicListViewModel.getMusicListItems().getValue()));
        stateHolder.setFavorite(isFavorite());
        stateHolder.setItemRemovable(isItemRemovable());
        stateHolder.setLayoutManagerState(mLinearLayoutManager.onSaveInstanceState());
        stateHolder.setMusicListName(mMusicListViewModel.getMusicListName());
        stateHolder.setPosition(position);

        Intent intent = new Intent(getContext(), MusicMultiChoiceActivity.class);
        startActivityForResult(intent, REQUEST_CODE_MULTI_CHOICE);
    }

    private boolean isFavorite() {
        return mMusicListViewModel.getMusicListName().equals(MusicStore.MUSIC_LIST_FAVORITE);
    }

    private boolean isItemRemovable() {
        return MusicStore.getInstance().isNameExists(mMusicListViewModel.getMusicListName());
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

    public void showSortDialog() {
        SingleChoiceDialog singleChoiceDialog = new SingleChoiceDialog.Builder(mContext)
                .setTitle(R.string.title_sort_music_list)
                .setItems(new int[]{R.string.item_sort_by_add_time,
                                R.string.item_sort_by_title,
                                R.string.item_sort_by_artist,
                                R.string.item_sort_by_album},
                        mMusicListViewModel.getSortOrder().ordinal(),
                        (dialog, which) -> {
                            dialog.dismiss();
                            MusicList.SortOrder sortOrder = MusicList.SortOrder.values()[which];
                            mMusicListViewModel.sortMusicList(sortOrder);
                        })
                .build();

        singleChoiceDialog.show(getParentFragmentManager(), "sortMusicList");
    }

    protected final void setNextPlay(@NonNull Music music) {
        mPlayerViewModel.setNextPlay(MusicUtil.asMusicItem(music));
        Toast.makeText(mContext, R.string.toast_added_to_playlist, Toast.LENGTH_SHORT).show();
    }

    protected final void addToFavorite(@NonNull Music music) {
        Preconditions.checkNotNull(music);
        MusicStore.getInstance().addToFavorite(music);
        Toast.makeText(mContext, R.string.toast_added, Toast.LENGTH_SHORT).show();
    }

    protected final void removeFavorite(@NonNull Music music) {
        Preconditions.checkNotNull(music);

        MessageDialog messageDialog = new MessageDialog.Builder(mContext)
                .setMessage(R.string.message_remove_from_favorite)
                .setPositiveTextColor(getResources().getColor(R.color.red_500))
                .setPositiveButtonClickListener((dialog, which) -> {
                    MusicStore.getInstance().removeFromFavorite(music);
                    Toast.makeText(mContext, R.string.toast_removed, Toast.LENGTH_SHORT).show();
                })
                .build();

        messageDialog.show(getParentFragmentManager(), "removeFavorite");
    }

    protected final void addToMusicList(@NonNull Music music) {
        AddToMusicListDialog dialog = AddToMusicListDialog.newInstance(music);
        dialog.show(getParentFragmentManager(), "addToMusicList");
    }

    protected final void setAsRingtone(@NonNull Music music) {
        MusicUtil.setAsRingtone(getParentFragmentManager(), music);
    }

    protected final void removeMusic(@NonNull Music music) {
        MessageDialog messageDialog = new MessageDialog.Builder(mContext)
                .setTitle(music.getTitle())
                .setMessage(R.string.message_remove_music)
                .setPositiveTextColor(getResources().getColor(R.color.red_500))
                .setPositiveButtonClickListener((dialog, which) -> {
                    mMusicListViewModel.removeMusic(music);
                    Toast.makeText(mContext, R.string.toast_removed, Toast.LENGTH_SHORT).show();
                })
                .build();

        messageDialog.show(getParentFragmentManager(), "removeMusic");
    }

    protected abstract BaseMusicListViewModel onCreateMusicListViewModel(ViewModelProvider viewModelProvider);

    protected abstract void showItemOptionMenu(@NonNull Music music);
}
