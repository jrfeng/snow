package snow.player.lifecycle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.arch.core.util.Function;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;
import androidx.lifecycle.ViewModel;

import com.google.common.base.Preconditions;

import snow.player.Player;
import snow.player.PlayerClient;
import snow.player.media.MusicItem;
import snow.player.playlist.Playlist;
import snow.player.playlist.PlaylistManager;

/**
 * 播放器状态 ViewModel。
 * <p>
 * 使用前必须先调用 {@link #init(PlayerClient, String, String)} 方法进行初始化。
 */
public class PlayerViewModel extends ViewModel {
    private PlayerClient mPlayerClient;

    private MutableLiveData<String> mTitle;
    private MutableLiveData<String> mArtist;
    private MutableLiveData<String> mIconUri;
    private MutableLiveData<Integer> mDuration;         // 单位：秒
    private MutableLiveData<Integer> mLivePlayProgress; // 单位：秒
    private MutableLiveData<Integer> mBufferedProgress; // 单位：秒
    private MutableLiveData<Integer> mPlayPosition;
    private MutableLiveData<Player.PlayMode> mPlayMode;
    private MutableLiveData<Player.PlaybackState> mPlaybackState;
    private MutableLiveData<Boolean> mStalled;
    private MutableLiveData<String> mErrorMessage;
    private PlaylistLiveData mPlaylist;

    private Player.OnPlayingMusicItemChangeListener mPlayingMusicItemChangeListener;
    private Player.OnPlaylistChangeListener mPlaylistChangeListener;
    private Player.OnPlayModeChangeListener mPlayModeChangeListener;
    private PlayerClient.OnPlaybackStateChangeListener mClientPlaybackStateChangeListener;
    private Player.OnBufferedProgressChangeListener mBufferedProgressChangeListener;
    private Player.OnSeekCompleteListener mSeekCompleteListener;
    private Player.OnStalledChangeListener mStalledChangeListener;

    private String mDefaultTitle;
    private String mDefaultArtist;

    private ProgressClock mProgressClock;

    /**
     * 初始化 PlayerStateViewModel
     *
     * @param playerClient  PlayerClient 对象
     * @param defaultTitle  默认标题
     * @param defaultArtist 默认艺术家
     */
    public void init(@NonNull PlayerClient playerClient,
                     @NonNull String defaultTitle,
                     @NonNull String defaultArtist) {
        Preconditions.checkNotNull(playerClient);
        Preconditions.checkNotNull(defaultTitle);
        Preconditions.checkNotNull(defaultArtist);

        mPlayerClient = playerClient;
        mDefaultTitle = defaultTitle;
        mDefaultArtist = defaultArtist;

        initAllLiveData();
        initAllListener();
        initProgressClock();

        addAllListener();
    }

    private void initAllListener() {
        mPlayingMusicItemChangeListener = new Player.OnPlayingMusicItemChangeListener() {
            @Override
            public void onPlayingMusicItemChanged(@Nullable MusicItem musicItem) {
                mProgressClock.cancel();
                if (musicItem == null) {
                    mTitle.setValue(mDefaultTitle);
                    mArtist.setValue(mDefaultArtist);
                    mIconUri.setValue("");
                    return;
                }

                mTitle.setValue(musicItem.getTitle());
                mArtist.setValue(musicItem.getArtist());
                mIconUri.setValue(musicItem.getIconUri());
                mDuration.setValue(getDurationSec());
                mLivePlayProgress.setValue(0);
                mBufferedProgress.setValue(0);
            }
        };

        mPlaylistChangeListener = new Player.OnPlaylistChangeListener() {
            @Override
            public void onPlaylistChanged(PlaylistManager playlistManager, int position) {
                mPlayPosition.setValue(position);
            }
        };

        mPlayModeChangeListener = new Player.OnPlayModeChangeListener() {
            @Override
            public void onPlayModeChanged(Player.PlayMode playMode) {
                mPlayMode.setValue(playMode);
                mProgressClock.setLoop(playMode == Player.PlayMode.LOOP);
            }
        };

        mClientPlaybackStateChangeListener = new PlayerClient.OnPlaybackStateChangeListener() {
            @Override
            public void onPlaybackStateChanged(Player.PlaybackState playbackState) {
                if (playbackState == Player.PlaybackState.ERROR) {
                    mErrorMessage.setValue(mPlayerClient.getErrorMessage());
                }

                mPlaybackState.setValue(playbackState);

                switch (playbackState) {
                    case PLAYING:
                        mProgressClock.start(mPlayerClient.getPlayPosition(),
                                mPlayerClient.getPlayProgressUpdateTime(),
                                mPlayerClient.getPlayingMusicItemDuration());
                        break;
                    case PAUSED:    // 注意！case 穿透！
                    case STOPPED:   // 注意！case 穿透！
                    case ERROR:     // 注意！case 穿透！
                        mProgressClock.cancel();
                        break;
                }
            }
        };

        mBufferedProgressChangeListener = new Player.OnBufferedProgressChangeListener() {
            @Override
            public void onBufferedProgressChanged(int bufferedProgress) {
                mBufferedProgress.setValue(bufferedProgress);
            }
        };

        mSeekCompleteListener = new Player.OnSeekCompleteListener() {
            @Override
            public void onSeekComplete(int progress, long updateTime) {
                if (Player.PlaybackState.PLAYING == mPlaybackState.getValue()) {
                    mProgressClock.start(progress, updateTime, mPlayerClient.getPlayingMusicItemDuration());
                }
            }
        };

        mStalledChangeListener = new Player.OnStalledChangeListener() {
            @Override
            public void onStalledChanged(boolean stalled) {
                mStalled.setValue(stalled);
            }
        };
    }

    private void initProgressClock() {
        mProgressClock = new ProgressClock(new ProgressClock.Callback() {
            @Override
            public void onUpdateProgress(int progressSec, int durationSec) {
                mLivePlayProgress.setValue(progressSec);
            }
        });
    }

    private void addAllListener() {
        mPlayerClient.addOnPlayingMusicItemChangeListener(mPlayingMusicItemChangeListener);
        mPlayerClient.addOnPlaylistChangeListener(mPlaylistChangeListener);
        mPlayerClient.addOnPlayModeChangeListener(mPlayModeChangeListener);
        mPlayerClient.addOnPlaybackStateChangeListener(mClientPlaybackStateChangeListener);
        mPlayerClient.addOnBufferedProgressChangeListener(mBufferedProgressChangeListener);
        mPlayerClient.addOnSeekCompleteListener(mSeekCompleteListener);
        mPlayerClient.addOnStalledChangeListener(mStalledChangeListener);
    }

    private void removeAllListener() {
        mPlayerClient.removeOnPlayingMusicItemChangeListener(mPlayingMusicItemChangeListener);
        mPlayerClient.removeOnPlaylistChangeListener(mPlaylistChangeListener);
        mPlayerClient.removeOnPlayModeChangeListener(mPlayModeChangeListener);
        mPlayerClient.removeOnPlaybackStateChangeListener(mClientPlaybackStateChangeListener);
        mPlayerClient.removeOnBufferedProgressChangeListener(mBufferedProgressChangeListener);
        mPlayerClient.removeOnSeekCompleteListener(mSeekCompleteListener);
        mPlayerClient.removeOnStalledChangeListener(mStalledChangeListener);
    }

    @Override
    protected void onCleared() {
        super.onCleared();

        mPlaylist.release();
        removeAllListener();
        mPlayerClient = null;
    }

    /**
     * 获取当前 {@link PlayerViewModel}关联到的 PlayerClient 对象。
     */
    @NonNull
    public PlayerClient getPlayerClient() {
        return mPlayerClient;
    }

    @NonNull
    public LiveData<String> getTitle() {
        return mTitle;
    }

    @NonNull
    public LiveData<String> getArtist() {
        return mArtist;
    }

    @NonNull
    public LiveData<String> getIconUri() {
        return mIconUri;
    }

    @NonNull
    public LiveData<Integer> getDuration() {
        return mDuration;
    }

    @NonNull
    public LiveData<Integer> getLivePlayProgress() {
        return mLivePlayProgress;
    }

    @NonNull
    public LiveData<Integer> getBufferedProgress() {
        return mBufferedProgress;
    }

    @NonNull
    public LiveData<Integer> getPlayPosition() {
        return mPlayPosition;
    }

    @NonNull
    public LiveData<Player.PlayMode> getPlayMode() {
        return mPlayMode;
    }

    @NonNull
    public LiveData<Player.PlaybackState> getPlaybackState() {
        return mPlaybackState;
    }

    @NonNull
    public LiveData<Boolean> isStalled() {
        return mStalled;
    }

    @NonNull
    public LiveData<Boolean> isError() {
        return Transformations.map(mPlaybackState, new Function<Player.PlaybackState, Boolean>() {
            @Override
            public Boolean apply(Player.PlaybackState input) {
                return input == Player.PlaybackState.ERROR;
            }
        });
    }

    @NonNull
    public LiveData<String> getErrorMessage() {
        return mErrorMessage;
    }

    @NonNull
    public LiveData<Playlist> getPlaylist() {
        return mPlaylist;
    }

    @NonNull
    public LiveData<String> getTextDuration() {
        return Transformations.map(mDuration, new Function<Integer, String>() {
            @Override
            public String apply(Integer input) {
                return ProgressClock.asText(input);
            }
        });
    }

    @NonNull
    public LiveData<String> getTextLivePlayProgress() {
        return Transformations.map(mLivePlayProgress, new Function<Integer, String>() {
            @Override
            public String apply(Integer input) {
                return ProgressClock.asText(input);
            }
        });
    }

    private void initAllLiveData() {
        mTitle = new MutableLiveData<>(mDefaultTitle);
        mArtist = new MutableLiveData<>(mDefaultArtist);
        mIconUri = new MutableLiveData<>(getIconUri(mPlayerClient));
        mDuration = new MutableLiveData<>(getDurationSec());
        mLivePlayProgress = new MutableLiveData<>(getPlayProgressSec());
        mBufferedProgress = new MutableLiveData<>(getBufferedProgressSec());
        mPlayPosition = new MutableLiveData<>(mPlayerClient.getPlayPosition());
        mPlayMode = new MutableLiveData<>(mPlayerClient.getPlayMode());
        mPlaybackState = new MutableLiveData<>(mPlayerClient.getPlaybackState());
        mStalled = new MutableLiveData<>(mPlayerClient.isStalled());
        mErrorMessage = new MutableLiveData<>(mPlayerClient.getErrorMessage());
        mPlaylist = new PlaylistLiveData();
        mPlaylist.init(mPlayerClient);
    }

    private int getDurationSec() {
        return mPlayerClient.getPlayingMusicItemDuration() / 1000;
    }

    private int getPlayProgressSec() {
        long realProgress = mPlayerClient.getPlayProgress() + (System.currentTimeMillis() - mPlayerClient.getPlayProgressUpdateTime());
        return (int) (realProgress / 1000);
    }

    private int getBufferedProgressSec() {
        return mPlayerClient.getBufferedProgress() / 1000;
    }

    private String getIconUri(PlayerClient playerClient) {
        MusicItem musicItem = playerClient.getPlayingMusicItem();
        if (musicItem == null) {
            return "";
        }

        return musicItem.getIconUri();
    }
}
