package snow.music;

import androidx.multidex.MultiDexApplication;

import java.io.File;

import snow.music.store.MusicStore;

public class Application extends MultiDexApplication {
    @Override
    public void onCreate() {
        super.onCreate();

        MusicStore.init(this);
    }
}
