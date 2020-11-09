package snow.music.fragment.musiclist;

import android.content.Context;
import android.content.Intent;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.common.base.Preconditions;

import java.util.List;
import java.util.Objects;

import snow.music.R;
import snow.music.dialog.MessageDialog;
import snow.music.service.AppPlayerService;
import snow.music.store.Music;
import snow.music.store.MusicStore;
import snow.music.util.MusicListUtil;
import snow.music.util.MusicUtil;
import snow.music.util.PlayerUtil;
import snow.player.audio.MusicItem;
import snow.player.lifecycle.PlayerViewModel;

public abstract class BaseMusicListFragment extends Fragment {
    private static final int REQUEST_CODE_WRITE_SETTINGS = 1;

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

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode != REQUEST_CODE_WRITE_SETTINGS) {
            return;
        }

        Music ringtoneMusic = mMusicListViewModel.getRingtoneMusic();

        if (ringtoneMusic != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && checkSetRingtonePermission()) {
            setAsRingtone(ringtoneMusic);
        }
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
        showItemOptionMenu(getMusicListItems().get(position));
    }

    private void onMusicListItemLongClicked(int position) {
        // TODO start multi select activity
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
        // TODO show add to music list dialog
    }

    protected final void setAsRingtone(@NonNull Music music) {
        if (checkSetRingtonePermission()) {
            RingtoneManager.setActualDefaultRingtoneUri(mContext, RingtoneManager.TYPE_RINGTONE, Uri.parse(music.getUri()));
            Toast.makeText(mContext, R.string.toast_set_successfully, Toast.LENGTH_SHORT).show();
            return;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            mMusicListViewModel.setRingtoneMusic(music);
            requestSetRingtonePermission();
        }
    }

    private boolean checkSetRingtonePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return Settings.System.canWrite(mContext);
        }
        return true;
    }

    @RequiresApi(Build.VERSION_CODES.M)
    private void requestSetRingtonePermission() {
        MessageDialog messageDialog = new MessageDialog.Builder(mContext)
                .setMessage(R.string.message_need_write_settings_permission)
                .setPositiveButton(R.string.positive_text_request, (dialog, which) -> {
                    Intent intent = new Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS);
                    intent.setData(Uri.parse("package:" + mContext.getApplicationContext().getPackageName()));
                    startActivityForResult(intent, REQUEST_CODE_WRITE_SETTINGS);
                })
                .build();

        messageDialog.show(getParentFragmentManager(), "requestSetRingtonePermission");
    }

    protected final void removeMusic(@NonNull Music music) {
        MessageDialog messageDialog = new MessageDialog.Builder(mContext)
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
