package snow.player;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.view.View;
import android.widget.RemoteViews;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.RoundedCorners;
import com.google.common.base.Preconditions;
import com.tencent.mmkv.MMKV;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import channel.helper.ChannelHelper;
import channel.helper.Dispatcher;
import channel.helper.pipe.MessengerPipe;
import io.reactivex.Single;
import io.reactivex.SingleEmitter;
import io.reactivex.SingleOnSubscribe;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Consumer;
import io.reactivex.functions.Function;
import io.reactivex.schedulers.Schedulers;
import media.helper.MediaButtonHelper;
import snow.player.media.MediaMusicPlayer;
import snow.player.media.MusicItem;
import snow.player.media.MusicPlayer;
import snow.player.playlist.AbstractPlaylistPlayer;
import snow.player.playlist.PlaylistManager;
import snow.player.playlist.PlaylistPlayer;
import snow.player.radio.AbstractRadioStationPlayer;
import snow.player.radio.RadioStation;
import snow.player.radio.RadioStationPlayer;
import snow.player.playlist.PersistentPlaylistState;
import snow.player.radio.PersistentRadioStationState;
import snow.player.playlist.PlaylistState;
import snow.player.playlist.PlaylistStateListener;
import snow.player.radio.RadioStationState;
import snow.player.radio.RadioStationStateListener;

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

    @Nullable
    private NotificationView mNotificationView;

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
        initNotificationView();
    }

    private void initPlayerState() {
        mPlaylistState = new PersistentPlaylistState(this, mPersistentId);
        mRadioStationState = new PersistentRadioStationState(this, mPersistentId);
    }

    private void initPlaylistManager() {
        mPlaylistManager = new PlaylistManager(this, mPersistentId) {
        };
    }

    private void initPlayer() {
        mPlaylistPlayer = new PlaylistPlayerImp(this, mPlaylistState, mPlaylistManager);
        mRadioStationPlayer = new RadioStationPlayerImp(this, mRadioStationState);
    }

    private void initControllerPipe() {
        final Dispatcher playerManagerDispatcher
                = ChannelHelper.newDispatcher(PlayerManager.class, this);

        final Dispatcher playlistDispatcher =
                ChannelHelper.newDispatcher(PlaylistPlayer.class, mPlaylistPlayer);

        final Dispatcher radioStationDispatcher =
                ChannelHelper.newDispatcher(RadioStationPlayer.class, mRadioStationPlayer);

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

    private void initNotificationView() {
        mNotificationView = onCreateNotificationView();

        if (noNotificationView()) {
            return;
        }

        mNotificationView.init(this,
                addOnStartCommandAction("snow.player.action.skip_to_previous", new Runnable() {
                    @Override
                    public void run() {
                        skipToPrevious();
                    }
                }),
                addOnStartCommandAction("snow.player.action.play_pause", new Runnable() {
                    @Override
                    public void run() {
                        playOrPause();
                    }
                }),
                addOnStartCommandAction("snow.player.action.skip_to_next", new Runnable() {
                    @Override
                    public void run() {
                        skipToNext();
                    }
                }),
                addOnStartCommandAction("snow.player.action.cancel", new Runnable() {
                    @Override
                    public void run() {
                        shutdown();
                    }
                }));

        MusicItem musicItem = getPlayingMusicItem();
        if (musicItem != null) {
            mNotificationView.setPlayingMusicItem(musicItem);
        }
    }

    private boolean noNotificationView() {
        return mNotificationView == null;
    }

    /***
     * 该方法用来创建 {@link NotificationView}，你可以通过覆盖该方法来实现自定义的 {@link NotificationView}。
     *
     * 该方法默认返回 {@link SimpleNotificationView}，如果你不需要 NotificationView，可以覆盖该方法并返
     * 回 null。
     *
     * @return {@link NotificationView} 对象，返回 null 时将隐藏 NotificationView
     */
    @Nullable
    protected NotificationView onCreateNotificationView() {
        return new SimpleNotificationView();
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

        if (mNotificationView != null) {
            mNotificationView.onRelease();
        }

        mPlaylistPlayer.release();
        mRadioStationPlayer.release();

        mPlaylistPlayer = null;
        mRadioStationPlayer = null;
    }

    /**
     * 关闭播放器。
     * <p>
     * 调用该方法后 Service 会要求所有已绑定的客户端断开连接，然后终止自己。
     */
    @Override
    public final void shutdown() {
        if (isPlaying()) {
            pause();
        }

        notifyOnShutdown();
        stopSelf();
    }

    @Override
    public void registerPlayerStateListener(String token, IBinder listener) {
        MessengerPipe pipe = new MessengerPipe(listener);

        addOnCommandCallback(token, ChannelHelper.newEmitter(OnCommandCallback.class, pipe));
        mPlaylistPlayer.addStateListener(token, ChannelHelper.newEmitter(PlaylistStateListener.class, pipe));
        mRadioStationPlayer.addStateListener(token, ChannelHelper.newEmitter(RadioStationStateListener.class, pipe));
    }

    @Override
    public void unregisterPlayerStateListener(String token) {
        removeOnConfigChangeListener(token);
        mPlaylistPlayer.removeStateListener(token);
        mRadioStationPlayer.removeStateListener(token);
    }

    /**
     * 添加一个自定义动作。
     * <p>
     * 该方法会返回一个 PendingIntent 对象，该 PendingIntent 对象会使用指定的 action 启动当前 Service。当
     * Service 在 {@link #onStartCommand(Intent, int, int)} 方法中检测到该 action 时，会执行其对应的 task。
     */
    protected final PendingIntent addOnStartCommandAction(@NonNull String action, @NonNull Runnable task) {
        Preconditions.checkNotNull(action);
        Preconditions.checkNotNull(task);

        mStartCommandActionMap.put(action, task);

        Context context = getApplicationContext();
        Intent intent = new Intent(context, this.getClass());
        intent.setAction(action);
        return PendingIntent.getService(context, 0, intent, 0);
    }

    /**
     * 移除一个已添加的自定义动作。
     * <p>
     * 需要指出的时，当一个自定义动作被移除后，其对应的 PendingIntent 仍然可以用来启动当前 Service，只不过
     * {@link #onStartCommand(Intent, int, int)} 不会在响应对应的 action。建议在调用该方法后同时取消注册
     * 时返回的那个 PendingIntent 对象（调用 PendingIntent 的 cancel() 方法进行取消）。
     */
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

    /**
     * 获取播放器类型。
     */
    protected final int getPlayerType() {
        return mPlayerType;
    }

    /**
     * 获取列表播放器的播放模式。
     */
    protected final PlaylistPlayer.PlayMode getPlaylistPlayMode() {
        return mPlaylistState.getPlayMode();
    }

    /**
     * 获取播放队列携带的额外参数（可为 null）。
     */
    @Nullable
    protected final Bundle getPlaylistExtra() {
        return mPlaylistPlayer.getPlaylistExtra();
    }

    /**
     * 获取电台携带的额外参数（可为 null）。
     */
    @Nullable
    protected final Bundle getRadioStationExtra() {
        return mRadioStationPlayer.getRadioStationExtra();
    }

    /**
     * 获取列表播放器。
     */
    protected final PlaylistPlayer getPlaylistPlayer() {
        return mPlaylistPlayer;
    }

    /**
     * 获取电台播放器。
     */
    protected final RadioStationPlayer getRadioStationPlayer() {
        return mRadioStationPlayer;
    }

    /**
     * 当前是否正在播放音乐。
     */
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

    /**
     * 获取当前正在播放的音乐的 MusicItem 对象。
     */
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

    /**
     * 播放器是否发生了错误。
     */
    protected final boolean isError() {
        return getErrorCode() != Player.Error.NO_ERROR;
    }

    /**
     * 获取错误码。
     * <p>
     * 该方法的返回值仅在发生错误（{@link #isError()} 方法返回 true）时才有意义。
     */
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

    /**
     * 获取错误信息。
     * <p>
     * 该方法的返回值仅在发生错误（{@link #isError()} 方法返回 true）时才有意义。
     */
    protected final String getErrorMessage() {
        return Player.Error.getErrorMessage(this, getErrorCode());
    }

    /**
     * 要求 Service 更新 NotificationView，如果没有设置 NotificationView，则忽略本次操作。
     */
    protected final void invalidateNotificationView() {
        if (noNotificationView()) {
            return;
        }

        MusicItem musicItem = getPlayingMusicItem();
        if (musicItem == null) {
            stopForegroundEx(true);
            return;
        }

        mNotificationView.setPlayingMusicItem(musicItem);

        if (isPlaying() && !isForeground()) {
            startForeground();
            return;
        }

        if (!isPlaying() && isForeground()) {
            stopForegroundEx(false);
        }

        updateNotification();
    }

    /**
     * 当前 Service 是否处于前台。
     */
    protected final boolean isForeground() {
        return mForeground;
    }

    /**
     * 启动前台 Service。
     */
    protected final void startForeground() {
        if (noNotificationView()) {
            return;
        }

        if (getPlayingMusicItem() == null) {
            stopForegroundEx(true);
            return;
        }

        mForeground = true;
        startForeground(mNotificationId, createNotification(mPlayerType));
    }

    /**
     * 停止前台 Service。
     *
     * @param removeNotification 是否清除 NotificationView
     */
    protected final void stopForegroundEx(boolean removeNotification) {
        mForeground = false;
        stopForeground(removeNotification);
    }

    private void updateNotification() {
        if (noNotificationView()) {
            return;
        }

        if (getPlayingMusicItem() == null) {
            stopForegroundEx(true);
            return;
        }

        mNotificationManager.notify(mNotificationId, createNotification(mPlayerType));
    }

    @NonNull
    private Notification createNotification(int playerType) {
        if (mNotificationView == null) {
            throw new IllegalStateException("NotificationView is null");
        }
        return mNotificationView.createNotification(playerType);
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
        musicPlayer.setDataSource(uri);
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

    /**
     * 播放。
     */
    protected final void play() {
        getPlayer().play();
    }

    /**
     * 暂停。
     */
    protected final void pause() {
        getPlayer().pause();
    }

    /**
     * 播放/暂停。
     */
    protected final void playOrPause() {
        getPlayer().playOrPause();
    }

    /**
     * 停止。
     */
    protected final void stop() {
        getPlayer().stop();
    }

    /**
     * 下一曲。
     */
    protected final void skipToNext() {
        if (mPlayerType == TYPE_RADIO_STATION) {
            mRadioStationPlayer.skipToNext();
            return;
        }

        mPlaylistPlayer.skipToNext();
    }

    /**
     * 上一曲。
     * <p>
     * 但播放器类型为 {@link PlayerManager#TYPE_RADIO_STATION}（电台模式）时，该方法无效。
     */
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
        invalidateNotificationView();
    }

    protected void onPaused() {
        invalidateNotificationView();
    }

    protected void onStalledChanged(boolean stalled) {
    }

    protected void onStopped() {
        invalidateNotificationView();
    }

    protected void onError(int errorCode, String errorMessage) {
        invalidateNotificationView();
    }

    protected void onPlayComplete(MusicItem musicItem) {
        invalidateNotificationView();
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
        invalidateNotificationView();
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

    /**
     * 用于显示通知栏控制器。
     */
    public static abstract class NotificationView {
        private PlayerService mPlayerService;

        private MusicItem mMusicItem;
        private boolean mMusicItemChanged;

        private PendingIntent mSkipToPrevious;
        private PendingIntent mPlayOrPause;
        private PendingIntent mSkipToNext;
        private PendingIntent mCancel;

        void init(PlayerService playerService,
                  PendingIntent skipPrevious,
                  PendingIntent playOrPause,
                  PendingIntent skipNext,
                  PendingIntent cancel) {
            mMusicItem = new MusicItem();
            mMusicItemChanged = false;

            mPlayerService = playerService;
            mSkipToPrevious = skipPrevious;
            mPlayOrPause = playOrPause;
            mSkipToNext = skipNext;
            mCancel = cancel;

            onInit(mPlayerService);
        }

        /**
         * 重写该方法创建一个新的 Notification 对象。
         *
         * @param playerType 播放器类型
         * @return Notification 对象，不能为 null。
         */
        @NonNull
        public abstract Notification onCreateNotification(int playerType);

        /**
         * 该方法会在初次创建 NotificationView 对象时调用，你可以重写该方法来进行一些初始化操作。
         */
        protected void onInit(Context context) {
        }

        /**
         * 该方法会在 Service 销毁时调用，可以在该方法中是否占用的资源。
         */
        protected void onRelease() {
        }

        protected final Context getContext() {
            return mPlayerService;
        }

        protected final String getPackageName() {
            return getContext().getPackageName();
        }

        /**
         * 获取播放器类型。
         */
        protected final int getPlayerType() {
            return mPlayerService.getPlayerType();
        }

        /**
         * 获取列表播放器的播放模式。
         */
        protected final PlaylistPlayer.PlayMode getPlaylistPlayMode() {
            return mPlayerService.getPlaylistPlayMode();
        }

        /**
         * 获取播放队列携带的额外参数（可为 null）。
         */
        @Nullable
        protected final Bundle getPlaylistExtra() {
            return mPlayerService.getPlaylistExtra();
        }

        /**
         * 获取电台携带的额外参数（可为 null）。
         */
        @Nullable
        protected final Bundle getRadioStationExtra() {
            return mPlayerService.getRadioStationExtra();
        }

        /**
         * 添加一个自定义动作。
         * <p>
         * 该方法会返回一个 PendingIntent 对象，该 PendingIntent 对象会使用指定的 action 启动当前 Service。当
         * Service 在 {@link #onStartCommand(Intent, int, int)} 方法中检测到该 action 时，会执行其对应的 task。
         */
        protected final PendingIntent addCustomAction(@NonNull String action, @NonNull Runnable task) {
            return mPlayerService.addOnStartCommandAction(action, task);
        }

        /**
         * 移除一个已添加的自定义动作。
         * <p>
         * 需要指出的时，当一个自定义动作被移除后，其对应的 PendingIntent 仍然可以用来启动当前 Service，只不过
         * {@link #onStartCommand(Intent, int, int)} 不会在响应对应的 action。建议在调用该方法后同时取消注册
         * 时返回的那个 PendingIntent 对象（调用 PendingIntent 的 cancel() 方法进行取消）。
         */
        protected final void removeCustomAction(@NonNull String action) {
            mPlayerService.removeOnStartCommandAction(action);
        }

        /**
         * 获取用来触发 “上一曲” 的 PendingIntent 对象。
         */
        protected final PendingIntent getSkipToPreviousPendingIntent() {
            return mSkipToPrevious;
        }

        /**
         * 获取用来触发 “播放/暂停” 的 PendingIntent 对象。
         */
        protected final PendingIntent getPlayOrPausePendingIntent() {
            return mPlayOrPause;
        }

        /**
         * 获取用来触发 “下一曲” 的 PendingIntent 对象。
         */
        protected final PendingIntent getSkipToNextPendingIntent() {
            return mSkipToNext;
        }

        /**
         * 获取用来触发 “shutdown” 的 PendingIntent 对象。
         */
        protected final PendingIntent getCancelPendingIntent() {
            return mCancel;
        }

        /**
         * 当前是否正在播放音乐。
         */
        public final boolean isPlaying() {
            return mPlayerService.isPlaying();
        }

        /**
         * 获取当前正在播放的音乐的 MusicItem 对象。
         */
        @NonNull
        public final MusicItem getPlayingMusicItem() {
            return mMusicItem;
        }

        /**
         * 播放器是否发生了错误。
         */
        public final boolean isError() {
            return mPlayerService.isError();
        }

        /**
         * 获取错误信息。
         * <p>
         * 该方法的返回值仅在发生错误（{@link #isError()} 方法返回 true）时才有意义。
         */
        @NonNull
        public final String getErrorMessage() {
            return mPlayerService.getErrorMessage();
        }

        /**
         * 要求 Service 更新 NotificationView，如果没有设置 NotificationView，则忽略本次操作。
         */
        public final void invalidate() {
            mPlayerService.invalidateNotificationView();
        }

        /**
         * 正在播放的音乐是否已改变。
         */
        protected final boolean isMusicItemChanged() {
            return mMusicItemChanged;
        }

        @NonNull
        Notification createNotification(int playerType) {
            return onCreateNotification(playerType);
        }

        void setPlayingMusicItem(@NonNull MusicItem musicItem) {
            Preconditions.checkNotNull(musicItem);

            mMusicItemChanged = !(mMusicItem.equals(musicItem));
            mMusicItem = musicItem;
        }
    }

    /**
     * 默认的 NotificationView 实现。
     */
    public static class SimpleNotificationView extends NotificationView {
        private Bitmap mDefaultIcon;
        private Bitmap mIcon;

        private Disposable mLoadIconDisposable;
        private Canvas mCanvas;

        @Override
        protected void onInit(Context context) {
            super.onInit(context);

            mCanvas = new Canvas();
            setDefaultIcon(BitmapFactory.decodeResource(context.getResources(), R.mipmap.snow_notif_default_icon));
        }

        @NonNull
        @Override
        public Notification onCreateNotification(int playerType) {
            if (isMusicItemChanged()) {
                reloadAllIcon();
            }

            return new NotificationCompat.Builder(getContext(), "player")
                    .setSmallIcon(R.drawable.snow_ic_music)
                    .setStyle(new androidx.media.app.NotificationCompat.DecoratedMediaCustomViewStyle())
                    .setCustomContentView(onCreateContentView(playerType))
                    .setCustomBigContentView(onCreateBigContentView(playerType))
                    .setContentIntent(getContentIntent())
                    .build();
        }

        @Override
        protected void onRelease() {
            super.onRelease();

            disposeLastLoadIcon();
        }

        protected void disposeLastLoadIcon() {
            if (mLoadIconDisposable != null) {
                mLoadIconDisposable.dispose();
            }
        }

        protected PendingIntent getContentIntent() {
            return null;
        }

        protected final void setDefaultIcon(@NonNull Bitmap defaultIcon) {
            Preconditions.checkNotNull(defaultIcon);
            mDefaultIcon = defaultIcon;
        }

        public final Bitmap getDefaultIcon() {
            return mDefaultIcon;
        }

        public final Bitmap getIcon() {
            if (mIcon == null) {
                return mDefaultIcon;
            }

            return mIcon;
        }

        public final void setIcon(@NonNull Bitmap icon) {
            mIcon = icon;
            invalidate();
        }

        protected CharSequence getContentTitle() {
            return getPlayingMusicItem().getTitle();
        }

        protected CharSequence getContentText() {
            if (isError()) {
                String errorMessage = getErrorMessage();
                SpannableString text = new SpannableString(errorMessage);
                text.setSpan(new ForegroundColorSpan(Color.RED), 0, errorMessage.length(), Spanned.SPAN_INCLUSIVE_EXCLUSIVE);
                return text;
            }

            return getPlayingMusicItem().getArtist();
        }

        protected void reloadAllIcon() {
            disposeLastLoadIcon();

            mLoadIconDisposable = loadIconAsync()
                    .observeOn(AndroidSchedulers.mainThread())
                    .map(asRoundRect())
                    .subscribe(new Consumer<Bitmap>() {
                        @Override
                        public void accept(Bitmap bitmap) {
                            setIcon(bitmap);
                        }
                    });
        }

        private Single<Bitmap> loadIconAsync() {
            return Single.create(new SingleOnSubscribe<Bitmap>() {
                @Override
                public void subscribe(SingleEmitter<Bitmap> emitter) {
                    String iconUri = getPlayingMusicItem().getIconUri();
                    Bitmap icon = null;

                    int bigIconSize = getContext().getResources()
                            .getDimensionPixelSize(R.dimen.snow_notif_icon_size_big);

                    if (isAvailable(iconUri)) {
                        icon = loadIconFromInternet(iconUri, bigIconSize);
                    }

                    if (emitter.isDisposed()) {
                        return;
                    }

                    if (icon == null) {
                        icon = getMusicItemEmbeddedIcon(bigIconSize);
                    }

                    if (emitter.isDisposed()) {
                        return;
                    }

                    if (icon == null) {
                        icon = getDefaultIcon();
                    }

                    emitter.onSuccess(icon);
                }
            }).subscribeOn(Schedulers.io());
        }

        @Nullable
        protected final Bitmap getMusicItemEmbeddedIcon(int size) {
            MusicItem musicItem = getPlayingMusicItem();

            MediaMetadataRetriever retriever = new MediaMetadataRetriever();
            retriever.setDataSource(musicItem.getUri());

            try {
                return Glide.with(getContext())
                        .asBitmap()
                        .load(retriever.getEmbeddedPicture())
                        .submit(size, size)
                        .get();
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                retriever.release();
            }

            return null;
        }

        private Function<Bitmap, Bitmap> asRoundRect() {
            return new Function<Bitmap, Bitmap>() {
                @Override
                public Bitmap apply(Bitmap bitmap) throws ExecutionException, InterruptedException {
                    Resources res = getContext().getResources();

                    int size = res.getDimensionPixelSize(R.dimen.snow_notif_icon_size_big);
                    int cornerRadius = res.getDimensionPixelSize(R.dimen.snow_notif_icon_corner_radius);

                    Bitmap icon = Bitmap.createBitmap(size, size, bitmap.getConfig());

                    mCanvas.setBitmap(icon);
                    Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
                    paint.setStyle(Paint.Style.FILL);

                    float left = (float) ((size - bitmap.getWidth()) / 2.0);
                    float top = (float) ((size - bitmap.getHeight()) / 2.0);

                    mCanvas.drawBitmap(bitmap, left, top, paint);

                    return Glide.with(getContext())
                            .asBitmap()
                            .load(icon)
                            .transform(new RoundedCorners(cornerRadius))
                            .submit()
                            .get();
                }
            };
        }

        private Bitmap loadIconFromInternet(String iconUri, int iconSize) {
            try {
                return Glide.with(getContext())
                        .asBitmap()
                        .load(iconUri)
                        .submit(iconSize, iconSize)
                        .get();
            } catch (Exception e) {
                e.printStackTrace();
            }

            return null;
        }

        protected final boolean isAvailable(String stringUri) {
            return stringUri != null && (!stringUri.isEmpty());
        }

        protected RemoteViews onCreateContentView(int playerType) {
            RemoteViews contentView = new RemoteViews(getPackageName(),
                    R.layout.snow_simple_notification_view);

            contentView.setImageViewBitmap(R.id.snow_notif_icon, getIcon());
            contentView.setTextViewText(R.id.snow_notif_title, getContentTitle());
            contentView.setTextViewText(R.id.snow_notif_text, getContentText());

            contentView.setOnClickPendingIntent(R.id.snow_notif_play_pause, getPlayOrPausePendingIntent());
            contentView.setOnClickPendingIntent(R.id.snow_notif_skip_to_next, getSkipToNextPendingIntent());
            contentView.setOnClickPendingIntent(R.id.snow_notif_shutdown, getCancelPendingIntent());

            if (isPlaying()) {
                contentView.setImageViewResource(R.id.snow_notif_play_pause, R.drawable.snow_ic_pause);
            }

            if (playerType == TYPE_RADIO_STATION) {
                contentView.setViewVisibility(R.id.snow_notif_radio_tip, View.VISIBLE);
            }

            return contentView;
        }

        protected RemoteViews onCreateBigContentView(int playerType) {
            RemoteViews bigContentView = new RemoteViews(getPackageName(),
                    R.layout.snow_simple_notification_view_big);

            bigContentView.setImageViewBitmap(R.id.snow_notif_icon, getIcon());
            bigContentView.setTextViewText(R.id.snow_notif_title, getContentTitle());
            bigContentView.setTextViewText(R.id.snow_notif_text, getContentText());

            bigContentView.setOnClickPendingIntent(R.id.snow_notif_play_pause, getPlayOrPausePendingIntent());
            bigContentView.setOnClickPendingIntent(R.id.snow_notif_skip_to_next, getSkipToNextPendingIntent());
            bigContentView.setOnClickPendingIntent(R.id.snow_notif_shutdown, getCancelPendingIntent());

            if (isPlaying()) {
                bigContentView.setImageViewResource(R.id.snow_notif_play_pause, R.drawable.snow_ic_pause);
            }

            if (playerType == TYPE_RADIO_STATION) {
                bigContentView.setViewVisibility(R.id.snow_notif_radio_tip, View.VISIBLE);
                bigContentView.setImageViewResource(R.id.snow_notif_skip_to_previous, R.drawable.snow_ic_skip_previous_disabled);
            } else {
                bigContentView.setOnClickPendingIntent(R.id.snow_notif_skip_to_previous, getSkipToPreviousPendingIntent());
            }

            return bigContentView;
        }
    }
}
