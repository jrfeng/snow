package snow.player.state;

import channel.helper.Channel;
import snow.player.playlist.PlaylistPlayer;

@Channel
public interface PlaylistStateListener extends PlayerStateListener,
        PlaylistPlayer.OnPositionChangeListener,
        PlaylistPlayer.OnPlaylistChangeListener,
        PlaylistPlayer.OnPlayModeChangeListener {

}
