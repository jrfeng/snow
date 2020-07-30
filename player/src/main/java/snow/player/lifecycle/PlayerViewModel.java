package snow.player.lifecycle;

import android.widget.SeekBar;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.arch.core.util.Function;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;
import androidx.lifecycle.ViewModel;

import com.google.common.base.Preconditions;

import snow.player.PlayMode;
import snow.player.PlaybackState;
import snow.player.Player;
import snow.player.PlayerClient;
import snow.player.media.MusicItem;
import snow.player.playlist.Playlist;
import snow.player.playlist.PlaylistManager;

/**
 * 播放器 ViewModel，支持 DataBinding。
 * <p>
 * <b>注意！使用前必须先调用 {@link #init(PlayerClient, String, String)} 方法进行初始化。</b>
 */
public class PlayerViewModel extends ViewModel {
    private PlayerClient mPlayerClient;

    private MutableLiveData<String> mTitle;
    private MutableLiveData<String> mArtist;
    private MutableLiveData<String> mIconUri;
    private MutableLiveData<Integer> mDuration;         // 单位：秒
    private MutableLiveData<Integer> mPlayProgress;     // 单位：秒
    private MutableLiveData<Integer> mBufferedProgress; // 单位：秒
    private MutableLiveData<Integer> mPlayPosition;
    private MutableLiveData<PlayMode> mPlayMode;
    private MutableLiveData<PlaybackState> mPlaybackState;
    private MutableLiveData<Boolean> mStalled;
    private MutableLiveData<String> mErrorMessage;
    private PlaylistLiveData mPlaylist;

    private Player.OnPlayingMusicItemChangeListener mPlayingMusicItemChangeListener;
    private Player.OnPositionChangeListener mPositionChangeListener;
    private Player.OnPlaylistChangeListener mPlaylistChangeListener;
    private Player.OnPlayModeChangeListener mPlayModeChangeListener;
    private PlayerClient.OnPlaybackStateChangeListener mClientPlaybackStateChangeListener;
    private Player.OnBufferedProgressChangeListener mBufferedProgressChangeListener;
    private Player.OnSeekCompleteListener mSeekCompleteListener;
    private Player.OnStalledChangeListener mStalledChangeListener;
    private PlayerClient.OnDisconnectListener mDisconnectListener;

    private String mDefaultTitle;
    private String mDefaultArtist;

    private ProgressClock mProgressClock;

    private boolean mInitialized;
    private boolean mDisconnectOnCleared;

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

        mInitialized = true;
    }

    /**
     * 设置是否在 ViewModel 被清理时断开 PlayerClient 的连接。
     *
     * @param disconnectOnCleared 如果为 true，则会在 ViewModel 被清理时断开 PlayerClient 的连接
     */
    public void setDisconnectOnCleared(boolean disconnectOnCleared) {
        mDisconnectOnCleared = disconnectOnCleared;
    }

    private void initAllListener() {
        mPlayingMusicItemChangeListener = new Player.OnPlayingMusicItemChangeListener() {
            @Override
            public void onPlayingMusicItemChanged(@Nullable MusicItem musicItem, int playProgress) {
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
                mPlayProgress.setValue(playProgress / 1000);
                mBufferedProgress.setValue(0);
            }
        };

        mPositionChangeListener = new Player.OnPositionChangeListener() {
            @Override
            public void onPositionChanged(int position) {
                mPlayPosition.setValue(position);
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
            public void onPlayModeChanged(PlayMode playMode) {
                mPlayMode.setValue(playMode);
                mProgressClock.setLoop(playMode == PlayMode.LOOP);
            }
        };

        mClientPlaybackStateChangeListener = new PlayerClient.OnPlaybackStateChangeListener() {
            @Override
            public void onPlaybackStateChanged(PlaybackState playbackState) {
                if (playbackState == PlaybackState.ERROR) {
                    mErrorMessage.setValue(mPlayerClient.getErrorMessage());
                }

                mPlaybackState.setValue(playbackState);

                switch (playbackState) {
                    case PLAYING:
                        mProgressClock.start(mPlayerClient.getPlayProgress(),
                                mPlayerClient.getPlayProgressUpdateTime(),
                                mPlayerClient.getPlayingMusicItemDuration());
                        break;
                    case STOPPED:
                        mPlayProgress.setValue(0);  // 注意！case 穿透！
                    case PAUSED:                    // 注意！case 穿透！
                    case ERROR:                     // 注意！case 穿透！
                        mProgressClock.cancel();
                        break;
                }
            }
        };

        mBufferedProgressChangeListener = new Player.OnBufferedProgressChangeListener() {
            @Override
            public void onBufferedProgressChanged(int bufferedProgress) {
                mBufferedProgress.setValue(getBufferedProgressSec());
            }
        };

        mSeekCompleteListener = new Player.OnSeekCompleteListener() {
            @Override
            public void onSeekComplete(int progress, long updateTime) {
                if (PlaybackState.PLAYING == mPlaybackState.getValue()) {
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

        mDisconnectListener = new PlayerClient.OnDisconnectListener() {
            @Override
            public void onDisconnected() {
                if (isInitialized()) {
                    mProgressClock.cancel();
                }
            }
        };
    }

    private void initProgressClock() {
        mProgressClock = new ProgressClock(new ProgressClock.Callback() {
            @Override
            public void onUpdateProgress(int progressSec, int durationSec) {
                mPlayProgress.setValue(progressSec);
            }
        });
    }

    private void addAllListener() {
        mPlayerClient.addOnPlayingMusicItemChangeListener(mPlayingMusicItemChangeListener);
        mPlayerClient.addOnPositionChangeListener(mPositionChangeListener);
        mPlayerClient.addOnPlaylistChangeListener(mPlaylistChangeListener);
        mPlayerClient.addOnPlayModeChangeListener(mPlayModeChangeListener);
        mPlayerClient.addOnPlaybackStateChangeListener(mClientPlaybackStateChangeListener);
        mPlayerClient.addOnBufferedProgressChangeListener(mBufferedProgressChangeListener);
        mPlayerClient.addOnSeekCompleteListener(mSeekCompleteListener);
        mPlayerClient.addOnStalledChangeListener(mStalledChangeListener);
        mPlayerClient.addOnDisconnectListener(mDisconnectListener);
    }

    private void removeAllListener() {
        mPlayerClient.removeOnPlayingMusicItemChangeListener(mPlayingMusicItemChangeListener);
        mPlayerClient.removeOnPositionChangeListener(mPositionChangeListener);
        mPlayerClient.removeOnPlaylistChangeListener(mPlaylistChangeListener);
        mPlayerClient.removeOnPlayModeChangeListener(mPlayModeChangeListener);
        mPlayerClient.removeOnPlaybackStateChangeListener(mClientPlaybackStateChangeListener);
        mPlayerClient.removeOnBufferedProgressChangeListener(mBufferedProgressChangeListener);
        mPlayerClient.removeOnSeekCompleteListener(mSeekCompleteListener);
        mPlayerClient.removeOnStalledChangeListener(mStalledChangeListener);
        mPlayerClient.removeOnDisconnectListener(mDisconnectListener);
    }

    @Override
    protected void onCleared() {
        super.onCleared();

        if (mInitialized && mDisconnectOnCleared) {
            mPlayerClient.disconnect();
        }

        if (mInitialized) {
            mInitialized = false;
            mProgressClock.cancel();
            mPlaylist.release();
            removeAllListener();
            mPlayerClient = null;
        }
    }

    /**
     * 是否已完成初始化。
     *
     * @return 如果返回 false，则必须先调用 {@link #init(PlayerClient, String, String)} 方法进行初始化后
     * 才能正常使用当前 {@link PlayerViewModel} 对象
     */
    public boolean isInitialized() {
        return mInitialized;
    }

    /**
     * 获取当前 {@link PlayerViewModel}关联到的 PlayerClient 对象。
     *
     * @throws IllegalStateException 如果当前 PlayerViewModel 对象还没有被初始化（{@link #isInitialized()} 返回 false）
     */
    @NonNull
    public PlayerClient getPlayerClient() throws IllegalStateException {
        if (!isInitialized()) {
            throw new IllegalStateException("PlayerViewModel not initialized yet.");
        }

        return mPlayerClient;
    }

    /**
     * 正在播放的歌曲的标题。
     */
    @NonNull
    public LiveData<String> getTitle() {
        return mTitle;
    }

    /**
     * 正在播放的歌曲的艺术家（歌手）。
     */
    @NonNull
    public LiveData<String> getArtist() {
        return mArtist;
    }

    /**
     * 正在播放的歌曲的图标的 Uri
     */
    @NonNull
    public LiveData<String> getIconUri() {
        return mIconUri;
    }

    /**
     * 正在播放的歌曲的持续时间（单位：秒）。
     */
    @NonNull
    public LiveData<Integer> getDuration() {
        return mDuration;
    }

    /**
     * 正在播放歌曲的实时播放进度（单位：秒），支持双向绑定。
     */
    @NonNull
    public MutableLiveData<Integer> getPlayProgress() {
        return mPlayProgress;
    }

    /**
     * 正在播放歌曲的缓存进度（单位：秒）。
     */
    @NonNull
    public LiveData<Integer> getBufferedProgress() {
        return mBufferedProgress;
    }

    /**
     * 正在播放歌曲在列表中的位置。
     */
    @NonNull
    public LiveData<Integer> getPlayPosition() {
        return mPlayPosition;
    }

    /**
     * 播放器的播放模式。
     */
    @NonNull
    public LiveData<PlayMode> getPlayMode() {
        return mPlayMode;
    }

    /**
     * 播放器的播放状态。
     */
    @NonNull
    public LiveData<PlaybackState> getPlaybackState() {
        return mPlaybackState;
    }

    /**
     * 播放器是否处于 stalled 状态。
     * <p>
     * 当缓冲去没有足够的数据支持播放器继续播放时，该值为 true。
     */
    @NonNull
    public LiveData<Boolean> getStalled() {
        return mStalled;
    }

    /**
     * 是否发生了错误。
     */
    @NonNull
    public LiveData<Boolean> isError() {
        return Transformations.map(mPlaybackState, new Function<PlaybackState, Boolean>() {
            @Override
            public Boolean apply(PlaybackState input) {
                return input == PlaybackState.ERROR;
            }
        });
    }

    /**
     * 错误信息，只在发生错误时该值才有意义。
     */
    @NonNull
    public LiveData<String> getErrorMessage() {
        return mErrorMessage;
    }

    /**
     * 获取播放列表。
     * <p>
     * 需要向 LiveData 至少注册一个观察者，才会在播放列表发生改变时实时更新当前 LiveData 的值。如果当前
     * LiveData 没有任何观察者，出于性能考虑，不会实时更新当前 LiveData 的值。
     *
     * @return 包含当前播放列表的 LiveData 对象
     */
    @NonNull
    public LiveData<Playlist> getPlaylist() {
        return mPlaylist;
    }

    /**
     * 获取歌曲的持续时间（单位：秒）对应的文本值，例如 @{code 82} 秒对应的文本值为 {@code "01:22"}
     */
    @NonNull
    public LiveData<String> getTextDuration() {
        return Transformations.map(mDuration, new Function<Integer, String>() {
            @Override
            public String apply(Integer input) {
                return ProgressClock.asText(input);
            }
        });
    }

    /**
     * 获取歌曲的实时播放进度（单位：秒）对应的文本值，例如 @{code 82} 秒对应的文本值为 {@code "01:22"}
     */
    @NonNull
    public LiveData<String> getTextPlayProgress() {
        return Transformations.map(mPlayProgress, new Function<Integer, String>() {
            @Override
            public String apply(Integer input) {
                return ProgressClock.asText(input);
            }
        });
    }

    /**
     * 设置播放列表。
     *
     * @param playlist 新的播放列表
     */
    public void setPlaylist(Playlist playlist) {
        if (isInitialized()) {
            mPlayerClient.setPlaylist(playlist);
        }
    }

    /**
     * 设置播放列表，并决定是否立即播放列表的第 1 首音乐。
     *
     * @param playlist 新的播放列表
     * @param play     是否立即播放列表的第 1 首音乐
     */
    public void setPlaylist(Playlist playlist, boolean play) {
        if (isInitialized()) {
            mPlayerClient.setPlaylist(playlist, play);
        }
    }

    /**
     * 设置播放列表，并决定是否立即播放列表的 position 位置处音乐。
     *
     * @param playlist 新的播放列表
     * @param position 要播放的歌曲的位置
     * @param play     是否立即播放列表的 position 位置处音乐
     */
    public void setPlaylist(Playlist playlist, int position, boolean play) {
        if (isInitialized()) {
            mPlayerClient.setPlaylist(playlist, position, play);
        }
    }

    /**
     * 播放
     */
    public void play() {
        if (isInitialized()) {
            mPlayerClient.play();
        }
    }

    /**
     * 暂停
     */
    public void pause() {
        if (isInitialized()) {
            mPlayerClient.pause();
        }
    }

    /**
     * 播放/暂停
     */
    public void playPause() {
        if (isInitialized()) {
            mPlayerClient.playPause();
        }
    }

    /**
     * 播放/暂停 position 位置处的音乐。
     * <p>
     * 如果 position 与当前正在播放的音乐的位置是一样的，则暂停播放，否则播放列表中 position 位置处的音乐。
     */
    public void playPause(int position) {
        if (isInitialized()) {
            mPlayerClient.playPause(position);
        }
    }

    /**
     * 停止
     */
    public void stop() {
        if (isInitialized()) {
            mPlayerClient.stop();
        }
    }

    /**
     * 上一曲
     */
    public void skipToPrevious() {
        if (isInitialized()) {
            mPlayerClient.skipToPrevious();
        }
    }

    /**
     * 像以前
     */
    public void skipToNext() {
        if (isInitialized()) {
            mPlayerClient.skipToNext();
        }
    }

    /**
     * 快进
     */
    public void fastForward() {
        if (isInitialized()) {
            mPlayerClient.fastForward();
        }
    }

    /**
     * 快退
     */
    public void rewind() {
        if (isInitialized()) {
            mPlayerClient.rewind();
        }
    }

    /**
     * 设置指定歌曲 “下一次播放”。
     *
     * @param musicItem 要设定为 “下一次播放” 的歌曲，如果歌曲已存在播放列表中，则会移动到 “下一曲播放” 的位
     *                  置，如果歌曲不存在，则插入到 “下一曲播放” 位置
     */
    public void setNextPlay(@NonNull MusicItem musicItem) {
        Preconditions.checkNotNull(musicItem);
        if (isInitialized()) {
            mPlayerClient.setNextPlay(musicItem);
        }
    }

    /**
     * 设置播放模式。
     *
     * @param playMode 播放模式
     */
    public void setPlayMode(@NonNull PlayMode playMode) {
        Preconditions.checkNotNull(playMode);
        if (isInitialized()) {
            mPlayerClient.setPlayMode(playMode);
        }
    }

    /**
     * 调整音乐播放进度。
     *
     * @param progress 要调整到的播放进度
     */
    public void seekTo(int progress) {
        if (isInitialized()) {
            mProgressClock.cancel();
            mPlayerClient.seekTo(progress);
        }
    }

    /**
     * 停止实时跟新播放进度。
     */
    public void cancelProgressClock() {
        if (isInitialized()) {
            mProgressClock.cancel();
        }
    }

    /**
     * For DataBinding。
     * <p>
     * 如果你启用了 DataBinding，并且使用 SeekBar 来显示和调整播放进度，那么请将 SeekBar 的
     * {@code android:onStartTrackingTouch} 事件绑定为当前方法。
     * <p>
     * 例如：<br>
     * {@code android:onStartTrackingTouch="@{playerViewModel::onStartTrackingTouch}"}
     */
    public void onStartTrackingTouch(SeekBar seekBar) {
        cancelProgressClock();
    }

    /**
     * For DataBinding。
     * <p>
     * 如果你启用了 DataBinding，并且使用 SeekBar 来显示和调整播放进度，那么请将 SeekBar 的
     * {@code android:onStopTrackingTouch} 事件绑定为当前方法。
     * <p>
     * 例如：<br>
     * {@code android:onStopTrackingTouch="@{playerViewModel::onStopTrackingTouch}"}
     */
    public void onStopTrackingTouch(SeekBar seekBar) {
        seekTo(seekBar.getProgress());
    }

    private void initAllLiveData() {
        mTitle = new MutableLiveData<>(mDefaultTitle);
        mArtist = new MutableLiveData<>(mDefaultArtist);
        mIconUri = new MutableLiveData<>(getIconUri(mPlayerClient));
        mDuration = new MutableLiveData<>(getDurationSec());
        mPlayProgress = new MutableLiveData<>(getPlayProgressSec());
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
        if (mPlayerClient.isPlaying()) {
            long realProgress = mPlayerClient.getPlayProgress() + (System.currentTimeMillis() - mPlayerClient.getPlayProgressUpdateTime());
            return (int) (realProgress / 1000);
        }

        return mPlayerClient.getPlayProgress();
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
