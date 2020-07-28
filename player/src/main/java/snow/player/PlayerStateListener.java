package snow.player;

import channel.helper.Channel;

@Channel
public interface PlayerStateListener extends Player.OnPlaybackStateChangeListener,
        Player.OnStalledChangeListener,
        Player.OnBufferedProgressChangeListener,
        Player.OnPlayingMusicItemChangeListener,
        Player.OnSeekListener,
        Player.OnPositionChangeListener,
        Player.OnPlaylistChangeListener,
        Player.OnPlayModeChangeListener {

}
