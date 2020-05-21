package snow.player.playlist;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.common.base.Objects;
import com.google.common.base.Preconditions;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

import snow.player.MusicItem;

public final class Playlist implements Iterable<MusicItem>, Parcelable {
    private ArrayList<MusicItem> mMusicItems;

    Playlist(@NonNull List<MusicItem> items) {
        Preconditions.checkNotNull(items);
        mMusicItems = new ArrayList<>(items);
    }

    public boolean contains(MusicItem musicItem) {
        return mMusicItems.contains(musicItem);
    }

    public MusicItem get(int index) throws IndexOutOfBoundsException {
        return mMusicItems.get(index);
    }

    public int indexOf(MusicItem musicItem) {
        return mMusicItems.indexOf(musicItem);
    }

    public boolean isEmpty() {
        return mMusicItems.isEmpty();
    }

    @NonNull
    @Override
    public Iterator<MusicItem> iterator() {
        return mMusicItems.iterator();
    }

    public int lastIndexOf(MusicItem musicItem) {
        return mMusicItems.lastIndexOf(musicItem);
    }

    public ListIterator<MusicItem> listIterator() {
        return mMusicItems.listIterator();
    }

    public ListIterator<MusicItem> listIterator(int index) {
        return mMusicItems.listIterator(index);
    }

    public int size() {
        return mMusicItems.size();
    }

    public List<MusicItem> getAllMusicItem() {
        return new ArrayList<>(mMusicItems);
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        if (!(obj instanceof Playlist)) {
            return false;
        }

        return Objects.equal(mMusicItems, ((Playlist) obj).mMusicItems);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(mMusicItems);
    }

    // Parcelable
    protected Playlist(Parcel in) {
        mMusicItems = in.createTypedArrayList(MusicItem.CREATOR);
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeTypedList(mMusicItems);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<Playlist> CREATOR = new Creator<Playlist>() {
        @Override
        public Playlist createFromParcel(Parcel in) {
            return new Playlist(in);
        }

        @Override
        public Playlist[] newArray(int size) {
            return new Playlist[size];
        }
    };
}
