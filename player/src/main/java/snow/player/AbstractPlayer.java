package snow.player;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.media.AudioManager;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.PowerManager;
import android.os.SystemClock;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.google.common.base.Preconditions;

import java.util.List;
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
import snow.player.playlist.PlaylistEditor;
import snow.player.playlist.PlaylistManager;
import snow.player.media.ErrorCode;
import snow.player.helper.NetworkHelper;

/**
 * 该类实现了 {@link Player} 接口，并实现大部分音乐播放器功能。
 */
abstract class AbstractPlayer implements Player, PlaylistEditor, PlaylistEditor.OnNewPlaylistListener {
    private static final String TAG = "AbstractPlayer";
    private static final int FORWARD_STEP = 15_000;     // 15 秒, 单位：毫秒 ms

    private Context mApplicationContext;
    private PlayerConfig mPlayerConfig;
    private PlayerState mPlayerState;
    private PlayerStateHelper mPlayerStateHelper;
    @Nullable
    private PlayerStateListener mPlayerStateListener;

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

    private PlaylistManagerImp mPlaylistManager;
    private Playlist mPlaylist;

    private Random mRandom;
    private Disposable mRetrieveUriDisposable;

    private boolean mReleased;

    private Disposable mRecordProgressDisposable;
    private Disposable mCheckCachedDisposable;

    private MediaSessionCompat mMediaSession;
    private PlaybackStateCompat.Builder mPlaybackStateBuilder;
    private PlaybackStateCompat.Builder mForbidSeekPlaybackStateBuilder;
    private MediaMetadataCompat.Builder mMediaMetadataBuilder;

    private PowerManager.WakeLock mWakeLock;
    private WifiManager.WifiLock mWifiLock;

    private boolean mConfirmNextPlay;

    /**
     * 创建一个 {@link AbstractPlayer} 对象。
     *
     * @param context         {@link Context} 对象，不能为 null
     * @param playerConfig    {@link PlayerConfig} 对象，保存了播放器的初始配置信息，不能为 null
     * @param playerState     {@link PlayerState} 对象，保存了播放器的初始状态，不能为 null
     * @param playlistManager {@link PlaylistManagerImp} 对象，用于管理播放列表，不能为 null
     * @param pref            {@link AppWidgetPreferences} 对象，用于在 PlayerService 与 AppWidget
     *                        之间进行状态同步，不能为 null
     */
    public AbstractPlayer(@NonNull Context context,
                          @NonNull PlayerConfig playerConfig,
                          @NonNull PlayerState playerState,
                          @NonNull PlaylistManagerImp playlistManager,
                          @NonNull AppWidgetPreferences pref) {
        Preconditions.checkNotNull(context);
        Preconditions.checkNotNull(playerConfig);
        Preconditions.checkNotNull(playerState);
        Preconditions.checkNotNull(playlistManager);
        Preconditions.checkNotNull(pref);

        mApplicationContext = context.getApplicationContext();
        mPlayerConfig = playerConfig;
        mPlayerState = playerState;
        mPlayerStateHelper = new PlayerStateHelper(mPlayerState, pref);
        mPlaylistManager = playlistManager;

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
    protected abstract MusicPlayer onCreateMusicPlayer(@NonNull Context context, @NonNull MusicItem musicItem, @NonNull Uri uri);

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
     * 该方法会在 stalled 状态改变时调用。
     * <p>
     * 你可以根据 stalled 参数的值来显示或隐藏缓冲进度条。如果缓冲区没有足够的数据支撑继续播放时，则该参数为
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
     * @see ErrorCode
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
     * 获取当前正在播放的音乐。
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
            onError(ErrorCode.ONLY_WIFI_NETWORK, ErrorCode.getErrorMessage(mApplicationContext, ErrorCode.ONLY_WIFI_NETWORK));
            return;
        }

        mPlayOnPrepared = playOnPrepared;
        mRetrieveUriDisposable = getMusicItemUri(musicItem, mPlayerConfig.getSoundQuality())
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(prepare(musicItem, preparedAction), notifyGetUrlFailed());
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

    private Consumer<Uri> prepare(@NonNull final MusicItem musicItem, @Nullable final Runnable preparedAction) {
        return new Consumer<Uri>() {
            @Override
            public void accept(Uri uri) {
                mMusicPlayer = onCreateMusicPlayer(mApplicationContext, musicItem, uri);
                attachListeners(mMusicPlayer);

                mPreparedAction = preparedAction;
                notifyPreparing();

                try {
                    if (!mMusicPlayer.isInvalid()) {
                        mMusicPlayer.prepare();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    notifyError(ErrorCode.DATA_LOAD_FAILED, ErrorCode.getErrorMessage(mApplicationContext, ErrorCode.DATA_LOAD_FAILED));
                }
            }
        };
    }

    private Consumer<Throwable> notifyGetUrlFailed() {
        return new Consumer<Throwable>() {
            @Override
            public void accept(Throwable throwable) {
                throwable.printStackTrace();
                notifyError(ErrorCode.GET_URL_FAILED, ErrorCode.getErrorMessage(mApplicationContext, ErrorCode.GET_URL_FAILED));
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

                notifySeekComplete(mp.getProgress(), SystemClock.elapsedRealtime(), mp.isStalled());

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
                notifyStalled(stalled);
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

                notifyError(errorCode, ErrorCode.getErrorMessage(mApplicationContext, errorCode));
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

        mForbidSeekPlaybackStateBuilder = new PlaybackStateCompat.Builder()
                .setActions(PlaybackStateCompat.ACTION_PLAY |
                        PlaybackStateCompat.ACTION_PAUSE |
                        PlaybackStateCompat.ACTION_PLAY_PAUSE |
                        PlaybackStateCompat.ACTION_STOP |
                        PlaybackStateCompat.ACTION_SKIP_TO_NEXT |
                        PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS |
                        PlaybackStateCompat.ACTION_SKIP_TO_QUEUE_ITEM |
                        PlaybackStateCompat.ACTION_SET_REPEAT_MODE |
                        PlaybackStateCompat.ACTION_SET_SHUFFLE_MODE);
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
        if (mPlayerState.isForbidSeek()) {
            return mForbidSeekPlaybackStateBuilder.setState(state, mPlayerState.getPlayProgress(), 1.0F, mPlayerState.getPlayProgressUpdateTime())
                    .build();
        }

        return mPlaybackStateBuilder.setState(state, mPlayerState.getPlayProgress(), 1.0F, mPlayerState.getPlayProgressUpdateTime())
                .build();
    }

    private PlaybackStateCompat buildErrorState(String errorMessage) {
        if (mPlayerState.isForbidSeek()) {
            return mForbidSeekPlaybackStateBuilder.setState(PlaybackStateCompat.STATE_ERROR, mPlayerState.getPlayProgress(), 1.0F, mPlayerState.getPlayProgressUpdateTime())
                    .setErrorMessage(PlaybackStateCompat.ERROR_CODE_APP_ERROR, errorMessage)
                    .build();
        }

        return mPlaybackStateBuilder.setState(PlaybackStateCompat.STATE_ERROR, mPlayerState.getPlayProgress(), 1.0F, mPlayerState.getPlayProgressUpdateTime())
                .setErrorMessage(PlaybackStateCompat.ERROR_CODE_APP_ERROR, errorMessage)
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
            notifyStalled(false);
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
     * 获取当前正在播放的音乐的 audio session id。
     * <p>
     * 如果当前没有播放任何音乐，或者播放器还没有准备完毕（{@link #isPrepared()} 返回了 false），则该方法会
     * 返回 0。
     *
     * @return 当前正在播放的音乐的 audio session id。
     */
    public final int getAudioSessionId() {
        if (isPrepared()) {
            return mMusicPlayer.getAudioSessionId();
        }

        return 0;
    }

    /**
     * 设置播放器状态监听器。
     *
     * @param listener 播放器状态监听器，为 null 时会清除已设置的监听器
     */
    public final void setPlayerStateListener(@Nullable PlayerStateListener listener) {
        mPlayerStateListener = listener;
    }

    private void notifyPreparing() {
        requireWakeLock();
        mPlayerStateHelper.onPreparing();

        onPreparing();

        if (mPlayerStateListener != null) {
            mPlayerStateListener.onPreparing();
        }
    }

    private void notifyPrepared(int audioSessionId) {
        mPlayerStateHelper.onPrepared(audioSessionId);

        onPrepared(audioSessionId);

        if (mPlayerStateListener != null) {
            mPlayerStateListener.onPrepared(audioSessionId);
        }
    }

    private void notifyPlaying(boolean stalled, int progress, long updateTime) {
        mPlayerStateHelper.onPlay(stalled, progress, updateTime);
        mMediaSession.setActive(true);

        if (!stalled) {
            mMediaSession.setPlaybackState(buildPlaybackState(PlaybackStateCompat.STATE_PLAYING));
        }

        startRecordProgress();

        mAudioFocusHelper.requestAudioFocus(AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN);
        mBecomeNoiseHelper.registerBecomeNoiseReceiver();

        onPlaying(progress, updateTime);

        if (mPlayerStateListener != null) {
            mPlayerStateListener.onPlay(stalled, progress, updateTime);
        }
    }

    private void notifyPaused() {
        cancelRecordProgress();
        releaseWakeLock();

        int playProgress = mPlayerState.getPlayProgress();
        long updateTime = mPlayerState.getPlayProgressUpdateTime();

        if (isPrepared()) {
            playProgress = mMusicPlayer.getProgress();
            updateTime = SystemClock.elapsedRealtime();
        }

        mPlayerStateHelper.onPaused(playProgress, updateTime);

        mMediaSession.setPlaybackState(buildPlaybackState(PlaybackStateCompat.STATE_PAUSED));

        mAudioFocusHelper.abandonAudioFocus();
        mBecomeNoiseHelper.unregisterBecomeNoiseReceiver();

        onPaused();

        if (mPlayerStateListener != null) {
            mPlayerStateListener.onPause(playProgress, updateTime);
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

        if (mPlayerStateListener != null) {
            mPlayerStateListener.onStop();
        }
    }

    private void notifyStalled(boolean stalled) {
        int playProgress = mPlayerState.getPlayProgress();
        long updateTime = mPlayerState.getPlayProgressUpdateTime();

        if (isPlaying()) {
            playProgress = mMusicPlayer.getProgress();
            updateTime = SystemClock.elapsedRealtime();
        }

        mPlayerStateHelper.onStalled(stalled, playProgress, updateTime);

        if (stalled) {
            cancelRecordProgress();
            mMediaSession.setPlaybackState(buildPlaybackState(PlaybackStateCompat.STATE_BUFFERING));
        } else if (isPlaying()) {
            startRecordProgress();
            mMediaSession.setPlaybackState(buildPlaybackState(PlaybackStateCompat.STATE_PLAYING));
        } else {
            mMediaSession.setPlaybackState(buildPlaybackState(PlaybackStateCompat.STATE_PAUSED));
        }

        onStalledChanged(stalled);

        if (mPlayerStateListener != null) {
            mPlayerStateListener.onStalledChanged(stalled, playProgress, updateTime);
        }
    }

    private void notifyError(int errorCode, String errorMessage) {
        releaseMusicPlayer();
        releaseWakeLock();

        mPlayerStateHelper.onError(errorCode, errorMessage);
        mMediaSession.setPlaybackState(buildErrorState(errorMessage));

        mAudioFocusHelper.abandonAudioFocus();
        mBecomeNoiseHelper.unregisterBecomeNoiseReceiver();

        onError(errorCode, errorMessage);

        if (mPlayerStateListener != null) {
            mPlayerStateListener.onError(errorCode, errorMessage);
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

        if (mPlayerStateListener != null) {
            mPlayerStateListener.onBufferedProgressChanged(bufferedProgress);
        }
    }

    private void notifyPlayingMusicItemChanged(@Nullable MusicItem musicItem, boolean play) {
        releaseMusicPlayer();

        mPlayerStateHelper.onPlayingMusicItemChanged(musicItem, 0);
        mMediaSession.setMetadata(buildMediaMetadata());

        onPlayingMusicItemChanged(musicItem);

        if (mPlayerStateListener != null) {
            mPlayerStateListener.onPlayingMusicItemChanged(musicItem, mPlayerState.getPlayProgress());
        }

        if (play) {
            play();
            return;
        }

        prepareMusicPlayer(false, null);
    }

    private void notifySeekComplete(int playProgress, long updateTime, boolean stalled) {
        mPlayerStateHelper.onSeekComplete(playProgress, updateTime, stalled);

        if (mPlayerStateListener != null) {
            mPlayerStateListener.onSeekComplete(playProgress, updateTime, stalled);
        }

        if (!isPlaying()) {
            notifyPaused();
            return;
        }

        if (!stalled) {
            mMediaSession.setPlaybackState(buildPlaybackState(PlaybackStateCompat.STATE_PLAYING));
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
            notifyPlaying(mMusicPlayer.isStalled(), mMusicPlayer.getProgress(), SystemClock.elapsedRealtime());
            return;
        }

        prepareMusicPlayer(true, null);
    }

    @Override
    public void pause() {
        if (isPreparing()) {
            mPlayOnPrepared = false;
            mPlayOnSeekComplete = false;
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
        if (mPlayerState.isForbidSeek()) {
            return;
        }

        if (isPreparing()) {
            mPlayOnSeekComplete = mPlayOnPrepared;
            mPlayOnPrepared = false;
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
     * 通知播放器当前的 {@link SoundQuality} 已改变。
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
     * 通知播放器当前的 {@code audioEffectEnabled} 状态已改变。
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
     * 通知播放器当前的 {@code onlyWifiNetwork} 状态已改变。
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
                    notifyError(ErrorCode.ONLY_WIFI_NETWORK,
                            ErrorCode.getErrorMessage(mApplicationContext, ErrorCode.ONLY_WIFI_NETWORK));
                }
            }
        };
    }

    private void reloadPlaylist() {
        reloadPlaylist(false, false);
    }

    private void reloadPlaylist(final boolean playingMusicChanged, final boolean play) {
        mLoadingPlaylist = true;
        mPlaylistManager.getPlaylist(new PlaylistManager.Callback() {
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

        if (mPlayerStateListener != null) {
            mPlayerStateListener.onPositionChanged(position);
        }
    }

    private void notifyPlayModeChanged(PlayMode playMode) {
        mPlayerStateHelper.onPlayModeChanged(playMode);

        if (mPlayerStateListener != null) {
            mPlayerStateListener.onPlayModeChanged(playMode);
        }
    }

    private void notifyPlaylistChanged(int position) {
        mPlayerStateHelper.onPlaylistChanged(position);

        if (mPlayerStateListener != null) {
            // 注意！playlistManager 参数为 null，客户端接收到该事件后，应该将其替换为自己的 PlaylistManager 对象
            mPlayerStateListener.onPlaylistChanged(null, position);
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

        int position = getNextPosition(mPlayerState.getPosition());

        mMediaSession.setPlaybackState(buildPlaybackState(PlaybackStateCompat.STATE_SKIPPING_TO_NEXT));
        notifyPlayingMusicItemChanged(mPlaylist.get(position), true);
        notifyPlayingMusicItemPositionChanged(position);
    }

    @Override
    public void skipToPosition(int position) {
        if (position == mPlayerState.getPosition()) {
            return;
        }

        playPause(position);
    }

    private int getNextPosition(int currentPosition) {
        PlayMode playMode = mPlayerState.getPlayMode();
        if (mConfirmNextPlay || playMode == PlayMode.SEQUENTIAL || playMode == PlayMode.LOOP) {
            mConfirmNextPlay = false;
            int position = currentPosition + 1;
            if (position >= getPlaylistSize()) {
                return 0;
            }
            return position;
        }

        return getRandomPosition(currentPosition);
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

        int position = getPreviousPosition(mPlayerState.getPosition());

        mMediaSession.setPlaybackState(buildPlaybackState(PlaybackStateCompat.STATE_SKIPPING_TO_PREVIOUS));
        notifyPlayingMusicItemChanged(mPlaylist.get(position), true);
        notifyPlayingMusicItemPositionChanged(position);
    }

    private int getPreviousPosition(int currentPosition) {
        int position = 0;

        switch (mPlayerState.getPlayMode()) {
            case SEQUENTIAL:   // 注意！case 穿透
            case LOOP:
                position = currentPosition - 1;
                if (position < 0) {
                    return getPlaylistSize() - 1;
                }
                break;
            case SHUFFLE:
                position = getRandomPosition(currentPosition);
                break;
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
    public void setPlaylist(Playlist playlist, int position, boolean play) {
        // （忽略）该方法不会被调用
        // 当前类已通过实现 PlaylistEditor.OnNewPlaylistListener 接口来响应设置新的播放列表事件
        // 具体请查看当前类的 onNewPlaylist(MusicItem, int, boolean) 方法
    }

    @Override
    public void onNewPlaylist(MusicItem musicItem, final int position, final boolean play) {
        stop();
        notifyPlaylistChanged(position);
        notifyPlayingMusicItemChanged(musicItem, play);
        reloadPlaylist();
    }

    private void onMusicItemMoved(int fromPosition, int toPosition) {
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
    }

    private void onMusicItemInserted(int position) {
        int playingPosition = mPlayerState.getPosition();

        if (position <= playingPosition) {
            playingPosition += 1;
        }

        notifyPlaylistChanged(playingPosition);
    }

    private void onMusicItemRemoved(final MusicItem musicItem) {
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
    }

    private boolean notInRegion(int position, int fromPosition, int toPosition) {
        return position > Math.max(fromPosition, toPosition) || position < Math.min(fromPosition, toPosition);
    }

    private void startRecordProgress() {
        cancelRecordProgress();

        if (mPlayerState.isForbidSeek()) {
            return;
        }

        mRecordProgressDisposable = Observable.interval(3, 3, TimeUnit.SECONDS, Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Consumer<Long>() {
                    @Override
                    public void accept(Long aLong) {
                        if (!isPrepared()) {
                            return;
                        }

                        mPlayerStateHelper.updatePlayProgress(mMusicPlayer.getProgress(), SystemClock.elapsedRealtime());
                    }
                });
    }

    private void cancelRecordProgress() {
        if (mRecordProgressDisposable == null || mRecordProgressDisposable.isDisposed()) {
            return;
        }

        mRecordProgressDisposable.dispose();
    }

    @Override
    public void insertMusicItem(final int position, @NonNull final MusicItem musicItem) {
        if (mLoadingPlaylist) {
            mPlaylistLoadedAction = new Runnable() {
                @Override
                public void run() {
                    insertMusicItem(position, musicItem);
                }
            };
            return;
        }

        List<MusicItem> musicItems = mPlaylist.getAllMusicItem();
        if (musicItems.contains(musicItem)) {
            moveMusicItem(musicItems.indexOf(musicItem), position);
            return;
        }

        musicItems.add(position, musicItem);

        updatePlaylist(musicItems);
        onMusicItemInserted(position);
    }

    @Override
    public void appendMusicItem(@NonNull MusicItem musicItem) {
        insertMusicItem(getPlaylistSize(), musicItem);
    }

    @Override
    public void moveMusicItem(final int fromPosition, final int toPosition) {
        if (fromPosition == toPosition) {
            return;
        }

        if (mLoadingPlaylist) {
            mPlaylistLoadedAction = new Runnable() {
                @Override
                public void run() {
                    moveMusicItem(fromPosition, toPosition);
                }
            };
            return;
        }

        List<MusicItem> musicItems = mPlaylist.getAllMusicItem();

        MusicItem from = musicItems.remove(fromPosition);
        musicItems.add(toPosition, from);

        updatePlaylist(musicItems);
        onMusicItemMoved(fromPosition, toPosition);
    }

    @Override
    public void removeMusicItem(@NonNull final MusicItem musicItem) {
        if (mLoadingPlaylist) {
            mPlaylistLoadedAction = new Runnable() {
                @Override
                public void run() {
                    removeMusicItem(musicItem);
                }
            };
            return;
        }

        List<MusicItem> musicItems = mPlaylist.getAllMusicItem();
        if (!musicItems.contains(musicItem)) {
            return;
        }

        musicItems.remove(musicItem);

        updatePlaylist(musicItems);
        onMusicItemRemoved(musicItem);
    }

    @Override
    public void setNextPlay(@NonNull final MusicItem musicItem) {
        if (musicItem == getMusicItem()) {
            return;
        }

        if (mLoadingPlaylist) {
            mPlaylistLoadedAction = new Runnable() {
                @Override
                public void run() {
                    setNextPlay(musicItem);
                }
            };
            return;
        }

        insertMusicItem(mPlayerState.getPosition() + 1, musicItem);
        mConfirmNextPlay = true;
    }

    private void updatePlaylist(List<MusicItem> musicItems) {
        mPlaylist = new Playlist(musicItems);
        mPlaylistManager.save(mPlaylist, null);
    }
}
