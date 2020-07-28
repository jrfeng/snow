package snow.player.lifecycle;

import androidx.annotation.NonNull;

import com.google.common.base.Preconditions;

import java.util.Locale;
import java.util.concurrent.TimeUnit;

import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Consumer;
import io.reactivex.schedulers.Schedulers;

public class ProgressClock {
    private Callback mCallback;

    private int mProgressSec;       // 单位：秒
    private int mDurationSec;       // 单位：秒

    private boolean mLoop;
    private Disposable mDisposable;

    public ProgressClock(@NonNull Callback callback) {
        Preconditions.checkNotNull(callback);
        mCallback = callback;
    }

    public void setLoop(boolean loop) {
        mLoop = loop;
    }

    /**
     * 启动定时器。
     *
     * @param progress   歌曲的播放进度（单位：毫秒）
     * @param updateTime 歌曲播放进度的更新时间（单位：毫秒）
     * @param duration   歌曲的持续时间（单位：毫秒）
     * @throws IllegalArgumentException 在 updateTime 大于当前时间时抛出该异常
     */
    public void start(int progress, long updateTime, int duration) throws IllegalArgumentException {
        long currentTime = System.currentTimeMillis();
        if (updateTime > currentTime) {
            throw new IllegalArgumentException("updateTime is illegal.");
        }

        cancel();

        long realProgress = progress + (currentTime - updateTime);

        mProgressSec = (int) (realProgress / 1000);
        mDurationSec = duration / 1000;

        if (mProgressSec >= mDurationSec) {
            mCallback.onUpdateProgress(mDurationSec, mDurationSec);
            return;
        }

        long delay = 1000 - (realProgress % 1000);

        mDisposable = Observable.interval(delay, 1000, TimeUnit.MILLISECONDS)
                .subscribeOn(Schedulers.computation())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Consumer<Long>() {
                    @Override
                    public void accept(Long aLong) {
                        updateProgress();
                    }
                });
    }

    public void cancel() {
        if (mDisposable != null && !mDisposable.isDisposed()) {
            mDisposable.dispose();
            mDisposable = null;
        }
    }

    private void updateProgress() {
        int newProgress = mProgressSec + 1;

        if (mLoop && newProgress > mDurationSec) {
            updateProgress(0);
            return;
        }

        if (!mLoop && newProgress >= mDurationSec) {
            cancel();
        }

        updateProgress(newProgress);
    }

    private void updateProgress(int progressSec/*单位：秒*/) {
        mProgressSec = progressSec;
        mCallback.onUpdateProgress(mProgressSec, mDurationSec);
    }

    /**
     * 将歌曲的播放进度格式化成一个形如 “00:00” 的字符串，方便在 UI 上显示。
     * <p>
     * 格式化后的字符串的格式为：[时:分:秒]（例如：01:30:45）。如果 “时” 为 0, 则会忽略, 此时的字符串格式
     * 是：[分:秒]（例如：04:35）。最多支持到 99:59:59, 超出时会截断。
     *
     * @param seconds 歌曲的播放进度，单位：秒
     * @return 返回格式化后的字符串
     */
    public static String asText(int seconds) {
        if (seconds <= 0) {
            return "00:00";
        }

        int second = seconds % 60;
        int minute = (seconds / 60) % 60;
        int hour = (seconds / 3600) % 99;

        if (hour <= 0) {
            return String.format(Locale.ENGLISH, "%02d:%02d", minute, second);
        }

        return String.format(Locale.ENGLISH, "%02d:%02d:%02d", hour, minute, second);
    }

    public interface Callback {
        void onUpdateProgress(int progressSec, int durationSec);
    }
}
