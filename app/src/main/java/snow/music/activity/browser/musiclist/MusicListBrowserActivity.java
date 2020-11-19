package snow.music.activity.browser.musiclist;

import android.os.Bundle;
import android.view.View;

import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import snow.music.R;
import snow.music.activity.ListActivity;
import snow.music.store.MusicList;

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
        // TODO
    }

    private void showOptionMenu(int position) {
        // TODO
    }
}