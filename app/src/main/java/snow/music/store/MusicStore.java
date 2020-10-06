package snow.music.store;

import android.content.Context;

import androidx.annotation.NonNull;

import com.google.common.base.Preconditions;

import java.io.File;

import io.objectbox.Box;
import io.objectbox.BoxStore;

public class MusicStore {
    private static MusicStore mInstance;

    private Box<Music> mMusicBox;
    private Box<MusicListEntity> mMusicListEntityBox;

    private MusicStore(BoxStore boxStore) {
        mMusicBox = boxStore.boxFor(Music.class);
        mMusicListEntityBox = boxStore.boxFor(MusicListEntity.class);
    }

    public synchronized static void init(@NonNull Context context) {
        Preconditions.checkNotNull(context);

        if (mInstance != null) {
            return;
        }

        BoxStore boxStore = MyObjectBox.builder()
                .directory(new File(context.getFilesDir(), "music_store"))
                .build();

        mInstance = new MusicStore(boxStore);
    }

    public synchronized static void init(@NonNull BoxStore boxStore) {
        Preconditions.checkNotNull(boxStore);

        if (mInstance != null) {
            return;
        }

        mInstance = new MusicStore(boxStore);
    }

    public static MusicStore getInstance() throws IllegalStateException {
        if (mInstance == null) {
            throw new IllegalStateException("music store not init yet.");
        }

        return mInstance;
    }


}
