package snow.player.ui.equalizer;

import android.media.MediaPlayer;
import android.media.audiofx.BassBoost;
import android.media.audiofx.Equalizer;
import android.media.audiofx.Virtualizer;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import snow.player.PlayerClient;
import snow.player.ui.util.AndroidAudioEffectConfigUtil;
import snow.player.ui.util.Preconditions;

public class EqualizerViewModel extends ViewModel {
    public static final int AUDIO_EFFECT_PRIORITY = 1000;

    private MutableLiveData<Boolean> mEnabled;

    private Equalizer mEqualizer;
    private BassBoost mBassBoost;
    private Virtualizer mVirtualizer;

    private PlayerClient mPlayerClient;
    private PlayerClient.OnAudioSessionChangeListener mOnAudioSessionChangeListener;
    private boolean mInitialized;
    private Bundle mAudioEffectConfig;

    private MediaPlayer mFakeMediaPlayer;

    public void init(@NonNull PlayerClient playerClient) {
        Preconditions.checkNotNull(playerClient);

        if (mInitialized) {
            return;
        }

        mInitialized = true;
        mPlayerClient = playerClient;

        mEnabled = new MutableLiveData<>(playerClient.isAudioEffectEnabled());
        mAudioEffectConfig = mPlayerClient.getAudioEffectConfig();
        attachAudioEffect(0);

        mOnAudioSessionChangeListener = new PlayerClient.OnAudioSessionChangeListener() {
            @Override
            public void onAudioSessionChanged(int audioSessionId) {
                if (mPlayerClient.isAudioEffectEnabled()) {
                    attachAudioEffect(audioSessionId);
                }
            }
        };

        mPlayerClient.addOnAudioSessionChangeListener(mOnAudioSessionChangeListener);
    }

    public boolean isInitialized() {
        return mInitialized;
    }

    @Override
    protected void onCleared() {
        if (!mInitialized) {
            return;
        }

        if (mFakeMediaPlayer != null) {
            mFakeMediaPlayer.release();
        }

        mPlayerClient.disconnect();
        mPlayerClient.removeOnAudioSessionChangeListener(mOnAudioSessionChangeListener);

        releaseAllEffect();
    }

    public void setAudioEffectEnabled(boolean enabled) {
        if (!mInitialized) {
            throw new IllegalStateException("EqualizerViewModel not init yet.");
        }

        Boolean value = mEnabled.getValue();
        assert value != null;
        if (enabled == value) {
            return;
        }

        mEnabled.setValue(enabled);
        mPlayerClient.setAudioEffectEnabled(enabled);

        if (enabled) {
            attachAudioEffect(mPlayerClient.getAudioSessionId());
        } else {
            attachAudioEffect(0);
        }
    }

    public MutableLiveData<Boolean> getEnabled() {
        return mEnabled;
    }

    public int getEqualizerNumberOfPresets() {
        if (!mInitialized) {
            throw new IllegalStateException("EqualizerViewModel not init yet.");
        }

        return mEqualizer.getNumberOfPresets();
    }

    public String getEqualizerPresetName(short preset) {
        if (!mInitialized) {
            throw new IllegalStateException("EqualizerViewModel not init yet.");
        }

        return mEqualizer.getPresetName(preset);
    }

    public void equalizerUsePreset(short preset) {
        if (!mInitialized) {
            throw new IllegalStateException("EqualizerViewModel not init yet.");
        }

        mEqualizer.usePreset(preset);
    }

    public short getEqualizerCurrentPreset() {
        if (!mInitialized) {
            throw new IllegalStateException("EqualizerViewModel not init yet.");
        }

        return mEqualizer.getCurrentPreset();
    }

    public int getEqualizerNumberOfBands() {
        if (!mInitialized) {
            throw new IllegalStateException("EqualizerViewModel not init yet.");
        }

        return mEqualizer.getNumberOfBands();
    }

    public int[] getEqualizerBandFreqRange(short band) {
        if (!mInitialized) {
            throw new IllegalStateException("EqualizerViewModel not init yet.");
        }

        return mEqualizer.getBandFreqRange(band);
    }

    public int getEqualizerCenterFreq(short band) {
        if (!mInitialized) {
            throw new IllegalStateException("EqualizerViewModel not init yet.");
        }

        return mEqualizer.getCenterFreq(band);
    }

    public short[] getEqualizerBandLevelRange() {
        if (!mInitialized) {
            throw new IllegalStateException("EqualizerViewModel not init yet.");
        }

        return mEqualizer.getBandLevelRange();
    }

    public short getEqualizerBandLevel(short band) {
        if (!mInitialized) {
            throw new IllegalStateException("EqualizerViewModel not init yet.");
        }

        return mEqualizer.getBandLevel(band);
    }

    public void setEqualizerBandLevel(short band, short level) {
        if (!mInitialized) {
            throw new IllegalStateException("EqualizerViewModel not init yet.");
        }

        mEqualizer.setBandLevel(band, level);
    }

    public void setBassBoostStrength(short strength) {
        if (!mInitialized) {
            throw new IllegalStateException("EqualizerViewModel not init yet.");
        }

        mBassBoost.setStrength(strength);
    }

    public short getBassBoostRoundedStrength() {
        if (!mInitialized) {
            throw new IllegalStateException("EqualizerViewModel not init yet.");
        }

        return mBassBoost.getRoundedStrength();
    }

    public void setVirtualizerStrength(short strength) {
        if (!mInitialized) {
            throw new IllegalStateException("EqualizerViewModel not init yet.");
        }

        mVirtualizer.setStrength(strength);
    }

    public short getVirtualizerStrength() {
        if (!mInitialized) {
            throw new IllegalStateException("EqualizerViewModel not init yet.");
        }

        return mVirtualizer.getRoundedStrength();
    }

    private int getFakeAudioSessionId() {
        if (mFakeMediaPlayer == null) {
            mFakeMediaPlayer = new MediaPlayer();
        }

        return mFakeMediaPlayer.getAudioSessionId();
    }

    private void attachAudioEffect(int audioSessionId) {
        releaseAllEffect();

        if (audioSessionId == 0) {
            audioSessionId = getFakeAudioSessionId();
        }

        mEqualizer = new Equalizer(AUDIO_EFFECT_PRIORITY, audioSessionId);
        mBassBoost = new BassBoost(AUDIO_EFFECT_PRIORITY, audioSessionId);
        mVirtualizer = new Virtualizer(AUDIO_EFFECT_PRIORITY, audioSessionId);

        AndroidAudioEffectConfigUtil.applySettings(mAudioEffectConfig, mEqualizer);
        AndroidAudioEffectConfigUtil.applySettings(mAudioEffectConfig, mBassBoost);
        AndroidAudioEffectConfigUtil.applySettings(mAudioEffectConfig, mVirtualizer);

        mEqualizer.setEnabled(true);
        mBassBoost.setEnabled(true);
        mVirtualizer.setEnabled(true);
    }

    /**
     * 提交修改。
     * <p>
     * 保存所有对音频特效配置的修改。
     */
    public void applyChanges() {
        if (!mInitialized) {
            throw new IllegalStateException("EqualizerViewModel not init yet.");
        }

        AndroidAudioEffectConfigUtil.updateSettings(mAudioEffectConfig, mEqualizer.getProperties());
        AndroidAudioEffectConfigUtil.updateSettings(mAudioEffectConfig, mBassBoost.getProperties());
        AndroidAudioEffectConfigUtil.updateSettings(mAudioEffectConfig, mVirtualizer.getProperties());

        if (mPlayerClient.isConnected()) {
            mPlayerClient.setAudioEffectConfig(mAudioEffectConfig);
            return;
        }

        mPlayerClient.connect(new PlayerClient.OnConnectCallback() {
            @Override
            public void onConnected(boolean success) {
                mPlayerClient.setAudioEffectConfig(mAudioEffectConfig);
            }
        });
    }

    private void releaseAllEffect() {
        if (mEqualizer != null) {
            mEqualizer.release();
        }

        if (mBassBoost != null) {
            mBassBoost.release();
        }

        if (mVirtualizer != null) {
            mVirtualizer.release();
        }
    }
}
