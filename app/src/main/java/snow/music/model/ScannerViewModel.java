package snow.music.model;

import android.app.Application;
import android.content.Context;
import android.database.Cursor;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.google.common.base.Preconditions;

import java.util.List;

import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.util.Log;

import media.helper.MediaStoreHelper;
import snow.music.store.Music;

/**
 * Local music scanner.
 */
public class ScannerViewModel extends AndroidViewModel {
    private MutableLiveData<Boolean> mScanningMusic;
    private MutableLiveData<Integer> mScannedProgress;

    private Handler mMainHandler;
    private MediaStoreHelper.Scanner<Music> mMusicScanner;

    public ScannerViewModel(@NonNull Application application) {
        super(application);

        mMainHandler = new Handler(Looper.getMainLooper());

        mScanningMusic = new MutableLiveData<>(false);
        mScannedProgress = new MutableLiveData<>(0);
    }

    @NonNull
    public LiveData<Boolean> getScanningMusic() {
        return mScanningMusic;
    }

    /**
     * 本地音乐扫描器的扫描进度，百分比值，范围为 [0, 100]。
     */
    @NonNull
    public LiveData<Integer> getScannedProgress() {
        return mScannedProgress;
    }

    @Override
    protected void onCleared() {
        super.onCleared();

        if (mMusicScanner != null) {
            mMusicScanner.cancel();
        }
    }

    /**
     * 扫描本地音乐。
     *
     * @param minDuration 本地音乐的最短时长，低于这个时长的本地音乐会被忽略（单位：毫秒）
     * @param listener    扫描结束事件监听器，该监听器会在扫描完成时调用。不能为 null
     */
    public void scan(int minDuration, @NonNull OnScanCompleteListener listener) {
        Preconditions.checkNotNull(listener);

        mMusicScanner = MediaStoreHelper.scanAudio(getApplication().getContentResolver(), new MusicDecoder(getApplication()))
                .updateThreshold(1000)
                .selection(MediaStore.MediaColumns.DURATION + ">=?")
                .selectionArgs(new String[]{String.valueOf(minDuration)});

        mMusicScanner.scan(new MediaStoreHelper.OnScanCallback<Music>() {
            @Override
            public void onStartScan() {
                mMainHandler.post(() -> mScanningMusic.setValue(true));
            }

            @Override
            public void onUpdateProgress(int progress, int max, Music item) {
                mMainHandler.post(() -> mScannedProgress.setValue((int) (1.0 * progress / max * 100.0)));

            }

            @Override
            public void onFinished(List<Music> items) {
                mMainHandler.post(() -> {
                    mScanningMusic.setValue(false);
                    listener.onScanComplete(items);
                });
            }
        });
    }

    /**
     * 用于监听扫描结束事件。
     */
    public interface OnScanCompleteListener {
        /**
         * 该方法会在扫描完成后调用。
         *
         * @param musicList 本次扫描到的歌曲。
         */
        void onScanComplete(@NonNull List<Music> musicList);
    }

    private static class MusicDecoder extends MediaStoreHelper.Decoder<Music> {
        private Context mContext;

        MusicDecoder(Context context) {
            mContext = context;
        }

        @Override
        public Music decode(Cursor cursor) {
            return new Music(0,
                    getMusicTitle(cursor),
                    optimizeText(getAudioArtist(cursor), snow.player.R.string.snow_music_item_unknown_artist),
                    optimizeText(getAudioAlbum(cursor), snow.player.R.string.snow_music_item_unknown_album),
                    getAudioUri(cursor).toString(),
                    "",
                    getDuration(cursor),
                    getDateModified(cursor));
        }

        public String getMusicTitle(Cursor cursor) {
            String title = getTitle(cursor);
            if ("<unknown>".equals(title)) {
                title = getDisplayName(cursor);
                title = title.substring(0, title.lastIndexOf("."));
            }

            return title;
        }

        public String optimizeText(String text, int stringId) {
            if ("<unknown>".equals(text)) {
                return mContext.getString(stringId);
            }

            return text;
        }
    }
}
