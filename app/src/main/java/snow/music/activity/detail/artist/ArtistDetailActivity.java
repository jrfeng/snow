package snow.music.activity.detail.artist;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import com.google.common.base.Preconditions;

import snow.music.R;
import snow.music.activity.detail.DetailActivity;
import snow.music.dialog.BottomMenuDialog;

public class ArtistDetailActivity extends DetailActivity {
    private static final String KEY_ARTIST = "ARTIST";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_artist_detail);

        initTitle();
        initDetailFragment();
    }

    @SuppressLint("SetTextI18n")
    private void initTitle() {
        TextView tvTitle = findViewById(R.id.tvTitle);
        String prefix = getString(R.string.title_prefix);
        tvTitle.setText(prefix + getArtist());
    }

    private void initDetailFragment() {
        FragmentManager fm = getSupportFragmentManager();

        Fragment fragment = fm.findFragmentById(R.id.musicListContainer);
        if (!(fragment instanceof ArtistDetailFragment)) {
            fm.beginTransaction()
                    .add(R.id.musicListContainer, ArtistDetailFragment.newInstance(getArtist()))
                    .commit();
        }
    }

    public static void start(@NonNull Context context, @NonNull String artist) {
        Preconditions.checkNotNull(context);
        Preconditions.checkNotNull(artist);

        Intent intent = new Intent(context, ArtistDetailActivity.class);
        intent.putExtra(KEY_ARTIST, artist);

        context.startActivity(intent);
    }

    @Nullable
    private String getArtist() {
        return getIntent().getStringExtra(KEY_ARTIST);
    }

    public void finishSelf(View view) {
        finish();
    }

    public void searchMusic(View view) {
        // TODO
    }
}