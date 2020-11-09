package snow.music.activity.favorite;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import android.os.Bundle;
import android.view.View;

import snow.music.R;
import snow.music.fragment.musiclist.FavoriteMusicListFragment;
import snow.music.fragment.musiclist.MusicListFragment;
import snow.music.store.MusicStore;

public class FavoriteActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_favorite);

        Fragment fragment = getSupportFragmentManager().findFragmentById(R.id.musicListContainer);
        if (fragment == null) {
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.musicListContainer, FavoriteMusicListFragment.newInstance(), "favoriteMusicList")
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