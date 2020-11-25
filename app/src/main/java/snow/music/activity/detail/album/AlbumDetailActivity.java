package snow.music.activity.detail.album;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import com.google.common.base.Preconditions;

import snow.music.R;
import snow.music.activity.detail.DetailActivity;
import snow.music.activity.search.SearchActivity;

public class AlbumDetailActivity extends DetailActivity {
    public static final String ALBUM_PREFIX = "album:";
    private static final String KEY_ALBUM = "ALBUM";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_album_detail);

        initTitle();
        initDetailFragment();
    }

    @SuppressLint("SetTextI18n")
    private void initTitle() {
        TextView tvTitle = findViewById(R.id.tvTitle);
        String prefix = getString(R.string.title_album_prefix);
        tvTitle.setText(prefix + getAlbumName());
    }

    private void initDetailFragment() {
        FragmentManager fm = getSupportFragmentManager();

        Fragment fragment = fm.findFragmentById(R.id.musicListContainer);
        if (!(fragment instanceof AlbumDetailFragment)) {
            fm.beginTransaction()
                    .add(R.id.musicListContainer, AlbumDetailFragment.newInstance(getAlbum()))
                    .commit();
        }
    }

    public static void start(@NonNull Context context, @NonNull String album) {
        Preconditions.checkNotNull(context);
        Preconditions.checkNotNull(album);

        Intent intent = new Intent(context, AlbumDetailActivity.class);
        intent.putExtra(KEY_ALBUM, ALBUM_PREFIX + album);   // 加上前缀，避免和自建歌单名重复

        context.startActivity(intent);
    }

    private String getAlbum() {
        String name = getIntent().getStringExtra(KEY_ALBUM);
        if (name == null || name.isEmpty()) {
            return "";
        }

        return name;
    }

    private String getAlbumName() {
        return getAlbum().substring(ALBUM_PREFIX.length());
    }

    public void finishSelf(View view) {
        finish();
    }

    public void searchMusic(View view) {
        SearchActivity.start(this, SearchActivity.Type.ALBUM, getAlbumName());
    }
}