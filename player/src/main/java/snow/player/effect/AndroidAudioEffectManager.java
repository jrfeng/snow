package snow.player.effect;

import android.media.MediaPlayer;
import android.media.audiofx.BassBoost;
import android.media.audiofx.Equalizer;
import android.media.audiofx.PresetReverb;
import android.media.audiofx.Virtualizer;
import android.os.Bundle;

import androidx.annotation.NonNull;

/**
 * Android 音频特效管理器。
 * <p>
 * 用于支持 Android 原生的音频特效：<br>
 * <ul>
 *     <li>Equalizer：均衡器</li>
 *     <li>BassBoost：低音增强</li>
 *     <li>PresetReverb：预置混响</li>
 *     <li>Virtualizer：环绕声</li>
 * </ul>
 */
public class AndroidAudioEffectManager implements AudioEffectManager {
    public static final String KEY_SETTING_EQUALIZER = "setting_equalizer";
    public static final String KEY_SETTING_BASS_BOOST = "setting_bass_boost";
    public static final String KEY_SETTING_PRESET_REVERB = "setting_preset_reverb";
    public static final String KEY_SETTING_VIRTUALIZER = "setting_virtualizer";

    public static final int PRIORITY = 1;

    private Bundle mConfig;
    private Bundle mDefaultConfig;

    private Equalizer mEqualizer;
    private BassBoost mBassBoost;
    private PresetReverb mPresetReverb;
    private Virtualizer mVirtualizer;

    @Override
    public void init(@NonNull Bundle config) {
        mConfig = new Bundle(config);
        initDefaultConfig();
    }

    private void initDefaultConfig() {
        MediaPlayer fakeMediaPlayer = new MediaPlayer();
        int audioSessionId = fakeMediaPlayer.getAudioSessionId();

        Equalizer equalizer = new Equalizer(0, audioSessionId);
        BassBoost bassBoost = new BassBoost(0, audioSessionId);
        PresetReverb presetReverb = new PresetReverb(0, audioSessionId);
        Virtualizer virtualizer = new Virtualizer(0, audioSessionId);

        mDefaultConfig = new Bundle();
        mDefaultConfig.putString(KEY_SETTING_EQUALIZER, equalizer.getProperties().toString());
        mDefaultConfig.putString(KEY_SETTING_BASS_BOOST, bassBoost.getProperties().toString());
        mDefaultConfig.putString(KEY_SETTING_PRESET_REVERB, presetReverb.getProperties().toString());
        mDefaultConfig.putString(KEY_SETTING_VIRTUALIZER, virtualizer.getProperties().toString());

        equalizer.release();
        bassBoost.release();
        presetReverb.release();
        virtualizer.release();
        fakeMediaPlayer.release();
    }

    @Override
    public void updateConfig(@NonNull Bundle config) {
        mConfig = new Bundle(config);

        if (mEqualizer.hasControl()) {
            mEqualizer.setProperties(getEqualizerSettings());
        }

        if (mBassBoost.hasControl()) {
            mBassBoost.setProperties(getBassBoostSettings());
        }

        if (mPresetReverb.hasControl()) {
            mPresetReverb.setProperties(getPresetReverbSettings());
        }

        if (mVirtualizer.hasControl()) {
            mVirtualizer.setProperties(getVirtualizerSettings());
        }
    }

    @Override
    public void attachAudioEffect(int audioSessionId) {
        releaseAudioEffect();

        mEqualizer = new Equalizer(PRIORITY, audioSessionId);
        mBassBoost = new BassBoost(PRIORITY, audioSessionId);
        mPresetReverb = new PresetReverb(PRIORITY, audioSessionId);
        mVirtualizer = new Virtualizer(PRIORITY, audioSessionId);

        mEqualizer.setProperties(getEqualizerSettings());
        mBassBoost.setProperties(getBassBoostSettings());
        mPresetReverb.setProperties(getPresetReverbSettings());
        mVirtualizer.setProperties(getVirtualizerSettings());

        mEqualizer.setEnabled(true);
        mBassBoost.setEnabled(true);
        mPresetReverb.setEnabled(true);
        mVirtualizer.setEnabled(true);
    }

    @Override
    public void detachAudioEffect() {
        releaseAudioEffect();
    }

    @NonNull
    @Override
    public Bundle getDefaultConfig() {
        return mDefaultConfig;
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

    private Equalizer.Settings getEqualizerSettings() {
        String settings = mConfig.getString(KEY_SETTING_EQUALIZER);
        if (settings != null && !settings.isEmpty()) {
            try {
                return new Equalizer.Settings(settings);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        return new Equalizer.Settings(mDefaultConfig.getString(KEY_SETTING_EQUALIZER));
    }

    private BassBoost.Settings getBassBoostSettings() {
        String settings = mConfig.getString(KEY_SETTING_BASS_BOOST);
        if (settings != null && !settings.isEmpty()) {
            try {
                return new BassBoost.Settings(settings);
            } catch (IllegalArgumentException e) {
                e.printStackTrace();
            }
        }

        return new BassBoost.Settings(mDefaultConfig.getString(KEY_SETTING_BASS_BOOST));
    }

    private PresetReverb.Settings getPresetReverbSettings() {
        String settings = mConfig.getString(KEY_SETTING_PRESET_REVERB);
        if (settings != null && !settings.isEmpty()) {
            try {
                return new PresetReverb.Settings(settings);
            } catch (IllegalArgumentException e) {
                e.printStackTrace();
            }
        }

        return new PresetReverb.Settings(mDefaultConfig.getString(KEY_SETTING_PRESET_REVERB));
    }

    private Virtualizer.Settings getVirtualizerSettings() {
        String settings = mConfig.getString(KEY_SETTING_VIRTUALIZER);
        if (settings != null && !settings.isEmpty()) {
            try {
                return new Virtualizer.Settings(settings);
            } catch (IllegalArgumentException e) {
                e.printStackTrace();
            }
        }

        return new Virtualizer.Settings(mDefaultConfig.getString(KEY_SETTING_VIRTUALIZER));
    }
}
