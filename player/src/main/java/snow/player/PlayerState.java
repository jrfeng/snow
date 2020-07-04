package snow.player;

import android.content.Context;
import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.common.base.Objects;
import com.google.common.base.Preconditions;

import snow.player.media.MusicItem;

/**
 * 用于保存基本的播放器状态。
 */
public class PlayerState implements Parcelable {
    private int mPlayProgress;
    private long mPlayProgressUpdateTime;
    private Player.SoundQuality mSoundQuality;
    private boolean mAudioEffectEnabled;
    private boolean mOnlyWifiNetwork;
    private boolean mIgnoreLossAudioFocus;
    @Nullable
    private MusicItem mMusicItem;

    // no persistent
    private Player.PlaybackState mPlaybackState;
    private int mAudioSessionId;
    private int mBufferingPercent;
    private long mBufferingPercentUpdateTime;
    private boolean mStalled;
    private int mErrorCode;
    private String mErrorMessage;

    public PlayerState() {
        mPlayProgress = 0;
        mPlayProgressUpdateTime = 0;
        mSoundQuality = Player.SoundQuality.STANDARD;
        mAudioEffectEnabled = false;
        mOnlyWifiNetwork = false;
        mIgnoreLossAudioFocus = false;

        mPlaybackState = Player.PlaybackState.UNKNOWN;
        mAudioSessionId = 0;
        mBufferingPercent = 0;
        mBufferingPercentUpdateTime = 0;
        mStalled = false;
        mErrorCode = Player.Error.NO_ERROR;
        mErrorMessage = "";
    }

    public PlayerState(PlayerState source) {
        mPlayProgress = source.mPlayProgress;
        mPlayProgressUpdateTime = source.mPlayProgressUpdateTime;
        mSoundQuality = source.mSoundQuality;
        mAudioEffectEnabled = source.mAudioEffectEnabled;
        mOnlyWifiNetwork = source.mOnlyWifiNetwork;
        mIgnoreLossAudioFocus = source.mIgnoreLossAudioFocus;
        if (source.mMusicItem != null) {
            mMusicItem = new MusicItem(source.mMusicItem);
        }

        mPlaybackState = source.mPlaybackState;
        mAudioSessionId = source.mAudioSessionId;
        mBufferingPercent = source.mBufferingPercent;
        mBufferingPercentUpdateTime = source.mBufferingPercentUpdateTime;
        mStalled = source.mStalled;
        mErrorCode = source.mErrorCode;
        mErrorMessage = source.mErrorMessage;
    }

    /**
     * 获取播放进度。
     *
     * @return 播放进度。
     */
    public int getPlayProgress() {
        return mPlayProgress;
    }

    /**
     * 设置播放进度。
     *
     * @param playProgress 播放进度（小于 0 时，相当于设置为 0）。
     */
    public void setPlayProgress(int playProgress) {
        if (playProgress < 0) {
            mPlayProgress = 0;
            return;
        }

        mPlayProgress = playProgress;
    }


    /**
     * 获取上次播放进度的更新时间（单位：毫秒 ms）。
     *
     * @return 上次播放进度的更新时间。
     */
    public long getPlayProgressUpdateTime() {
        return mPlayProgressUpdateTime;
    }

    /**
     * 设置上次播放进度的更新时间。
     *
     * @param updateTime 上次播放进度的更新时间。
     */
    public void setPlayProgressUpdateTime(long updateTime) {
        mPlayProgressUpdateTime = updateTime;
    }

    /**
     * 获取首选音质。
     *
     * @return 当前的首选音质。
     * @see Player.SoundQuality
     */
    public Player.SoundQuality getSoundQuality() {
        return mSoundQuality;
    }

    /**
     * 设置首选音质。
     *
     * @param soundQuality 要设置的首选音质。只能是这些值之一：{@link Player.SoundQuality#STANDARD},
     *                     {@link Player.SoundQuality#LOW},
     *                     {@link Player.SoundQuality#HIGH},
     *                     {@link Player.SoundQuality#SUPER}
     * @see Player.SoundQuality
     */
    public void setSoundQuality(@NonNull Player.SoundQuality soundQuality) {
        Preconditions.checkNotNull(soundQuality);
        mSoundQuality = soundQuality;
    }

    /**
     * 判断是否已启用音频特效。
     *
     * @return 是否已启用音频特效。
     */
    public boolean isAudioEffectEnabled() {
        return mAudioEffectEnabled;
    }

    /**
     * 设置是否启用音频特效。
     *
     * @param audioEffectEnabled 是否启用音频特效。
     */
    public void setAudioEffectEnabled(boolean audioEffectEnabled) {
        mAudioEffectEnabled = audioEffectEnabled;
    }

    /**
     * 是否只允许在 WiFi 网络下联网（默认为 false）。
     *
     * @return 如果返回 true，则表示只允许在 WiFi 网络下联网（默认为 false）。
     */
    public boolean isOnlyWifiNetwork() {
        return mOnlyWifiNetwork;
    }

    /**
     * 设置是否只允许在 WiFi 网络下联网（默认为 true）。
     *
     * @param onlyWifiNetwork 是否只允许在 WiFi 网络下联网（默认为 true）。
     */
    public void setOnlyWifiNetwork(boolean onlyWifiNetwork) {
        mOnlyWifiNetwork = onlyWifiNetwork;
    }

    /**
     * 判断是否忽略音频焦点丢失事件。
     *
     * @return 是否忽略音频焦点丢失事件。
     */
    public boolean isIgnoreLossAudioFocus() {
        return mIgnoreLossAudioFocus;
    }

    /**
     * 设置是否忽略音频焦点丢失事件。
     *
     * @param ignoreLossAudioFocus 是否忽略音频焦点丢失事件。
     */
    public void setIgnoreLossAudioFocus(boolean ignoreLossAudioFocus) {
        mIgnoreLossAudioFocus = ignoreLossAudioFocus;
    }

    /**
     * 获取当前播放歌曲的 MusicItem 对象。
     */
    @Nullable
    public MusicItem getMusicItem() {
        return mMusicItem;
    }

    /**
     * 设置当前播放歌曲的 MusicItem 对象。
     */
    public void setMusicItem(@Nullable MusicItem musicItem) {
        mMusicItem = musicItem;
    }

    /**
     * 获取播放状态。
     *
     * @return 返回当前播放状态。
     * @see Player.PlaybackState
     */
    public Player.PlaybackState getPlaybackState() {
        return mPlaybackState;
    }

    /**
     * 设置播放状态。
     *
     * @param playbackState 要设置的播放状态。只能为这些值之一：{@link Player.PlaybackState#UNKNOWN},
     *                      {@link Player.PlaybackState#PREPARING},
     *                      {@link Player.PlaybackState#PREPARED},
     *                      {@link Player.PlaybackState#PLAYING},
     *                      {@link Player.PlaybackState#PAUSED},
     *                      {@link Player.PlaybackState#STOPPED},
     *                      {@link Player.PlaybackState#ERROR}
     * @see Player.PlaybackState
     */
    public void setPlaybackState(@NonNull Player.PlaybackState playbackState) {
        Preconditions.checkNotNull(playbackState);
        mPlaybackState = playbackState;

        if (playbackState != Player.PlaybackState.ERROR) {
            mErrorCode = Player.Error.NO_ERROR;
            mErrorMessage = "";
        }
    }

    /**
     * 获取当前正在播放的音乐的 audio session id。
     * <p>
     * 注意！可能会返回 0 （API 21: {@link android.media.AudioManager#AUDIO_SESSION_ID_GENERATE}），表示当前音乐的
     * audio session id 不可用。
     */
    public int getAudioSessionId() {
        return mAudioSessionId;
    }

    /**
     * 设置当前音乐的 audio session id。
     *
     * @param audioSessionId 当前音乐的 audio session id。如果当前音乐的 audio session id 不可用，则可以
     *                       设为 0 （API 21: {@link android.media.AudioManager#AUDIO_SESSION_ID_GENERATE}）。
     */
    public void setAudioSessionId(int audioSessionId) {
        mAudioSessionId = audioSessionId;
    }

    /**
     * 获取当前音乐的缓存进度。百分比值，范围为 [0 ~ 100]。
     */
    public int getBufferingPercent() {
        return mBufferingPercent;
    }

    /**
     * 设置当前音乐的缓存进度。百分比值，范围为 [0 ~ 100]。
     */
    public void setBufferingPercent(int bufferingPercent) {
        if (bufferingPercent < 0) {
            mBufferingPercent = 0;
            return;
        }

        if (bufferingPercent > 100) {
            mBufferingPercent = 100;
            return;
        }

        mBufferingPercent = bufferingPercent;
    }

    /**
     * 获取上一次缓存进度的更新时间（单位：毫秒 ms）。
     */
    public long getBufferingPercentUpdateTime() {
        return mBufferingPercentUpdateTime;
    }

    /**
     * 设置上一次缓存进度的更新时间（单位：毫秒 ms）。
     */
    public void setBufferingPercentUpdateTime(long bufferingPercentUpdateTime) {
        mBufferingPercentUpdateTime = bufferingPercentUpdateTime;
    }

    public boolean isStalled() {
        return mStalled;
    }

    public void setStalled(boolean stalled) {
        mStalled = stalled;
    }

    /**
     * 获取错误码。如果没有发生任何错误，则返回 {@link Player.Error#NO_ERROR}。
     *
     * @see Player.Error
     */
    public int getErrorCode() {
        return mErrorCode;
    }

    /**
     * 设置错误码。
     *
     * @param errorCode 错误码。预定义的错误码：{@link Player.Error#NO_ERROR},
     *                  {@link Player.Error#ONLY_WIFI_NETWORK},
     *                  {@link Player.Error#PLAYER_ERROR},
     *                  {@link Player.Error#NETWORK_UNAVAILABLE}
     * @see Player.Error
     */
    public void setErrorCode(int errorCode) {
        mErrorCode = errorCode;
    }

    /**
     * 获取错误信息。
     */
    public String getErrorMessage() {
        return mErrorMessage;
    }

    /**
     * 设置错误信息（不能为 null）。
     *
     * @param errorMessage 错误信息（不能为 null）。
     * @see Player.Error#getErrorMessage(Context, int)
     */
    public void setErrorMessage(@NonNull String errorMessage) {
        Preconditions.checkNotNull(errorMessage);
        mErrorMessage = errorMessage;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PlayerState)) return false;

        PlayerState other = (PlayerState) o;

        return Objects.equal(mPlayProgress, other.mPlayProgress)
                && Objects.equal(mPlayProgressUpdateTime, other.mPlayProgressUpdateTime)
                && Objects.equal(mSoundQuality, other.mSoundQuality)
                && Objects.equal(mAudioEffectEnabled, other.mAudioEffectEnabled)
                && Objects.equal(mOnlyWifiNetwork, other.mOnlyWifiNetwork)
                && Objects.equal(mIgnoreLossAudioFocus, other.mIgnoreLossAudioFocus)
                && Objects.equal(mMusicItem, other.mMusicItem)
                && Objects.equal(mPlaybackState, other.mPlaybackState)
                && Objects.equal(mAudioSessionId, other.mAudioSessionId)
                && Objects.equal(mBufferingPercent, other.mBufferingPercent)
                && Objects.equal(mBufferingPercentUpdateTime, other.mBufferingPercentUpdateTime)
                && Objects.equal(mStalled, other.mStalled)
                && Objects.equal(mErrorCode, other.mErrorCode)
                && Objects.equal(mErrorMessage, other.mErrorMessage);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(mPlayProgress,
                mPlayProgressUpdateTime,
                mSoundQuality,
                mAudioEffectEnabled,
                mOnlyWifiNetwork,
                mIgnoreLossAudioFocus,
                mMusicItem,
                mPlaybackState,
                mAudioSessionId,
                mBufferingPercent,
                mBufferingPercentUpdateTime,
                mStalled,
                mErrorCode,
                mErrorMessage);
    }

    protected PlayerState(Parcel in) {
        mPlayProgress = in.readInt();
        mPlayProgressUpdateTime = in.readLong();
        mSoundQuality = Player.SoundQuality.values()[in.readInt()];
        mAudioEffectEnabled = in.readByte() != 0;
        mOnlyWifiNetwork = in.readByte() != 0;
        mIgnoreLossAudioFocus = in.readByte() != 0;
        mMusicItem = in.readParcelable(Thread.currentThread().getContextClassLoader());

        mPlaybackState = Player.PlaybackState.values()[in.readInt()];
        mAudioSessionId = in.readInt();
        mBufferingPercent = in.readInt();
        mBufferingPercentUpdateTime = in.readLong();
        mStalled = in.readByte() != 0;
        mErrorCode = in.readInt();
        mErrorMessage = in.readString();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(mPlayProgress);
        dest.writeLong(mPlayProgressUpdateTime);
        dest.writeInt(mSoundQuality.ordinal());
        dest.writeByte((byte) (mAudioEffectEnabled ? 1 : 0));
        dest.writeByte((byte) (mOnlyWifiNetwork ? 1 : 0));
        dest.writeByte((byte) (mIgnoreLossAudioFocus ? 1 : 0));
        dest.writeParcelable(mMusicItem, flags);

        dest.writeInt(mPlaybackState.ordinal());
        dest.writeInt(mAudioSessionId);
        dest.writeInt(mBufferingPercent);
        dest.writeLong(mBufferingPercentUpdateTime);
        dest.writeByte((byte) (mStalled ? 1 : 0));
        dest.writeInt(mErrorCode);
        dest.writeString(mErrorMessage);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<PlayerState> CREATOR = new Creator<PlayerState>() {
        @Override
        public PlayerState createFromParcel(Parcel in) {
            return new PlayerState(in);
        }

        @Override
        public PlayerState[] newArray(int size) {
            return new PlayerState[size];
        }
    };
}
