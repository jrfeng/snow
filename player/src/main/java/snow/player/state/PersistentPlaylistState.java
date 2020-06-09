package snow.player.state;

import android.content.Context;

import androidx.annotation.NonNull;

import com.google.common.base.Preconditions;
import com.tencent.mmkv.MMKV;

import snow.player.Player;
import snow.player.playlist.PlaylistPlayer;

/**
 * 用于对播放队列的状态进行持久化。
 */
public class PersistentPlaylistState extends PlaylistState {
    private static final String KEY_PLAY_PROGRESS = "play_progress";
    private static final String KEY_PLAY_PROGRESS_UPDATE_TIME = "play_progress_update_time";
    private static final String KEY_SOUND_QUALITY = "sound_quality";
    private static final String KEY_AUDIO_EFFECT_ENABLED = "audio_effect_enabled";
    private static final String KEY_ONLY_WIFI_NETWORK = "only_wifi_network";
    private static final String KEY_IGNORE_LOSS_AUDIO_FOCUS = "ignore_loss_audio_focus";

    private static final String KEY_POSITION = "position";
    private static final String KEY_PLAY_MODE = "play_mode";

    private MMKV mMMKV;

    public PersistentPlaylistState(@NonNull Context context, @NonNull String id) {
        Preconditions.checkNotNull(context);
        Preconditions.checkNotNull(id);

        MMKV.initialize(context);

        mMMKV = MMKV.mmkvWithID(id);

        super.setPlayProgress(mMMKV.decodeLong(KEY_PLAY_PROGRESS, 0L));
        super.setSoundQuality(mMMKV.decodeInt(KEY_SOUND_QUALITY, Player.SoundQuality.STANDARD));
        super.setAudioEffectEnabled(mMMKV.decodeBool(KEY_AUDIO_EFFECT_ENABLED, false));
        super.setOnlyWifiNetwork(mMMKV.decodeBool(KEY_ONLY_WIFI_NETWORK, true));
        super.setIgnoreLossAudioFocus(mMMKV.decodeBool(KEY_IGNORE_LOSS_AUDIO_FOCUS, false));

        super.setPosition(mMMKV.decodeInt(KEY_POSITION, 0));
        super.setPlayMode(mMMKV.decodeInt(KEY_PLAY_MODE, PlaylistPlayer.PlayMode.SEQUENTIAL));
    }

    @Override
    public void setPlayProgress(long playProgress) {
        super.setPlayProgress(playProgress);

        mMMKV.encode(KEY_PLAY_PROGRESS, playProgress);
    }

    @Override
    public void setPlayProgressUpdateTime(long updateTime) {
        super.setPlayProgressUpdateTime(updateTime);

        mMMKV.encode(KEY_PLAY_PROGRESS_UPDATE_TIME, updateTime);
    }

    @Override
    public void setSoundQuality(int soundQuality) {
        super.setSoundQuality(soundQuality);

        mMMKV.encode(KEY_SOUND_QUALITY, soundQuality);
    }

    @Override
    public void setAudioEffectEnabled(boolean audioEffectEnabled) {
        super.setAudioEffectEnabled(audioEffectEnabled);

        mMMKV.encode(KEY_AUDIO_EFFECT_ENABLED, audioEffectEnabled);
    }

    @Override
    public void setOnlyWifiNetwork(boolean onlyWifiNetwork) {
        super.setOnlyWifiNetwork(onlyWifiNetwork);

        mMMKV.encode(KEY_ONLY_WIFI_NETWORK, onlyWifiNetwork);
    }

    @Override
    public void setIgnoreLossAudioFocus(boolean ignoreLossAudioFocus) {
        super.setIgnoreLossAudioFocus(ignoreLossAudioFocus);

        mMMKV.encode(KEY_IGNORE_LOSS_AUDIO_FOCUS, ignoreLossAudioFocus);
    }

    @Override
    public void setPosition(int position) {
        super.setPosition(position);

        mMMKV.encode(KEY_POSITION, position);
    }

    @Override
    public void setPlayMode(int playMode) {
        super.setPlayMode(playMode);

        mMMKV.encode(KEY_PLAY_MODE, playMode);
    }

    public PlaylistState getPlaylistState() {
        return new PlaylistState(this);
    }
}
