package snow.music.activity.localmusic;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

import io.reactivex.Single;
import io.reactivex.SingleOnSubscribe;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Consumer;
import io.reactivex.schedulers.Schedulers;
import snow.music.R;
import snow.music.adapter.MusicListAdapter;
import snow.music.dialog.BottomMenuDialog;
import snow.music.dialog.MessageDialog;
import snow.music.service.AppPlayerService;
import snow.music.store.Music;
import snow.music.store.MusicStore;
import snow.music.util.MusicUtil;
import snow.music.util.PlayerUtil;
import snow.player.audio.MusicItem;
import snow.player.lifecycle.PlayerViewModel;

public class LocalMusicActivity extends AppCompatActivity {
    private PlayerViewModel mPlayerViewModel;
    private MusicListAdapter mMusicListAdapter;
    private Disposable mGetAllMusicDisposable;

    private LocalMusicViewModel mLocalMusicViewModel;
    private Disposable mCheckFavoriteDisposable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_local_music);

        ViewModelProvider viewModelProvider = new ViewModelProvider(this);

        mPlayerViewModel = viewModelProvider.get(PlayerViewModel.class);
        mLocalMusicViewModel = viewModelProvider.get(LocalMusicViewModel.class);

        PlayerUtil.initPlayerViewModel(this, mPlayerViewModel, AppPlayerService.class);
        initRecyclerView();
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (!mPlayerViewModel.getPlayerClient().isConnected()) {
            mPlayerViewModel.getPlayerClient().connect();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();

        if (isFinishing() && mGetAllMusicDisposable != null) {
            mGetAllMusicDisposable.dispose();
        }
    }

    public void finishSelf(View view) {
        finish();
    }

    public void onOptionMenuClicked(View view) {
        // TODO
    }

    private void initRecyclerView() {
        RecyclerView rvMusicList = findViewById(R.id.rvMusicList);
        rvMusicList.setLayoutManager(new LinearLayoutManager(this));

        initMusicListAdapter();
        rvMusicList.setAdapter(mMusicListAdapter);

        mGetAllMusicDisposable = Single.create((SingleOnSubscribe<List<Music>>) emitter -> emitter.onSuccess(MusicStore.getInstance()
                .getAllMusic())).subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(musicList -> {
                    mLocalMusicViewModel.setMusicList(new ArrayList<>(musicList));
                    mMusicListAdapter.setMusicList(musicList, mPlayerViewModel.getPlayerClient().getPlayPosition());
                });

        mPlayerViewModel.getPlayingMusicItem()
                .observe(this, playingMusicItem -> {
                    if (playingMusicItem != null &&
                            MusicStore.MUSIC_LIST_LOCAL_MUSIC.equals(mPlayerViewModel.getPlayerClient().getPlaylistToken())) {
                        int playPosition = mLocalMusicViewModel.getMusicList().indexOf(MusicUtil.asMusic(playingMusicItem));
                        mMusicListAdapter.setPlayPosition(playPosition);
                    } else {
                        mMusicListAdapter.setPlayPosition(-1);
                    }
                });
    }

    private void initMusicListAdapter() {
        mMusicListAdapter = new MusicListAdapter(mLocalMusicViewModel.getMusicList(),
                mPlayerViewModel.getPlayerClient().getPlayPosition());

        mMusicListAdapter.setOnItemClickListener((position, viewId, view, holder) -> {
            switch (viewId) {
                case R.id.musicListItem:
                    onMusicListItemClicked(position);
                    break;
                case R.id.btnOptionMenu:
                    onItemMenuClicked(position);
                    break;
            }
        });
    }

    private void onMusicListItemClicked(int position) {
        if (mLocalMusicViewModel.getPlaylistLastModified() != mPlayerViewModel.getPlayerClient().getLastModified()) {
            getFreshPlaylist(playPause(position));
            return;
        }

        playPause(position).run();
    }

    private void onItemMenuClicked(int position) {
        if (mCheckFavoriteDisposable != null) {
            mCheckFavoriteDisposable.dispose();
        }

        final Music music = mLocalMusicViewModel.getMusicList().get(position);
        mCheckFavoriteDisposable = Single.create((SingleOnSubscribe<Boolean>) emitter -> {
            boolean result = MusicStore.getInstance().isFavorite(music);
            if (emitter.isDisposed()) {
                return;
            }

            emitter.onSuccess(result);
        }).subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(showMusicOptionMenu(music));
    }

    private Consumer<Boolean> showMusicOptionMenu(final Music music) {
        return favorite -> {
            int drawableId = R.drawable.ic_menu_item_favorite_false;
            int stringId = R.string.menu_item_add_to_favorite;
            if (favorite) {
                drawableId = R.drawable.ic_menu_item_favorite_true;
                stringId = R.string.menu_item_remove_from_favorite;
            }

            BottomMenuDialog dialog = new BottomMenuDialog.Builder(getApplicationContext())
                    .setTitle(music.getTitle())
                    .addMenuItem(R.drawable.ic_menu_item_next_play, R.string.menu_item_next_play)
                    .addMenuItem(drawableId, stringId)
                    .addMenuItem(R.drawable.ic_menu_item_add, R.string.menu_item_add_to_music_list)
                    .addMenuItem(R.drawable.ic_menu_item_rington, R.string.menu_item_set_as_ringtone)
                    .addMenuItem(R.drawable.ic_menu_item_remove, R.string.menu_item_remove)
                    .setOnMenuItemClickListener((dialog2, clickedPosition) -> {
                        dialog2.dismiss();
                        onMusicOptionMenuClicked(clickedPosition, music, favorite);
                    })
                    .build();

            dialog.show(getSupportFragmentManager(), "MusicOptionMenu");
        };
    }

    private void onMusicOptionMenuClicked(int clickedPosition, Music music, boolean favorite) {
        switch (clickedPosition) {
            case 0:
                setNextPlay(music);
                break;
            case 1:
                if (favorite) {
                    showRemoveFromFavoriteDialog(music);
                } else {
                    addToFavorite(music);
                }
                break;
            case 2:
                addToMusicList(music);
                break;
            case 3:
                setAsRingtone(music);
                break;
            case 4:
                showRemoveMusicDialog(music);
                break;
        }
    }

    private void setNextPlay(Music music) {
        mPlayerViewModel.setNextPlay(MusicUtil.asMusicItem(music));
        Toast.makeText(getApplicationContext(), R.string.toast_added_to_playlist, Toast.LENGTH_SHORT).show();
    }

    @SuppressLint("CheckResult")
    private void addToFavorite(Music music) {
        Single.create((SingleOnSubscribe<Boolean>) emitter -> {
            MusicStore.getInstance()
                    .addToFavorite(music);
            emitter.onSuccess(true);
        }).subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(aBoolean -> Toast.makeText(getApplicationContext(), R.string.toast_added_to_favorites, Toast.LENGTH_SHORT).show());
    }

    private void showRemoveFromFavoriteDialog(Music music) {
        MessageDialog messageDialog = new MessageDialog.Builder(getApplicationContext())
                .setTitle(music.getTitle())
                .setMessage(R.string.message_remove_from_favorite)
                .setPositiveTextColor(getResources().getColor(R.color.red_500))
                .setPositiveButtonClickListener((dialog, which) -> removeFromFavorite(music))
                .build();

        messageDialog.show(getSupportFragmentManager(), "removeFromFavorite");
    }

    @SuppressLint("CheckResult")
    private void removeFromFavorite(Music music) {
        Single.create((SingleOnSubscribe<Boolean>) emitter -> {
            MusicStore.getInstance()
                    .removeFromFavorite(music);
            emitter.onSuccess(true);
        }).subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(aBoolean -> Toast.makeText(getApplicationContext(), R.string.toast_removed_from_favorites, Toast.LENGTH_SHORT).show());
    }

    private void addToMusicList(Music music) {
        // TODO
    }

    private void setAsRingtone(Music music) {
        // TODO
    }

    private void showRemoveMusicDialog(Music music) {
        MessageDialog messageDialog = new MessageDialog.Builder(getApplicationContext())
                .setTitle(music.getTitle())
                .setMessage(R.string.message_remove_music)
                .setPositiveTextColor(getResources().getColor(R.color.red_500))
                .setPositiveButtonClickListener((dialog, which) -> removeMusic(music))
                .build();

        messageDialog.show(getSupportFragmentManager(), "RemoveMusic");
    }

    private void removeMusic(Music music) {
        // TODO
    }

    private void getFreshPlaylist(Runnable task) {
        mPlayerViewModel.getPlayerClient().getPlaylist(playlist -> {
            mLocalMusicViewModel.setPlaylistLastModified(mPlayerViewModel.getPlayerClient().getLastModified());
            mLocalMusicViewModel.setPlaylist(playlist);
            task.run();
        });
    }

    private Runnable playPause(final int position) {
        return () -> {
            Music music = mLocalMusicViewModel.getMusicList().get(position);
            MusicItem musicItem = MusicUtil.asMusicItem(music);

            int index = mLocalMusicViewModel.getPlaylist().indexOf(musicItem);
            if (index > -1) {
                mPlayerViewModel.getPlayerClient().playPause(index);
                return;
            }

            mPlayerViewModel.getPlayerClient()
                    .setPlaylist(MusicUtil.asPlaylist(position, mLocalMusicViewModel.getMusicList(), MusicStore.MUSIC_LIST_LOCAL_MUSIC), position, true);
        };
    }
}