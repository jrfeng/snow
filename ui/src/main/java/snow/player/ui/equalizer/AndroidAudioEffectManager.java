package snow.player.ui.equalizer;

import android.media.audiofx.BassBoost;
import android.media.audiofx.Equalizer;
import android.media.audiofx.Virtualizer;
import android.os.Bundle;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import snow.player.PlayerService;
import snow.player.effect.AudioEffectManager;
import snow.player.ui.util.AndroidAudioEffectConfigUtil;

/**
 * Android 音频特效管理器。
 * <p>
 * 用于与 {@link EqualizerActivity} 配合使用，以支持 Android 原生的音频特效：<br>
 * <ul>
 *     <li>Equalizer：均衡器</li>
 *     <li>BassBoost：低音增强</li>
 *     <li>Virtualizer：环绕声</li>
 * </ul>
 * <p>
 * 要使用该类，请覆盖 {@link PlayerService} 的 {@code onCreateAudioEffectManager()} 方法，
 * 并通过 {@link EqualizerActivity} 俩配置音频特效。
 */
public final class AndroidAudioEffectManager implements AudioEffectManager {
    private static final String TAG = "AudioEffectManager";

    /**
     * 音频特效的优先级。
     * <p>
     * 值为：1
     *
     * 注意！该属性已弃用，请不要再使用，并且将在下个版本移除。
     */
    @Deprecated()
    public static final int PRIORITY = 1;

    private Bundle mConfig;

    private boolean mAudioEffectAvailable;
    private int mAudioSessionId;

    @Nullable
    private Equalizer mEqualizer;
    @Nullable
    private BassBoost mBassBoost;
    @Nullable
    private Virtualizer mVirtualizer;

    @Override
    public void init(@NonNull Bundle config) {
        mConfig = config;
    }

    @Override
    public void updateConfig(@NonNull Bundle config) {
        mConfig = config;

        boolean takeControl = AndroidAudioEffectConfigUtil.isTakeControl(mConfig);

        if (takeControl) {
            return;
        }

        int priority = AndroidAudioEffectConfigUtil.getUIPriority(mConfig) + 1;

        if (!mAudioEffectAvailable && mAudioSessionId > 0) {
            mAudioEffectAvailable = initAudioEffect(mAudioSessionId, priority);
        }

        if (mAudioEffectAvailable) {
            AndroidAudioEffectConfigUtil.setPriority(mConfig, priority);
        }

        if (mEqualizer != null && mEqualizer.hasControl()) {
            AndroidAudioEffectConfigUtil.applySettings(mConfig, mEqualizer);
        }

        if (mBassBoost != null && mBassBoost.hasControl()) {
            AndroidAudioEffectConfigUtil.applySettings(mConfig, mBassBoost);
        }

        if (mVirtualizer != null && mVirtualizer.hasControl()) {
            AndroidAudioEffectConfigUtil.applySettings(mConfig, mVirtualizer);
        }
    }

    @Override
    public void attachAudioEffect(int audioSessionId) {
        releaseAudioEffect();

        mAudioSessionId = audioSessionId;
        mAudioEffectAvailable = initAudioEffect(audioSessionId, AndroidAudioEffectConfigUtil.getPriority(mConfig));
    }

    private boolean initAudioEffect(int audioSessionId, int priority) {
        try {
            mEqualizer = new Equalizer(priority, audioSessionId);
            mBassBoost = new BassBoost(priority, audioSessionId);
            mVirtualizer = new Virtualizer(priority, audioSessionId);

            AndroidAudioEffectConfigUtil.applySettings(mConfig, mEqualizer);
            AndroidAudioEffectConfigUtil.applySettings(mConfig, mBassBoost);
            AndroidAudioEffectConfigUtil.applySettings(mConfig, mVirtualizer);

            mEqualizer.setEnabled(true);
            mBassBoost.setEnabled(true);
            mVirtualizer.setEnabled(true);
        } catch (Exception e) {
            Log.e(TAG, "audio effect init failed", e);
            return false;
        }

        return true;
    }

    @Override
    public void detachAudioEffect() {
        mAudioSessionId = 0;

        AndroidAudioEffectConfigUtil.setPriority(mConfig, 1);
        AndroidAudioEffectConfigUtil.releaseControl(mConfig, 1);

        releaseAudioEffect();
    }

    @Override
    public void release() {
        mAudioSessionId = 0;
        releaseAudioEffect();
    }

    private void releaseAudioEffect() {
        mAudioEffectAvailable = false;

        if (mEqualizer != null) {
            mEqualizer.release();
            mEqualizer = null;
        }

        if (mBassBoost != null) {
            mBassBoost.release();
            mBassBoost = null;
        }

        if (mVirtualizer != null) {
            mVirtualizer.release();
            mVirtualizer = null;
        }
    }
}
