package snow.music.activity.detail.album;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModelProvider;

import io.reactivex.Single;
import io.reactivex.SingleOnSubscribe;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import snow.music.R;
import snow.music.dialog.BottomDialog;
import snow.music.dialog.BottomMenuDialog;
import snow.music.fragment.musiclist.BaseMusicListFragment;
import snow.music.fragment.musiclist.BaseMusicListViewModel;
import snow.music.store.Music;
import snow.music.store.MusicStore;

public class AlbumDetailFragment extends BaseMusicListFragment {
    private static final String KEY_ALBUM = "ALBUM";

    private Disposable mCheckFavoriteDisposable;

    public static AlbumDetailFragment newInstance(String album) {
        AlbumDetailFragment fragment = new AlbumDetailFragment();

        Bundle args = new Bundle();
        args.putString(KEY_ALBUM, album);

        fragment.setArguments(args);
        return fragment;
    }

    private String getAlbum() {
        Bundle args = getArguments();
        if (args == null) {
            return "";
        }

        return args.getString(KEY_ALBUM, "");
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        disposeCheckFavorite();
    }

    @Override
    protected BaseMusicListViewModel onCreateMusicListViewModel(ViewModelProvider viewModelProvider) {
        AlbumDetailViewModel viewModel = viewModelProvider.get(AlbumDetailViewModel.class);
        viewModel.init(getAlbum());
        return viewModel;
    }

    @Override
    protected void showItemOptionMenu(@NonNull Music music) {
        disposeCheckFavorite();
        mCheckFavoriteDisposable = Single.create((SingleOnSubscribe<Boolean>) emitter -> {
            boolean result = MusicStore.getInstance().isFavorite(music);
            if (emitter.isDisposed()) {
                return;
            }
            emitter.onSuccess(result);
        }).subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(favorite -> showItemOptionMenu(favorite, music));
    }

    private void disposeCheckFavorite() {
        if (mCheckFavoriteDisposable != null && !mCheckFavoriteDisposable.isDisposed()) {
            mCheckFavoriteDisposable.dispose();
        }
    }

    private void showItemOptionMenu(boolean favorite, @NonNull Music music) {
        int favoriteIconRes = R.drawable.ic_menu_item_favorite_false;
        int favoriteTitleRes = R.string.menu_item_add_to_favorite;
        if (favorite) {
            favoriteIconRes = R.drawable.ic_menu_item_favorite_true;
            favoriteTitleRes = R.string.menu_item_remove_from_favorite;
        }

        BottomDialog bottomDialog = new BottomMenuDialog.Builder(requireContext())
                .setTitle(music.getTitle())
                .addMenuItem(R.drawable.ic_menu_item_next_play, R.string.menu_item_next_play)
                .addMenuItem(favoriteIconRes, favoriteTitleRes)
                .addMenuItem(R.drawable.ic_menu_item_add, R.string.menu_item_add_to_music_list)
                .addMenuItem(R.drawable.ic_menu_item_rington, R.string.menu_item_set_as_ringtone)
                .setOnMenuItemClickListener((dialog, position) -> {
                    dialog.dismiss();
                    switch (position) {
                        case 0:
                            setNextPlay(music);
                            break;
                        case 1:
                            if (favorite) {
                                removeFavorite(music);
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
                    }
                })
                .build();

        bottomDialog.show(getParentFragmentManager(), "showItemOptionMenu");
    }
}
