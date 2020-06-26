package snow.player;

public interface PlayerStateListener extends Player.OnPlaybackStateChangeListener,
        Player.OnStalledChangeListener,
        Player.OnBufferingPercentChangeListener,
        Player.OnPlayingMusicItemChangeListener,
        Player.OnSeekCompleteListener {

}
