package snow.music.activity.detail.musiclist;

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
import snow.music.fragment.musiclist.MusicListFragment;

public class MusicListDetailActivity extends DetailActivity {
    private static final String KEY_MUSIC_LIST_NAME = "MUSIC_LIST_NAME";
    private MusicListFragment mMusicListFragment;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_music_list_detail);

        initTitle();

        FragmentManager fm = getSupportFragmentManager();

        Fragment fragment = fm.findFragmentById(R.id.musicListContainer);
        if (fragment instanceof MusicListFragment) {
            mMusicListFragment = (MusicListFragment) fragment;
        } else {
            mMusicListFragment = MusicListFragment.newInstance(getMusicListName());
            fm.beginTransaction()
                    .add(R.id.musicListContainer, mMusicListFragment)
                    .commit();
        }
    }

    private void initTitle() {
        TextView tvTitle = findViewById(R.id.tvTitle);
        tvTitle.setText(getMusicListName());
    }

    public static void start(@NonNull Context context, @NonNull String musicListName) {
        Preconditions.checkNotNull(context);
        Preconditions.checkNotNull(musicListName);

        Intent intent = new Intent(context, MusicListDetailActivity.class);
        intent.putExtra(KEY_MUSIC_LIST_NAME, musicListName);

        context.startActivity(intent);
    }

    @Nullable
    private String getMusicListName() {
        return getIntent().getStringExtra(KEY_MUSIC_LIST_NAME);
    }

    public void finishSelf(View view) {
        finish();
    }

    public void onOptionMenuClicked(View view) {
        int id = view.getId();
        if (id == R.id.btnSearch) {
            startSearchActivity();
        } else if (id == R.id.btnSort) {
            mMusicListFragment.showSortDialog();
        }
    }

    public void startSearchActivity() {
        // TODO
    }
}
