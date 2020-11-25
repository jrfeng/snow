package snow.music.activity.detail.album;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import com.google.common.base.Preconditions;

import snow.music.R;
import snow.music.activity.detail.DetailActivity;

public class AlbumDetailActivity extends DetailActivity {
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
        tvTitle.setText(prefix + getAlbum());
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
        intent.putExtra(KEY_ALBUM, album);

        context.startActivity(intent);
    }

    @Nullable
    private String getAlbum() {
        return getIntent().getStringExtra(KEY_ALBUM);
    }

    public void finishSelf(View view) {
        finish();
    }

    public void searchMusic(View view) {
        // TODO
    }
}