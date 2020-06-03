package snow.player;

import android.os.Parcel;
import android.os.Parcelable;

import com.google.common.base.Objects;

/**
 * 用于保存基本的播放器状态。
 */
class PlayerState implements Parcelable {
    private int mPlaybackState;
    private int mSoundQuality;
    private boolean mAudioEffectEnabled;
    private boolean mOnlyWifiNetwork;
    private boolean mIgnoreLossAudioFocus;

    PlayerState() {
        mPlaybackState = Player.PlaybackState.UNKNOWN;
        mSoundQuality = Player.SoundQuality.STANDARD;
        mAudioEffectEnabled = false;
        mOnlyWifiNetwork = true;
        mIgnoreLossAudioFocus = false;
    }

    PlayerState(PlayerState source) {
        mPlaybackState = source.mPlaybackState;
        mSoundQuality = source.mSoundQuality;
        mAudioEffectEnabled = source.mAudioEffectEnabled;
        mOnlyWifiNetwork = source.mOnlyWifiNetwork;
        mIgnoreLossAudioFocus = source.mIgnoreLossAudioFocus;
    }

    /**
     * 获取播放状态。
     *
     * @return 返回当前播放状态。
     * @see Player.PlaybackState
     */
    int getPlaybackState() {
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
    void setPlaybackState(int playbackState) {
        mPlaybackState = playbackState;
    }

    /**
     * 获取首选音质。
     *
     * @return 当前的首选音质。
     * @see Player.SoundQuality
     */
    int getSoundQuality() {
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
    void setSoundQuality(int soundQuality) {
        mSoundQuality = soundQuality;
    }

    /**
     * 判断是否已启用音频特效。
     *
     * @return 是否已启用音频特效。
     */
    boolean isAudioEffectEnabled() {
        return mAudioEffectEnabled;
    }

    /**
     * 设置是否启用音频特效。
     *
     * @param audioEffectEnabled 是否启用音频特效。
     */
    void setAudioEffectEnabled(boolean audioEffectEnabled) {
        mAudioEffectEnabled = audioEffectEnabled;
    }

    /**
     * 是否只允许在 WiFi 网络下联网（默认为 true）。
     *
     * @return 如果返回 true，则表示只允许在 WiFi 网络下联网（默认为 true）。
     */
    boolean isOnlyWifiNetwork() {
        return mOnlyWifiNetwork;
    }

    /**
     * 设置是否只允许在 WiFi 网络下联网（默认为 true）。
     *
     * @param onlyWifiNetwork 是否只允许在 WiFi 网络下联网（默认为 true）。
     */
    void setOnlyWifiNetwork(boolean onlyWifiNetwork) {
        mOnlyWifiNetwork = onlyWifiNetwork;
    }

    /**
     * 判断是否忽略音频焦点丢失事件。
     *
     * @return 是否忽略音频焦点丢失事件。
     */
    boolean isIgnoreLossAudioFocus() {
        return mIgnoreLossAudioFocus;
    }

    /**
     * 设置是否忽略音频焦点丢失事件。
     *
     * @param ignoreLossAudioFocus 是否忽略音频焦点丢失事件。
     */
    void setIgnoreLossAudioFocus(boolean ignoreLossAudioFocus) {
        mIgnoreLossAudioFocus = ignoreLossAudioFocus;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PlayerState)) return false;

        PlayerState other = (PlayerState) o;

        return Objects.equal(mPlaybackState, other.mPlaybackState)
                && Objects.equal(mSoundQuality, other.mSoundQuality)
                && Objects.equal(mAudioEffectEnabled, other.mAudioEffectEnabled)
                && Objects.equal(mOnlyWifiNetwork, other.mOnlyWifiNetwork)
                && Objects.equal(mIgnoreLossAudioFocus, other.mIgnoreLossAudioFocus);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(mPlaybackState,
                mSoundQuality,
                mAudioEffectEnabled,
                mOnlyWifiNetwork,
                mIgnoreLossAudioFocus);
    }

    protected PlayerState(Parcel in) {
        mPlaybackState = in.readInt();
        mSoundQuality = in.readInt();
        mAudioEffectEnabled = in.readByte() != 0;
        mOnlyWifiNetwork = in.readByte() != 0;
        mIgnoreLossAudioFocus = in.readByte() != 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(mPlaybackState);
        dest.writeInt(mSoundQuality);
        dest.writeByte((byte) (mAudioEffectEnabled ? 1 : 0));
        dest.writeByte((byte) (mOnlyWifiNetwork ? 1 : 0));
        dest.writeByte((byte) (mIgnoreLossAudioFocus ? 1 : 0));
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
