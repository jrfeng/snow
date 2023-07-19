package snow.player.debug;

import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import snow.player.PlayerService;
import snow.player.effect.AudioEffectManager;
import snow.player.exo.ExoMusicPlayer;
import snow.player.audio.MusicItem;
import snow.player.audio.MusicPlayer;
import snow.player.ui.equalizer.AndroidAudioEffectManager;

public class MyPlayerService extends PlayerService {
    @Override
    public void onCreate() {
        super.onCreate();

        setMaxIDLETime(10);
        setIgnoreAudioFocus(false);
    }

    @NonNull
    @Override
    protected MusicPlayer onCreateMusicPlayer(@NonNull Context context, @NonNull MusicItem musicItem, @NonNull Uri uri) {
        return new ExoMusicPlayer(context, uri);
    }

    @Nullable
    @Override
    protected AudioEffectManager onCreateAudioEffectManager() {
        return new AndroidAudioEffectManager();
    }

    @Nullable
    @Override
    protected Class<? extends AppWidgetProvider> getAppWidget() {
        return ExampleAppWidgetProvider.class;
    }
}
