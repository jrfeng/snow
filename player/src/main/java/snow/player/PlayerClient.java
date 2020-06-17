package snow.player;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.os.IBinder;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.common.base.Charsets;
import com.google.common.base.Preconditions;
import com.google.common.hash.Hashing;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import channel.helper.DispatcherUtil;
import channel.helper.pipe.MessengerPipe;
import snow.player.media.MusicItem;
import snow.player.playlist.PlaylistManager;
import snow.player.playlist.PlaylistPlayer;
import snow.player.playlist.PlaylistPlayerChannel;
import snow.player.radio.RadioStation;
import snow.player.radio.RadioStationPlayer;
import snow.player.radio.RadioStationPlayerChannel;
import snow.player.state.PlayerState;
import snow.player.state.PlayerStateListener;
import snow.player.state.PlaylistState;
import snow.player.state.PlaylistStateListener;
import snow.player.state.PlaylistStateListenerChannel;
import snow.player.state.RadioStationState;
import snow.player.state.RadioStationStateListener;
import snow.player.state.RadioStationStateListenerChannel;
import snow.player.util.ErrorUtil;

public class PlayerClient {
    private Context mApplicationContext;
    private Class<? extends PlayerService> mPlayerService;
    private String mToken;

    private ServiceConnection mServiceConnection;

    private boolean mConnected;
    private int mPlayerType;

    private PlayerManager mPlayerManager;

    private PlaylistController mPlaylistController;
    private RadioStationController mRadioStationController;

    private PlayerManager.OnCommandCallback mCommandCallback;

    private List<PlayerManager.OnPlayerTypeChangeListener> mAllPlayerTypeChangeListener;

    private PlayerClient(Context context, Class<? extends PlayerService> playerService) {
        mApplicationContext = context.getApplicationContext();
        mPlayerService = playerService;
        mToken = generateToken();

        mConnected = false;
        mAllPlayerTypeChangeListener = new ArrayList<>();

        initServiceConnection();
        initAllController();
        initConfigChangeListener();
    }

    public static PlayerClient newInstance(@NonNull Context context,
                                           @NonNull Class<? extends PlayerService> playerService) {
        Preconditions.checkNotNull(context);
        Preconditions.checkNotNull(playerService);

        if (serviceNotFound(context, playerService)) {
            throw new IllegalArgumentException("PlayerService not found, Please check your 'AndroidManifest.xml'");
        }

        return new PlayerClient(context, playerService);
    }

    private static boolean serviceNotFound(Context context, Class<? extends PlayerService> playerService) {
        PackageManager pm = context.getPackageManager();
        Intent intent = new Intent(context, playerService);
        return pm.resolveService(intent, 0) == null;
    }

    @SuppressWarnings("all")
    private String generateToken() {
        return Hashing.sha256().newHasher()
                .putLong(hashCode())
                .putLong(System.nanoTime())
                .putInt(new Random().nextInt())
                .hash()
                .toString();
    }

    private void initServiceConnection() {
        mServiceConnection = new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName name, IBinder service) {
                onConnected(service);
            }

            @Override
            public void onServiceDisconnected(ComponentName name) {
                onDisconnected();
            }
        };
    }

    private void initAllController() {
        mPlaylistController = new PlaylistController(mApplicationContext, mToken);
        mRadioStationController = new RadioStationController();
    }

    private void initConfigChangeListener() {
        mCommandCallback = new PlayerManager.OnCommandCallback() {
            @Override
            public void onShutdown() {
                disconnect();
            }

            @Override
            public void onPlayerTypeChanged(int playerType) {
                mPlayerType = playerType;
                notifyPlayerTypeChanged();
            }

            @Override
            public void syncPlayerState(int playerType, PlaylistState playlistState, RadioStationState radioStationState) {
                mPlayerType = playerType;
                notifyPlayerTypeChanged();

                mPlaylistController.setPlaylistState(playlistState);
                mRadioStationController.setRadioStationState(radioStationState);
            }
        };
    }

    private void notifyPlayerTypeChanged() {
        for (PlayerManager.OnPlayerTypeChangeListener listener : mAllPlayerTypeChangeListener) {
            listener.onPlayerTypeChanged(mPlayerType);
        }
    }

    private void onConnected(IBinder service) {
        mConnected = true;

        MessengerPipe controllerPipe = new MessengerPipe(service);

        mPlayerManager = new PlayerManagerChannel.Emitter(controllerPipe);

        mPlaylistController.setDelegate(new PlaylistPlayerChannel.Emitter(controllerPipe));
        mRadioStationController.setDelegate(new RadioStationPlayerChannel.Emitter(controllerPipe));

        mPlaylistController.setConnected(true);
        mRadioStationController.setConnected(true);

        MessengerPipe listenerPipe = new MessengerPipe(DispatcherUtil.merge(
                new OnCommandCallbackChannel.Dispatcher(mCommandCallback),
                new PlaylistStateListenerChannel.Dispatcher(mPlaylistController.getPlaylistStateListener()),
                new RadioStationStateListenerChannel.Dispatcher(mRadioStationController.getRadioStationStateListener())
        ));

        mPlayerManager.registerPlayerStateListener(mToken, listenerPipe.getBinder());
    }

    private void onDisconnected() {
        mConnected = false;
        mPlaylistController.setConnected(false);
        mRadioStationController.setConnected(false);
        mPlayerManager.unregisterPlayerStateListener(mToken);
    }

    public void connect() {
        if (isConnected()) {
            return;
        }

        Intent intent = new Intent(mApplicationContext, mPlayerService);
        mApplicationContext.bindService(intent, mServiceConnection, Context.BIND_AUTO_CREATE);
    }

    public void disconnect() {
        if (!isConnected()) {
            return;
        }

        onDisconnected();
        mApplicationContext.unbindService(mServiceConnection);
    }

    public boolean isConnected() {
        return mConnected;
    }

    public int getPlayerType() {
        return mPlayerType;
    }

    public PlaylistController getPlaylistController() {
        return mPlaylistController;
    }

    public RadioStationController getRadioStationController() {
        return mRadioStationController;
    }

    public void shutdown() {
        if (isConnected()) {
            mPlayerManager.shutdown();
        }
    }

    public void addOnPlayerTypeChangeListener(PlayerManager.OnPlayerTypeChangeListener listener) {
        if (mAllPlayerTypeChangeListener.contains(listener)) {
            return;
        }

        mAllPlayerTypeChangeListener.add(listener);
    }

    public void removeOnPlayerTypeChangeListener(PlayerManager.OnPlayerTypeChangeListener listener) {
        mAllPlayerTypeChangeListener.remove(listener);
    }

    public static class PlaylistController implements PlaylistPlayer {
        private PlaylistManagerImp mPlaylistManager;
        private PlaylistPlayerChannel.Emitter mDelegate;
        private PlaylistStateHolder mPlaylistStateHolder;
        private boolean mConnected;

        PlaylistController(Context context, String playlistId) {
            mPlaylistManager = new PlaylistManagerImp(context, playlistId);
            mPlaylistStateHolder = new PlaylistStateHolder(mPlaylistManager);
            mConnected = false;

            mPlaylistManager.setOnModifyPlaylistListener(new PlaylistManager.OnModifyPlaylistListener() {
                @Override
                public void onPlaylistSwapped(int position, boolean playOnPrepared) {
                    notifyPlaylistSwapped(position, playOnPrepared);
                }

                @Override
                public void onMusicItemMoved(int fromPosition, int toPosition) {
                    notifyMusicItemMoved(fromPosition, toPosition);
                }

                @Override
                public void onMusicItemInserted(int position, int count) {
                    notifyMusicItemInserted(position, count);
                }

                @Override
                public void onMusicItemRemoved(List<Integer> positions) {
                    notifyMusicItemRemoved(positions);
                }
            });
        }

        void setDelegate(PlaylistPlayerChannel.Emitter delegate) {
            mDelegate = delegate;
        }

        void setConnected(boolean connected) {
            mConnected = connected;
            mPlaylistManager.setAvailable(connected);
        }

        void setPlaylistState(PlaylistState playlistState) {
            mPlaylistStateHolder.setPlaylistState(playlistState);
        }

        PlaylistStateListener getPlaylistStateListener() {
            return mPlaylistStateHolder;
        }

        public boolean isConnected() {
            return mConnected;
        }

        /**
         * 获取当前 PlayerController 的 PlaylistManager 对象。
         *
         * @return 当前 PlayerController 的 PlaylistManager 对象（如果没有连接或已断开连接，则会返回 null）。
         */
        @Nullable
        public PlaylistManager getPlaylistManager() {
            if (!mConnected) {
                return null;
            }

            return mPlaylistManager;
        }

        public long getPlayProgress() {
            return mPlaylistStateHolder.mPlaylistState.getPlayProgress();
        }

        public long getPlayProgressUpdateTime() {
            return mPlaylistStateHolder.mPlaylistState.getPlayProgressUpdateTime();
        }

        public boolean isLooping() {
            return mPlaylistStateHolder.mPlaylistState.isLooping();
        }

        public int getSoundQuality() {
            return mPlaylistStateHolder.mPlaylistState.getSoundQuality();
        }

        public boolean isAudioEffectEnabled() {
            return mPlaylistStateHolder.mPlaylistState.isAudioEffectEnabled();
        }

        public boolean isOnlyWifiNetwork() {
            return mPlaylistStateHolder.mPlaylistState.isOnlyWifiNetwork();
        }

        public boolean isIgnoreLossAudioFocus() {
            return mPlaylistStateHolder.mPlaylistState.isIgnoreLossAudioFocus();
        }

        @Nullable
        public MusicItem getPlayingMusicItem() {
            return mPlaylistStateHolder.mPlaylistState.getMusicItem();
        }

        public int getPlaybackState() {
            return mPlaylistStateHolder.mPlaylistState.getPlaybackState();
        }

        public int getAudioSessionId() {
            return mPlaylistStateHolder.mPlaylistState.getAudioSessionId();
        }

        public int getBufferingPercent() {
            return mPlaylistStateHolder.mPlaylistState.getBufferingPercent();
        }

        public long getBufferingPrecentUpdateTime() {
            return mPlaylistStateHolder.mPlaylistState.getBufferingPercentUpdateTime();
        }

        public boolean isError() {
            return getErrorCode() != ErrorUtil.ERROR_NO_ERROR;
        }

        public int getErrorCode() {
            return mPlaylistStateHolder.mPlaylistState.getErrorCode();
        }

        public String getErrorMessage() {
            return mPlaylistStateHolder.mPlaylistState.getErrorMessage();
        }

        public int getPlayMode() {
            return mPlaylistStateHolder.mPlaylistState.getPlayMode();
        }

        public int getPlayPosition() {
            return mPlaylistStateHolder.mPlaylistState.getPosition();
        }

        @Override
        public void skipToNext() {
            if (!mConnected) {
                return;
            }

            mDelegate.skipToNext();
        }

        @Override
        public void skipToPrevious() {
            if (!mConnected) {
                return;
            }

            mDelegate.skipToPrevious();
        }

        @Override
        public void playOrPause(int position) {
            if (!mConnected) {
                return;
            }

            mDelegate.playOrPause(position);
        }

        @Override
        public void setPlayMode(int playMode) {
            if (!mConnected) {
                return;
            }

            mDelegate.setPlayMode(playMode);
        }

        @Override
        public void notifyPlaylistSwapped(int position, boolean playOnPrepared) {
            if (!mConnected) {
                return;
            }

            mDelegate.notifyPlaylistSwapped(position, playOnPrepared);
        }

        @Override
        public void notifyMusicItemMoved(int fromPosition, int toPosition) {
            if (!mConnected) {
                return;
            }

            mDelegate.notifyMusicItemMoved(fromPosition, toPosition);
        }

        @Override
        public void notifyMusicItemInserted(int position, int count) {
            if (!mConnected) {
                return;
            }

            mDelegate.notifyMusicItemInserted(position, count);
        }

        @Override
        public void notifyMusicItemRemoved(List<Integer> positions) {
            if (!mConnected) {
                return;
            }

            mDelegate.notifyMusicItemRemoved(positions);
        }

        @Override
        public void play() {
            if (!mConnected) {
                return;
            }

            mDelegate.play();
        }

        @Override
        public void pause() {
            if (!mConnected) {
                return;
            }

            mDelegate.pause();
        }

        @Override
        public void stop() {
            if (!mConnected) {
                return;
            }

            mDelegate.stop();
        }

        @Override
        public void playOrPause() {
            if (!mConnected) {
                return;
            }

            mDelegate.playOrPause();
        }

        @Override
        public void setLooping(boolean looping) {
            if (!mConnected) {
                return;
            }

            mDelegate.setLooping(looping);
        }

        @Override
        public void seekTo(long progress) {
            if (!mConnected) {
                return;
            }

            mDelegate.seekTo(progress);
        }

        @Override
        public void fastForward() {
            if (!mConnected) {
                return;
            }

            mDelegate.fastForward();
        }

        @Override
        public void rewind() {
            if (!mConnected) {
                return;
            }

            mDelegate.rewind();
        }

        @Override
        public void setSoundQuality(int soundQuality) {
            if (!mConnected) {
                return;
            }

            mDelegate.setSoundQuality(soundQuality);
        }

        @Override
        public void setAudioEffectEnabled(boolean enabled) {
            if (!mConnected) {
                return;
            }

            mDelegate.setAudioEffectEnabled(enabled);
        }

        @Override
        public void setOnlyWifiNetwork(boolean onlyWifiNetwork) {
            if (!mConnected) {
                return;
            }

            mDelegate.setOnlyWifiNetwork(onlyWifiNetwork);
        }

        @Override
        public void setIgnoreLossAudioFocus(boolean ignoreLossAudioFocus) {
            if (!mConnected) {
                return;
            }

            mDelegate.setIgnoreLossAudioFocus(ignoreLossAudioFocus);
        }

        public void addOnPlaybackStateChangeListener(Player.OnPlaybackStateChangeListener listener) {
            mPlaylistStateHolder.addOnPlaybackStateChangeListener(listener);
        }

        public void removeOnPlaybackStateChangeListener(Player.OnPlaybackStateChangeListener listener) {
            mPlaylistStateHolder.removeOnPlaybackStateChangeListener(listener);
        }

        public void addOnBufferingPercentChangeListener(Player.OnBufferingPercentChangeListener listener) {
            mPlaylistStateHolder.addOnBufferingPercentChangeListener(listener);
        }

        public void removeOnBufferingPercentChangeListener(Player.OnBufferingPercentChangeListener listener) {
            mPlaylistStateHolder.removeOnBufferingPercentChangeListener(listener);
        }

        public void addOnPlayingMusicItemChangeListener(Player.OnPlayingMusicItemChangeListener listener) {
            mPlaylistStateHolder.addOnPlayingMusicItemChangeListener(listener);
        }

        public void removeOnPlayingMusicItemChangeListener(Player.OnPlayingMusicItemChangeListener listener) {
            mPlaylistStateHolder.removeOnPlayingMusicItemChangeListener(listener);
        }

        public void addOnSeekCompleteListener(Player.OnSeekCompleteListener listener) {
            mPlaylistStateHolder.addOnSeekCompleteListener(listener);
        }

        public void removeOnSeekCompleteListener(Player.OnSeekCompleteListener listener) {
            mPlaylistStateHolder.removeOnSeekCompleteListener(listener);
        }

        public void addOnPlaylistChangeListener(PlaylistPlayer.OnPlaylistChangeListener listener) {
            mPlaylistStateHolder.addOnPlaylistChangeListener(listener);
        }

        public void removeOnPlaylistChangeListener(PlaylistPlayer.OnPlaylistChangeListener listener) {
            mPlaylistStateHolder.removeOnPlaylistChangeListener(listener);
        }

        public void addOnPlayModeChangeListener(PlaylistPlayer.OnPlayModeChangeListener listener) {
            mPlaylistStateHolder.addOnPlayModeChangeListener(listener);
        }

        public void removeOnPlayModeChangeListener(PlaylistPlayer.OnPlayModeChangeListener listener) {
            mPlaylistStateHolder.removeOnPlayModeChangeListener(listener);
        }

        public void addOnPositionChangeListener(PlaylistPlayer.OnPositionChangeListener listener) {
            mPlaylistStateHolder.addOnPositionChangeListener(listener);
        }

        public void removeOnPositionChangeListener(PlaylistPlayer.OnPositionChangeListener listener) {
            mPlaylistStateHolder.removeOnPositionChangeListener(listener);
        }

        private static class PlaylistManagerImp extends PlaylistManager {
            protected PlaylistManagerImp(Context context, String playlistId) {
                super(context, playlistId);
            }

            @Override
            public void setAvailable(boolean available) {
                super.setAvailable(available);
            }
        }
    }

    public static class RadioStationController implements RadioStationPlayer {
        private RadioStationPlayerChannel.Emitter mDelegate;
        private RadioStationStateHolder mRadioStationStateHolder;
        private boolean mConnected;

        RadioStationController() {
            mRadioStationStateHolder = new RadioStationStateHolder();
        }

        void setDelegate(RadioStationPlayerChannel.Emitter delegate) {
            mDelegate = delegate;
        }

        void setRadioStationState(RadioStationState radioStationState) {
            mRadioStationStateHolder.setRadioStationState(radioStationState);
        }

        RadioStationStateListener getRadioStationStateListener() {
            return mRadioStationStateHolder;
        }

        void setConnected(boolean connected) {
            mConnected = connected;
        }

        public boolean isConnected() {
            return mConnected;
        }

        public long getPlayProgress() {
            return mRadioStationStateHolder.mRadioStationState.getPlayProgress();
        }

        public long getPlayProgressUpdateTime() {
            return mRadioStationStateHolder.mRadioStationState.getPlayProgressUpdateTime();
        }

        public boolean isLooping() {
            return mRadioStationStateHolder.mRadioStationState.isLooping();
        }

        public int getSoundQuality() {
            return mRadioStationStateHolder.mRadioStationState.getSoundQuality();
        }

        public boolean isAudioEffectEnabled() {
            return mRadioStationStateHolder.mRadioStationState.isAudioEffectEnabled();
        }

        public boolean isOnlyWifiNetwork() {
            return mRadioStationStateHolder.mRadioStationState.isOnlyWifiNetwork();
        }

        public boolean isIgnoreLossAudioFocus() {
            return mRadioStationStateHolder.mRadioStationState.isIgnoreLossAudioFocus();
        }

        @Nullable
        public MusicItem getPlayingMusicItem() {
            return mRadioStationStateHolder.mRadioStationState.getMusicItem();
        }

        public int getPlaybackState() {
            return mRadioStationStateHolder.mRadioStationState.getPlaybackState();
        }

        public int getAudioSessionId() {
            return mRadioStationStateHolder.mRadioStationState.getAudioSessionId();
        }

        public int getBufferingPercent() {
            return mRadioStationStateHolder.mRadioStationState.getBufferingPercent();
        }

        public long getBufferingPrecentUpdateTime() {
            return mRadioStationStateHolder.mRadioStationState.getBufferingPercentUpdateTime();
        }

        public boolean isError() {
            return getErrorCode() != ErrorUtil.ERROR_NO_ERROR;
        }

        public int getErrorCode() {
            return mRadioStationStateHolder.mRadioStationState.getErrorCode();
        }

        public String getErrorMessage() {
            return mRadioStationStateHolder.mRadioStationState.getErrorMessage();
        }

        public RadioStation getRadioStation() {
            return mRadioStationStateHolder.mRadioStationState.getRadioStation();
        }

        @Override
        public void skipToNext() {
            if (!mConnected) {
                return;
            }

            mDelegate.skipToNext();
        }

        @Override
        public void setRadioStation(RadioStation radioStation) {
            if (!mConnected) {
                return;
            }

            mDelegate.setRadioStation(radioStation);
        }

        @Override
        public void play() {
            if (!mConnected) {
                return;
            }

            mDelegate.play();
        }

        @Override
        public void pause() {
            if (!mConnected) {
                return;
            }

            mDelegate.pause();
        }

        @Override
        public void stop() {
            if (!mConnected) {
                return;
            }

            mDelegate.stop();
        }

        @Override
        public void playOrPause() {
            if (!mConnected) {
                return;
            }

            mDelegate.playOrPause();
        }

        @Override
        public void setLooping(boolean looping) {
            if (!mConnected) {
                return;
            }

            mDelegate.setLooping(looping);
        }

        @Override
        public void seekTo(long progress) {
            if (!mConnected) {
                return;
            }

            mDelegate.seekTo(progress);
        }

        @Override
        public void fastForward() {
            if (!mConnected) {
                return;
            }

            mDelegate.fastForward();
        }

        @Override
        public void rewind() {
            if (!mConnected) {
                return;
            }

            mDelegate.rewind();
        }

        @Override
        public void setSoundQuality(int soundQuality) {
            if (!mConnected) {
                return;
            }

            mDelegate.setSoundQuality(soundQuality);
        }

        @Override
        public void setAudioEffectEnabled(boolean enabled) {
            if (!mConnected) {
                return;
            }

            mDelegate.setAudioEffectEnabled(enabled);
        }

        @Override
        public void setOnlyWifiNetwork(boolean onlyWifiNetwork) {
            if (!mConnected) {
                return;
            }

            mDelegate.setOnlyWifiNetwork(onlyWifiNetwork);
        }

        @Override
        public void setIgnoreLossAudioFocus(boolean ignoreLossAudioFocus) {
            if (!mConnected) {
                return;
            }

            mDelegate.setIgnoreLossAudioFocus(ignoreLossAudioFocus);
        }

        public void addOnPlaybackStateChangeListener(Player.OnPlaybackStateChangeListener listener) {
            mRadioStationStateHolder.addOnPlaybackStateChangeListener(listener);
        }

        public void removeOnPlaybackStateChangeListener(Player.OnPlaybackStateChangeListener listener) {
            mRadioStationStateHolder.removeOnPlaybackStateChangeListener(listener);
        }

        public void addOnBufferingPercentChangeListener(Player.OnBufferingPercentChangeListener listener) {
            mRadioStationStateHolder.addOnBufferingPercentChangeListener(listener);
        }

        public void removeOnBufferingPercentChangeListener(Player.OnBufferingPercentChangeListener listener) {
            mRadioStationStateHolder.removeOnBufferingPercentChangeListener(listener);
        }

        public void addOnPlayingMusicItemChangeListener(Player.OnPlayingMusicItemChangeListener listener) {
            mRadioStationStateHolder.addOnPlayingMusicItemChangeListener(listener);
        }

        public void removeOnPlayingMusicItemChangeListener(Player.OnPlayingMusicItemChangeListener listener) {
            mRadioStationStateHolder.removeOnPlayingMusicItemChangeListener(listener);
        }

        public void addOnSeekCompleteListener(Player.OnSeekCompleteListener listener) {
            mRadioStationStateHolder.addOnSeekCompleteListener(listener);
        }

        public void removeOnSeekCompleteListener(Player.OnSeekCompleteListener listener) {
            mRadioStationStateHolder.removeOnSeekCompleteListener(listener);
        }

        public void addOnRadioStationChangeListener(RadioStationPlayer.OnRadioStationChangeListener listener) {
            mRadioStationStateHolder.addOnRadioStationChangeListener(listener);
        }

        public void removeOnRadioStationChangeListener(RadioStationPlayer.OnRadioStationChangeListener listener) {
            mRadioStationStateHolder.removeOnRadioStationChangeListener(listener);
        }
    }

    private static class PlayerStateHolder implements PlayerStateListener {
        private PlayerState mPlayerState;
        private boolean mConnected;

        private List<Player.OnPlaybackStateChangeListener> mAllPlaybackStateChangeListener;
        private List<Player.OnBufferingPercentChangeListener> mAllBufferingPercentChangeListener;
        private List<Player.OnPlayingMusicItemChangeListener> mAllPlayingMusicItemChangeListener;
        private List<Player.OnSeekCompleteListener> mAllSeekCompleteListener;

        PlayerStateHolder() {
            mAllPlaybackStateChangeListener = new ArrayList<>();
            mAllBufferingPercentChangeListener = new ArrayList<>();
            mAllPlayingMusicItemChangeListener = new ArrayList<>();
            mAllSeekCompleteListener = new ArrayList<>();
        }

        void addOnPlaybackStateChangeListener(Player.OnPlaybackStateChangeListener listener) {
            if (mAllPlaybackStateChangeListener.contains(listener)) {
                return;
            }

            mAllPlaybackStateChangeListener.add(listener);
            notifyPlaybackStateChanged(listener);
        }

        void removeOnPlaybackStateChangeListener(Player.OnPlaybackStateChangeListener listener) {
            mAllPlaybackStateChangeListener.remove(listener);
        }

        void addOnBufferingPercentChangeListener(Player.OnBufferingPercentChangeListener listener) {
            if (mAllBufferingPercentChangeListener.contains(listener)) {
                return;
            }

            mAllBufferingPercentChangeListener.add(listener);
            notifyOnBufferingPercentChanged(listener);
        }

        void removeOnBufferingPercentChangeListener(Player.OnBufferingPercentChangeListener listener) {
            mAllBufferingPercentChangeListener.remove(listener);
        }

        void addOnPlayingMusicItemChangeListener(Player.OnPlayingMusicItemChangeListener listener) {
            if (mAllPlayingMusicItemChangeListener.contains(listener)) {
                return;
            }

            mAllPlayingMusicItemChangeListener.add(listener);
            notifyPlayingMusicItemChanged(listener);
        }

        void removeOnPlayingMusicItemChangeListener(Player.OnPlayingMusicItemChangeListener listener) {
            mAllPlayingMusicItemChangeListener.remove(listener);
        }

        void addOnSeekCompleteListener(Player.OnSeekCompleteListener listener) {
            if (mAllSeekCompleteListener.contains(listener)) {
                return;
            }

            mAllSeekCompleteListener.add(listener);
            notifySeekComplete(listener);
        }

        void removeOnSeekCompleteListener(Player.OnSeekCompleteListener listener) {
            mAllSeekCompleteListener.remove(listener);
        }

        void setPlayerState(PlayerState playerState) {
            mPlayerState = playerState;

            if (notConnect()) {
                return;
            }

            notifyPlayingMusicItemChanged();
            notifyPlaybackStateChanged();
            notifyOnBufferingPercentChanged();
        }

        boolean notConnect() {
            return !mConnected;
        }

        void setConnected(boolean connected) {
            mConnected = connected;
        }

        private void notifyPlaybackStateChanged(Player.OnPlaybackStateChangeListener listener) {
            if (notConnect()) {
                return;
            }

            switch (mPlayerState.getPlaybackState()) {
                case Player.PlaybackState.PREPARING:
                    listener.onPreparing();
                    break;
                case Player.PlaybackState.PREPARED:
                    listener.onPrepared(mPlayerState.getAudioSessionId());
                    break;
                case Player.PlaybackState.PLAYING:
                    listener.onPlay(mPlayerState.getPlayProgress(), mPlayerState.getPlayProgressUpdateTime());
                    break;
                case Player.PlaybackState.PAUSED:
                    listener.onPause();
                    break;
                case Player.PlaybackState.STOPPED:
                    listener.onStop();
                    break;
                case Player.PlaybackState.STALLED:
                    listener.onStalled();
                    break;
                case Player.PlaybackState.ERROR:
                    listener.onError(mPlayerState.getErrorCode(), mPlayerState.getErrorMessage());
                    break;
                default:
                    break;
            }
        }

        private void notifyPlaybackStateChanged() {
            if (notConnect()) {
                return;
            }

            for (Player.OnPlaybackStateChangeListener listener : mAllPlaybackStateChangeListener) {
                notifyPlaybackStateChanged(listener);
            }
        }

        private void notifyOnBufferingPercentChanged(Player.OnBufferingPercentChangeListener listener) {
            if (notConnect()) {
                return;
            }

            listener.onBufferingPercentChanged(mPlayerState.getBufferingPercent(),
                    mPlayerState.getBufferingPercentUpdateTime());
        }

        private void notifyOnBufferingPercentChanged() {
            if (notConnect()) {
                return;
            }

            for (Player.OnBufferingPercentChangeListener listener : mAllBufferingPercentChangeListener) {
                notifyOnBufferingPercentChanged(listener);
            }
        }

        private void notifyPlayingMusicItemChanged(Player.OnPlayingMusicItemChangeListener listener) {
            if (notConnect()) {
                return;
            }

            listener.onPlayingMusicItemChanged(mPlayerState.getMusicItem());
        }

        private void notifyPlayingMusicItemChanged() {
            if (notConnect()) {
                return;
            }

            for (Player.OnPlayingMusicItemChangeListener listener : mAllPlayingMusicItemChangeListener) {
                notifyPlayingMusicItemChanged(listener);
            }
        }

        private void notifySeekComplete(Player.OnSeekCompleteListener listener) {
            if (notConnect()) {
                return;
            }

            listener.onSeekComplete(mPlayerState.getPlayProgress());
        }

        private void notifySeekComplete() {
            if (notConnect()) {
                return;
            }

            for (Player.OnSeekCompleteListener listener : mAllSeekCompleteListener) {
                notifySeekComplete(listener);
            }
        }

        @Override
        public void onPreparing() {
            mPlayerState.setPlaybackState(Player.PlaybackState.PREPARING);

            notifyPlaybackStateChanged();
        }

        @Override
        public void onPrepared(int audioSessionId) {
            mPlayerState.setPlaybackState(Player.PlaybackState.PREPARED);

            notifyPlaybackStateChanged();
        }

        @Override
        public void onPlay(long playProgress, long playProgressUpdateTime) {
            mPlayerState.setPlaybackState(Player.PlaybackState.PLAYING);
            mPlayerState.setPlayProgress(playProgress);
            mPlayerState.setPlayProgressUpdateTime(playProgressUpdateTime);

            notifyPlaybackStateChanged();
        }

        @Override
        public void onPause() {
            mPlayerState.setPlaybackState(Player.PlaybackState.PAUSED);

            notifyPlaybackStateChanged();
        }

        @Override
        public void onStop() {
            mPlayerState.setPlaybackState(Player.PlaybackState.STOPPED);

            notifyPlaybackStateChanged();
        }

        @Override
        public void onStalled() {
            mPlayerState.setPlaybackState(Player.PlaybackState.STALLED);

            notifyPlaybackStateChanged();
        }

        @Override
        public void onError(int errorCode, String errorMessage) {
            mPlayerState.setPlaybackState(Player.PlaybackState.ERROR);
            mPlayerState.setErrorCode(errorCode);
            mPlayerState.setErrorMessage(errorMessage);

            notifyPlaybackStateChanged();
        }

        @Override
        public void onSeekComplete(long progress) {
            mPlayerState.setPlayProgress(progress);

            notifySeekComplete();
        }

        @Override
        public void onBufferingPercentChanged(int percent, long updateTime) {
            mPlayerState.setBufferingPercent(percent);
            mPlayerState.setBufferingPercentUpdateTime(updateTime);

            notifyOnBufferingPercentChanged();
        }

        @Override
        public void onPlayingMusicItemChanged(@Nullable MusicItem musicItem) {
            mPlayerState.setMusicItem(musicItem);

            notifyPlayingMusicItemChanged();
        }
    }

    private static class PlaylistStateHolder extends PlayerStateHolder implements PlaylistStateListener {
        private PlaylistManager mPlaylistManager;
        private PlaylistState mPlaylistState;

        private List<PlaylistPlayer.OnPlaylistChangeListener> mAllPlaylistChangeListener;
        private List<PlaylistPlayer.OnPlayModeChangeListener> mAllPlayModeChangeListener;
        private List<PlaylistPlayer.OnPositionChangeListener> mAllPositionChangeListener;

        PlaylistStateHolder(PlaylistManager playlistManager) {
            mPlaylistManager = playlistManager;

            mAllPlaylistChangeListener = new ArrayList<>();
            mAllPlayModeChangeListener = new ArrayList<>();
            mAllPositionChangeListener = new ArrayList<>();

            mPlaylistState = new PlaylistState();
        }

        void setPlaylistState(PlaylistState playlistState) {
            mPlaylistState = playlistState;

            notifyPlaylistChanged();
            notifyPlayModeChanged();
            super.setPlayerState(playlistState);
        }

        void addOnPlaylistChangeListener(PlaylistPlayer.OnPlaylistChangeListener listener) {
            if (mAllPlaylistChangeListener.contains(listener)) {
                return;
            }

            mAllPlaylistChangeListener.add(listener);
            notifyPlaylistChanged(listener);
        }

        void removeOnPlaylistChangeListener(PlaylistPlayer.OnPlaylistChangeListener listener) {
            mAllPlaylistChangeListener.remove(listener);
        }

        void addOnPlayModeChangeListener(PlaylistPlayer.OnPlayModeChangeListener listener) {
            if (mAllPlayModeChangeListener.contains(listener)) {
                return;
            }

            mAllPlayModeChangeListener.add(listener);
            notifyPlayModeChanged();
        }

        void removeOnPlayModeChangeListener(PlaylistPlayer.OnPlayModeChangeListener listener) {
            mAllPlayModeChangeListener.remove(listener);
        }

        void addOnPositionChangeListener(PlaylistPlayer.OnPositionChangeListener listener) {
            if (mAllPositionChangeListener.contains(listener)) {
                return;
            }

            mAllPositionChangeListener.add(listener);
            notifyPositionChanged(listener);
        }

        void removeOnPositionChangeListener(PlaylistPlayer.OnPositionChangeListener listener) {
            mAllPositionChangeListener.remove(listener);
        }

        private void notifyPlaylistChanged(PlaylistPlayer.OnPlaylistChangeListener listener) {
            if (notConnect()) {
                return;
            }

            listener.onPlaylistChanged(mPlaylistManager, mPlaylistState.getPosition());
        }

        private void notifyPlaylistChanged() {
            if (notConnect()) {
                return;
            }

            for (PlaylistPlayer.OnPlaylistChangeListener listener : mAllPlaylistChangeListener) {
                notifyPlaylistChanged(listener);
            }
        }

        private void notifyPlayModeChanged(PlaylistPlayer.OnPlayModeChangeListener listener) {
            if (notConnect()) {
                return;
            }

            listener.onPlayModeChanged(mPlaylistState.getPlayMode());
        }

        private void notifyPlayModeChanged() {
            if (notConnect()) {
                return;
            }

            for (PlaylistPlayer.OnPlayModeChangeListener listener : mAllPlayModeChangeListener) {
                notifyPlayModeChanged(listener);
            }
        }

        private void notifyPositionChanged(PlaylistPlayer.OnPositionChangeListener listener) {
            if (notConnect()) {
                return;
            }

            listener.onPositionChanged(mPlaylistState.getPosition());
        }

        private void notifyPositionChanged() {
            if (notConnect()) {
                return;
            }

            for (PlaylistPlayer.OnPositionChangeListener listener : mAllPositionChangeListener) {
                notifyPositionChanged(listener);
            }
        }

        @Override
        public void onPlaylistChanged(PlaylistManager playlistManager, int position) {
            mPlaylistState.setPosition(position);

            notifyPlaylistChanged();
        }

        @Override
        public void onPlayModeChanged(int playMode) {
            mPlaylistState.setPlayMode(playMode);

            notifyPlayModeChanged();
        }

        @Override
        public void onPositionChanged(int position) {
            mPlaylistState.setPosition(position);

            notifyPositionChanged();
        }
    }

    private static class RadioStationStateHolder extends PlayerStateHolder implements RadioStationStateListener {
        private RadioStationState mRadioStationState;

        private List<RadioStationPlayer.OnRadioStationChangeListener> mAllRadioStationChangeListener;

        RadioStationStateHolder() {
            mAllRadioStationChangeListener = new ArrayList<>();
            mRadioStationState = new RadioStationState();
        }

        void setRadioStationState(RadioStationState radioStationState) {
            mRadioStationState = radioStationState;

            notifyRadioStationChanged();
            super.setPlayerState(radioStationState);
        }

        void addOnRadioStationChangeListener(RadioStationPlayer.OnRadioStationChangeListener listener) {
            if (mAllRadioStationChangeListener.contains(listener)) {
                return;
            }

            mAllRadioStationChangeListener.add(listener);
            notifyRadioStationChanged(listener);
        }

        void removeOnRadioStationChangeListener(RadioStationPlayer.OnRadioStationChangeListener listener) {
            mAllRadioStationChangeListener.remove(listener);
        }

        private void notifyRadioStationChanged(RadioStationPlayer.OnRadioStationChangeListener listener) {
            if (notConnect()) {
                return;
            }

            listener.onRadioStationChanged(mRadioStationState.getRadioStation());
        }

        private void notifyRadioStationChanged() {
            if (notConnect()) {
                return;
            }

            for (RadioStationPlayer.OnRadioStationChangeListener listener : mAllRadioStationChangeListener) {
                notifyRadioStationChanged(listener);
            }
        }

        @Override
        public void onRadioStationChanged(RadioStation radioStation) {
            mRadioStationState.setRadioStation(radioStation);

            notifyRadioStationChanged();
        }
    }
}
