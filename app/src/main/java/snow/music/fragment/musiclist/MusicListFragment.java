package snow.music.fragment.musiclist;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModelProvider;

import com.google.common.base.Preconditions;

public class MusicListFragment extends BaseMusicListFragment {
    private static final String KEY_MUSIC_LIST_NAME = "MUSIC_LIST_NAME";

    public static MusicListFragment newInstance(@NonNull String musicListName) {
        Preconditions.checkNotNull(musicListName);

        Bundle args = new Bundle();
        args.putString(KEY_MUSIC_LIST_NAME, musicListName);

        MusicListFragment fragment = new MusicListFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    protected BaseMusicListViewModel onCreateMusicListViewModel(ViewModelProvider viewModelProvider) {
        Bundle args = getArguments();
        String musicListName = "";
        if (args != null) {
            musicListName = args.getString(KEY_MUSIC_LIST_NAME, "");
        }

        MusicListViewModel musicListViewModel = viewModelProvider.get(MusicListViewModel.class);
        musicListViewModel.init(musicListName);

        return musicListViewModel;
    }

    @Override
    protected void showItemOptionMenu() {

    }
}
