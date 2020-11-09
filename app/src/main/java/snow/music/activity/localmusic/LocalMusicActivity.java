package snow.music.activity.localmusic;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import android.os.Bundle;
import android.view.View;

import snow.music.R;
import snow.music.fragment.musiclist.MusicListFragment;
import snow.music.store.MusicStore;

public class LocalMusicActivity extends AppCompatActivity {
    private MusicListFragment mMusicListFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_local_music);

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