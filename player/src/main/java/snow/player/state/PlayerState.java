package snow.player.state;

import android.content.Context;
import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.common.base.Objects;
import com.google.common.base.Preconditions;

import snow.player.media.MusicItem;
import snow.player.Player;
import snow.player.util.ErrorUtil;

/**
 * 用于保存基本的播放器状态。
 */
public class PlayerState implements Parcelable, Cloneable {
    private long mPlayProgress;
    private long mPlayProgressUpdateTime;
    private boolean mLooping;
    private int mSoundQuality;
    private boolean mAudioEffectEnabled;
    private boolean mOnlyWifiNetwork;
    private boolean mIgnoreLossAudioFocus;
    @Nullable
    private MusicItem mMusicItem;

    // no persistent
    private int mPlaybackState;
    private int mAudioSessionId;
    private int mBufferingPercent;
    private long mBufferingPercentUpdateTime;
    private int mErrorCode;
    private String mErrorMessage;

    public PlayerState() {
        mPlayProgress = 0;
        mPlayProgressUpdateTime = System.currentTimeMillis();
        mLooping = false;
        mSoundQuality = Player.SoundQuality.STANDARD;
        mAudioEffectEnabled = false;
        mOnlyWifiNetwork = true;
        mIgnoreLossAudioFocus = false;

        mPlaybackState = Player.PlaybackState.UNKNOWN;
        mAudioSessionId = 0;
        mBufferingPercent = 0;
        mBufferingPercentUpdateTime = System.currentTimeMillis();
        mErrorCode = ErrorUtil.ERROR_NO_ERROR;
        mErrorMessage = "";
    }

    public PlayerState(PlayerState source) {
        mPlayProgress = source.mPlayProgress;
        mPlayProgressUpdateTime = source.mPlayProgressUpdateTime;
        mLooping = source.mLooping;
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
        mErrorCode = source.mErrorCode;
        mErrorMessage = source.mErrorMessage;
    }

    /**
     * 获取播放进度。
     *
     * @return 播放进度。
     */
    public long getPlayProgress() {
        return mPlayProgress;
    }

    /**
     * 设置播放进度。
     *
     * @param playProgress 播放进度（小于 0 时，相当于设置为 0）。
     */
    public void setPlayProgress(long playProgress) {
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
     * 是否循环播放。
     */
    public boolean isLooping() {
        return mLooping;
    }

    /**
     * 设置是否循环播放。
     *
     * @param looping 是否循环播放。
     */
    public void setLooping(boolean looping) {
        mLooping = looping;
    }

    /**
     * 获取首选音质。
     *
     * @return 当前的首选音质。
     * @see Player.SoundQuality
     */
    public int getSoundQuality() {
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
    public void setSoundQuality(int soundQuality) {
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
     * 是否只允许在 WiFi 网络下联网（默认为 true）。
     *
     * @return 如果返回 true，则表示只允许在 WiFi 网络下联网（默认为 true）。
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
    public int getPlaybackState() {
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
     *                      {@link Player.PlaybackState#STALLED},
     *                      {@link Player.PlaybackState#ERROR}
     * @see Player.PlaybackState
     */
    public void setPlaybackState(int playbackState) {
        mPlaybackState = playbackState;

        if (playbackState != Player.PlaybackState.ERROR) {
            mErrorCode = ErrorUtil.ERROR_NO_ERROR;
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

    /**
     * 获取错误码。如果没有发生任何错误，则返回 {@link ErrorUtil#ERROR_NO_ERROR}。
     *
     * @see ErrorUtil
     */
    public int getErrorCode() {
        return mErrorCode;
    }

    /**
     * 设置错误码。
     *
     * @param errorCode 错误码。预定义的错误码：{@link ErrorUtil#ERROR_NO_ERROR},
     *                  {@link ErrorUtil#ERROR_ONLY_WIFI_NETWORK},
     *                  {@link ErrorUtil#ERROR_PLAYER_ERROR},
     *                  {@link ErrorUtil#ERROR_NETWORK_UNAVAILABLE}
     * @see ErrorUtil
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
     * @see ErrorUtil#getErrorMessage(Context, int)
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
                && Objects.equal(mLooping, other.mLooping)
                && Objects.equal(mSoundQuality, other.mSoundQuality)
                && Objects.equal(mAudioEffectEnabled, other.mAudioEffectEnabled)
                && Objects.equal(mOnlyWifiNetwork, other.mOnlyWifiNetwork)
                && Objects.equal(mIgnoreLossAudioFocus, other.mIgnoreLossAudioFocus)
                && Objects.equal(mMusicItem, other.mMusicItem)
                && Objects.equal(mPlaybackState, other.mPlaybackState)
                && Objects.equal(mAudioSessionId, other.mAudioSessionId)
                && Objects.equal(mBufferingPercent, other.mBufferingPercent)
                && Objects.equal(mBufferingPercentUpdateTime, other.mBufferingPercentUpdateTime)
                && Objects.equal(mErrorCode, other.mErrorCode)
                && Objects.equal(mErrorMessage, other.mErrorMessage);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(mPlayProgress,
                mPlayProgressUpdateTime,
                mLooping,
                mSoundQuality,
                mAudioEffectEnabled,
                mOnlyWifiNetwork,
                mIgnoreLossAudioFocus,
                mMusicItem,
                mPlaybackState,
                mAudioSessionId,
                mBufferingPercent,
                mBufferingPercentUpdateTime,
                mErrorCode,
                mErrorMessage);
    }

    @NonNull
    @Override
    protected PlayerState clone() throws CloneNotSupportedException {
        super.clone();
        return new PlayerState(this);
    }

    protected PlayerState(Parcel in) {
        mPlayProgress = in.readLong();
        mPlayProgressUpdateTime = in.readLong();
        mLooping = in.readByte() != 0;
        mSoundQuality = in.readInt();
        mAudioEffectEnabled = in.readByte() != 0;
        mOnlyWifiNetwork = in.readByte() != 0;
        mIgnoreLossAudioFocus = in.readByte() != 0;
        mMusicItem = in.readParcelable(Thread.currentThread().getContextClassLoader());

        mPlaybackState = in.readInt();
        mAudioSessionId = in.readInt();
        mBufferingPercent = in.readInt();
        mBufferingPercentUpdateTime = in.readLong();
        mErrorCode = in.readInt();
        mErrorMessage = in.readString();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeLong(mPlayProgress);
        dest.writeLong(mPlayProgressUpdateTime);
        dest.writeByte((byte) (mLooping ? 1 : 0));
        dest.writeInt(mSoundQuality);
        dest.writeByte((byte) (mAudioEffectEnabled ? 1 : 0));
        dest.writeByte((byte) (mOnlyWifiNetwork ? 1 : 0));
        dest.writeByte((byte) (mIgnoreLossAudioFocus ? 1 : 0));
        dest.writeParcelable(mMusicItem, flags);

        dest.writeInt(mPlaybackState);
        dest.writeInt(mAudioSessionId);
        dest.writeInt(mBufferingPercent);
        dest.writeLong(mBufferingPercentUpdateTime);
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
