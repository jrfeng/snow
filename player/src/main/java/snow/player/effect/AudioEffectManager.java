package snow.player.effect;

import android.os.Bundle;

import androidx.annotation.NonNull;

/**
 * 用于管理和同步音频特效的配置。
 */
public interface AudioEffectManager {
    void init(@NonNull Bundle config);

    void updateConfig(@NonNull Bundle config);

    void attachAudioEffect(int audioSessionId);

    void detachAudioEffect();

    @NonNull
    Bundle getDefaultConfig();

    void release();
}
