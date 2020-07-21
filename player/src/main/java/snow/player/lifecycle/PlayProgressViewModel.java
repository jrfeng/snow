package snow.player.lifecycle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleObserver;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.OnLifecycleEvent;
import androidx.lifecycle.ViewModel;

import com.google.common.base.Preconditions;

import java.util.Locale;
import java.util.concurrent.TimeUnit;

import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Consumer;
import snow.player.Player;
import snow.player.PlayerClient;
import snow.player.PlayerManager;
import snow.player.media.MusicItem;
import snow.player.playlist.PlaylistPlayer;

/**
 * 一个与播放器播放进度相关的 ViewModel。
 * <p>
 * 注意！创建一个该类的对象后，需要调用 {@link #init(LifecycleOwner, PlayerClient)} 对其进行初始化。
 */
public class PlayProgressViewModel extends ViewModel {
    private PlayerClient mPlayerClient;

    private MutableLiveData<Integer> mDuration;     // 单位：秒
    private MutableLiveData<Integer> mLiveProgress; // 单位：秒

    private MutableLiveData<String> mTextDuration;
    private MutableLiveData<String> mTextLiveProgress;

    private MutableLiveData<Integer> mBufferingPercent;
    private MutableLiveData<Boolean> mStalled;

    private Player.OnPlaybackStateChangeListener mPlaybackStateChangeListener;
    private Player.OnBufferingPercentChangeListener mBufferingPercentChangeListener;
    private Player.OnStalledChangeListener mStalledChangeListener;
    private Player.OnSeekListener mSeekCompleteListener;
    private Player.OnPlayingMusicItemChangeListener mPlayingMusicItemChangeListener;
    private PlaylistPlayer.OnPlayModeChangeListener mPlayModeChangeListener;

    private boolean mLoop;
    private int mDurationSec; // 单位：秒
    private Player.PlaybackState mPlaybackState;
    private Disposable mDisposable;

    public PlayProgressViewModel() {
        mDuration = new MutableLiveData<>(0);
        mLiveProgress = new MutableLiveData<>(0);

        mTextDuration = new MutableLiveData<>(formatSeconds(0));
        mTextLiveProgress = new MutableLiveData<>(formatSeconds(0));

        mBufferingPercent = new MutableLiveData<>(0);
        mStalled = new MutableLiveData<>(false);

        mPlaybackState = Player.PlaybackState.UNKNOWN;
    }

    private void initAllListener() {
        mPlaybackStateChangeListener = new Player.OnPlaybackStateChangeListener() {
            @Override
            public void onPreparing() {
                mPlaybackState = Player.PlaybackState.PREPARING;
                stopTimer();
            }

            @Override
            public void onPrepared(int audioSessionId) {
                mPlaybackState = Player.PlaybackState.PREPARED;
            }

            @Override
            public void onPlay(int playProgress, long playProgressUpdateTime) {
                mPlaybackState = Player.PlaybackState.PLAYING;
                updateProgress(playProgress, playProgressUpdateTime);
                startTimer();
            }

            @Override
            public void onPause() {
                mPlaybackState = Player.PlaybackState.STOPPED;
                stopTimer();
            }

            @Override
            public void onStop() {
                mPlaybackState = Player.PlaybackState.STOPPED;
                stopTimer();
            }

            @Override
            public void onError(int errorCode, String errorMessage) {
                mPlaybackState = Player.PlaybackState.ERROR;
                stopTimer();
            }
        };

        mBufferingPercentChangeListener = new Player.OnBufferingPercentChangeListener() {
            @Override
            public void onBufferingPercentChanged(int percent, long updateTime) {
                mBufferingPercent.setValue(percent);
            }
        };

        mStalledChangeListener = new Player.OnStalledChangeListener() {
            @Override
            public void onStalledChanged(boolean stalled) {
                mStalled.setValue(stalled);

                if (stalled) {
                    stopTimer();
                    return;
                }

                if (mPlaybackState == Player.PlaybackState.PLAYING) {
                    startTimer();
                }
            }
        };

        mSeekCompleteListener = new Player.OnSeekListener() {
            @Override
            public void onSeeking() {
                stopTimer();
            }

            @Override
            public void onSeekComplete(int progress, long updateTime) {
                updateProgress(progress, updateTime);

                if (mPlaybackState == Player.PlaybackState.PLAYING) {
                    startTimer();
                }
            }
        };

        mPlayingMusicItemChangeListener = new Player.OnPlayingMusicItemChangeListener() {
            @Override
            public void onPlayingMusicItemChanged(@Nullable MusicItem musicItem) {
                if (musicItem == null) {
                    stopTimer();
                    updateDuration(0);
                    updateProgress(0, System.currentTimeMillis());
                    return;
                }

                updateDuration(musicItem.getDuration());
            }
        };

        mPlayModeChangeListener = new PlaylistPlayer.OnPlayModeChangeListener() {
            @Override
            public void onPlayModeChanged(PlaylistPlayer.PlayMode playMode) {
                mLoop = playMode == PlaylistPlayer.PlayMode.LOOP;
            }
        };
    }

    /**
     * 初始化当前的 {@link PlayProgressViewModel} 对象。
     *
     * @param owner        一个 {@link LifecycleOwner} 对象，不能为 null。通常是当前 ViewModel 关联到的
     *                     那个 Activity 对象
     * @param playerClient 一个 {@link PlayerClient} 对象，不能为 null
     */
    public void init(@NonNull LifecycleOwner owner, @NonNull final PlayerClient playerClient) {
        Preconditions.checkNotNull(playerClient);

        mPlayerClient = playerClient;

        owner.getLifecycle().addObserver(new LifecycleObserver() {
            @OnLifecycleEvent(Lifecycle.Event.ON_START)
            public void onStart() {
                registerAllListener();
            }

            @OnLifecycleEvent(Lifecycle.Event.ON_STOP)
            public void onStop() {
                unregisterAllListener();
            }
        });
    }

    public LiveData<Integer> getDuration() {
        return mDuration;
    }

    public LiveData<Integer> getLiveProgress() {
        return mLiveProgress;
    }

    public LiveData<String> getTextDuration() {
        return mTextDuration;
    }

    public LiveData<String> getTextLiveProgress() {
        return mTextLiveProgress;
    }

    public LiveData<Integer> getBufferingPercent() {
        return mBufferingPercent;
    }

    public LiveData<Boolean> getStalled() {
        return mStalled;
    }

    public void stopProgressClock() {
        stopTimer();
    }

    private void registerAllListener() {
        PlayerClient.PlaylistController playlistController = mPlayerClient.getPlaylistController();

        playlistController.addOnPlaybackStateChangeListener(mPlaybackStateChangeListener);
        playlistController.addOnBufferingPercentChangeListener(mBufferingPercentChangeListener);
        playlistController.addOnStalledChangeListener(mStalledChangeListener);
        playlistController.addOnSeekCompleteListener(mSeekCompleteListener);
        playlistController.addOnPlayingMusicItemChangeListener(mPlayingMusicItemChangeListener);
        playlistController.addOnPlayModeChangeListener(mPlayModeChangeListener);
    }

    private void unregisterAllListener() {
        PlayerClient.PlaylistController playlistController = mPlayerClient.getPlaylistController();

        playlistController.removeOnPlaybackStateChangeListener(mPlaybackStateChangeListener);
        playlistController.removeOnBufferingPercentChangeListener(mBufferingPercentChangeListener);
        playlistController.removeOnStalledChangeListener(mStalledChangeListener);
        playlistController.removeOnSeekCompleteListener(mSeekCompleteListener);
        playlistController.removeOnPlayingMusicItemChangeListener(mPlayingMusicItemChangeListener);
        playlistController.removeOnPlayModeChangeListener(mPlayModeChangeListener);
    }

    private void updateDuration(int durationMS/*单位：毫秒*/) {
        mDurationSec = (int) durationMS / 1000;

        mDuration.setValue(mDurationSec);
        mTextDuration.setValue(formatSeconds(mDurationSec));
    }

    private void updateProgress(int progressMS/*单位：毫秒*/, long updateTime) {
        long currentTime = System.currentTimeMillis();

        int progressSec = (int) ((progressMS + currentTime - updateTime) / 1000);
        updateProgress(progressSec);
    }

    private void startTimer() {
        stopTimer();

        mLoop = mPlayerClient.getPlaylistController().isLooping();

        mDisposable = Observable.interval(1000, 1000, TimeUnit.MILLISECONDS, AndroidSchedulers.mainThread())
                .subscribe(new Consumer<Long>() {
                    @Override
                    public void accept(Long aLong) {
                        updateProgress();
                    }
                });
    }

    private void stopTimer() {
        if (mDisposable != null && !mDisposable.isDisposed()) {
            mDisposable.dispose();
            mDisposable = null;
        }
    }

    private void updateProgress() {
        Integer oldValue = mLiveProgress.getValue();

        if (oldValue == null) {
            updateProgress(0);
            return;
        }

        int newValue = oldValue + 1;

        if (mLoop && newValue > mDurationSec) {
            updateProgress(0);
            return;
        }

        if (!mLoop && newValue >= mDurationSec) {
            stopTimer();
        }

        updateProgress(newValue);
    }

    private void updateProgress(int progressSec/*单位：秒*/) {
        mLiveProgress.setValue(progressSec);
        mTextLiveProgress.setValue(formatSeconds(progressSec));
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
    private String formatSeconds(int seconds) {
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
}
