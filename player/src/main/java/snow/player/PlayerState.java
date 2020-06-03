package snow.player;

import android.os.Parcel;
import android.os.Parcelable;

class PlayerState implements Parcelable {
    private int mPlaybackState;
    private int mSoundQuality;
    private boolean mAudioEffectEnabled;
    private boolean mOnlyWifiNetwork;
    private boolean mIgnoreLossAudioFocus;

    PlayerState() {
    }

    int getPlaybackState() {
        return mPlaybackState;
    }

    void setPlaybackState(int playbackState) {
        mPlaybackState = playbackState;
    }

    int getSoundQuality() {
        return mSoundQuality;
    }

    void setSoundQuality(int soundQuality) {
        mSoundQuality = soundQuality;
    }

    boolean isAudioEffectEnabled() {
        return mAudioEffectEnabled;
    }

    void setAudioEffectEnabled(boolean audioEffectEnabled) {
        mAudioEffectEnabled = audioEffectEnabled;
    }

    boolean isOnlyWifiNetwork() {
        return mOnlyWifiNetwork;
    }

    void setOnlyWifiNetwork(boolean onlyWifiNetwork) {
        mOnlyWifiNetwork = onlyWifiNetwork;
    }

    boolean isIgnoreLossAudioFocus() {
        return mIgnoreLossAudioFocus;
    }

    void setIgnoreLossAudioFocus(boolean ignoreLossAudioFocus) {
        mIgnoreLossAudioFocus = ignoreLossAudioFocus;
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
