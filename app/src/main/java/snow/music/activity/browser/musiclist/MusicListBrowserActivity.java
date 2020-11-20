package snow.music.activity.browser.musiclist;

import android.content.Context;
import android.os.Bundle;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import snow.music.R;
import snow.music.activity.ListActivity;
import snow.music.dialog.InputDialog;
import snow.music.store.MusicList;
import snow.music.store.MusicStore;

public class MusicListBrowserActivity extends ListActivity {
    private MusicListBrowserViewModel mViewModel;
    private RecyclerView rvMusicListBrowser;

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

        MusicListBrowserAdapter adapter = new MusicListBrowserAdapter(allMusicList);
        rvMusicListBrowser.setAdapter(adapter);

        adapter.setOnItemClickListener((position, which) -> {
            switch (which) {
                case MusicListBrowserAdapter.OnItemClickListener.ITEM_VIEW:
                    mViewModel.navigateToMusicList(this, position);
                    break;
                case MusicListBrowserAdapter.OnItemClickListener.OPTION_MENU:
                    showOptionMenu(position);
                    break;
            }
        });

        mViewModel.getAllMusicList().observe(this, adapter::setMusicLists);
    }

    public void finishSelf(View view) {
        finish();
    }

    public void showCreateMusicListDialog(View view) {
        InputDialog inputDialog = new InputDialog.Builder(this)
                .setTitle(R.string.title_create_music_list)
                .setHint(R.string.hint_music_list_title)
                .setOnInputConfirmListener(new InputValidator(this), input -> {
                    if (input == null) {
                        return;
                    }

                    mViewModel.createMusicList(input);
                })
                .build();

        inputDialog.show(getSupportFragmentManager(), "createMusicList");
    }

    private void showOptionMenu(int position) {
        // TODO
    }

    private static class InputValidator implements InputDialog.Validator {
        private Context mContext;
        private String mInvalidateHint;

        InputValidator(Context context) {
            mContext = context;
        }

        @Override
        public boolean isValid(@Nullable String input) {
            if (input == null || input.isEmpty()) {
                mInvalidateHint = mContext.getString(R.string.hint_please_input_music_list_title);
                return false;
            }

            if (MusicStore.getInstance().isNameExists(input)) {
                mInvalidateHint = mContext.getString(R.string.hint_music_list_name_exists);
                return false;
            }

            return true;
        }

        @NonNull
        @Override
        public String getInvalidateHint() {
            return mInvalidateHint;
        }
    }
}