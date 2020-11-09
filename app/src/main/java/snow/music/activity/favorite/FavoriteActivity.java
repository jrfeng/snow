package snow.music.activity.favorite;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import android.os.Bundle;
import android.view.View;

import snow.music.R;
import snow.music.fragment.musiclist.FavoriteMusicListFragment;

public class FavoriteActivity extends AppCompatActivity {
    private FavoriteMusicListFragment mFavoriteMusicListFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_favorite);

        Fragment fragment = getSupportFragmentManager().findFragmentById(R.id.musicListContainer);
        if (fragment instanceof FavoriteMusicListFragment) {
            mFavoriteMusicListFragment = (FavoriteMusicListFragment) fragment;
        } else {
            mFavoriteMusicListFragment = FavoriteMusicListFragment.newInstance();

            getSupportFragmentManager().beginTransaction()
                    .add(R.id.musicListContainer, mFavoriteMusicListFragment, "favoriteMusicList")
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
                mFavoriteMusicListFragment.showSortDialog();
                break;
        }
    }
}