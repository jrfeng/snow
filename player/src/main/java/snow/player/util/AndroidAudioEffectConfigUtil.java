package snow.player.util;

import android.media.audiofx.BassBoost;
import android.media.audiofx.Equalizer;
import android.media.audiofx.PresetReverb;
import android.media.audiofx.Virtualizer;
import android.os.Bundle;

import androidx.annotation.NonNull;

/**
 * 用于获取和修改 Android 音频特效的配置信息。
 */
public class AndroidAudioEffectConfigUtil {
    public static final String KEY_SETTING_EQUALIZER = "setting_equalizer";
    public static final String KEY_SETTING_BASS_BOOST = "setting_bass_boost";
    public static final String KEY_SETTING_VIRTUALIZER = "setting_virtualizer";
    public static final String KEY_SETTING_PRESET_REVERB = "setting_preset_reverb";

    /**
     * 从 config 中恢复 Equalizer 的配置。
     *
     * @param config    Bundle 对象，包含音频特效的配置信息，不能为 null
     * @param equalizer 要恢复配置的 Equalizer 对象，不能为 null
     * @return 如果恢复成功，则返回 true；否则返回 false
     */
    public static boolean restoreSettings(@NonNull Bundle config, @NonNull Equalizer equalizer) {
        String settings = config.getString(KEY_SETTING_EQUALIZER);
        if (settings == null || settings.isEmpty()) {
            return false;
        }

        try {
            equalizer.setProperties(new Equalizer.Settings(settings));
            return true;
        } catch (IllegalArgumentException | IllegalStateException | UnsupportedOperationException e) {
            return false;
        }
    }

    /**
     * 从 config 中恢复 BassBoost 的配置。
     *
     * @param config    Bundle 对象，包含音频特效的配置信息，不能为 null
     * @param bassBoost 要恢复配置的 BassBoost 对象，不能为 null
     * @return 如果恢复成功，则返回 true；否则返回 false
     */
    public static boolean restoreSettings(@NonNull Bundle config, @NonNull BassBoost bassBoost) {
        String settings = config.getString(KEY_SETTING_BASS_BOOST);
        if (settings == null || settings.isEmpty()) {
            return false;
        }

        try {
            bassBoost.setProperties(new BassBoost.Settings(settings));
            return true;
        } catch (IllegalArgumentException | IllegalStateException | UnsupportedOperationException e) {
            return false;
        }
    }

    /**
     * 从 config 中恢复 Virtualizer 的配置。
     *
     * @param config      Bundle 对象，包含音频特效的配置信息，不能为 null
     * @param virtualizer 要恢复配置的 Virtualizer 对象，不能为 null
     * @return 如果恢复成功，则返回 true；否则返回 false
     */
    public static boolean restoreSettings(@NonNull Bundle config, @NonNull Virtualizer virtualizer) {
        String settings = config.getString(KEY_SETTING_VIRTUALIZER);
        if (settings == null || settings.isEmpty()) {
            return false;
        }

        try {
            virtualizer.setProperties(new Virtualizer.Settings(settings));
            return true;
        } catch (IllegalArgumentException | IllegalStateException | UnsupportedOperationException e) {
            return false;
        }
    }

    /**
     * 从 config 中恢复 PresetReverb 的配置。
     *
     * @param config       Bundle 对象，包含音频特效的配置信息，不能为 null
     * @param presetReverb 要恢复配置的 PresetReverb 对象，不能为 null
     * @return 如果恢复成功，则返回 true；否则返回 false
     */
    public static boolean restoreSettings(@NonNull Bundle config, @NonNull PresetReverb presetReverb) {
        String settings = config.getString(KEY_SETTING_PRESET_REVERB);
        if (settings == null || settings.isEmpty()) {
            return false;
        }

        try {
            presetReverb.setProperties(new PresetReverb.Settings(settings));
            return true;
        } catch (IllegalArgumentException | IllegalStateException | UnsupportedOperationException e) {
            return false;
        }
    }

    /**
     * 使用 Equalizer 对象更新音频特效的 Equalizer 配置。
     *
     * @param config    Bundle 对象，包含音频特效的配置信息，不能为 null
     * @param equalizer 用于更新配置的 Equalizer 对象，不能为 null
     */
    public static void updateSettings(@NonNull Bundle config, @NonNull Equalizer equalizer) {
        config.putString(KEY_SETTING_EQUALIZER, equalizer.getProperties().toString());
    }

    /**
     * 使用 BassBoost 对象更新音频特效的 BassBoost 配置。
     *
     * @param config    Bundle 对象，包含音频特效的配置信息，不能为 null
     * @param bassBoost 用于更新配置的 BassBoost 对象，不能为 null
     */
    public static void updateSettings(@NonNull Bundle config, @NonNull BassBoost bassBoost) {
        config.putString(KEY_SETTING_BASS_BOOST, bassBoost.getProperties().toString());
    }

    /**
     * 使用 Virtualizer 对象更新音频特效的 Virtualizer 配置。
     *
     * @param config      Bundle 对象，包含音频特效的配置信息，不能为 null
     * @param virtualizer 用于更新配置的 Virtualizer 对象，不能为 null
     */
    public static void updateSettings(@NonNull Bundle config, @NonNull Virtualizer virtualizer) {
        config.putString(KEY_SETTING_VIRTUALIZER, virtualizer.getProperties().toString());
    }

    /**
     * 使用 PresetReverb 对象更新音频特效的 PresetReverb 配置。
     *
     * @param config       Bundle 对象，包含音频特效的配置信息，不能为 null
     * @param presetReverb 用于更新配置的 PresetReverb 对象，不能为 null
     */
    public static void updateSettings(@NonNull Bundle config, @NonNull PresetReverb presetReverb) {
        config.putString(KEY_SETTING_PRESET_REVERB, presetReverb.getProperties().toString());
    }
}
