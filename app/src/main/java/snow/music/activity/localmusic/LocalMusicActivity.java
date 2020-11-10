package snow.music.activity.localmusic;

import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import android.os.Bundle;
import android.view.View;

import snow.music.R;
import snow.music.activity.ListActivity;
import snow.music.fragment.musiclist.MusicListFragment;
import snow.music.service.AppPlayerService;
import snow.music.store.MusicStore;
import snow.music.util.PlayerUtil;
import snow.player.lifecycle.PlayerViewModel;

public class LocalMusicActivity extends ListActivity {
    private MusicListFragment mMusicListFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_local_music);

        initPlayerClient();

        Fragment fragment = getSupportFragmentManager().findFragmentById(R.id.musicListContainer);
        if (fragment instanceof MusicListFragment) {
            mMusicListFragment = (MusicListFragment) fragment;
        } else {
            mMusicListFragment = MusicListFragment.newInstance(MusicStore.MUSIC_LIST_LOCAL_MUSIC);

            getSupportFragmentManager().beginTransaction()
                    .add(R.id.musicListContainer, mMusicListFragment, "MusicList")
                    .commit();
        }
    }

    private void initPlayerClient() {
        ViewModelProvider viewModelProvider = new ViewModelProvider(this);
        PlayerViewModel playerViewModel = viewModelProvider.get(PlayerViewModel.class);
        PlayerUtil.initPlayerViewModel(this, playerViewModel, AppPlayerService.class);
        setPlayerClient(playerViewModel.getPlayerClient());
    }

    public void finishSelf(View view) {
        finish();
    }

    public void onOptionMenuClicked(View view) {
        switch (view.getId()) {
            case R.id.btnSearch:
                // TODO
                break;
            case R.id.btnSort:
                mMusicListFragment.showSortDialog();
                break;
            case R.id.btnScan:
                // TODO
                break;
        }
    }
}