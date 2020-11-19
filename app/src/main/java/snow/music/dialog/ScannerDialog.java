package snow.music.dialog;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Application;
import android.content.Context;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatDialog;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModelProvider;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

import io.reactivex.Observable;
import io.reactivex.Single;
import io.reactivex.SingleOnSubscribe;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import media.helper.MediaStoreHelper;
import snow.music.R;
import snow.music.service.AppPlayerService;
import snow.music.store.Music;
import snow.music.store.MusicList;
import snow.music.store.MusicStore;
import snow.music.util.MusicListUtil;
import snow.music.util.PlayerUtil;
import snow.player.PlayerClient;
import snow.player.lifecycle.PlayerViewModel;

public class ScannerDialog extends BottomDialog {
    private static final String KEY_UPDATE_PLAYLIST = "UPDATE_PLAYLIST";
    private static final int PERMISSION_REQUEST_CODE = 1;

    private static final int MIN_DURATION = 30_000;   // 单位：毫秒

    private ScannerViewModel mScannerViewModel;

    private ProgressBar mProgressBar;
    private TextView mTextProgress;

    private boolean mKeepOnRestarted = true;
    @Nullable
    private Disposable mDelayDismissDisposable;

    /**
     * 创建一个 {@link ScannerDialog} 对象。
     *
     * @param updatePlaylist 是否更新播放列表，如果为 true，则会在扫描完成后，使用扫描到的音乐设置一个新的播放列表。
     * @return {@link ScannerDialog} 对象
     */
    public static ScannerDialog newInstance(boolean updatePlaylist) {
        ScannerDialog dialog = new ScannerDialog();

        Bundle args = new Bundle();
        args.putBoolean(KEY_UPDATE_PLAYLIST, updatePlaylist);

        dialog.setArguments(args);
        return dialog;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ViewModelProvider viewModelProvider = new ViewModelProvider(this);
        mScannerViewModel = viewModelProvider.get(ScannerViewModel.class);
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        if (!mScannerViewModel.isStarted()) {
            startScanner();
        }
    }

    @Override
    protected boolean keepOnRestarted() {
        return mKeepOnRestarted;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mDelayDismissDisposable != null) {
            mDelayDismissDisposable.dispose();
        }
    }

    @Override
    protected void onInitDialog(AppCompatDialog dialog) {
        dialog.setContentView(R.layout.dialog_scanner);
        dialog.setCanceledOnTouchOutside(false);

        mProgressBar = dialog.findViewById(R.id.progressBar);
        mTextProgress = dialog.findViewById(R.id.tvProgress);

        assert mProgressBar != null;
        assert mTextProgress != null;

        observerProgress();
        observeFinished();

        updateProgress();
    }

    private void showScanResultDialog(FragmentActivity activity, List<Music> musicList) {
        MessageDialog.Builder builder = new MessageDialog.Builder(activity);

        switch (musicList.size()) {
            case 0:
                builder.setMessage(R.string.message_no_new_songs_found)
                        .setHideNegativeButton(true);
                break;
            case 1:
                builder.setMessage(R.string.message_one_song_found);
                break;
            default:
                String message = activity.getString(R.string.message_many_songs_found);
                builder.setMessage(message.replaceFirst("n", String.valueOf(musicList.size())));
                break;
        }

        MessageDialog messageDialog = builder.setPositiveButtonClickListener(
                (dialog, which) -> addToLocalMusicList(activity, musicList, updatePlaylist()))
                .build();

        messageDialog.show(getParentFragmentManager(), "scanComplete");
    }

    @SuppressLint("CheckResult")
    private void addToLocalMusicList(FragmentActivity activity, List<Music> musicList, boolean updatePlaylist) {
        if (musicList.size() < 1) {
            return;
        }

        Single.create((SingleOnSubscribe<Boolean>) emitter -> {
            MusicStore musicStore = MusicStore.getInstance();
            MusicList localMusic = musicStore.getLocalMusicList();

            localMusic.getMusicElements().addAll(musicList);
            musicStore.updateMusicList(localMusic);

            emitter.onSuccess(true);
        }).subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(aBoolean -> {
                    MusicStore.getInstance().notifyScanComplete();
                    if (updatePlaylist) {
                        getPlayerClient(activity).setPlaylist(
                                MusicListUtil.asPlaylist(MusicStore.MUSIC_LIST_LOCAL_MUSIC, musicList, 0));
                    }
                });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode != PERMISSION_REQUEST_CODE) {
            return;
        }

        if (grantResults.length <= 0 || grantResults[0] == PackageManager.PERMISSION_DENIED) {
            delayDismiss();
            Toast.makeText(getContext(), R.string.toast_no_read_storage_permission, Toast.LENGTH_SHORT).show();
            return;
        }

        startScanner();
    }

    private PlayerClient getPlayerClient(FragmentActivity activity) {
        ViewModelProvider parentProvider = new ViewModelProvider(activity);

        PlayerViewModel playerViewModel = parentProvider.get(PlayerViewModel.class);
        PlayerUtil.initPlayerViewModel(activity, playerViewModel, AppPlayerService.class);

        return playerViewModel.getPlayerClient();
    }

    private void observerProgress() {
        mScannerViewModel.getScanPercent()
                .observe(this, percent -> updateProgress());
    }

    private void observeFinished() {
        mScannerViewModel.getFinished()
                .observe(this, finished -> {
                    if (finished) {
                        delayDismiss();
                    }
                });
    }

    private void delayDismiss() {
        mKeepOnRestarted = false;
        mDelayDismissDisposable = Observable.timer(800, TimeUnit.MILLISECONDS)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(aLong -> {
                    FragmentActivity activity = Objects.requireNonNull(getActivity());
                    dismiss();
                    if (mScannerViewModel.isStarted()) {
                        showScanResultDialog(activity, mScannerViewModel.getScannedMusic());
                    }
                });
    }

    @SuppressLint("SetTextI18n")
    private void updateProgress() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            mProgressBar.setProgress(mScannerViewModel.getProgress(), true);
        } else {
            mProgressBar.setProgress(mScannerViewModel.getProgress());
        }

        mTextProgress.setText(mScannerViewModel.getProgress() + "%");
    }

    private void startScanner() {
        if (havePermission()) {
            setCancelable(false);
            mScannerViewModel.start(MIN_DURATION);
            return;
        }

        requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, PERMISSION_REQUEST_CODE);
    }

    private boolean havePermission() {
        int result = ContextCompat.checkSelfPermission(Objects.requireNonNull(getContext()), Manifest.permission.READ_EXTERNAL_STORAGE);
        return result == PackageManager.PERMISSION_GRANTED;
    }

    private boolean updatePlaylist() {
        Bundle args = getArguments();
        if (args == null) {
            return false;
        }
        return args.getBoolean(KEY_UPDATE_PLAYLIST, false);
    }

    public static class ScannerViewModel extends AndroidViewModel {
        private boolean mStarted;
        private boolean mCancelled;
        private int mProgress;
        private MediaStoreHelper.Scanner<Music> mMusicScanner;

        public MutableLiveData<Boolean> mFinished;
        public MutableLiveData<Integer> mScanPercent;

        private List<Music> mScannedMusic;

        public ScannerViewModel(@NonNull Application application) {
            super(application);

            mStarted = false;
            mCancelled = false;
            mFinished = new MutableLiveData<>(false);
            mScanPercent = new MutableLiveData<>(0);
            mScannedMusic = Collections.emptyList();
        }

        @Override
        protected void onCleared() {
            super.onCleared();
            cancel();
        }

        public void start(int minDuration) {
            if (mStarted || mCancelled) {
                return;
            }

            mStarted = true;

            mMusicScanner = MediaStoreHelper.scanAudio(getApplication().getContentResolver(), new MusicDecoder(getApplication()))
                    .updateThreshold(1000)
                    .selection(MediaStore.MediaColumns.DURATION + ">=?")
                    .selectionArgs(new String[]{String.valueOf(minDuration)});

            mMusicScanner.scan(new MediaStoreHelper.OnScanCallback<Music>() {
                @Override
                public void onStartScan() {
                    mScanPercent.setValue(0);
                }

                @Override
                public void onUpdateProgress(int progress, int max, Music item) {
                    mProgress = progress;
                    mScanPercent.setValue(progress);
                }

                @Override
                public void onFinished(List<Music> items) {
                    if (mCancelled) {
                        return;
                    }

                    mProgress = 100;
                    mScanPercent.setValue(100);
                    mFinished.setValue(true);

                    updateLocalMusicList(items);
                }
            });
        }

        public void cancel() {
            mCancelled = true;
            mFinished.setValue(true);
            if (mMusicScanner != null) {
                mMusicScanner.cancel();
            }
        }

        public LiveData<Integer> getScanPercent() {
            return mScanPercent;
        }

        public LiveData<Boolean> getFinished() {
            return mFinished;
        }

        public boolean isStarted() {
            return mStarted;
        }

        public int getProgress() {
            return mProgress;
        }

        public List<Music> getScannedMusic() {
            return mScannedMusic;
        }

        @SuppressLint("CheckResult")
        private void updateLocalMusicList(List<Music> items) {
            Single.create((SingleOnSubscribe<List<Music>>) emitter -> {
                MusicStore.getInstance().putAllMusic(getAllNewMusic(items));
                emitter.onSuccess(getAllNoLocalMusic(items));
            }).subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(musicList -> mScannedMusic = musicList);
        }

        private List<Music> getAllNewMusic(List<Music> items) {
            MusicStore musicStore = MusicStore.getInstance();
            List<Music> allNewMusic = new ArrayList<>();
            for (Music music : items) {
                long id = musicStore.getId(music.getUri());
                music.id = id;
                if (id <= 0) {
                    allNewMusic.add(music);
                }
            }
            return allNewMusic;
        }

        private List<Music> getAllNoLocalMusic(List<Music> items) {
            MusicStore musicStore = MusicStore.getInstance();
            List<Music> allNoLocalMusic = new ArrayList<>();

            for (Music music : items) {
                if (!musicStore.isLocalMusic(music.getUri())) {
                    allNoLocalMusic.add(music);
                }
            }

            return allNoLocalMusic;
        }
    }

    private static class MusicDecoder extends MediaStoreHelper.Decoder<Music> {
        private Context mContext;

        MusicDecoder(Context context) {
            mContext = context;
        }

        @Override
        public Music decode(Cursor cursor) {
            return new Music(0,
                    getTitle(cursor),
                    optimizeText(getAudioArtist(cursor), snow.player.R.string.snow_music_item_unknown_artist),
                    optimizeText(getAudioAlbum(cursor), snow.player.R.string.snow_music_item_unknown_album),
                    getAudioUri(cursor).toString(),
                    "",
                    getDuration(cursor),
                    getDateModified(cursor));
        }

        public String optimizeText(String text, int stringId) {
            if ("<unknown>".equals(text)) {
                return mContext.getString(stringId);
            }

            return text;
        }
    }
}
