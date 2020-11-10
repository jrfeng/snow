package snow.music.activity.favorite;

import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import android.os.Bundle;
import android.view.View;

import snow.music.R;
import snow.music.activity.ListActivity;
import snow.music.fragment.musiclist.FavoriteMusicListFragment;
import snow.music.service.AppPlayerService;
import snow.music.util.PlayerUtil;
import snow.player.lifecycle.PlayerViewModel;

public class FavoriteActivity extends ListActivity {
    private FavoriteMusicListFragment mFavoriteMusicListFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_favorite);

        initPlayerClient();

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
                mFavoriteMusicListFragment.showSortDialog();
                break;
        }
    }
}