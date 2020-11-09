package snow.music.activity.localmusic;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import android.os.Bundle;
import android.view.View;

import snow.music.R;
import snow.music.fragment.musiclist.MusicListFragment;
import snow.music.store.MusicStore;

public class LocalMusicActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_local_music);

        Fragment fragment = getSupportFragmentManager().findFragmentById(R.id.musicListContainer);
        if (fragment == null) {
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.musicListContainer, MusicListFragment.newInstance(MusicStore.MUSIC_LIST_LOCAL_MUSIC), "MusicList")
                    .commit();
        }
    }

    public void finishSelf(View view) {
        finish();
    }

    public void onOptionMenuClicked(View view) {
        // TODO
    }
}