package snow.player;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.media.AudioManager;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.PowerManager;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.google.common.base.Preconditions;

import java.util.HashMap;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import io.reactivex.Observable;
import io.reactivex.Single;
import io.reactivex.SingleEmitter;
import io.reactivex.SingleOnSubscribe;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Consumer;
import io.reactivex.schedulers.Schedulers;
import media.helper.AudioFocusHelper;
import media.helper.BecomeNoiseHelper;
import snow.player.appwidget.AppWidgetPreferences;
import snow.player.media.MusicItem;
import snow.player.media.MusicPlayer;
import snow.player.playlist.Playlist;
import snow.player.playlist.PlaylistManager;
import snow.player.util.ErrorUtil;
import snow.player.helper.NetworkHelper;

/**
 * 该类实现了 {@link Player} 接口，并实现大部分音乐播放器功能。
 */
public abstract class AbstractPlayer implements Player {
    private static final String TAG = "AbstractPlayer";
    private static final int FORWARD_STEP = 15_000;     // 15 秒, 单位：毫秒 ms

    private Context mApplicationContext;
    private PlayerConfig mPlayerConfig;
    private PlayerState mPlayerState;
    private PlayerStateHelper mPlayerStateHelper;
    private HashMap<String, PlayerStateListener> mStateListenerMap;

    private MusicPlayer.OnPreparedListener mPreparedListener;
    private MusicPlayer.OnCompletionListener mCompletionListener;
    private MusicPlayer.OnSeekCompleteListener mSeekCompleteListener;
    private MusicPlayer.OnStalledListener mStalledListener;
    private MusicPlayer.OnBufferingUpdateListener mBufferingUpdateListener;
    private MusicPlayer.OnErrorListener mErrorListener;

    private AudioFocusHelper mAudioFocusHelper;
    private BecomeNoiseHelper mBecomeNoiseHelper;
    private NetworkHelper mNetworkHelper;

    private MusicPlayer mMusicPlayer;

    private boolean mLoadingPlaylist;

    private boolean mPlayOnPrepared;
    private boolean mPlayOnSeekComplete;
    private Runnable mPreparedAction;
    private Runnable mSeekCompleteAction;
    private Runnable mPlaylistLoadedAction;

    private PlaylistManager mPlaylistManager;
    private Playlist mPlaylist;

    private Random mRandom;
    private Disposable mRetrieveUriDisposable;

    private boolean mReleased;

    private boolean mRecordProgress;
    private Disposable mRecordProgressDisposable;
    private Disposable mCheckCachedDisposable;

    private MediaSessionCompat mMediaSession;
    private PlaybackStateCompat.Builder mPlaybackStateBuilder;
    private MediaMetadataCompat.Builder mMediaMetadataBuilder;

    private PowerManager.WakeLock mWakeLock;
    private WifiManager.WifiLock mWifiLock;

    /**
     * 创建一个 {@link AbstractPlayer} 对象。
     *
     * @param context         {@link Context} 对象，不能为 null
     * @param playerConfig    {@link PlayerConfig} 对象，保存了播放器的初始配置信息，不能为 null
     * @param playerState     {@link PlayerState} 对象，保存了播放器的初始状态，不能为 null
     * @param playlistManager {@link PlaylistManager} 对象，用于管理播放列表，不能为 null
     */
    public AbstractPlayer(@NonNull Context context,
                          @NonNull PlayerConfig playerConfig,
                          @NonNull PlayerState playerState,
                          @NonNull PlaylistManager playlistManager,
                          @Nullable AppWidgetPreferences pref) {
        Preconditions.checkNotNull(context);
        Preconditions.checkNotNull(playerConfig);
        Preconditions.checkNotNull(playerState);
        Preconditions.checkNotNull(playlistManager);

        mApplicationContext = context.getApplicationContext();
        mPlayerConfig = playerConfig;
        mPlayerState = playerState;
        mPlayerStateHelper = new PlayerStateHelper(mPlayerState, pref);
        mPlaylistManager = playlistManager;
        mStateListenerMap = new HashMap<>();
        mRecordProgress = true;

        initAllListener();
        initAllHelper();
        initWakeLock();

        mNetworkHelper.subscribeNetworkState();
        reloadPlaylist();
        prepareMusicPlayer(false, null);
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
    protected abstract boolean isCached(MusicItem musicItem, SoundQuality soundQuality);

    /**
     * 该方法会在创建 MusicPlayer 对象时调用。
     * <p>
     * 你可以重写该方法来返回你自己的 MusicPlayer 实现。
     */
    @NonNull
    protected abstract MusicPlayer onCreateMusicPlayer(Context context);

    /**
     * 获取音乐的播放链接。
     * <p>
     * 该方法会在异步线程中执行，因此可以执行各种耗时操作，例如访问网络。
     *
     * @param musicItem    要播放的音乐
     * @param soundQuality 要播放的音乐的音质
     * @return 音乐的播放链接
     * @throws Exception 获取音乐播放链接的过程中发生的任何异常
     */
    @Nullable
    protected abstract Uri retrieveMusicItemUri(@NonNull MusicItem musicItem, @NonNull SoundQuality soundQuality) throws Exception;

    /**
     * 对指定的 audio session id 应用音频特效。
     * <p>
     * 子类可以覆盖该方法来对指定的 audio session id 应用音频特效。
     *
     * @param audioSessionId 当前正在播放的音乐的 audio session id。如果为 0，则可以忽略。
     */
    protected void attachAudioEffect(int audioSessionId) {
    }

    /**
     * 取消当前的音频特效。
     * <p>
     * 子类可以覆盖该方法来取消指定的 audio session id 的音频特效。
     */
    protected void detachAudioEffect() {
    }

    /**
     * 该方法会在开始准备音乐播放器时调用。
     */
    protected void onPreparing() {
    }

    /**
     * 该方法会在音乐播放器准备完毕后调用。
     *
     * @param audioSessionId 当前正准备播放的音乐的 audio session id。
     */
    protected void onPrepared(int audioSessionId) {
    }

    /**
     * 该方法会在开始播放时调用。
     *
     * @param progress   当前的播放进度。
     * @param updateTime 播放进度的更新时间。
     */
    protected void onPlaying(int progress, long updateTime) {
    }

    /**
     * 该方法会在暂停播放时调用。
     */
    protected void onPaused() {
    }

    /**
     * 该方法会在 stalled 暂停改变时调用。
     * <p>
     * 你可以根据该 stalled 参数的值来显示或隐藏缓冲进度条。如果缓冲区没有足够的数据支撑继续播放时，则该参数为
     * true，当缓冲区缓存了足够的数据可以继续播放时，该参数为 false。
     *
     * @param stalled 如果缓冲区没有足够的数据继续播放时，则该参数为 true，当缓冲区缓存了足够的数据可以继续
     *                播放时，该参数为 false。
     */
    protected void onStalledChanged(boolean stalled) {
    }

    /**
     * 该方法会在停止播放时调用。
     */
    protected void onStopped() {
    }

    /**
     * 该方法会在错误发生时调用。
     *
     * @param errorCode    错误码
     * @param errorMessage 错误信息
     * @see ErrorUtil
     */
    protected void onError(int errorCode, String errorMessage) {
    }

    /**
     * 该方法会在当前播放的 MusicItem 对象改变时调用。
     *
     * @param musicItem 本次要播放的 MusicItem 对象（可能为 null）。
     */
    protected void onPlayingMusicItemChanged(@Nullable MusicItem musicItem) {
    }

    /**
     * 释放播放器所占用的资源。注意！调用该方法后，就不允许在使用当前 Player 对象了，否则会导致不可预见的错误。
     */
    public void release() {
        mReleased = true;
        disposeRetrieveUri();
        releaseMusicPlayer();
        releaseWakeLock();

        mStateListenerMap.clear();

        mAudioFocusHelper.abandonAudioFocus();
        mBecomeNoiseHelper.unregisterBecomeNoiseReceiver();
        mNetworkHelper.unsubscribeNetworkState();

        mAudioFocusHelper = null;
        mBecomeNoiseHelper = null;
        mNetworkHelper = null;

        mPreparedAction = null;
        mSeekCompleteAction = null;
        mPlaylistLoadedAction = null;
    }

    /**
     * 设置是否实时记录播放进度。
     *
     * @param enable 是否实时记录播放进度（默认为 true）。为 true 时，会在播放时每隔 3 秒将歌曲的播放进度记
     *               录到本地磁盘，这样即使应用被突然终止，下次播放时也能自动恢复到与上次播放相近的播放进度。
     */
    public final void setRecordProgress(boolean enable) {
        mRecordProgress = enable;
    }

    /**
     * 当前正在播放的音乐。
     *
     * @return 如果没有正在播放的音乐，则返回 null
     */
    @Nullable
    protected final MusicItem getMusicItem() {
        return mPlayerState.getMusicItem();
    }

    /**
     * 准备当前播放器所持有的 {@link MusicItem} 对象（测试用）。
     *
     * @param preparedAction 在音乐播放器准备完成后要执行的操作
     */
    private void prepareMusicPlayer(boolean playOnPrepared, @Nullable Runnable preparedAction) {
        releaseMusicPlayer();
        disposeRetrieveUri();

        MusicItem musicItem = mPlayerState.getMusicItem();
        if (musicItem == null) {
            return;
        }

        if (mPlayerConfig.isOnlyWifiNetwork() && !isWiFiNetwork()) {
            onError(ErrorUtil.ONLY_WIFI_NETWORK, ErrorUtil.getErrorMessage(mApplicationContext, ErrorUtil.ONLY_WIFI_NETWORK));
            return;
        }

        mPlayOnPrepared = playOnPrepared;
        mRetrieveUriDisposable = getMusicItemUri(musicItem, mPlayerConfig.getSoundQuality())
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(prepare(preparedAction), notifyGetUrlFailed());
    }

    private void disposeRetrieveUri() {
        if (mRetrieveUriDisposable != null && !mRetrieveUriDisposable.isDisposed()) {
            mRetrieveUriDisposable.dispose();
        }
    }

    private Single<Uri> getMusicItemUri(@NonNull final MusicItem musicItem, @NonNull final SoundQuality soundQuality) {
        return Single.create(new SingleOnSubscribe<Uri>() {
            @Override
            public void subscribe(SingleEmitter<Uri> emitter) {
                Uri uri = null;

                try {
                    uri = retrieveMusicItemUri(musicItem, soundQuality);
                } catch (Exception e) {
                    emitter.onError(e);
                }

                if (emitter.isDisposed()) {
                    return;
                }

                if (uri == null) {
                    emitter.onError(new IllegalStateException("get uri failed."));
                    return;
                }

                emitter.onSuccess(uri);
            }
        });
    }

    private Consumer<Uri> prepare(final @Nullable Runnable preparedAction) {
        return new Consumer<Uri>() {
            @Override
            public void accept(Uri uri) {
                mMusicPlayer = onCreateMusicPlayer(mApplicationContext);
                attachListeners(mMusicPlayer);

                mPreparedAction = preparedAction;
                notifyPreparing();

                try {
                    onPrepareMusicPlayer(mMusicPlayer, uri);
                } catch (Exception e) {
                    e.printStackTrace();
                    notifyError(ErrorUtil.DATA_LOAD_FAILED, ErrorUtil.getErrorMessage(mApplicationContext, ErrorUtil.DATA_LOAD_FAILED));
                }
            }
        };
    }

    private void onPrepareMusicPlayer(MusicPlayer musicPlayer, Uri uri) throws Exception {
        if (!musicPlayer.isInvalid()) {
            musicPlayer.prepare(uri);
        }
    }

    private Consumer<Throwable> notifyGetUrlFailed() {
        return new Consumer<Throwable>() {
            @Override
            public void accept(Throwable throwable) {
                throwable.printStackTrace();
                notifyError(ErrorUtil.GET_URL_FAILED, ErrorUtil.getErrorMessage(mApplicationContext, ErrorUtil.GET_URL_FAILED));
            }
        };
    }

    private boolean isWiFiNetwork() {
        return mNetworkHelper.isWifiNetwork();
    }

    private void initAllListener() {
        mPreparedListener = new MusicPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(MusicPlayer mp) {
                if (mReleased) {
                    return;
                }

                mp.setLooping(isLooping());

                if (mPlayerConfig.isAudioEffectEnabled()) {
                    attachAudioEffect(mp.getAudioSessionId());
                }

                notifyPrepared(mp.getAudioSessionId());

                if (mPlayerState.getPlayProgress() > 0) {
                    mPlayOnSeekComplete = mPlayOnPrepared;
                    mPlayOnPrepared = false;
                    seekTo(mPlayerState.getPlayProgress(), mPreparedAction);
                    mPreparedAction = null;
                    return;
                }

                if (mPlayOnPrepared) {
                    mPlayOnPrepared = false;
                    play();
                } else if (mPreparedAction == null) {
                    notifyPaused();
                }

                if (mPreparedAction != null) {
                    mPreparedAction.run();
                    mPreparedAction = null;
                }
            }
        };

        mCompletionListener = new MusicPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MusicPlayer mp) {
                if (mPlayerState.getPlayMode() == PlayMode.LOOP) {
                    return;
                }

                skipToNext();
            }
        };

        mSeekCompleteListener = new MusicPlayer.OnSeekCompleteListener() {
            @Override
            public void onSeekComplete(MusicPlayer mp) {
                if (mReleased) {
                    return;
                }

                notifySeekComplete(mp.getProgress());

                if (isPlaying()) {
                    mMediaSession.setPlaybackState(buildPlaybackState(PlaybackStateCompat.STATE_PLAYING));
                } else if (!mPlayOnSeekComplete) {
                    notifyPaused();
                }

                if (mPlayOnSeekComplete) {
                    mPlayOnSeekComplete = false;
                    play();
                }

                if (mSeekCompleteAction != null) {
                    mSeekCompleteAction.run();
                    mSeekCompleteAction = null;
                }
            }
        };

        mStalledListener = new MusicPlayer.OnStalledListener() {
            @Override
            public void onStalled(boolean stalled) {
                notifyStalled(stalled, mMusicPlayer.getProgress(), System.currentTimeMillis());
            }
        };

        mBufferingUpdateListener = new MusicPlayer.OnBufferingUpdateListener() {
            @Override
            public void onBufferingUpdate(MusicPlayer mp, int buffered, boolean isPercent) {
                notifyBufferedChanged(buffered, isPercent);
            }
        };

        mErrorListener = new MusicPlayer.OnErrorListener() {
            @Override
            public void onError(MusicPlayer mp, int errorCode) {
                Log.e("MusicPlayer", "errorCode:" + errorCode);

                notifyError(errorCode, ErrorUtil.getErrorMessage(mApplicationContext, errorCode));
            }
        };
    }

    private void initAllHelper() {
        mAudioFocusHelper = new AudioFocusHelper(mApplicationContext, new AudioFocusHelper.OnAudioFocusChangeListener() {
            private boolean playing;

            @Override
            public void onLoss() {
                pause();
            }

            @Override
            public void onLossTransient() {
                playing = isPlaying();
                pause();
            }

            @Override
            public void onLossTransientCanDuck() {
                playing = isPlaying();
                if (playing) {
                    mMusicPlayer.quiet();
                }
            }

            @Override
            public void onGain(boolean lossTransient, boolean lossTransientCanDuck) {
                if (!playing) {
                    return;
                }

                if (lossTransient) {
                    play();
                    return;
                }

                if (lossTransientCanDuck && isPlaying()) {
                    mMusicPlayer.dismissQuiet();
                }
            }
        });

        mBecomeNoiseHelper = new BecomeNoiseHelper(mApplicationContext, new BecomeNoiseHelper.OnBecomeNoiseListener() {
            @Override
            public void onBecomeNoise() {
                pause();
            }
        });

        mNetworkHelper = NetworkHelper.newInstance(mApplicationContext, new NetworkHelper.OnNetworkStateChangeListener() {
            @Override
            public void onNetworkStateChanged(boolean connected, boolean wifiNetwork) {
                if (!isPrepared()) {
                    return;
                }

                checkNetworkType(mPlayerConfig.isOnlyWifiNetwork(), wifiNetwork);
            }
        });
    }

    public final void setMediaSession(MediaSessionCompat mediaSession) {
        initMediaMetadataBuilder();
        initPlaybackStateBuilder();

        mMediaSession = mediaSession;
        mMediaSession.setPlaybackState(buildPlaybackState(PlaybackStateCompat.STATE_NONE));
        mMediaSession.setMetadata(buildMediaMetadata());
    }

    private void initPlaybackStateBuilder() {
        mPlaybackStateBuilder = new PlaybackStateCompat.Builder()
                .setActions(PlaybackStateCompat.ACTION_PLAY |
                        PlaybackStateCompat.ACTION_PAUSE |
                        PlaybackStateCompat.ACTION_PLAY_PAUSE |
                        PlaybackStateCompat.ACTION_STOP |
                        PlaybackStateCompat.ACTION_SKIP_TO_NEXT |
                        PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS |
                        PlaybackStateCompat.ACTION_SKIP_TO_QUEUE_ITEM |
                        PlaybackStateCompat.ACTION_SET_REPEAT_MODE |
                        PlaybackStateCompat.ACTION_SET_SHUFFLE_MODE |
                        PlaybackStateCompat.ACTION_FAST_FORWARD |
                        PlaybackStateCompat.ACTION_REWIND |
                        PlaybackStateCompat.ACTION_SEEK_TO);
    }

    private void initMediaMetadataBuilder() {
        mMediaMetadataBuilder = new MediaMetadataCompat.Builder();
    }

    private void initWakeLock() {
        PowerManager pm = (PowerManager) mApplicationContext.getSystemService(Context.POWER_SERVICE);
        WifiManager wm = (WifiManager) mApplicationContext.getApplicationContext().getSystemService(Context.WIFI_SERVICE);

        String tag = "snow.player:AbstractPlayer";

        if (pm != null) {
            mWakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, tag);
            mWakeLock.setReferenceCounted(false);
        }

        if (wm != null) {
            mWifiLock = wm.createWifiLock(WifiManager.WIFI_MODE_FULL, tag);
            mWifiLock.setReferenceCounted(false);
        }
    }

    private void requireWakeLock() {
        if (wakeLockPermissionDenied()) {
            Log.w(TAG, "need permission: 'android.permission.WAKE_LOCK'");
            return;
        }

        if (mWakeLock != null && !mWakeLock.isHeld()) {
            mWakeLock.acquire(getMusicItemDuration() + 5_000);
        }

        if (mWifiLock != null && !mWifiLock.isHeld()) {
            mWifiLock.acquire();
        }
    }

    private boolean wakeLockPermissionDenied() {
        return PackageManager.PERMISSION_DENIED ==
                ContextCompat.checkSelfPermission(mApplicationContext, Manifest.permission.WAKE_LOCK);
    }

    private void releaseWakeLock() {
        if (mWakeLock != null && mWakeLock.isHeld()) {
            mWakeLock.release();
        }

        if (mWifiLock != null && mWifiLock.isHeld()) {
            mWifiLock.release();
        }
    }

    private PlaybackStateCompat buildPlaybackState(int state) {
        return mPlaybackStateBuilder.setState(state, getPlayProgress(), 1.0F, mPlayerState.getPlayProgressUpdateTime())
                .build();
    }

    private MediaMetadataCompat buildMediaMetadata() {
        MusicItem musicItem = getMusicItem();

        if (musicItem != null) {
            return mMediaMetadataBuilder.putString(MediaMetadataCompat.METADATA_KEY_TITLE, musicItem.getTitle())
                    .putString(MediaMetadataCompat.METADATA_KEY_ARTIST, musicItem.getArtist())
                    .putString(MediaMetadataCompat.METADATA_KEY_ALBUM, musicItem.getAlbum())
                    .putString(MediaMetadataCompat.METADATA_KEY_DISPLAY_ICON_URI, musicItem.getIconUri())
                    .putLong(MediaMetadataCompat.METADATA_KEY_DURATION, musicItem.getDuration())
                    .build();
        }

        return null;
    }

    private void attachListeners(MusicPlayer musicPlayer) {
        musicPlayer.setOnPreparedListener(mPreparedListener);
        musicPlayer.setOnCompletionListener(mCompletionListener);
        musicPlayer.setOnSeekCompleteListener(mSeekCompleteListener);
        musicPlayer.setOnStalledListener(mStalledListener);
        musicPlayer.setOnBufferingUpdateListener(mBufferingUpdateListener);
        musicPlayer.setOnErrorListener(mErrorListener);
    }

    /**
     * 释放当前播放器所持有的 {@link MusicPlayer} 对象（测试用）。
     */
    private void releaseMusicPlayer() {
        cancelRecordProgress();
        if (mMusicPlayer != null) {
            mMusicPlayer.release();
            mMusicPlayer = null;
        }

        mPlayerStateHelper.clearPrepareState();
        mPlayOnPrepared = false;
        mPlayOnSeekComplete = false;

        mPreparedAction = null;
        mSeekCompleteAction = null;

        if (mPlayerState.isStalled()) {
            notifyStalled(false, mPlayerState.getPlayProgress(), mPlayerState.getPlayProgressUpdateTime());
        }
    }

    /**
     * 播放器当前是否处已准备完毕。
     *
     * @return 如果播放器已准备完毕，则返回 true，否则返回 false
     */
    public final boolean isPrepared() {
        return mPlayerState.isPrepared();
    }

    /**
     * 播放器当前是否处正在准备中。
     *
     * @return 如果播放器正在准备中，则返回 true，否则返回 false
     */
    public final boolean isPreparing() {
        return mPlayerState.isPreparing();
    }

    private boolean isPlaying() {
        return isPrepared() && mMusicPlayer.isPlaying();
    }

    /**
     * 获取当前播放进度。
     *
     * @return 当前播放进度
     */
    public final int getPlayProgress() {
        if (isPrepared()) {
            mPlayerStateHelper.updatePlayProgress(mMusicPlayer.getProgress(), System.currentTimeMillis());
        }

        return mPlayerState.getPlayProgress();
    }

    /**
     * 获取播放器当前的播放状态。
     *
     * @return 播放器当前的播放状态。
     */
    @NonNull
    public final PlaybackState getPlaybackState() {
        return mPlayerState.getPlaybackState();
    }

    /**
     * 获取播放器的播放模式。
     *
     * @return 播放器的播放模式。
     */
    @NonNull
    public final PlayMode getPlayMode() {
        return mPlayerState.getPlayMode();
    }

    /**
     * 当前播放器是否处于 {@code stalled} 状态。
     *
     * @return 当缓冲区没有足够的数据支持播放器继续播放时，该方法会返回 {@code true}，否则返回 false
     */
    public final boolean isStalled() {
        return mPlayerState.isStalled();
    }

    /**
     * 获取当前正在播放的应用的 audio session id。
     * <p>
     * 如果当前没有播放任何音乐，或者播放器还没有准备完毕（{@link #isPrepared()} 返回了 false），则该方法会
     * 返回 0。
     *
     * @return 当前正在播放的应用的 audio session id。
     */
    public final int getAudioSessionId() {
        if (isPrepared()) {
            return mMusicPlayer.getAudioSessionId();
        }

        return 0;
    }

    /**
     * 添加播放器状态监听器。
     *
     * @param token    监听器的 token（不能为 null），请务必保证该参数的唯一性。
     * @param listener 监听器（不能为 null）。
     */
    public final void addStateListener(@NonNull String token, @NonNull PlayerStateListener listener) {
        Preconditions.checkNotNull(token);
        Preconditions.checkNotNull(listener);

        mStateListenerMap.put(token, listener);
    }

    /**
     * 移除播放器状态监听器。
     *
     * @param token 监听器的 token（不能为 null）
     */
    public final void removeStateListener(@NonNull String token) {
        Preconditions.checkNotNull(token);

        mStateListenerMap.remove(token);
    }

    private void notifyPreparing() {
        requireWakeLock();
        mPlayerStateHelper.onPreparing();

        onPreparing();

        for (String key : mStateListenerMap.keySet()) {
            PlayerStateListener listener = mStateListenerMap.get(key);
            if (listener != null) {
                listener.onPreparing();
            }
        }
    }

    private void notifyPrepared(int audioSessionId) {
        mPlayerStateHelper.onPrepared(audioSessionId);

        onPrepared(audioSessionId);

        for (String key : mStateListenerMap.keySet()) {
            PlayerStateListener listener = mStateListenerMap.get(key);
            if (listener != null) {
                listener.onPrepared(audioSessionId);
            }
        }
    }

    private void notifyPlaying(int progress, long updateTime) {
        mPlayerStateHelper.onPlay(progress, updateTime);
        mMediaSession.setActive(true);
        mMediaSession.setPlaybackState(buildPlaybackState(PlaybackStateCompat.STATE_PLAYING));

        startRecordProgress();

        mAudioFocusHelper.requestAudioFocus(AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN);
        mBecomeNoiseHelper.registerBecomeNoiseReceiver();

        onPlaying(progress, updateTime);

        for (String key : mStateListenerMap.keySet()) {
            PlayerStateListener listener = mStateListenerMap.get(key);
            if (listener != null) {
                listener.onPlay(progress, updateTime);
            }
        }
    }

    private void notifyPaused() {
        cancelRecordProgress();
        releaseWakeLock();

        mPlayerStateHelper.onPaused();

        if (isPrepared()) {
            mPlayerStateHelper.updatePlayProgress(mMusicPlayer.getProgress(), System.currentTimeMillis());
        }

        mMediaSession.setPlaybackState(buildPlaybackState(PlaybackStateCompat.STATE_PAUSED));

        mAudioFocusHelper.abandonAudioFocus();
        mBecomeNoiseHelper.unregisterBecomeNoiseReceiver();

        onPaused();

        for (String key : mStateListenerMap.keySet()) {
            PlayerStateListener listener = mStateListenerMap.get(key);
            if (listener != null) {
                listener.onPause();
            }
        }
    }

    private void notifyStopped() {
        cancelRecordProgress();
        releaseWakeLock();

        mPlayerStateHelper.onStopped();
        mMediaSession.setActive(false);
        mMediaSession.setPlaybackState(buildPlaybackState(PlaybackStateCompat.STATE_STOPPED));

        mAudioFocusHelper.abandonAudioFocus();
        mBecomeNoiseHelper.unregisterBecomeNoiseReceiver();

        onStopped();

        for (String key : mStateListenerMap.keySet()) {
            PlayerStateListener listener = mStateListenerMap.get(key);
            if (listener != null) {
                listener.onStop();
            }
        }
    }

    private void notifyStalled(boolean stalled, int playProgress, long updateTime) {
        mPlayerStateHelper.onStalled(stalled, playProgress, updateTime);
        if (!stalled && isPlaying()) {
            startRecordProgress();
            mMediaSession.setPlaybackState(buildPlaybackState(PlaybackStateCompat.STATE_PLAYING));
        } else {
            cancelRecordProgress();
            mMediaSession.setPlaybackState(buildPlaybackState(PlaybackStateCompat.STATE_BUFFERING));
        }

        onStalledChanged(stalled);

        for (String key : mStateListenerMap.keySet()) {
            PlayerStateListener listener = mStateListenerMap.get(key);
            if (listener != null) {
                listener.onStalledChanged(stalled, playProgress, updateTime);
            }
        }
    }

    private void notifyError(int errorCode, String errorMessage) {
        releaseMusicPlayer();
        releaseWakeLock();

        mPlayerStateHelper.onError(errorCode, errorMessage);
        mMediaSession.setPlaybackState(buildPlaybackState(PlaybackStateCompat.STATE_ERROR));

        mAudioFocusHelper.abandonAudioFocus();
        mBecomeNoiseHelper.unregisterBecomeNoiseReceiver();

        onError(errorCode, errorMessage);

        for (String key : mStateListenerMap.keySet()) {
            PlayerStateListener listener = mStateListenerMap.get(key);
            if (listener != null) {
                listener.onError(errorCode, errorMessage);
            }
        }
    }

    private int getMusicItemDuration() {
        MusicItem musicItem = getMusicItem();
        if (musicItem == null) {
            return 0;
        }

        return musicItem.getDuration();
    }

    private void notifyBufferedChanged(int buffered, boolean isPercent) {
        int bufferedProgress = buffered;

        if (isPercent) {
            bufferedProgress = (int) ((buffered / 100.0) * getMusicItemDuration());
        }

        mPlayerStateHelper.onBufferedChanged(bufferedProgress);

        for (String key : mStateListenerMap.keySet()) {
            PlayerStateListener listener = mStateListenerMap.get(key);
            if (listener != null) {
                listener.onBufferedProgressChanged(bufferedProgress);
            }
        }
    }

    private void notifyPlayingMusicItemChanged(@Nullable MusicItem musicItem, boolean play) {
        releaseMusicPlayer();

        mPlayerStateHelper.onPlayingMusicItemChanged(musicItem, 0);
        mMediaSession.setMetadata(buildMediaMetadata());

        onPlayingMusicItemChanged(musicItem);

        for (String key : mStateListenerMap.keySet()) {
            PlayerStateListener listener = mStateListenerMap.get(key);
            if (listener != null) {
                listener.onPlayingMusicItemChanged(musicItem, mPlayerState.getPlayProgress());
            }
        }

        if (play) {
            play();
            return;
        }

        prepareMusicPlayer(false, null);
    }

    private void notifySeekComplete(int playProgress) {
        mPlayerStateHelper.onSeekComplete(playProgress, System.currentTimeMillis());

        for (String key : mStateListenerMap.keySet()) {
            PlayerStateListener listener = mStateListenerMap.get(key);
            if (listener != null) {
                listener.onSeekComplete(playProgress, mPlayerState.getPlayProgressUpdateTime());
            }
        }
    }

    @Override
    public void play() {
        if (isPlaying()) {
            return;
        }

        if (isPreparing()) {
            mPreparedAction = new Runnable() {
                @Override
                public void run() {
                    play();
                }
            };
            return;
        }

        if (isPrepared()) {
            mMusicPlayer.start();
            notifyPlaying(mMusicPlayer.getProgress(), System.currentTimeMillis());
            return;
        }

        prepareMusicPlayer(true, null);
    }

    @Override
    public void pause() {
        if (isPreparing()) {
            mPlayOnPrepared = false;
            return;
        }

        if (!isPlaying()) {
            return;
        }

        if (isPlaying()) {
            mMusicPlayer.pause();
        }

        notifyPaused();
    }

    @Override
    public void stop() {
        if (getPlaybackState() == PlaybackState.STOPPED || getMusicItem() == null) {
            return;
        }

        if (isPrepared()) {
            mMusicPlayer.stop();
        }

        releaseMusicPlayer();
        notifyStopped();
    }

    @Override
    public void playPause() {
        if (isPreparing() && mPlayOnPrepared) {
            pause();
            return;
        }

        if (isPlaying()) {
            pause();
        } else {
            play();
        }
    }

    private void seekTo(final int progress, final Runnable seekCompleteAction) {
        if (isPreparing()) {
            mPreparedAction = new Runnable() {
                @Override
                public void run() {
                    seekTo(progress, seekCompleteAction);
                }
            };
            return;
        }

        if (!isPrepared()) {
            return;
        }

        mSeekCompleteAction = seekCompleteAction;
        mMusicPlayer.seekTo(progress);
    }

    @Override
    public void seekTo(final int progress) {
        seekTo(progress, null);
    }

    @Override
    public void fastForward() {
        if (isPreparing()) {
            mPreparedAction = new Runnable() {
                @Override
                public void run() {
                    fastForward();
                }
            };
            return;
        }

        if (!isPrepared()) {
            return;
        }

        int progress = Math.min(mMusicPlayer.getDuration(),
                mMusicPlayer.getProgress() + FORWARD_STEP);

        mMediaSession.setPlaybackState(buildPlaybackState(PlaybackStateCompat.STATE_FAST_FORWARDING));
        seekTo(progress);
    }

    @Override
    public void rewind() {
        if (isPreparing()) {
            mPreparedAction = new Runnable() {
                @Override
                public void run() {
                    rewind();
                }
            };
            return;
        }

        if (!isPrepared()) {
            return;
        }

        int progress = Math.max(0, mMusicPlayer.getProgress() - FORWARD_STEP);

        mMediaSession.setPlaybackState(buildPlaybackState(PlaybackStateCompat.STATE_REWINDING));
        seekTo(progress);
    }

    /**
     * 通知播放器，当前的 {@link SoundQuality} 已改变。
     * <p>
     * 该方法应该在调用与当前播放器管理的 {@link PlayerConfig} 对象的
     * {@link PlayerConfig#setSoundQuality(SoundQuality)} 方法后调用。
     */
    public final void notifySoundQualityChanged() {
        if (!isPrepared()) {
            return;
        }

        boolean playing = mMusicPlayer.isPlaying();
        final int position = mMusicPlayer.getProgress();

        releaseMusicPlayer();
        prepareMusicPlayer(playing, new Runnable() {
            @Override
            public void run() {
                if (position > 0) {
                    seekTo(position);
                }
            }
        });
    }

    /**
     * 通知播放器，当前的 {@code audioEffectEnabled} 状态已改变。
     * <p>
     * 该方法应该在调用与当前播放器管理的 {@link PlayerConfig} 对象的
     * {@link PlayerConfig#setAudioEffectEnabled(boolean)} 方法后调用。
     */
    public final void notifyAudioEffectEnableChanged() {
        if (!isPrepared()) {
            return;
        }

        if (mPlayerConfig.isAudioEffectEnabled()) {
            attachAudioEffect(getAudioSessionId());
            return;
        }

        detachAudioEffect();
    }

    /**
     * 通知播放器，当前的 {@code onlyWifiNetwork} 状态已改变。
     * <p>
     * 该方法应该在调用与当前播放器管理的 {@link PlayerConfig} 对象的
     * {@link PlayerConfig#setOnlyWifiNetwork(boolean)} 方法后调用。
     */
    public final void notifyOnlyWifiNetworkChanged() {
        if (!isPrepared()) {
            return;
        }

        checkNetworkType(mPlayerConfig.isOnlyWifiNetwork(), mNetworkHelper.isWifiNetwork());
    }

    private void checkNetworkType(boolean onlyWifiNetwork, boolean isWifiNetwork) {
        disposeCheckCached();

        if (!mNetworkHelper.networkAvailable()) {
            return;
        }

        mCheckCachedDisposable = playingMusicIsCached()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(checkNetworkTypeConsumer(onlyWifiNetwork, isWifiNetwork));
    }

    private void disposeCheckCached() {
        if (mCheckCachedDisposable != null) {
            mCheckCachedDisposable.dispose();
            mCheckCachedDisposable = null;
        }
    }

    private Single<Boolean> playingMusicIsCached() {
        return Single.create(new SingleOnSubscribe<Boolean>() {
            @Override
            public void subscribe(SingleEmitter<Boolean> emitter) {
                boolean cached = isCached(getMusicItem(), mPlayerConfig.getSoundQuality());
                if (emitter.isDisposed()) {
                    return;
                }

                emitter.onSuccess(cached);
            }
        });
    }

    private Consumer<Boolean> checkNetworkTypeConsumer(final boolean onlyWifiNetwork, final boolean isWifiNetwork) {
        return new Consumer<Boolean>() {
            @Override
            public void accept(Boolean cached) {
                if (onlyWifiNetwork && !isWifiNetwork && !cached) {
                    pause();
                    releaseMusicPlayer();
                    notifyError(ErrorUtil.ONLY_WIFI_NETWORK,
                            ErrorUtil.getErrorMessage(mApplicationContext, ErrorUtil.ONLY_WIFI_NETWORK));
                }
            }
        };
    }

    private void reloadPlaylist() {
        reloadPlaylist(false, false);
    }

    private void reloadPlaylist(final boolean playingMusicChanged, final boolean play) {
        mLoadingPlaylist = true;
        mPlaylistManager.getPlaylistAsync(new PlaylistManager.Callback() {
            @Override
            public void onFinished(@NonNull final Playlist playlist) {
                if (mReleased) {
                    return;
                }

                mPlaylist = playlist;
                mLoadingPlaylist = false;

                if (playingMusicChanged) {
                    MusicItem musicItem = null;
                    if (mPlaylist.size() > 0) {
                        musicItem = mPlaylist.get(mPlayerState.getPosition());
                    }

                    notifyPlayingMusicItemChanged(musicItem, play);
                }

                if (mPlaylistLoadedAction != null) {
                    mPlaylistLoadedAction.run();
                    mPlaylistLoadedAction = null;
                }
            }
        });
    }

    private int getRandomPosition(int exclude) {
        if (mPlaylist == null || getPlaylistSize() < 2) {
            return 0;
        }

        if (mRandom == null) {
            mRandom = new Random();
        }

        int position = mRandom.nextInt(getPlaylistSize());

        if (position != exclude) {
            return position;
        }

        return getRandomPosition(exclude);
    }

    private void notifyPlayingMusicItemPositionChanged(int position) {
        mPlayerStateHelper.onPositionChanged(position);

        for (String key : mStateListenerMap.keySet()) {
            PlayerStateListener listener = mStateListenerMap.get(key);
            if (listener != null) {
                listener.onPositionChanged(position);
            }
        }
    }

    private void notifyPlayModeChanged(PlayMode playMode) {
        mPlayerStateHelper.onPlayModeChanged(playMode);

        for (String key : mStateListenerMap.keySet()) {
            PlayerStateListener listener = mStateListenerMap.get(key);
            if (listener != null) {
                listener.onPlayModeChanged(playMode);
            }
        }
    }

    private void notifyPlaylistChanged(int position) {
        mPlayerStateHelper.onPlaylistChanged(position);

        for (String key : mStateListenerMap.keySet()) {
            PlayerStateListener listener = mStateListenerMap.get(key);
            if (listener != null) {
                // 注意！playlistManager 参数为 null，客户端接收到该事件后，应该将其替换为自己的 PlaylistManager 对象
                listener.onPlaylistChanged(null, position);
            }
        }
    }

    /**
     * 获取播放列表的大小。
     *
     * @return 播放列表的大小
     */
    protected final int getPlaylistSize() {
        return mPlaylistManager.getPlaylistSize();
    }

    /**
     * 获取播放列表携带的额外参数。
     *
     * @return 播放列表携带的额外参数，可能为 null
     */
    @Nullable
    public final Bundle getPlaylistExtra() {
        if (mPlaylist == null) {
            return null;
        }

        return mPlaylist.getExtra();
    }

    private boolean isLooping() {
        return mPlayerState.getPlayMode() == PlayMode.LOOP;
    }

    @Override
    public void skipToNext() {
        if (mLoadingPlaylist) {
            mPlaylistLoadedAction = new Runnable() {
                @Override
                public void run() {
                    skipToNext();
                }
            };
            return;
        }

        if (getPlaylistSize() < 1) {
            return;
        }

        int position = mPlayerState.getPosition();

        switch (mPlayerState.getPlayMode()) {
            case SEQUENTIAL:   // 注意！case 穿透
            case LOOP:
                position = getNextPosition(position);
                break;
            case SHUFFLE:
                position = getRandomPosition(position);
                break;
        }

        mMediaSession.setPlaybackState(buildPlaybackState(PlaybackStateCompat.STATE_SKIPPING_TO_NEXT));
        notifyPlayingMusicItemChanged(mPlaylist.get(position), true);
        notifyPlayingMusicItemPositionChanged(position);
    }

    private int getNextPosition(int currentPosition) {
        int position = currentPosition + 1;

        if (position >= getPlaylistSize()) {
            return 0;
        }

        return position;
    }

    @Override
    public void skipToPrevious() {
        if (mLoadingPlaylist) {
            mPlaylistLoadedAction = new Runnable() {
                @Override
                public void run() {
                    skipToPrevious();
                }
            };
            return;
        }

        if (getPlaylistSize() < 1) {
            return;
        }

        int position = mPlayerState.getPosition();

        switch (mPlayerState.getPlayMode()) {
            case SEQUENTIAL:   // 注意！case 穿透
            case LOOP:
                position = getPreviousPosition(position);
                break;
            case SHUFFLE:
                position = getRandomPosition(position);
                break;
        }

        mMediaSession.setPlaybackState(buildPlaybackState(PlaybackStateCompat.STATE_SKIPPING_TO_PREVIOUS));
        notifyPlayingMusicItemChanged(mPlaylist.get(position), true);
        notifyPlayingMusicItemPositionChanged(position);
    }

    private int getPreviousPosition(int currentPosition) {
        int position = currentPosition - 1;

        if (position < 0) {
            return getPlaylistSize() - 1;
        }

        return position;
    }

    @Override
    public void playPause(final int position) {
        if (mLoadingPlaylist) {
            mPlaylistLoadedAction = new Runnable() {
                @Override
                public void run() {
                    playPause(position);
                }
            };
            return;
        }

        if (position == mPlayerState.getPosition()) {
            playPause();
            return;
        }

        mMediaSession.setPlaybackState(buildPlaybackState(PlaybackStateCompat.STATE_SKIPPING_TO_QUEUE_ITEM));
        notifyPlayingMusicItemChanged(mPlaylist.get(position), true);
        notifyPlayingMusicItemPositionChanged(position);
    }

    @Override
    public void setPlayMode(@NonNull PlayMode playMode) {
        Preconditions.checkNotNull(playMode);
        if (playMode == mPlayerState.getPlayMode()) {
            return;
        }

        if (isPrepared()) {
            mMusicPlayer.setLooping(playMode == PlayMode.LOOP);
        }

        notifyPlayModeChanged(playMode);
    }

    @Override
    public void onNewPlaylist(MusicItem musicItem, final int position, final boolean play) {
        stop();
        notifyPlaylistChanged(position);
        notifyPlayingMusicItemChanged(musicItem, play);
        reloadPlaylist();
    }

    @Override
    public void onMusicItemMoved(int fromPosition, int toPosition) {
        int position = mPlayerState.getPosition();
        if (notInRegion(position, fromPosition, toPosition)) {
            notifyPlaylistChanged(position);
            reloadPlaylist();
            return;
        }

        if (fromPosition < position) {
            position -= 1;
        } else if (fromPosition == position) {
            position = toPosition;
        } else {
            position += 1;
        }

        notifyPlaylistChanged(position);
        reloadPlaylist();
    }

    @Override
    public void onMusicItemInserted(int position, MusicItem musicItem) {
        int playingPosition = mPlayerState.getPosition();

        if (position <= playingPosition) {
            playingPosition += 1;
        }

        notifyPlaylistChanged(playingPosition);
        reloadPlaylist();
    }

    @Override
    public void onMusicItemRemoved(final MusicItem musicItem) {
        int removePosition = mPlaylist.indexOf(musicItem);
        if (removePosition < 0) {
            return;
        }

        if (getPlaylistSize() < 1) {
            notifyPlaylistChanged(-1);
            reloadPlaylist(true, false);
            return;
        }

        int position = mPlayerState.getPosition();

        if (removePosition < position) {
            position -= 1;
        } else if (removePosition == position) {
            position = getNextPosition(position - 1);
            notifyPlayingMusicItemPositionChanged(position);
            reloadPlaylist(true, isPlaying());
            return;
        }

        notifyPlaylistChanged(position);
        reloadPlaylist();
    }

    private boolean notInRegion(int position, int fromPosition, int toPosition) {
        return position > Math.max(fromPosition, toPosition) || position < Math.min(fromPosition, toPosition);
    }

    private void startRecordProgress() {
        cancelRecordProgress();
        if (!mRecordProgress) {
            return;
        }

        mRecordProgressDisposable = Observable.interval(3, 3, TimeUnit.SECONDS, Schedulers.io())
                .subscribe(new Consumer<Long>() {
                    @Override
                    public void accept(Long aLong) {
                        if (!isPrepared()) {
                            return;
                        }

                        mPlayerState.setPlayProgress(mMusicPlayer.getProgress());
                    }
                });
    }

    private void cancelRecordProgress() {
        if (mRecordProgressDisposable == null || mRecordProgressDisposable.isDisposed()) {
            return;
        }

        mRecordProgressDisposable.dispose();
    }
}
