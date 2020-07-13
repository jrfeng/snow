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

import snow.player.Player;
import snow.player.PlayerClient;
import snow.player.PlayerManager;
import snow.player.media.MusicItem;

public class PlayerStateViewModel extends ViewModel {
    private MutableLiveData<PlayerManager.PlayerType> mPlayerType;

    private MutableLiveData<Integer> mPlayProgress;
    private MutableLiveData<Long> mPlayProgressUpdateTime;
    private MutableLiveData<MusicItem> mMusicItem;
    private MutableLiveData<Player.PlaybackState> mPlaybackState;
    private MutableLiveData<Integer> mAudioSessionId;
    private MutableLiveData<Integer> mBufferingPercent;
    private MutableLiveData<Long> mBufferingPercentUpdateTime;
    private MutableLiveData<Boolean> mStalled;
    private MutableLiveData<Integer> mErrorCode;
    private MutableLiveData<String> mErrorMessage;

    private PlayerManager.OnPlayerTypeChangeListener mPlayerTypeChangeListener;
    private Player.OnPlaybackStateChangeListener mPlaybackStateChangeListener;
    private Player.OnStalledChangeListener mStalledChangeListener;
    private Player.OnSeekCompleteListener mSeekCompleteListener;
    private Player.OnBufferingPercentChangeListener mBufferingPercentChangeListener;
    private Player.OnPlayingMusicItemChangeListener mPlayingMusicItemChangeListener;

    public PlayerStateViewModel() {
        initAllLiveData();
        initAllListener();
    }

    private void initAllLiveData() {
        mPlayerType = new MutableLiveData<>(PlayerManager.PlayerType.PLAYLIST);

        mPlayProgress = new MutableLiveData<>(0);
        mPlayProgressUpdateTime = new MutableLiveData<>(0L);
        mMusicItem = new MutableLiveData<>(new MusicItem());
        mPlaybackState = new MutableLiveData<>(Player.PlaybackState.UNKNOWN);
        mAudioSessionId = new MutableLiveData<>(0);
        mBufferingPercent = new MutableLiveData<>(0);
        mBufferingPercentUpdateTime = new MutableLiveData<>(0L);
        mStalled = new MutableLiveData<>(false);
        mErrorCode = new MutableLiveData<>(0);
        mErrorMessage = new MutableLiveData<>("");
    }

    private void initAllListener() {
        mPlayerTypeChangeListener = new PlayerManager.OnPlayerTypeChangeListener() {
            @Override
            public void onPlayerTypeChanged(PlayerManager.PlayerType playerType) {
                mPlayerType.setValue(playerType);
            }
        };

        mPlaybackStateChangeListener = new Player.OnPlaybackStateChangeListener() {
            @Override
            public void onPreparing() {
                mPlaybackState.setValue(Player.PlaybackState.PREPARING);
            }

            @Override
            public void onPrepared(int audioSessionId) {
                mPlaybackState.setValue(Player.PlaybackState.PREPARED);
                mAudioSessionId.setValue(audioSessionId);
            }

            @Override
            public void onPlay(int playProgress, long playProgressUpdateTime) {
                mPlaybackState.setValue(Player.PlaybackState.PLAYING);
                mPlayProgress.setValue(playProgress);
                mPlayProgressUpdateTime.setValue(playProgressUpdateTime);
            }

            @Override
            public void onPause() {
                mPlaybackState.setValue(Player.PlaybackState.PAUSED);
            }

            @Override
            public void onStop() {
                mPlaybackState.setValue(Player.PlaybackState.STOPPED);
            }

            @Override
            public void onError(int errorCode, String errorMessage) {
                mPlaybackState.setValue(Player.PlaybackState.ERROR);
                mErrorCode.setValue(errorCode);
                mErrorMessage.setValue(errorMessage);
            }
        };

        mStalledChangeListener = new Player.OnStalledChangeListener() {
            @Override
            public void onStalledChanged(boolean stalled) {
                mStalled.setValue(stalled);
            }
        };

        mSeekCompleteListener = new Player.OnSeekCompleteListener() {
            @Override
            public void onSeekComplete(int progress) {
                mPlayProgress.setValue(progress);
            }
        };

        mBufferingPercentChangeListener = new Player.OnBufferingPercentChangeListener() {
            @Override
            public void onBufferingPercentChanged(int percent, long updateTime) {
                mBufferingPercent.setValue(percent);
                mBufferingPercentUpdateTime.setValue(updateTime);
            }
        };

        mPlayingMusicItemChangeListener = new Player.OnPlayingMusicItemChangeListener() {
            @Override
            public void onPlayingMusicItemChanged(@Nullable MusicItem musicItem) {
                mMusicItem.setValue(musicItem);
            }
        };
    }

    private void registerAllListener(PlayerClient playerClient) {
        playerClient.addOnPlayerTypeChangeListener(mPlayerTypeChangeListener);

        PlayerClient.PlaylistController playlistController = playerClient.getPlaylistController();
        PlayerClient.RadioStationController radioStationController = playerClient.getRadioStationController();

        playlistController.addOnPlaybackStateChangeListener(mPlaybackStateChangeListener);
        radioStationController.addOnPlaybackStateChangeListener(mPlaybackStateChangeListener);

        playlistController.addOnStalledChangeListener(mStalledChangeListener);
        radioStationController.addOnStalledChangeListener(mStalledChangeListener);

        playlistController.addOnSeekCompleteListener(mSeekCompleteListener);
        radioStationController.addOnSeekCompleteListener(mSeekCompleteListener);

        playlistController.addOnBufferingPercentChangeListener(mBufferingPercentChangeListener);
        radioStationController.addOnBufferingPercentChangeListener(mBufferingPercentChangeListener);

        playlistController.addOnPlayingMusicItemChangeListener(mPlayingMusicItemChangeListener);
        radioStationController.addOnPlayingMusicItemChangeListener(mPlayingMusicItemChangeListener);
    }

    private void unregisterAllListener(PlayerClient playerClient) {
        playerClient.removeOnPlayerTypeChangeListener(mPlayerTypeChangeListener);

        PlayerClient.PlaylistController playlistController = playerClient.getPlaylistController();
        PlayerClient.RadioStationController radioStationController = playerClient.getRadioStationController();

        playlistController.removeOnPlaybackStateChangeListener(mPlaybackStateChangeListener);
        radioStationController.removeOnPlaybackStateChangeListener(mPlaybackStateChangeListener);

        playlistController.removeOnStalledChangeListener(mStalledChangeListener);
        radioStationController.removeOnStalledChangeListener(mStalledChangeListener);

        playlistController.removeOnSeekCompleteListener(mSeekCompleteListener);
        radioStationController.removeOnSeekCompleteListener(mSeekCompleteListener);

        playlistController.removeOnBufferingPercentChangeListener(mBufferingPercentChangeListener);
        radioStationController.removeOnBufferingPercentChangeListener(mBufferingPercentChangeListener);

        playlistController.removeOnPlayingMusicItemChangeListener(mPlayingMusicItemChangeListener);
        radioStationController.removeOnPlayingMusicItemChangeListener(mPlayingMusicItemChangeListener);
    }

    public void init(@NonNull LifecycleOwner owner, @NonNull final PlayerClient playerClient) {
        Preconditions.checkNotNull(playerClient);

        owner.getLifecycle().addObserver(new LifecycleObserver() {
            @OnLifecycleEvent(Lifecycle.Event.ON_START)
            public void onStart() {
                registerAllListener(playerClient);
            }

            @OnLifecycleEvent(Lifecycle.Event.ON_STOP)
            public void onStop() {
                unregisterAllListener(playerClient);
            }
        });
    }

    public LiveData<PlayerManager.PlayerType> getPlayerType() {
        return mPlayerType;
    }

    public LiveData<Integer> getPlayProgress() {
        return mPlayProgress;
    }

    public LiveData<Long> getPlayProgressUpdateTime() {
        return mPlayProgressUpdateTime;
    }

    public LiveData<MusicItem> getMusicItem() {
        return mMusicItem;
    }

    public LiveData<Player.PlaybackState> getPlaybackState() {
        return mPlaybackState;
    }

    public LiveData<Integer> getAudioSessionId() {
        return mAudioSessionId;
    }

    public LiveData<Integer> getBufferingPercent() {
        return mBufferingPercent;
    }

    public LiveData<Long> getBufferingPercentUpdateTime() {
        return mBufferingPercentUpdateTime;
    }

    public LiveData<Boolean> getStalled() {
        return mStalled;
    }

    public LiveData<Integer> getErrorCode() {
        return mErrorCode;
    }

    public LiveData<String> getErrorMessage() {
        return mErrorMessage;
    }
}
