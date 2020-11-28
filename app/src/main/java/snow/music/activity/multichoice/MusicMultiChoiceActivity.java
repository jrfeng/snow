package snow.music.activity.multichoice;

import android.graphics.Color;
import android.os.Bundle;
import android.os.Parcelable;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import snow.music.R;
import snow.music.activity.BaseActivity;
import snow.music.dialog.AddToMusicListDialog;
import snow.music.dialog.MessageDialog;
import snow.music.store.Music;

public class MusicMultiChoiceActivity extends BaseActivity {
    public static final int RESULT_CODE_NONE = 1;
    public static final int RESULT_CODE_MODIFIED = 2;

    private MultiChoiceStateHolder mStateHolder;
    private MusicMultiChoiceViewModel mViewModel;

    private TextView tvTitle;
    private TextView tvSelect;
    private RecyclerView rvMultiChoice;
    private MusicMultiChoiceAdapter mMultiChoiceAdapter;
    private View addToFavorite;
    private View removeSelected;

    private LinearLayoutManager mLayoutManager;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        overridePendingTransition(0, 0);
        setContentView(R.layout.activity_music_multi_choice);

        mStateHolder = MultiChoiceStateHolder.getInstance();
        initViewModel();

        findViews();
        initViews();
        initRecyclerView();
        updateTitle();
        updateSelectText();
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        mViewModel.setAllSelectedPosition(mMultiChoiceAdapter.getAllSelectedPosition());
    }

    private void initViewModel() {
        ViewModelProvider provider = new ViewModelProvider(this);
        mViewModel = provider.get(MusicMultiChoiceViewModel.class);

        if (mViewModel.isInitialized()) {
            return;
        }

        mViewModel.init(mStateHolder.getMusicList(), mStateHolder.getPosition());
    }

    private void findViews() {
        tvTitle = findViewById(R.id.tvTitle);
        tvSelect = findViewById(R.id.tvSelect);
        rvMultiChoice = findViewById(R.id.rvMultiChoice);
        addToFavorite = findViewById(R.id.addToFavorite);
        removeSelected = findViewById(R.id.removeSelected);
    }

    private void initViews() {
        if (mStateHolder.isFavorite()) {
            addToFavorite.setVisibility(View.GONE);
        }

        if (!mStateHolder.isItemRemovable()) {
            removeSelected.setVisibility(View.GONE);
        }

        tvSelect.setOnClickListener(v -> {
            if (isAllItemSelected()) {
                mMultiChoiceAdapter.clearSelect();
            } else {
                mMultiChoiceAdapter.selectAll();
            }
            updateSelectText();
        });
    }

    private void initRecyclerView() {
        mLayoutManager = new LinearLayoutManager(this);

        Parcelable layoutManagerState = mStateHolder.consumeLayoutManagerState();
        if (layoutManagerState != null) {
            mLayoutManager.onRestoreInstanceState(layoutManagerState);
        }

        rvMultiChoice.setLayoutManager(mLayoutManager);

        mMultiChoiceAdapter = new MusicMultiChoiceAdapter(mStateHolder.getMusicList(),
                mViewModel.getAllSelectedPosition());
        rvMultiChoice.setAdapter(mMultiChoiceAdapter);

        mMultiChoiceAdapter.setOnSelectCountChangeListener(selectedCount -> {
            updateTitle();
            updateSelectText();
        });
    }

    private boolean isAllItemSelected() {
        return mMultiChoiceAdapter.getSelectedCount() == getMusicListSize();
    }

    private int getMusicListSize() {
        return mStateHolder.getMusicListSize();
    }

    private boolean musicListIsEmpty() {
        return getMusicListSize() < 1;
    }

    private boolean noItemSelected() {
        return mMultiChoiceAdapter.getSelectedCount() < 1;
    }

    private void updateTitle() {
        int count = mMultiChoiceAdapter.getSelectedCount();

        if (count < 1) {
            tvTitle.setText(R.string.text_multi_select_zero_items_selected);
        } else if (count == 1) {
            tvTitle.setText(R.string.text_multi_select_one_item_selected);
        } else {
            String title = getString(R.string.text_multi_select_more_items_selected);
            tvTitle.setText(title.replaceFirst("n", String.valueOf(count)));
        }
    }

    private void updateSelectText() {
        if (isAllItemSelected()) {
            tvSelect.setText(R.string.text_multi_select_deselect_all);
        } else {
            tvSelect.setText(R.string.text_multi_select_select_all);
        }
    }

    @Override
    public void finish() {
        super.finish();
        mStateHolder.setLayoutManagerState(mLayoutManager.onSaveInstanceState());
        overridePendingTransition(0, 0);
    }

    public void finishSelf(View view) {
        setResult(RESULT_CODE_NONE);
        finish();
    }

    public void addToFavorite(View view) {
        if (musicListIsEmpty() || noItemSelected()) {
            return;
        }

        Toast.makeText(this, R.string.toast_added_successfully, Toast.LENGTH_SHORT).show();
        mViewModel.addToFavorite(mMultiChoiceAdapter.getAllSelectedMusic());
        mMultiChoiceAdapter.clearSelect();

        setResult(RESULT_CODE_NONE);
        finish();
    }

    public void addToMusicList(View view) {
        if (musicListIsEmpty() || noItemSelected()) {
            return;
        }

        AddToMusicListDialog dialog = AddToMusicListDialog.newInstance(mMultiChoiceAdapter.getAllSelectedMusic(),
                mStateHolder.getMusicListName());

        dialog.setOnFinishedListener(this::finish);

        dialog.show(getSupportFragmentManager(), "addToMusicList");
    }

    public void remove(View view) {
        if (musicListIsEmpty() || noItemSelected()) {
            return;
        }

        MessageDialog dialog = new MessageDialog.Builder(this)
                .setMessage(R.string.message_remove_all_selected_songs)
                .setPositiveTextColor(Color.RED)
                .setPositiveButtonClickListener((dialog1, which) -> {
                    setResult(RESULT_CODE_MODIFIED);

                    List<Music> selectedMusic = mMultiChoiceAdapter.getAllSelectedMusic();
                    mStateHolder.remove(selectedMusic);
                    mViewModel.remove(mStateHolder.getMusicListName(), selectedMusic);
                    mMultiChoiceAdapter.setMusicList(mStateHolder.getMusicList());
                    mMultiChoiceAdapter.clearSelect();
                    updateTitle();
                    updateSelectText();

                    Toast.makeText(getApplicationContext(), R.string.toast_removed, Toast.LENGTH_SHORT).show();

                    if (mStateHolder.getMusicListSize() < 1) {
                        finish();
                    }
                })
                .build();

        dialog.show(getSupportFragmentManager(), "removeSelectedSongs");
    }
}
