package snow.music.activity.browser.musiclist;

import android.os.Bundle;
import android.view.View;

import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import snow.music.R;
import snow.music.activity.ListActivity;
import snow.music.dialog.BottomMenuDialog;
import snow.music.dialog.InputDialog;
import snow.music.dialog.MessageDialog;
import snow.music.store.MusicList;
import snow.music.util.InputValidator;

public class MusicListBrowserActivity extends ListActivity {
    private MusicListBrowserViewModel mViewModel;
    private RecyclerView rvMusicListBrowser;
    private MusicListBrowserAdapter mAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_music_list_browser);

        ViewModelProvider provider = new ViewModelProvider(this);
        mViewModel = provider.get(MusicListBrowserViewModel.class);

        rvMusicListBrowser = findViewById(R.id.rvMusicListBrowser);

        initRecyclerView();
    }

    private void initRecyclerView() {
        rvMusicListBrowser.setLayoutManager(new LinearLayoutManager(this));

        List<MusicList> allMusicList = mViewModel.getAllMusicList().getValue();
        assert allMusicList != null;

        mAdapter = new MusicListBrowserAdapter(allMusicList);
        rvMusicListBrowser.setAdapter(mAdapter);

        mAdapter.setOnItemClickListener((position, which) -> {
            switch (which) {
                case MusicListBrowserAdapter.OnItemClickListener.ITEM_VIEW:
                    mViewModel.navigateToMusicList(this, position);
                    break;
                case MusicListBrowserAdapter.OnItemClickListener.OPTION_MENU:
                    showOptionMenu(position);
                    break;
            }
        });

        mViewModel.getAllMusicList().observe(this, mAdapter::setMusicLists);
    }

    public void finishSelf(View view) {
        finish();
    }

    public void showCreateMusicListDialog(View view) {
        InputDialog inputDialog = new InputDialog.Builder(this)
                .setTitle(R.string.title_create_music_list)
                .setHint(R.string.hint_music_list_title)
                .setOnInputConfirmListener(new InputValidator(this), input -> {
                    assert input != null;
                    mViewModel.createMusicList(input);
                })
                .build();

        inputDialog.show(getSupportFragmentManager(), "createMusicList");
    }

    private void showOptionMenu(int position) {
        MusicList musicList = mViewModel.getMusicList(position);

        BottomMenuDialog dialog = new BottomMenuDialog.Builder(this)
                .setTitle(musicList.getName())
                .addMenuItem(R.drawable.ic_menu_item_rename_music_list, R.string.menu_item_rename_music_list)
                .addMenuItem(R.drawable.ic_menu_item_remove, R.string.menu_item_delete_music_list)
                .setOnMenuItemClickListener((dialog1, clickItemPosition) -> {
                    dialog1.dismiss();
                    switch (clickItemPosition) {
                        case 0:
                            renameMusicList(position, musicList);
                            break;
                        case 1:
                            deleteMusicList(musicList);
                            break;
                    }
                })
                .build();

        dialog.show(getSupportFragmentManager(), "editMusicList");
    }

    private void renameMusicList(int position, MusicList musicList) {
        InputDialog dialog = new InputDialog.Builder(this)
                .setTitle(R.string.menu_item_rename_music_list)
                .setText(musicList.getName())
                .setHint(R.string.hint_music_list_title)
                .setOnInputConfirmListener(new InputValidator(this), input -> {
                    assert input != null;
                    mViewModel.renameMusicList(musicList, input);
                    mAdapter.notifyItemChanged(position);
                })
                .build();

        dialog.show(getSupportFragmentManager(), "renameMusicList");
    }

    private void deleteMusicList(MusicList musicList) {
        MessageDialog messageDialog = new MessageDialog.Builder(this)
                .setTitle(musicList.getName())
                .setMessage(R.string.message_delete_music_list)
                .setPositiveButtonClickListener((dialog, which) -> mViewModel.deleteMusicList(musicList))
                .build();

        messageDialog.show(getSupportFragmentManager(), "deleteMusicList");
    }
}