package snow.player;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.IBinder;
import android.widget.RemoteViews;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.google.common.base.Preconditions;
import com.tencent.mmkv.MMKV;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import channel.helper.Dispatcher;
import channel.helper.pipe.MessengerPipe;
import media.helper.MediaButtonHelper;
import snow.player.media.MediaMusicPlayer;
import snow.player.media.MusicItem;
import snow.player.media.MusicPlayer;
import snow.player.playlist.AbstractPlaylistPlayer;
import snow.player.playlist.PlaylistManager;
import snow.player.playlist.PlaylistPlayer;
import snow.player.playlist.PlaylistPlayerChannel;
import snow.player.radio.AbstractRadioStationPlayer;
import snow.player.radio.RadioStation;
import snow.player.radio.RadioStationPlayer;
import snow.player.radio.RadioStationPlayerChannel;
import snow.player.state.PersistentPlaylistState;
import snow.player.state.PersistentRadioStationState;
import snow.player.state.PlaylistState;
import snow.player.state.PlaylistStateListenerChannel;
import snow.player.state.RadioStationState;
import snow.player.state.RadioStationStateListenerChannel;

public class PlayerService extends Service implements PlayerManager {
    private static final String KEY_PLAYER_TYPE = "player_type";

    private String mPersistentId;
    private int mNotificationId;

    private PlaylistState mPlaylistState;
    private RadioStationState mRadioStationState;

    private PlaylistManager mPlaylistManager;
    private PlaylistPlayerImp mPlaylistPlayer;
    private RadioStationPlayerImp mRadioStationPlayer;
    private MessengerPipe mControllerPipe;

    private HashMap<String, OnCommandCallback> mCommandCallbackMap;

    private MMKV mMMKV;
    private int mPlayerType;
    private boolean mForeground;

    private MediaButtonHelper mMediaButtonHelper;
    private NotificationManager mNotificationManager;

    private Map<String, Runnable> mStartCommandActionMap;

    @Override
    public void onCreate() {
        super.onCreate();
        startService(new Intent(this, this.getClass()));

        mPersistentId = getPersistentId();
        mNotificationId = getNotificationId();
        mCommandCallbackMap = new HashMap<>();
        mStartCommandActionMap = new HashMap<>();

        MMKV.initialize(this);
        mMMKV = MMKV.mmkvWithID(mPersistentId);
        mPlayerType = mMMKV.decodeInt(KEY_PLAYER_TYPE, TYPE_PLAYLIST);

        mNotificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

        initPlayerState();
        initPlaylistManager();
        initPlayer();
        initControllerPipe();
        initMediaButtonHelper();
    }

    private void initPlayerState() {
        mPlaylistState = new PersistentPlaylistState(this, mPersistentId);
        mRadioStationState = new PersistentRadioStationState(this, mPersistentId);
    }

    private void initPlaylistManager() {
        mPlaylistManager = PlaylistManager.newInstance(this, mPersistentId);
    }

    private void initPlayer() {
        mPlaylistPlayer = new PlaylistPlayerImp(this, mPlaylistState, mPlaylistManager);
        mRadioStationPlayer = new RadioStationPlayerImp(this, mRadioStationState);
    }

    private void initControllerPipe() {
        final PlayerManagerChannel.Dispatcher playerManagerDispatcher
                = new PlayerManagerChannel.Dispatcher(this);

        final PlaylistPlayerChannel.Dispatcher playlistDispatcher =
                new PlaylistPlayerChannel.Dispatcher(mPlaylistPlayer);

        final RadioStationPlayerChannel.Dispatcher radioStationDispatcher =
                new RadioStationPlayerChannel.Dispatcher(mRadioStationPlayer);

        mControllerPipe = new MessengerPipe(new Dispatcher() {
            @Override
            public boolean dispatch(Map<String, Object> data) {
                if (playerManagerDispatcher.dispatch(data)) {
                    return true;
                }

                if (playlistDispatcher.dispatch(data)) {
                    notifyPlayerTypeChanged(PlayerManager.TYPE_PLAYLIST);
                    return true;
                }

                if (radioStationDispatcher.dispatch(data)) {
                    notifyPlayerTypeChanged(PlayerManager.TYPE_RADIO_STATION);
                    return true;
                }

                return false;
            }
        });
    }

    private void initMediaButtonHelper() {
        mMediaButtonHelper = new MediaButtonHelper(this, new MediaButtonHelper.MediaListener() {
            @Override
            public void onPlay() {
                play();
            }

            @Override
            public void onPause() {
                pause();
            }

            @Override
            public void onPlayPause() {
                playOrPause();
            }

            @Override
            public void onStop() {
                stop();
            }

            @Override
            public void onNext() {
                skipToNext();
            }

            @Override
            public void onPrevious() {
                skipToPrevious();
            }

            @Override
            public void onHeadsetHookClicked(int clickCount) {
                switch (clickCount) {
                    case 1:
                        onPlayPause();
                        break;
                    case 2:
                        onNext();
                        break;
                    case 3:
                        onPrevious();
                        break;
                }
            }
        });
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return mControllerPipe.getBinder();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Runnable task = mStartCommandActionMap.get(intent.getAction());
        if (task != null) {
            task.run();
        }
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        stopForegroundEx(true);

        mPlaylistPlayer.release();
        mRadioStationPlayer.release();

        mPlaylistPlayer = null;
        mRadioStationPlayer = null;
    }

    @Override
    public final void shutdown() {
        if (isPlaying()) {
            pause();
        }

        notifyOnShutdown();
    }

    @Override
    public void registerPlayerStateListener(String token, IBinder listener) {
        MessengerPipe pipe = new MessengerPipe(listener);

        addOnCommandCallback(token, new OnCommandCallbackChannel.Emitter(pipe));
        mPlaylistPlayer.addStateListener(token, new PlaylistStateListenerChannel.Emitter(pipe));
        mRadioStationPlayer.addStateListener(token, new RadioStationStateListenerChannel.Emitter(pipe));
    }

    @Override
    public void unregisterPlayerStateListener(String token) {
        removeOnConfigChangeListener(token);
        mPlaylistPlayer.removeStateListener(token);
        mRadioStationPlayer.removeStateListener(token);
    }

    protected final PendingIntent addOnStartCommandAction(@NonNull String action, @NonNull Runnable task) {
        Preconditions.checkNotNull(action);
        Preconditions.checkNotNull(task);

        mStartCommandActionMap.put(action, task);

        Context context = getApplicationContext();
        Intent intent = new Intent(context, this.getClass());
        intent.setAction(action);
        return PendingIntent.getService(context, 0, intent, 0);
    }

    protected final void removeOnStartCommandAction(@NonNull String action) {
        Preconditions.checkNotNull(action);
        mStartCommandActionMap.remove(action);
    }

    private void notifyPlayerTypeChanged(int playerType) {
        if (playerType == mPlayerType) {
            return;
        }

        mPlayerType = playerType;
        mMMKV.encode(KEY_PLAYER_TYPE, mPlayerType);

        for (String key : mCommandCallbackMap.keySet()) {
            OnCommandCallback listener = mCommandCallbackMap.get(key);
            if (listener != null) {
                listener.onPlayerTypeChanged(mPlayerType);
            }
        }
    }

    private void syncPlayerState(OnCommandCallback listener) {
        listener.syncPlayerState(mPlayerType, new PlaylistState(mPlaylistState), new RadioStationState(mRadioStationState));
    }

    private void addOnCommandCallback(@NonNull String token, @NonNull OnCommandCallback listener) {
        Preconditions.checkNotNull(token);
        Preconditions.checkNotNull(listener);

        mCommandCallbackMap.put(token, listener);
        syncPlayerState(listener);
    }

    private void removeOnConfigChangeListener(@NonNull String token) {
        Preconditions.checkNotNull(token);

        mCommandCallbackMap.remove(token);
    }

    private void notifyOnShutdown() {
        for (String key : mCommandCallbackMap.keySet()) {
            OnCommandCallback callback = mCommandCallbackMap.get(key);
            if (callback != null) {
                callback.onShutdown();
            }
        }

        mCommandCallbackMap.clear();
        stopSelf();
    }

    /**
     * 返回一个字符串 ID。该 ID 将用于对播放器的状态进行持久化。请确保该 ID 的唯一性。
     * <p>
     * 默认的 ID 值为：this.getClass().getName()
     */
    @NonNull
    protected String getPersistentId() {
        return this.getClass().getName();
    }

    /**
     * 获取 Notification 的 ID（默认返回 1）。
     */
    protected int getNotificationId() {
        return 1;
    }

    protected final int getPlayerType() {
        return mPlayerType;
    }

    protected final PlaylistPlayer getPlaylistPlayer() {
        return mPlaylistPlayer;
    }

    protected final RadioStationPlayer getRadioStationPlayer() {
        return mRadioStationPlayer;
    }

    protected final boolean isPlaying() {
        switch (mPlayerType) {
            case TYPE_PLAYLIST:
                return mPlaylistPlayer.isPlaying();
            case TYPE_RADIO_STATION:
                return mRadioStationPlayer.isPlaying();
            default:
                return false;
        }
    }

    protected final MusicItem getPlayingMusicItem() {
        switch (mPlayerType) {
            case TYPE_PLAYLIST:
                return mPlaylistState.getMusicItem();
            case TYPE_RADIO_STATION:
                return mRadioStationState.getMusicItem();
            default:
                return null;
        }
    }

    protected final boolean isError() {
        return getErrorCode() != Player.Error.NO_ERROR;
    }

    protected final int getErrorCode() {
        switch (mPlayerType) {
            case TYPE_PLAYLIST:
                return mPlaylistState.getErrorCode();
            case TYPE_RADIO_STATION:
                return mRadioStationState.getErrorCode();
            default:
                return Player.Error.NO_ERROR;
        }
    }

    protected final String getErrorMessage() {
        return Player.Error.getErrorMessage(this, getErrorCode());
    }

    protected final void invalidateNotification() {
        if (getPlayingMusicItem() == null) {
            stopForegroundEx(true);
            return;
        }

        if (isPlaying() && !isForeground()) {
            startForeground();
            return;
        }

        if (!isPlaying() && isForeground()) {
            stopForegroundEx(false);
        }

        updateNotification();
    }

    protected final boolean isForeground() {
        return mForeground;
    }

    protected final void startForeground() {
        if (hideNotification()) {
            stopForegroundEx(true);
            return;
        }

        if (getPlayingMusicItem() == null) {
            stopForegroundEx(true);
            return;
        }

        mForeground = true;
        startForeground(mNotificationId, onCreateNotification(mPlayerType));
    }

    protected final void stopForegroundEx(boolean removeNotification) {
        mForeground = false;
        stopForeground(removeNotification);
    }

    private void updateNotification() {
        if (hideNotification()) {
            stopForegroundEx(true);
            return;
        }

        if (getPlayingMusicItem() == null) {
            stopForegroundEx(true);
            return;
        }

        mNotificationManager.notify(mNotificationId, onCreateNotification(mPlayerType));
    }

    @NonNull
    protected Notification onCreateNotification(int playerType) {
        return new NotificationCompat.Builder(this, getNotificationChannelId())
                .setSmallIcon(getSmallIconId())
                .setContentTitle(getApplicationLabel())
                .setContentText(getApplicationLabel())
                .setStyle(new androidx.media.app.NotificationCompat.DecoratedMediaCustomViewStyle())
                .setCustomContentView(onCreateContentView(playerType))
                .setCustomBigContentView(onCreateBigContentView(playerType))
                .setContentIntent(getNotificationContentIntent())
                .build();
    }

    protected boolean hideNotification() {
        return false;
    }

    @DrawableRes
    protected int getSmallIconId() {
        return R.drawable.snow_ic_music;
    }

    protected String getNotificationChannelId() {
        return "player";
    }

    protected PendingIntent getNotificationContentIntent() {
        return null;
    }

    private CharSequence getApplicationLabel() {
        PackageManager pm = getPackageManager();
        ApplicationInfo info = getApplicationInfo();

        if (info == null) {
            return "unknown";
        }

        return pm.getApplicationLabel(info);
    }

    private RemoteViews onCreateContentView(int playerType) {
        if (playerType == TYPE_RADIO_STATION) {
            return onCreateRadioStationContentView();
        }

        return onCreatePlaylistContentView();
    }

    private RemoteViews onCreateBigContentView(int playerType) {
        if (playerType == TYPE_RADIO_STATION) {
            return onCreateRadioStationBigContentView();
        }

        return onCreatePlaylistBigContentView();
    }

    @NonNull
    protected RemoteViews onCreatePlaylistContentView() {
        // TODO
        return null;
    }

    @NonNull
    protected RemoteViews onCreateRadioStationContentView() {
        // TODO
        return null;
    }

    @NonNull
    protected RemoteViews onCreatePlaylistBigContentView() {
        // TODO
        return null;
    }

    @NonNull
    protected RemoteViews onCreateRadioStationBigContentView() {
        // TODO
        return null;
    }

    /**
     * 查询具有 soundQuality 音质的 MusicItem 表示的的音乐是否已被缓存。
     * <p>
     * 该方法会在异步线程中被调用。
     *
     * @param musicItem    要查询的 MusicItem 对象
     * @param soundQuality 音乐的音质
     * @return 如果已被缓存，则返回 true，否则返回 false
     */
    protected boolean isCached(MusicItem musicItem, Player.SoundQuality soundQuality) {
        return false;
    }

    /**
     * 获取已缓存的具有 soundQuality 音质的 MusicItem 表示的的音乐的 Uri。
     *
     * @param musicItem    MusicItem 对象
     * @param soundQuality 音乐的音质
     * @return 音乐的 Uri。可为 null，返回 null 时播放器会忽略本次播放。
     */
    @Nullable
    protected Uri getCachedUri(MusicItem musicItem, Player.SoundQuality soundQuality) {
        return null;
    }

    /**
     * 从网络获取具有 soundQuality 音质的 MusicItem 表示的的音乐的 Uri。
     * <p>
     * 该方法会在异步线程中被调用。
     *
     * @param musicItem    MusicItem 对象
     * @param soundQuality 音乐的音质
     * @return 音乐的 Uri。可为 null，返回 null 时播放器会忽略本次播放。
     */
    @Nullable
    protected Uri getUri(MusicItem musicItem, Player.SoundQuality soundQuality) {
        return Uri.parse(musicItem.getUri());
    }

    /**
     * 该方法会在创建 MusicPlayer 对象时调用。
     * <p>
     * 你可以重写该方法来返回你自己的 MusicPlayer 实现。
     *
     * @param uri 要播放的音乐的 uri
     * @return 音乐播放器（不能为 null）
     */
    @NonNull
    protected MusicPlayer onCreateMusicPlayer(Uri uri) throws IOException {
        MusicPlayer musicPlayer = new MediaMusicPlayer(this);
        musicPlayer.setDataSource(this, uri);
        return musicPlayer;
    }

    /**
     * 获取 “电台” 的下一首音乐。
     * <p>
     * 该方法会在异步线程中调用。
     *
     * @param radioStation 用于表示电台的 RadioStation 对象
     * @return “电台” 的下一首音乐（返回 null 时会停止播放）
     */
    @Nullable
    protected MusicItem getNextMusicItem(RadioStation radioStation) {
        return null;
    }

    private Player getPlayer() {
        if (mPlayerType == TYPE_RADIO_STATION) {
            return mRadioStationPlayer;
        }

        return mPlaylistPlayer;
    }

    protected final void play() {
        getPlayer().play();
    }

    protected final void pause() {
        getPlayer().pause();
    }

    protected final void playOrPause() {
        getPlayer().playOrPause();
    }

    protected final void stop() {
        getPlayer().stop();
    }

    protected final void skipToNext() {
        if (mPlayerType == TYPE_RADIO_STATION) {
            mRadioStationPlayer.skipToNext();
        }

        mPlaylistPlayer.skipToNext();
    }

    protected final void skipToPrevious() {
        if (mPlayerType == TYPE_RADIO_STATION) {
            return;
        }

        mPlaylistPlayer.skipToPrevious();
    }

    protected void onPreparing() {
    }

    protected void onPrepared(int audioSessionId) {
    }

    protected void onPlaying(long progress, long updateTime) {
        invalidateNotification();
    }

    protected void onPaused() {
        invalidateNotification();
    }

    protected void onStalledChanged(boolean stalled) {
    }

    protected void onStopped() {
        invalidateNotification();
    }

    protected void onError(int errorCode, String errorMessage) {
        invalidateNotification();
    }

    protected void onPlayComplete(MusicItem musicItem) {
    }

    protected void onRequestAudioFocus(boolean success) {
        if (success) {
            mMediaButtonHelper.registerMediaButtonReceiver();
        }
    }

    protected void onLossAudioFocus() {
        mMediaButtonHelper.unregisterMediaButtonReceiver();
    }

    protected void onPlayingMusicItemChanged(@Nullable MusicItem musicItem) {
        invalidateNotification();
    }

    private class PlaylistPlayerImp extends AbstractPlaylistPlayer {

        public PlaylistPlayerImp(@NonNull Context context,
                                 @NonNull PlaylistState playlistState,
                                 @NonNull PlaylistManager playlistManager) {
            super(context, playlistState, playlistManager);
        }

        @Override
        protected boolean isCached(MusicItem musicItem, SoundQuality soundQuality) {
            return PlayerService.this.isCached(musicItem, soundQuality);
        }

        @Nullable
        @Override
        protected Uri getCachedUri(MusicItem musicItem, SoundQuality soundQuality) {
            return PlayerService.this.getCachedUri(musicItem, soundQuality);
        }

        @Nullable
        @Override
        protected Uri getUri(MusicItem musicItem, SoundQuality soundQuality) {
            return PlayerService.this.getUri(musicItem, soundQuality);
        }

        @NonNull
        @Override
        protected MusicPlayer onCreateMusicPlayer(Uri uri) throws IOException {
            return PlayerService.this.onCreateMusicPlayer(uri);
        }

        @Override
        protected void onPreparing() {
            super.onPreparing();
            PlayerService.this.onPreparing();
        }

        @Override
        protected void onPrepared(int audioSessionId) {
            super.onPrepared(audioSessionId);
            PlayerService.this.onPrepared(audioSessionId);
        }

        @Override
        protected void onPlaying(long progress, long updateTime) {
            super.onPlaying(progress, updateTime);
            PlayerService.this.onPlaying(progress, updateTime);
        }

        @Override
        protected void onPaused() {
            super.onPaused();
            PlayerService.this.onPaused();
        }

        @Override
        protected void onStalledChanged(boolean stalled) {
            super.onStalledChanged(stalled);
            PlayerService.this.onStalledChanged(stalled);
        }

        @Override
        protected void onStopped() {
            super.onStopped();
            PlayerService.this.onStopped();
        }

        @Override
        protected void onError(int errorCode, String errorMessage) {
            super.onError(errorCode, errorMessage);
            PlayerService.this.onError(errorCode, errorMessage);
        }

        @Override
        protected void onPlayComplete(MusicItem musicItem) {
            super.onPlayComplete(musicItem);
            PlayerService.this.onPlayComplete(musicItem);
        }

        @Override
        protected void onRequestAudioFocus(boolean success) {
            super.onRequestAudioFocus(success);
            PlayerService.this.onRequestAudioFocus(success);
        }

        @Override
        protected void onLossAudioFocus() {
            super.onLossAudioFocus();
            PlayerService.this.onLossAudioFocus();
        }

        @Override
        protected void onPlayingMusicItemChanged(@Nullable MusicItem musicItem) {
            super.onPlayingMusicItemChanged(musicItem);
            PlayerService.this.onPlayingMusicItemChanged(musicItem);
        }
    }

    private class RadioStationPlayerImp extends AbstractRadioStationPlayer {

        public RadioStationPlayerImp(@NonNull Context context, @NonNull RadioStationState radioStationState) {
            super(context, radioStationState);
        }

        @Nullable
        @Override
        protected MusicItem getNextMusicItem(RadioStation radioStation) {
            return PlayerService.this.getNextMusicItem(radioStation);
        }

        @Override
        protected boolean isCached(MusicItem musicItem, SoundQuality soundQuality) {
            return PlayerService.this.isCached(musicItem, soundQuality);
        }

        @Nullable
        @Override
        protected Uri getCachedUri(MusicItem musicItem, SoundQuality soundQuality) {
            return PlayerService.this.getCachedUri(musicItem, soundQuality);
        }

        @Nullable
        @Override
        protected Uri getUri(MusicItem musicItem, SoundQuality soundQuality) {
            return PlayerService.this.getUri(musicItem, soundQuality);
        }

        @NonNull
        @Override
        protected MusicPlayer onCreateMusicPlayer(Uri uri) throws IOException {
            return PlayerService.this.onCreateMusicPlayer(uri);
        }

        @Override
        protected void onPreparing() {
            super.onPreparing();
            PlayerService.this.onPreparing();
        }

        @Override
        protected void onPrepared(int audioSessionId) {
            super.onPrepared(audioSessionId);
            PlayerService.this.onPrepared(audioSessionId);
        }

        @Override
        protected void onPlaying(long progress, long updateTime) {
            super.onPlaying(progress, updateTime);
            PlayerService.this.onPlaying(progress, updateTime);
        }

        @Override
        protected void onPaused() {
            super.onPaused();
            PlayerService.this.onPaused();
        }

        @Override
        protected void onStalledChanged(boolean stalled) {
            super.onStalledChanged(stalled);
            PlayerService.this.onStalledChanged(stalled);
        }

        @Override
        protected void onStopped() {
            super.onStopped();
            PlayerService.this.onStopped();
        }

        @Override
        protected void onError(int errorCode, String errorMessage) {
            super.onError(errorCode, errorMessage);
            PlayerService.this.onError(errorCode, errorMessage);
        }

        @Override
        protected void onPlayComplete(MusicItem musicItem) {
            super.onPlayComplete(musicItem);
            PlayerService.this.onPlayComplete(musicItem);
        }

        @Override
        protected void onRequestAudioFocus(boolean success) {
            super.onRequestAudioFocus(success);
            PlayerService.this.onRequestAudioFocus(success);
        }

        @Override
        protected void onLossAudioFocus() {
            super.onLossAudioFocus();
            PlayerService.this.onLossAudioFocus();
        }

        @Override
        protected void onPlayingMusicItemChanged(@Nullable MusicItem musicItem) {
            super.onPlayingMusicItemChanged(musicItem);
            PlayerService.this.onPlayingMusicItemChanged(musicItem);
        }
    }
}
