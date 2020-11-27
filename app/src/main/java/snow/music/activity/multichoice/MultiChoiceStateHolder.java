package snow.music.activity.multichoice;

import android.os.Parcelable;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.common.base.Preconditions;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import snow.music.store.Music;

public class MultiChoiceStateHolder {
    private static MultiChoiceStateHolder sInstance;

    private List<Music> mMusicList;
    private boolean mFavorite;
    private boolean mItemRemovable;
    @Nullable
    private Parcelable mLayoutManagerState;
    private String mMusicListName;
    private int position;

    private MultiChoiceStateHolder() {
        mMusicList = Collections.emptyList();
        mMusicListName = "";
    }

    public synchronized static MultiChoiceStateHolder getInstance() {
        if (sInstance == null) {
            sInstance = new MultiChoiceStateHolder();
        }
        return sInstance;
    }

    public void setMusicList(@NonNull List<Music> musicList) {
        Preconditions.checkNotNull(musicList);

        mMusicList = new ArrayList<>(musicList);
    }

    @NonNull
    public List<Music> getMusicList() {
        return mMusicList;
    }

    public int getMusicListSize() {
        return mMusicList.size();
    }

    public void remove(@NonNull List<Music> musics) {
        Preconditions.checkNotNull(musics);
        mMusicList.removeAll(musics);
    }

    public void setFavorite(boolean favorite) {
        mFavorite = favorite;
    }

    public boolean isFavorite() {
        return mFavorite;
    }

    public void setItemRemovable(boolean itemRemovable) {
        mItemRemovable = itemRemovable;
    }

    public boolean isItemRemovable() {
        return mItemRemovable;
    }

    public void setLayoutManagerState(@Nullable Parcelable layoutManagerState) {
        mLayoutManagerState = layoutManagerState;
    }

    @Nullable
    public Parcelable consumeLayoutManagerState() {
        Parcelable result = mLayoutManagerState;
        mLayoutManagerState = null;
        return result;
    }

    public void setMusicListName(String musicListName) {
        mMusicListName = musicListName;
    }

    public String getMusicListName() {
        return mMusicListName;
    }

    public void setPosition(int position) {
        this.position = position;
    }

    public int getPosition() {
        return position;
    }

    public void release() {
        synchronized (MultiChoiceStateHolder.class) {
            sInstance = null;
        }
    }
}
