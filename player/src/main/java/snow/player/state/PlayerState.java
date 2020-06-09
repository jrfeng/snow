package snow.player.state;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.common.base.Objects;

import snow.player.MusicItem;
import snow.player.Player;

/**
 * 用于保存基本的播放器状态。
 */
public class PlayerState implements Parcelable, Cloneable {
    private long mPlayProgress;
    private long mPlayProgressUpdateTime;
    private boolean mLooping;
    private int mPlaybackState;
    private int mSoundQuality;
    private boolean mAudioEffectEnabled;
    private boolean mOnlyWifiNetwork;
    private boolean mIgnoreLossAudioFocus;
    @Nullable
    private MusicItem mMusicItem;

    public PlayerState() {
        mPlayProgress = 0;
        mPlayProgressUpdateTime = System.currentTimeMillis();
        mLooping = false;
        mPlaybackState = Player.PlaybackState.UNKNOWN;
        mSoundQuality = Player.SoundQuality.STANDARD;
        mAudioEffectEnabled = false;
        mOnlyWifiNetwork = true;
        mIgnoreLossAudioFocus = false;
    }

    public PlayerState(PlayerState source) {
        mPlayProgress = source.mPlayProgress;
        mPlayProgressUpdateTime = source.mPlayProgressUpdateTime;
        mLooping = source.mLooping;
        mPlaybackState = source.mPlaybackState;
        mSoundQuality = source.mSoundQuality;
        mAudioEffectEnabled = source.mAudioEffectEnabled;
        mOnlyWifiNetwork = source.mOnlyWifiNetwork;
        mIgnoreLossAudioFocus = source.mIgnoreLossAudioFocus;
        mMusicItem = new MusicItem(source.mMusicItem);
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
     * 获取上次播放进度的更新时间。
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PlayerState)) return false;

        PlayerState other = (PlayerState) o;

        return Objects.equal(mPlayProgress, other.mPlayProgress)
                && Objects.equal(mPlayProgressUpdateTime, other.mPlayProgressUpdateTime)
                && Objects.equal(mLooping, other.mLooping)
                && Objects.equal(mPlaybackState, other.mPlaybackState)
                && Objects.equal(mSoundQuality, other.mSoundQuality)
                && Objects.equal(mAudioEffectEnabled, other.mAudioEffectEnabled)
                && Objects.equal(mOnlyWifiNetwork, other.mOnlyWifiNetwork)
                && Objects.equal(mIgnoreLossAudioFocus, other.mIgnoreLossAudioFocus)
                && Objects.equal(mMusicItem, other.mMusicItem);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(mPlayProgress,
                mPlayProgressUpdateTime,
                mLooping,
                mPlaybackState,
                mSoundQuality,
                mAudioEffectEnabled,
                mOnlyWifiNetwork,
                mIgnoreLossAudioFocus,
                mMusicItem);
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
        mPlaybackState = in.readInt();
        mSoundQuality = in.readInt();
        mAudioEffectEnabled = in.readByte() != 0;
        mOnlyWifiNetwork = in.readByte() != 0;
        mIgnoreLossAudioFocus = in.readByte() != 0;
        mMusicItem = in.readParcelable(Thread.currentThread().getContextClassLoader());
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeLong(mPlayProgress);
        dest.writeLong(mPlayProgressUpdateTime);
        dest.writeByte((byte) (mLooping ? 1 : 0));
        dest.writeInt(mPlaybackState);
        dest.writeInt(mSoundQuality);
        dest.writeByte((byte) (mAudioEffectEnabled ? 1 : 0));
        dest.writeByte((byte) (mOnlyWifiNetwork ? 1 : 0));
        dest.writeByte((byte) (mIgnoreLossAudioFocus ? 1 : 0));
        dest.writeParcelable(mMusicItem, flags);
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
