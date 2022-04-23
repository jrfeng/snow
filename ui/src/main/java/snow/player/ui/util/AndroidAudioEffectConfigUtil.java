package snow.player.ui.util;

import android.content.Context;
import android.media.audiofx.BassBoost;
import android.media.audiofx.Equalizer;
import android.media.audiofx.Virtualizer;
import android.os.Bundle;

import androidx.annotation.NonNull;

import snow.player.ui.R;

/**
 * 用于获取和修改 Android 音频特效的配置信息。
 */
public final class AndroidAudioEffectConfigUtil {
    public static final String KEY_SETTING_EQUALIZER = "setting_equalizer";
    public static final String KEY_SETTING_BASS_BOOST = "setting_bass_boost";
    public static final String KEY_SETTING_VIRTUALIZER = "setting_virtualizer";
    public static final String KEY_TAKE_CONTROL = "take_control";

    /**
     * 在 Service 端运行的音频特效的优先级。
     */
    public static final String KEY_PRIORITY = "priority";

    /**
     * UI 控制权的音频特效的优先级。
     */
    public static final String KEY_UI_PRIORITY = "ui_priority";

    private AndroidAudioEffectConfigUtil() {
        throw new AssertionError();
    }

    /**
     * 从 config 中应用 Equalizer 的配置。
     *
     * @param config    Bundle 对象，包含音频特效的配置信息，不能为 null
     * @param equalizer 要恢复配置的 Equalizer 对象，不能为 null
     */
    public static void applySettings(@NonNull Bundle config, @NonNull Equalizer equalizer) {
        Preconditions.checkNotNull(config);
        Preconditions.checkNotNull(equalizer);

        String settings = config.getString(KEY_SETTING_EQUALIZER);
        if (settings == null || settings.isEmpty()) {
            return;
        }

        try {
            equalizer.setProperties(new Equalizer.Settings(settings));
        } catch (IllegalArgumentException | IllegalStateException | UnsupportedOperationException e) {
            e.printStackTrace();
        }
    }

    /**
     * 设置播放器的 AudioEffect 的优先级。
     *
     * 当在 Service 端运行的音频特效重新生效时，将使用该方法更新 priority 值，以便下次启动
     * {@link snow.player.ui.equalizer.EqualizerActivity} 可以获取到正确的 priority 值。
     *
     * @see #getPriority(Bundle)
     */
    public static void setPriority(@NonNull Bundle config, int priority) {
        Preconditions.checkNotNull(config);
        config.putInt(KEY_PRIORITY, priority);
    }

    /**
     * 获取播放器的 AudioEffect 的优先级。
     * <p>
     * {@link snow.player.ui.equalizer.EqualizerActivity} 需要使用该参数来实时修改音频特效。
     *
     * @see #setPriority(Bundle, int)
     */
    public static int getPriority(@NonNull Bundle config) {
        Preconditions.checkNotNull(config);
        return config.getInt(KEY_PRIORITY, 1);
    }

    /**
     * 获取 UI 控制器的 AudioEffect 的优先级。
     * <p>
     * 在 Service 端运行的 {@link snow.player.ui.equalizer.AndroidAudioEffectManager} 需要使用该参数来保证在退出 UI 后已设置的音频特效依然生效。
     */
    public static int getUIPriority(@NonNull Bundle config) {
        Preconditions.checkNotNull(config);
        return config.getInt(KEY_UI_PRIORITY, 0);
    }

    /**
     * 是否获取 AudioEffect 控制权。
     * <p>
     * <p>
     * 当 {@link snow.player.ui.equalizer.EqualizerActivity} 被启动后，使用该方法获取音频特效控制权。
     */
    public static boolean isTakeControl(@NonNull Bundle config) {
        Preconditions.checkNotNull(config);
        return config.getBoolean(KEY_TAKE_CONTROL, false);
    }


    /**
     * 获取音频特效的控制权。
     * <p>
     * 当 {@link snow.player.ui.equalizer.EqualizerActivity} 被启动后，当 UI 端使用该方法获取音频特效控制权。
     * 此时在 Service 端运行的 {@link snow.player.ui.equalizer.AndroidAudioEffectManager} 将会忽略
     * 音频特效的更新，直到重新获取到控制权。
     *
     * @see #releaseControl(Bundle, int)
     */
    public static void takeControl(@NonNull Bundle config) {
        Preconditions.checkNotNull(config);
        config.putBoolean(KEY_TAKE_CONTROL, true);
    }

    /**
     * 释放音频特效的控制权。
     * <p>
     * 当 UI 端使用该方法释放音频特效控制权后，在 Service 端运行的 {@link snow.player.ui.equalizer.AndroidAudioEffectManager}
     * 将会恢复响应音频特效的更新。
     *
     * @see #takeControl(Bundle)
     */
    public static void releaseControl(@NonNull Bundle config, int uiPriority) {
        Preconditions.checkNotNull(config);
        config.putBoolean(KEY_TAKE_CONTROL, false);
        config.putInt(KEY_UI_PRIORITY, uiPriority);
    }

    /**
     * 从 config 中应用 BassBoost 的配置。
     *
     * @param config    Bundle 对象，包含音频特效的配置信息，不能为 null
     * @param bassBoost 要恢复配置的 BassBoost 对象，不能为 null
     */
    public static void applySettings(@NonNull Bundle config, @NonNull BassBoost bassBoost) {
        Preconditions.checkNotNull(config);
        Preconditions.checkNotNull(bassBoost);

        String settings = config.getString(KEY_SETTING_BASS_BOOST);
        if (settings == null || settings.isEmpty()) {
            return;
        }

        try {
            bassBoost.setProperties(new BassBoost.Settings(settings));
        } catch (IllegalArgumentException | IllegalStateException | UnsupportedOperationException e) {
            e.printStackTrace();
        }
    }

    /**
     * 从 config 中应用 Virtualizer 的配置。
     *
     * @param config      Bundle 对象，包含音频特效的配置信息，不能为 null
     * @param virtualizer 要恢复配置的 Virtualizer 对象，不能为 null
     */
    public static void applySettings(@NonNull Bundle config, @NonNull Virtualizer virtualizer) {
        Preconditions.checkNotNull(config);
        Preconditions.checkNotNull(virtualizer);

        String settings = config.getString(KEY_SETTING_VIRTUALIZER);
        if (settings == null || settings.isEmpty()) {
            return;
        }

        try {
            virtualizer.setProperties(new Virtualizer.Settings(settings));
        } catch (IllegalArgumentException | IllegalStateException | UnsupportedOperationException e) {
            e.printStackTrace();
        }
    }

    /**
     * 更新音频特效的 Equalizer 配置。
     *
     * @param config   Bundle 对象，包含音频特效的配置信息，不能为 null
     * @param settings 最新的 Equalizer 配置，不能为 null
     */
    public static void updateSettings(@NonNull Bundle config, @NonNull Equalizer.Settings settings) {
        Preconditions.checkNotNull(config);
        Preconditions.checkNotNull(settings);

        config.putString(KEY_SETTING_EQUALIZER, settings.toString());
    }

    /**
     * 更新音频特效的 BassBoost 配置。
     *
     * @param config   Bundle 对象，包含音频特效的配置信息，不能为 null
     * @param settings 最新的 BassBoost 配置，不能为 null
     */
    public static void updateSettings(@NonNull Bundle config, @NonNull BassBoost.Settings settings) {
        Preconditions.checkNotNull(config);
        Preconditions.checkNotNull(settings);

        config.putString(KEY_SETTING_BASS_BOOST, settings.toString());
    }

    /**
     * 更新音频特效的 Virtualizer 配置。
     *
     * @param config   Bundle 对象，包含音频特效的配置信息，不能为 null
     * @param settings 最新的 Virtualizer 配置，不能为 null
     */
    public static void updateSettings(@NonNull Bundle config, @NonNull Virtualizer.Settings settings) {
        Preconditions.checkNotNull(config);
        Preconditions.checkNotNull(settings);

        config.putString(KEY_SETTING_VIRTUALIZER, settings.toString());
    }

    public static String optimizeEqualizerPresetName(@NonNull Context context, @NonNull String presetName) {
        switch (presetName) {
            case "Normal":
                return context.getString(R.string.snow_ui_equalizer_preset_normal);
            case "Classical":
                return context.getString(R.string.snow_ui_equalizer_preset_classical);
            case "Dance":
                return context.getString(R.string.snow_ui_equalizer_preset_dance);
            case "Flat":
                return context.getString(R.string.snow_ui_equalizer_preset_flat);
            case "Folk":
                return context.getString(R.string.snow_ui_equalizer_preset_folk);
            case "Heavy Metal":
                return context.getString(R.string.snow_ui_equalizer_preset_heavy_metal);
            case "Hip Hop":
                return context.getString(R.string.snow_ui_equalizer_preset_hip_hop);
            case "Jazz":
                return context.getString(R.string.snow_ui_equalizer_preset_jazz);
            case "Pop":
                return context.getString(R.string.snow_ui_equalizer_preset_pop);
            case "Rock":
                return context.getString(R.string.snow_ui_equalizer_preset_rock);
        }

        return presetName;
    }
}
