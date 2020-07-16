package snow.player.effect;

import android.os.Bundle;

import androidx.annotation.NonNull;

/**
 * 音频特效引擎。
 */
public interface AudioEffectEngine {
    void init(@NonNull Bundle config);

    void updateConfig(@NonNull Bundle config);

    void attachAudioEffect(int audioSessionId);

    void detachAudioEffect();

    @NonNull
    Bundle getDefaultConfig();

    void release();
}
