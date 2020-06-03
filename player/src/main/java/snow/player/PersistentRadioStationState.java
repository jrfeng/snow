package snow.player;

import android.content.Context;

import androidx.annotation.NonNull;

import com.google.common.base.Preconditions;
import com.tencent.mmkv.MMKV;

import snow.player.radio.RadioStation;

import static snow.player.PersistentPlaylistState.KEY_AUDIO_EFFECT_ENABLED;
import static snow.player.PersistentPlaylistState.KEY_IGNORE_LOSS_AUDIO_FOCUS;
import static snow.player.PersistentPlaylistState.KEY_ONLY_WIFI_NETWORK;
import static snow.player.PersistentPlaylistState.KEY_PLAY_PROGRESS;
import static snow.player.PersistentPlaylistState.KEY_SOUND_QUALITY;

/**
 * 用于对 “电台” 状态进行持久化。
 */
class PersistentRadioStationState extends RadioStationState {
    private static final String KEY_RADIO_STATION = "radio_station";

    private MMKV mMMKV;

    PersistentRadioStationState(@NonNull Context context, @NonNull String id) {
        Preconditions.checkNotNull(context);
        Preconditions.checkNotNull(id);

        MMKV.initialize(context);

        mMMKV = MMKV.mmkvWithID(id);

        super.setPlayProgress(mMMKV.decodeLong(KEY_PLAY_PROGRESS, 0L));
        super.setSoundQuality(mMMKV.decodeInt(KEY_SOUND_QUALITY, Player.SoundQuality.STANDARD));
        super.setAudioEffectEnabled(mMMKV.decodeBool(KEY_AUDIO_EFFECT_ENABLED, false));
        super.setOnlyWifiNetwork(mMMKV.decodeBool(KEY_ONLY_WIFI_NETWORK, true));
        super.setIgnoreLossAudioFocus(mMMKV.decodeBool(KEY_IGNORE_LOSS_AUDIO_FOCUS, false));

        super.setRadioStation(mMMKV.decodeParcelable(KEY_RADIO_STATION, RadioStation.class, new RadioStation()));
    }

    @Override
    void setPlayProgress(long playProgress) {
        super.setPlayProgress(playProgress);

        mMMKV.encode(KEY_PLAY_PROGRESS, playProgress);
    }

    @Override
    void setSoundQuality(int soundQuality) {
        super.setSoundQuality(soundQuality);

        mMMKV.encode(KEY_SOUND_QUALITY, soundQuality);
    }

    @Override
    void setAudioEffectEnabled(boolean audioEffectEnabled) {
        super.setAudioEffectEnabled(audioEffectEnabled);

        mMMKV.encode(KEY_AUDIO_EFFECT_ENABLED, audioEffectEnabled);
    }

    @Override
    void setOnlyWifiNetwork(boolean onlyWifiNetwork) {
        super.setOnlyWifiNetwork(onlyWifiNetwork);

        mMMKV.encode(KEY_ONLY_WIFI_NETWORK, onlyWifiNetwork);
    }

    @Override
    void setIgnoreLossAudioFocus(boolean ignoreLossAudioFocus) {
        super.setIgnoreLossAudioFocus(ignoreLossAudioFocus);

        mMMKV.encode(KEY_IGNORE_LOSS_AUDIO_FOCUS, ignoreLossAudioFocus);
    }

    @Override
    public void setRadioStation(@NonNull RadioStation radioStation) {
        Preconditions.checkNotNull(radioStation);
        super.setRadioStation(radioStation);

        mMMKV.encode(KEY_RADIO_STATION, radioStation);
    }
}
