package snow.player;

import android.os.Parcel;

import androidx.annotation.NonNull;

import com.google.common.base.Preconditions;

import snow.player.radio.RadioStation;

/**
 * 用于保存 “电台” 的状态。
 */
class RadioStationState extends PlayerState {
    private RadioStation mRadioStation;

    /**
     * 创建一个 RadioStationState 对象。
     *
     * @param radioStation “电台”
     */
    RadioStationState(@NonNull RadioStation radioStation) {
        Preconditions.checkNotNull(radioStation);

        mRadioStation = radioStation;
    }

    /**
     * 获取 “电台”。
     */
    public RadioStation getRadioStation() {
        return mRadioStation;
    }

    /**
     * 设置 “电台”
     *
     * @param radioStation 不能为 null。
     */
    public void setRadioStation(@NonNull RadioStation radioStation) {
        Preconditions.checkNotNull(radioStation);
        mRadioStation = radioStation;
    }

    protected RadioStationState(Parcel in) {
        super(in);
        mRadioStation = in.readParcelable(Thread.currentThread().getContextClassLoader());
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        super.writeToParcel(dest, flags);
        dest.writeParcelable(mRadioStation, flags);
    }

    public static final Creator<RadioStationState> CREATOR = new Creator<RadioStationState>() {
        @Override
        public RadioStationState createFromParcel(Parcel in) {
            return new RadioStationState(in);
        }

        @Override
        public RadioStationState[] newArray(int size) {
            return new RadioStationState[size];
        }
    };
}
