package snow.player.state;

import snow.player.Player;

public interface PlayerStateListener extends Player.OnPlaybackStateChangeListener,
        Player.OnBufferingPercentChangeListener,
        Player.OnPlayingMusicItemChangeListener,
        Player.OnSeekCompleteListener {

}
