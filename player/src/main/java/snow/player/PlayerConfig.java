package snow.player;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;

import com.google.common.base.Objects;
import com.google.common.base.Preconditions;

/**
 * 用于保存播放器的通用配置信息。
 */
public class PlayerConfig implements Parcelable {
    private PlayerManager.PlayerType mPlayerType;
    private Player.SoundQuality mSoundQuality;
    private boolean mAudioEffectEnabled;
    private boolean mOnlyWifiNetwork;
    private boolean mIgnoreLossAudioFocus;

    public PlayerConfig() {
        mPlayerType = PlayerManager.PlayerType.PLAYLIST;
        mSoundQuality = Player.SoundQuality.STANDARD;
        mAudioEffectEnabled = false;
        mOnlyWifiNetwork = false;
        mIgnoreLossAudioFocus = false;
    }

    public PlayerConfig(PlayerConfig source) {
        mPlayerType = source.mPlayerType;
        mSoundQuality = source.mSoundQuality;
        mAudioEffectEnabled = source.mAudioEffectEnabled;
        mOnlyWifiNetwork = source.mOnlyWifiNetwork;
        mIgnoreLossAudioFocus = source.mIgnoreLossAudioFocus;
    }

    /**
     * 获取播放器类型。
     *
     * @see PlayerManager.PlayerType#PLAYLIST
     * @see PlayerManager.PlayerType#RADIO_STATION
     */
    public PlayerManager.PlayerType getPlayerType() {
        return mPlayerType;
    }

    /**
     * 设置播放器类型。
     *
     * @see PlayerManager.PlayerType
     */
    public void setPlayerType(PlayerManager.PlayerType playerType) {
        mPlayerType = playerType;
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PlayerConfig)) return false;
        PlayerConfig that = (PlayerConfig) o;
        return mPlayerType == that.mPlayerType &&
                mAudioEffectEnabled == that.mAudioEffectEnabled &&
                mOnlyWifiNetwork == that.mOnlyWifiNetwork &&
                mIgnoreLossAudioFocus == that.mIgnoreLossAudioFocus &&
                mSoundQuality == that.mSoundQuality;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(mSoundQuality,
                mAudioEffectEnabled,
                mOnlyWifiNetwork,
                mIgnoreLossAudioFocus);
    }

    protected PlayerConfig(Parcel in) {
        mPlayerType = PlayerManager.PlayerType.values()[in.readInt()];
        mSoundQuality = Player.SoundQuality.values()[in.readInt()];
        mAudioEffectEnabled = in.readByte() != 0;
        mOnlyWifiNetwork = in.readByte() != 0;
        mIgnoreLossAudioFocus = in.readByte() != 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(mPlayerType.ordinal());
        dest.writeInt(mSoundQuality.ordinal());
        dest.writeByte((byte) (mAudioEffectEnabled ? 1 : 0));
        dest.writeByte((byte) (mOnlyWifiNetwork ? 1 : 0));
        dest.writeByte((byte) (mIgnoreLossAudioFocus ? 1 : 0));
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<PlayerConfig> CREATOR = new Creator<PlayerConfig>() {
        @Override
        public PlayerConfig createFromParcel(Parcel in) {
            return new PlayerConfig(in);
        }

        @Override
        public PlayerConfig[] newArray(int size) {
            return new PlayerConfig[size];
        }
    };
}
