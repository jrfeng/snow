package snow.player.effect;

import android.os.Parcelable;

import androidx.annotation.NonNull;

/**
 * 音频特效引擎。
 */
public interface AudioEffectEngine {
    void init(@NonNull Config config);

    void updateConfig(@NonNull Config config);

    void attachAudioEffect(int audioSessionId);

    void detachAudioEffect();

    @NonNull
    Config getDefaultConfig();

    void release();

    /**
     * 音频特效引擎的配置信息。
     */
    interface Config extends Parcelable {
    }
}
