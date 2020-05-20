package snow.player.radio;

import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.common.base.Objects;
import com.google.common.base.Preconditions;

public class RadioStation implements Parcelable {
    private String mId;
    private String mName;
    private String mDescription;
    private Bundle mExtra;

    public RadioStation(@NonNull String id, @NonNull String name, @NonNull String description) {
        Preconditions.checkNotNull(id);
        Preconditions.checkNotNull(name);
        Preconditions.checkNotNull(description);

        mId = id;
        mName = name;
        mDescription = description;
    }

    public RadioStation() {
        mId = "";
        mName = "unknown";
        mDescription = "";
    }

    protected RadioStation(Parcel in) {
        mId = in.readString();
        mName = in.readString();
        mDescription = in.readString();
        mExtra = in.readBundle(Thread.currentThread().getContextClassLoader());
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(mId);
        dest.writeString(mName);
        dest.writeString(mDescription);
        dest.writeBundle(mExtra);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<RadioStation> CREATOR = new Creator<RadioStation>() {
        @Override
        public RadioStation createFromParcel(Parcel in) {
            return new RadioStation(in);
        }

        @Override
        public RadioStation[] newArray(int size) {
            return new RadioStation[size];
        }
    };

    public String getId() {
        return mId;
    }

    public void setId(@NonNull String id) {
        Preconditions.checkNotNull(id);
        mId = id;
    }

    public String getName() {
        return mName;
    }

    public void setName(@NonNull String name) {
        Preconditions.checkNotNull(name);
        mName = name;
    }

    public String getDescription() {
        return mDescription;
    }

    public void setDescription(@NonNull String description) {
        Preconditions.checkNotNull(description);
        mDescription = description;
    }

    @Nullable
    public Bundle getExtra() {
        return mExtra;
    }

    public void setExtra(@Nullable Bundle extra) {
        mExtra = extra;
    }

    /**
     * ignore extra.
     */
    @Override
    public boolean equals(@Nullable Object obj) {
        if (!(obj instanceof RadioStation)) {
            return false;
        }

        RadioStation other = (RadioStation) obj;

        return Objects.equal(mId, other.mId)
                && Objects.equal(mName, other.mName)
                && Objects.equal(mDescription, other.mDescription);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(mId, mName, mDescription);
    }

    @NonNull
    @Override
    public String toString() {
        return "[" + mId + ", " + mName + ", " + mDescription + "]";
    }
}
