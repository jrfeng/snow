package snow.music.dialog;

import android.annotation.SuppressLint;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatDialog;
import androidx.fragment.app.FragmentManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.common.base.Preconditions;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import io.reactivex.Single;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import pinyin.util.PinyinComparator;
import recyclerview.helper.ItemClickHelper;
import recyclerview.helper.SelectableHelper;
import snow.music.R;
import snow.music.store.Music;
import snow.music.store.MusicList;
import snow.music.store.MusicStore;
import snow.music.util.InputValidator;

public class AddToMusicListDialog extends BottomDialog {
    private Music mTargetMusic;
    private List<String> mAllContainsMusicListName;
    private Disposable mLoadNameDisposable;

    public static AddToMusicListDialog newInstance(@NonNull Music targetMusic) {
        Preconditions.checkNotNull(targetMusic);

        AddToMusicListDialog dialog = new AddToMusicListDialog();
        dialog.setTargetMusic(targetMusic);

        return dialog;
    }

    private void setTargetMusic(Music targetMusic) {
        mTargetMusic = targetMusic;
    }

    @Override
    protected void onInitDialog(AppCompatDialog dialog) {
        dialog.setContentView(R.layout.dialog_add_to_music_list);

        RecyclerView rvItems = dialog.findViewById(R.id.rvItems);
        Button btnSubmit = dialog.findViewById(R.id.btnSubmit);

        assert rvItems != null;
        assert btnSubmit != null;

        rvItems.setLayoutManager(new LinearLayoutManager(getContext()));

        List<String> allMusicListName = new ArrayList<>(MusicStore.getInstance().getAllCustomMusicListName());
        Collections.sort(allMusicListName, new PinyinComparator());

        AllMusicListAdapter adapter = new AllMusicListAdapter(allMusicListName, mAllContainsMusicListName);
        rvItems.setAdapter(adapter);

        adapter.setOnSelectCountChangeListener(selectedCount -> btnSubmit.setEnabled(selectedCount > 0));
        adapter.setOnCreateClickListener(v -> showCreateMusicListDialog());

        btnSubmit.setOnClickListener(view -> {
            dismiss();
            Toast.makeText(getContext(), R.string.toast_added_successfully, Toast.LENGTH_SHORT).show();

            Single.create(emitter ->
                    MusicStore.getInstance().addToAllMusicList(mTargetMusic, adapter.getAllSelectedMusicList())
            ).subscribeOn(Schedulers.io())
                    .subscribe();
        });
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mLoadNameDisposable != null && !mLoadNameDisposable.isDisposed()) {
            mLoadNameDisposable.dispose();
        }
    }

    @Override
    protected boolean keepOnRestarted() {
        return false;
    }

    @Override
    public void show(@NonNull FragmentManager manager, @Nullable String tag) {
        mLoadNameDisposable = loadAllContainsMusicListName()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(names -> {
                    mAllContainsMusicListName = names;
                    super.show(manager, tag);
                });
    }

    private Single<List<String>> loadAllContainsMusicListName() {
        return Single.create(emitter -> {
            List<String> names = MusicStore.getInstance().getAllCustomMusicListName(mTargetMusic);
            if (emitter.isDisposed()) {
                return;
            }
            emitter.onSuccess(names);
        });
    }

    private void showCreateMusicListDialog() {
        Context context = Objects.requireNonNull(getContext()).getApplicationContext();

        InputDialog dialog = new InputDialog.Builder(Objects.requireNonNull(getContext()))
                .setTitle(R.string.title_create_music_list)
                .setHint(R.string.hint_music_list_title)
                .setOnInputConfirmListener(new InputValidator(context), input -> {
                    assert input != null;
                    createMusicList(context, input);
                })
                .build();

        FragmentManager fm = getParentFragmentManager();
        dismiss();
        dialog.show(fm, "createMusicList");
    }

    @SuppressLint("CheckResult")
    private void createMusicList(Context context, String name) {
        Single.create(emitter -> {
            MusicList musicList = MusicStore.getInstance().createCustomMusicList(name);
            musicList.getMusicElements().add(mTargetMusic);
            MusicStore.getInstance().updateMusicList(musicList);

            emitter.onSuccess(true);
        }).subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(result ->
                        Toast.makeText(context, R.string.toast_added_successfully, Toast.LENGTH_SHORT).show()
                );
    }

    private static class AllMusicListAdapter extends RecyclerView.Adapter<AllMusicListAdapter.ViewHolder> {
        private static final int TYPE_EMPTY = 1;
        private static final int TYPE_ITEM = 2;

        private final List<String> mAllMusicListName;
        private final List<String> mAllContainsMusicListName;
        private final SelectableHelper mSelectableHelper;
        private final ItemClickHelper mItemClickHelper;

        private View.OnClickListener mOnClickListener;

        AllMusicListAdapter(@NonNull List<String> allMusicListName, List<String> allContainsMusicListName) {
            Preconditions.checkNotNull(allMusicListName);

            mAllMusicListName = new ArrayList<>(allMusicListName);
            mAllContainsMusicListName = new ArrayList<>(allContainsMusicListName);

            mSelectableHelper = new SelectableHelper(this);
            mItemClickHelper = new ItemClickHelper();

            mSelectableHelper.setSelectMode(SelectableHelper.SelectMode.MULTIPLE);
            mItemClickHelper.setOnItemClickListener((position, viewId, view, holder) -> {
                if (ignore(position)) {
                    return;
                }

                mSelectableHelper.setSelect(position, !mSelectableHelper.isSelected(position));
            });
        }

        private boolean ignore(int position) {
            return mAllContainsMusicListName.contains(mAllMusicListName.get(position));
        }

        @Override
        public void onAttachedToRecyclerView(@NonNull RecyclerView recyclerView) {
            super.onAttachedToRecyclerView(recyclerView);
            mSelectableHelper.attachToRecyclerView(recyclerView);
            mItemClickHelper.attachToRecyclerView(recyclerView);
        }

        @Override
        public void onDetachedFromRecyclerView(@NonNull RecyclerView recyclerView) {
            super.onDetachedFromRecyclerView(recyclerView);
            mSelectableHelper.detach();
            mItemClickHelper.detach();
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            int layoutId = R.layout.item_add_to_music_list;
            boolean empty = (viewType == TYPE_EMPTY);

            if (empty) {
                layoutId = R.layout.empty_add_to_music_list;
            }

            View itemView = LayoutInflater.from(parent.getContext())
                    .inflate(layoutId, parent, false);

            return new ViewHolder(itemView, empty);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            if (holder.empty) {
                holder.btnCreate.setOnClickListener(mOnClickListener);
                return;
            }

            holder.tvItemTitle.setText(mAllMusicListName.get(position));

            if (ignore(position)) {
                holder.checkBox.setChecked(true);
                holder.checkBox.setEnabled(false);
                return;
            }

            mSelectableHelper.updateSelectState(holder, position);
            mItemClickHelper.bindClickListener(holder.itemView);

            holder.checkBox.setOnCheckedChangeListener((buttonView, isChecked) ->
                    mSelectableHelper.setSelect(position, isChecked)
            );
        }

        @Override
        public int getItemCount() {
            if (mAllMusicListName.isEmpty()) {
                return 1;
            }

            return mAllMusicListName.size();
        }

        @Override
        public int getItemViewType(int position) {
            if (mAllMusicListName.isEmpty()) {
                return TYPE_EMPTY;
            }

            return TYPE_ITEM;
        }

        public List<String> getAllSelectedMusicList() {
            List<String> allSelectedMusicList = new ArrayList<>();

            for (Integer i : mSelectableHelper.getSelectedPositions()) {
                allSelectedMusicList.add(mAllMusicListName.get(i));
            }

            return allSelectedMusicList;
        }

        public void setOnCreateClickListener(@Nullable View.OnClickListener listener) {
            mOnClickListener = listener;
        }

        public void setOnSelectCountChangeListener(SelectableHelper.OnSelectCountChangeListener listener) {
            mSelectableHelper.setOnSelectCountChangeListener(listener);
        }

        private static class ViewHolder extends RecyclerView.ViewHolder
                implements SelectableHelper.Selectable {
            boolean empty;

            Button btnCreate;

            TextView tvItemTitle;
            CheckBox checkBox;

            public ViewHolder(@NonNull View itemView, boolean empty) {
                super(itemView);

                this.empty = empty;
                if (empty) {
                    btnCreate = itemView.findViewById(R.id.btnCreate);
                    return;
                }

                tvItemTitle = itemView.findViewById(R.id.tvItemTitle);
                checkBox = itemView.findViewById(R.id.checkbox);
            }

            @Override
            public void onSelected() {
                if (empty) {
                    return;
                }

                checkBox.setChecked(true);
            }

            @Override
            public void onUnselected() {
                if (empty) {
                    return;
                }

                checkBox.setChecked(false);
            }
        }
    }
}
