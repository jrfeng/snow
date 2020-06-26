package snow.player.playlist;

import channel.helper.Channel;
import snow.player.PlayerStateListener;

@Channel
public interface PlaylistStateListener extends PlayerStateListener,
        PlaylistPlayer.OnPositionChangeListener,
        PlaylistPlayer.OnPlaylistChangeListener,
        PlaylistPlayer.OnPlayModeChangeListener {

}
