package snow.player;

import android.os.Bundle;

import androidx.annotation.NonNull;

/**
 * 音频特效引擎。
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
