package snow.demo;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import snow.player.HistoryRecorder;
import snow.player.Inject;
import snow.player.PlayerService;
import snow.player.SoundQuality;
import snow.player.media.MusicItem;

public class MyFactory extends PlayerService.ComponentFactory {
    @Override
    public boolean isCached(MusicItem musicItem, SoundQuality soundQuality) {
        return false;
    }

    @Inject
    @Nullable
    @Override
    public HistoryRecorder createHistoryRecorder() {
        return new HistoryRecorder() {
            @Override
            public void recordHistory(@NonNull MusicItem musicItem) {
                // DEBUG
                Log.d("App", musicItem.toString());
            }
        };
    }
}
