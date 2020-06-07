package snow.player.state;

import android.os.Parcel;

import androidx.annotation.NonNull;

import com.google.common.base.Objects;
import com.google.common.base.Preconditions;

import snow.player.radio.RadioStation;

/**
 * 用于保存 “电台” 的状态。
 */
public class RadioStationState extends PlayerState {
    private RadioStation mRadioStation;

    /**
     * 创建一个 RadioStationState 对象。
     */
    public RadioStationState() {
        mRadioStation = new RadioStation();
    }

    public RadioStationState(RadioStationState source) {
        mRadioStation = new RadioStation(source.mRadioStation.getId(),
                source.mRadioStation.getName(),
                source.mRadioStation.getDescription());
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        RadioStationState that = (RadioStationState) o;
        return Objects.equal(mRadioStation, that.mRadioStation);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(super.hashCode(), mRadioStation);
    }

    @NonNull
    @Override
    protected RadioStationState clone() throws CloneNotSupportedException {
        super.clone();
        return new RadioStationState(this);
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
