package snow.player;

import android.content.Context;

import androidx.annotation.NonNull;

import com.google.common.base.Preconditions;
import com.tencent.mmkv.MMKV;

/**
 * 用于持久化保存播放器状态（支持跨进程访问）。
 */
public class PersistentPlayerConfig extends PlayerConfig {
    private static final String KEY_PLAYER_TYPE = "player_type";
    private static final String KEY_SOUND_QUALITY = "sound_quality";
    private static final String KEY_AUDIO_EFFECT_ENABLED = "audio_effect_enabled";
    private static final String KEY_ONLY_WIFI_NETWORK = "only_wifi_network";
    private static final String KEY_IGNORE_LOSS_AUDIO_FOCUS = "ignore_loss_audio_focus";

    private MMKV mMMKV;

    public PersistentPlayerConfig(@NonNull Context context, @NonNull String id) {
        Preconditions.checkNotNull(context);
        Preconditions.checkNotNull(id);

        MMKV.initialize(context);

        mMMKV = MMKV.mmkvWithID(id, MMKV.MULTI_PROCESS_MODE);

        super.setPlayerType(PlayerManager.PlayerType.values()[mMMKV.decodeInt(KEY_PLAYER_TYPE, 0)]);
        super.setSoundQuality(Player.SoundQuality.values()[mMMKV.decodeInt(KEY_SOUND_QUALITY, 0)]);
        super.setAudioEffectEnabled(mMMKV.decodeBool(KEY_AUDIO_EFFECT_ENABLED, false));
        super.setOnlyWifiNetwork(mMMKV.decodeBool(KEY_ONLY_WIFI_NETWORK, false));
        super.setIgnoreLossAudioFocus(mMMKV.decodeBool(KEY_IGNORE_LOSS_AUDIO_FOCUS, false));
    }

    @Override
    public void setPlayerType(PlayerManager.PlayerType playerType) {
        super.setPlayerType(playerType);

        mMMKV.encode(KEY_PLAYER_TYPE, playerType.ordinal());
    }

    @Override
    public void setSoundQuality(@NonNull Player.SoundQuality soundQuality) {
        super.setSoundQuality(soundQuality);

        mMMKV.encode(KEY_SOUND_QUALITY, soundQuality.ordinal());
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
}
