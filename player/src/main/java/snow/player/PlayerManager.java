package snow.player;

import android.os.IBinder;

import androidx.annotation.NonNull;

import channel.helper.Channel;

@Channel
public interface PlayerManager {
    int MODE_PLAYLIST = 0;
    int MODE_RADIO_STATION = 1;

    void setPlayerMode(int mode);

    void registerPlayerStateListener(@NonNull String token, IBinder observer);

    void unregisterPlayerStateListener(@NonNull String token);
}
