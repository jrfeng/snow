package snow.music.activity.browser.album;

import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.os.Bundle;
import android.view.View;

import java.util.List;

import snow.music.R;
import snow.music.activity.ListActivity;
import snow.music.service.AppPlayerService;
import snow.music.util.PlayerUtil;
import snow.player.lifecycle.PlayerViewModel;

public class AlbumBrowserActivity extends ListActivity {
    private RecyclerView rvAlbumBrowser;
    private AlbumBrowserViewModel mViewModel;
    private PlayerViewModel mPlayerViewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_album_browser);

        ViewModelProvider provider = new ViewModelProvider(this);
        mViewModel = provider.get(AlbumBrowserViewModel.class);
        mPlayerViewModel = provider.get(PlayerViewModel.class);

        PlayerUtil.initPlayerViewModel(this, mPlayerViewModel, AppPlayerService.class);

        rvAlbumBrowser = findViewById(R.id.rvAlbumBrowser);
        initRecyclerView();
    }

    private void initRecyclerView() {
        rvAlbumBrowser.setLayoutManager(new LinearLayoutManager(this));

        List<String> allAlbum = mViewModel.getAllAlbum().getValue();
        assert allAlbum != null;

        AlbumBrowserAdapter adapter = new AlbumBrowserAdapter(allAlbum);
        rvAlbumBrowser.setAdapter(adapter);

        mViewModel.getAllAlbum()
                .observe(this, adapter::setAllAlbum);

        mPlayerViewModel.getPlayingMusicItem()
                .observe(this, musicItem -> {
                    if (musicItem == null) {
                        adapter.clearMark();
                        return;
                    }

                    List<String> albumList = mViewModel.getAllAlbum().getValue();
                    adapter.setMarkPosition(albumList.indexOf(musicItem.getAlbum()));
                });

        adapter.setOnItemClickListener((position, viewId, view, holder) ->
                navigateToAlbumDetail(mViewModel.getAlbum(position))
        );
    }

    public void finishSelf(View view) {
        finish();
    }

    public void navigateToAlbumDetail(String albumName) {
        // TODO
    }
}