package snow.music.activity.localmusic;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.os.Bundle;
import android.view.View;

import java.util.ArrayList;
import java.util.List;

import io.reactivex.Single;
import io.reactivex.SingleOnSubscribe;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import snow.music.R;
import snow.music.adapter.MusicListAdapter;
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

    public void onActionMenuClicked(View view) {
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
        // TODO show option menu
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