package snow.player.effect;

import android.media.audiofx.BassBoost;
import android.media.audiofx.Equalizer;
import android.media.audiofx.PresetReverb;
import android.media.audiofx.Virtualizer;
import android.os.Bundle;

import androidx.annotation.NonNull;

import snow.player.util.AndroidAudioEffectConfigUtil;

/**
 * Android 音频特效管理器。
 * <p>
 * 用于支持 Android 原生的音频特效：<br>
 * <ul>
 *     <li>Equalizer：均衡器</li>
 *     <li>BassBoost：低音增强</li>
 *     <li>Virtualizer：环绕声</li>
 *     <li>PresetReverb：预置混响</li>
 * </ul>
 */
public final class AndroidAudioEffectManager implements AudioEffectManager {
    /**
     * 音频特效的优先级。
     * <p>
     * 值为：1
     */
    public static final int PRIORITY = 1;

    private Bundle mConfig;

    private Equalizer mEqualizer;
    private BassBoost mBassBoost;
    private PresetReverb mPresetReverb;
    private Virtualizer mVirtualizer;

    @Override
    public void init(@NonNull Bundle config) {
        mConfig = new Bundle(config);
    }

    @Override
    public void updateConfig(@NonNull Bundle config) {
        mConfig = new Bundle(config);

        if (mEqualizer.hasControl()) {
            AndroidAudioEffectConfigUtil.applySettings(mConfig, mEqualizer);
        }

        if (mBassBoost.hasControl()) {
            AndroidAudioEffectConfigUtil.applySettings(mConfig, mBassBoost);
        }

        if (mVirtualizer.hasControl()) {
            AndroidAudioEffectConfigUtil.applySettings(mConfig, mVirtualizer);
        }

        if (mPresetReverb.hasControl()) {
            AndroidAudioEffectConfigUtil.applySettings(mConfig, mPresetReverb);
        }
    }

    @Override
    public void attachAudioEffect(int audioSessionId) {
        releaseAudioEffect();

        mEqualizer = new Equalizer(PRIORITY, audioSessionId);
        mBassBoost = new BassBoost(PRIORITY, audioSessionId);
        mVirtualizer = new Virtualizer(PRIORITY, audioSessionId);
        mPresetReverb = new PresetReverb(PRIORITY, audioSessionId);

        AndroidAudioEffectConfigUtil.applySettings(mConfig, mEqualizer);
        AndroidAudioEffectConfigUtil.applySettings(mConfig, mBassBoost);
        AndroidAudioEffectConfigUtil.applySettings(mConfig, mVirtualizer);
        AndroidAudioEffectConfigUtil.applySettings(mConfig, mPresetReverb);

        mEqualizer.setEnabled(true);
        mBassBoost.setEnabled(true);
        mVirtualizer.setEnabled(true);
        mPresetReverb.setEnabled(true);
    }

    @Override
    public void detachAudioEffect() {
        releaseAudioEffect();
    }

    @Override
    public void release() {
        releaseAudioEffect();
    }

    private void releaseAudioEffect() {
        if (mEqualizer != null) {
            mEqualizer.release();
            mEqualizer = null;
        }

        if (mBassBoost != null) {
            mBassBoost.release();
            mBassBoost = null;
        }

        if (mPresetReverb != null) {
            mPresetReverb.release();
            mPresetReverb = null;
        }

        if (mVirtualizer != null) {
            mVirtualizer.release();
            mVirtualizer = null;
        }
    }
}
