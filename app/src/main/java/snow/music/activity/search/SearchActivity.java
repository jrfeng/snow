package snow.music.activity.search;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.databinding.DataBindingUtil;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.common.base.Preconditions;

import java.util.Objects;

import snow.music.R;
import snow.music.activity.BaseActivity;
import snow.music.databinding.ActivitySearchBinding;
import snow.music.service.AppPlayerService;
import snow.player.PlayerClient;

public class SearchActivity extends BaseActivity {
    private static final String KEY_SEARCH_TYPE = "SEARCH_TYPE";
    private static final String KEY_TYPE_NAME = "TYPE_NAME";

    private ActivitySearchBinding mBinding;
    private SearchViewModel mSearchViewModel;
    private PlayerClient mPlayerClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        overridePendingTransition(R.anim.activity_fade_in, R.anim.activity_no_transition);
        mBinding = DataBindingUtil.setContentView(this, R.layout.activity_search);

        initViewModel();

        mBinding.setSearchViewModel(mSearchViewModel);
        mBinding.setLifecycleOwner(this);

        initPlayerClient();
        addViewListener();
        showSoftInput();
        initRecyclerView();
    }

    public static void start(@NonNull Context context, @NonNull Type type, @NonNull String typeName) {
        Preconditions.checkNotNull(context);
        Preconditions.checkNotNull(type);
        Preconditions.checkNotNull(typeName);

        Intent intent = new Intent(context, SearchActivity.class);
        intent.putExtra(KEY_SEARCH_TYPE, type.ordinal());
        intent.putExtra(KEY_TYPE_NAME, typeName);

        context.startActivity(intent);
    }

    private Type getSearchType() {
        Intent intent = getIntent();
        int typeOrdinal = intent.getIntExtra(KEY_SEARCH_TYPE, -1);
        if (typeOrdinal == -1) {
            throw new IllegalArgumentException("Illegal search type");
        }

        return Type.values()[typeOrdinal];
    }

    private String getTypeName() {
        Intent intent = getIntent();
        String typeName = intent.getStringExtra(KEY_TYPE_NAME);
        if (typeName == null) {
            throw new IllegalArgumentException("typeName is null");
        }
        return typeName;
    }

    private void initViewModel() {
        ViewModelProvider provider = new ViewModelProvider(this);

        mSearchViewModel = provider.get(SearchViewModel.class);
        mSearchViewModel.init(getSearchType(), getTypeName());
    }

    private void initPlayerClient() {
        mPlayerClient = PlayerClient.newInstance(this, AppPlayerService.class);
        mPlayerClient.setAutoConnect(true);
        mPlayerClient.connect();
    }

    private void showSoftInput() {
        mBinding.etInput.requestFocus();
        InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
        imm.showSoftInput(mBinding.etInput, InputMethodManager.SHOW_IMPLICIT);
    }

    private void initRecyclerView() {
        RecyclerView recyclerView = findViewById(R.id.rvSearchResult);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        SearchResultAdapter adapter = new SearchResultAdapter(Objects.requireNonNull(mSearchViewModel.getSearchResult().getValue()));
        recyclerView.setAdapter(adapter);

        mSearchViewModel.getSearchResult()
                .observe(this, adapter::setSearchResult);

        mSearchViewModel.getEmptyMessage()
                .observe(this, adapter::setEmptyMessage);

        mSearchViewModel.getInput()
                .observe(this, input -> mSearchViewModel.search());

        adapter.setOnItemClickListener((position, viewId, view, holder) -> {
            mPlayerClient.setPlaylist(mSearchViewModel.resultAsPlaylist(position), position, true);
            Toast.makeText(getApplicationContext(), R.string.toast_start_playing, Toast.LENGTH_SHORT).show();
        });
    }

    private void addViewListener() {
        mBinding.etInput.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                mSearchViewModel.search();
                return true;
            }

            return false;
        });
    }

    @Override
    public void finish() {
        super.finish();
        overridePendingTransition(R.anim.activity_no_transition, R.anim.activity_fade_out);
    }

    public void finishSelf(View view) {
        finish();
    }

    public enum Type {
        MUSIC_LIST,
        ARTIST,
        ALBUM
    }
}