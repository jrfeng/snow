package snow.player.effect;

import android.content.res.Resources;
import android.media.MediaPlayer;
import android.media.audiofx.BassBoost;
import android.media.audiofx.Equalizer;
import android.media.audiofx.PresetReverb;
import android.media.audiofx.Virtualizer;
import android.os.Bundle;

import androidx.annotation.NonNull;

import com.google.common.base.Preconditions;

import snow.player.PlayerClient;
import snow.player.R;
import snow.player.util.AndroidAudioEffectConfigUtil;

/**
 * 用于编辑和同步播放器的音频特效效果。
 * <p>
 * 当不再需要 {@link AndroidAudioEffectEditor} 对象时，请调用 {@link #release()} 方法释放占用的资源。
 */
public final class AndroidAudioEffectEditor {
    private PlayerClient mPlayerClient;
    private Bundle mConfig;

    private MediaPlayer mMediaPlayer;   // 仅用于获取 AudioSessionId
    private Equalizer mFakeEqualizer;
    private BassBoost mFakeBassBoost;
    private Virtualizer mFakeVirtualizer;
    private PresetReverb mFakePresetReverb;

    /**
     * 创建一个 {@link AndroidAudioEffectEditor} 对象。
     *
     * @param playerClient {@link PlayerClient} 对象，不能为 null
     */
    public AndroidAudioEffectEditor(@NonNull PlayerClient playerClient) {
        Preconditions.checkNotNull(playerClient);
        mPlayerClient = playerClient;
        mConfig = playerClient.getAudioEffectConfig();

        mMediaPlayer = new MediaPlayer();
        int audioSessionId = mMediaPlayer.getAudioSessionId();

        mFakeEqualizer = new Equalizer(0, audioSessionId);
        mFakeBassBoost = new BassBoost(0, audioSessionId);
        mFakeVirtualizer = new Virtualizer(0, audioSessionId);
        mFakePresetReverb = new PresetReverb(0, audioSessionId);

        mFakeEqualizer.setEnabled(true);
        mFakeBassBoost.setEnabled(true);
        mFakeVirtualizer.setEnabled(true);
        mFakePresetReverb.setEnabled(true);
    }

    /**
     * 释放占用的资源。
     */
    public void release() {
        if (mPlayerClient == null) {
            return;
        }

        mMediaPlayer.release();
        mFakeEqualizer.release();
        mFakeBassBoost.release();
        mFakeVirtualizer.release();
        mFakePresetReverb.release();
        mPlayerClient = null;
        mConfig = null;
    }

    private void updateEqualizerSettings() {
        AndroidAudioEffectConfigUtil.updateSettings(mConfig, mFakeEqualizer.getProperties());
        mPlayerClient.setAudioEffectConfig(mConfig);
    }

    private void updateBassBoostSettings() {
        AndroidAudioEffectConfigUtil.updateSettings(mConfig, mFakeBassBoost.getProperties());
        mPlayerClient.setAudioEffectConfig(mConfig);
    }

    private void updateVirtualizerSettings() {
        AndroidAudioEffectConfigUtil.updateSettings(mConfig, mFakeVirtualizer.getProperties());
        mPlayerClient.setAudioEffectConfig(mConfig);
    }

    private void updatePresetReverbSettings() {
        AndroidAudioEffectConfigUtil.updateSettings(mConfig, mFakePresetReverb.getProperties());
        mPlayerClient.setAudioEffectConfig(mConfig);
    }

    // *********************** Equalizer ********************

    /**
     * 获取均衡器对给定频率影响最大的频段。
     *
     * @param frequency 频率（单位：Hz/ms，赫兹/每毫秒），将通过返回的频段进行均衡
     * @return 对给定频率影响最大的频段
     */
    public short getEQBand(int frequency)
            throws IllegalArgumentException, IllegalStateException, UnsupportedOperationException {
        return mFakeEqualizer.getBand(frequency);
    }

    /**
     * 获取均衡器给定频段的频率范围。
     *
     * @param band 请求其频率范围的频段。频段编号从 0 开始，以（频段数量-1）结束
     * @return 一个长度为 2 的 int[] 数组，数组的第一个值是频段频率的最小值，数组的第二个值是频段频率的最大
     * 值。可以根据均衡器的频段数在 for 循环中使用该方法获取所有频段的频率范围（单位：Hz/ms，赫兹/每毫秒）
     */
    public int[] getEQBandFreqRange(short band)
            throws IllegalArgumentException, IllegalStateException, UnsupportedOperationException {
        return mFakeEqualizer.getBandFreqRange(band);
    }

    /**
     * 获取给定均衡器频段的增益设置。
     *
     * @param band 要求增益的频段。频段编号从 0 开始，以（频段数量-1）结束
     * @return 给定频段的千分贝增益
     */
    public short getEQBandLevel(short band)
            throws IllegalArgumentException, IllegalStateException, UnsupportedOperationException {
        return mFakeEqualizer.getBandLevel(band);
    }

    /**
     * 获取均衡器频段的可调节范围（响度的调节范围）。
     *
     * @return 返回一个长度为 2 的 short[] 数组，数组的第一个值是可调节范围的下限（通常为 -1500），数组的第
     * 二个值是可调节范围上限（通常为 1500）（单位：mB, milliBel，转换为 dB 需要除以 100）。
     */
    public short[] getEQBandLevelRange()
            throws IllegalArgumentException, IllegalStateException, UnsupportedOperationException {
        return mFakeEqualizer.getBandLevelRange();
    }

    /**
     * 获取均衡器给定频段的中心频率（单位：Hz/ms，赫兹/每毫秒）
     *
     * @param band 请求中心频率的频段。频段编号从 0 开始，以（频段数量-1）结束
     */
    public int getEQCenterFreq(short band)
            throws IllegalArgumentException, IllegalStateException, UnsupportedOperationException {
        return mFakeEqualizer.getCenterFreq(band);
    }

    /**
     * 获取均衡器当前使用的预设。
     *
     * @return 当前使用的预设
     */
    public short getEQCurrentPreset()
            throws IllegalArgumentException, IllegalStateException, UnsupportedOperationException {
        return mFakeEqualizer.getCurrentPreset();
    }

    /**
     * 获取均衡器引擎支持的频段数。
     *
     * @return 均衡器引擎支持的频段数
     */
    public short getEQNumberOfBands()
            throws IllegalArgumentException, IllegalStateException, UnsupportedOperationException {
        return mFakeEqualizer.getNumberOfBands();
    }

    /**
     * 获取均衡器支持的预设总数。预设的索引范围为 [0，预设数量-1]。
     *
     * @return 均衡器支持的预设数量
     */
    public short getEQNumberOfPresets()
            throws IllegalArgumentException, IllegalStateException, UnsupportedOperationException {
        return mFakeEqualizer.getNumberOfPresets();
    }

    /**
     * 根据索引获取预设名称。
     *
     * @param preset 预设的索引。预设的索引范围为 [0，预设数量-1]
     * @return 包含给定预设名称的字符串
     */
    public String getEQPresetName(short preset) {
        return mFakeEqualizer.getPresetName(preset);
    }

    /**
     * 将给定的均衡器频段设置为给定的增益值。
     *
     * @param band  要设置新增益的频段。频段编号从 0 开始，以（频段数量-1）结束
     * @param level 将以给定频段设置的新的增益（单位：mB, milliBel, 转换为 dB 需要除以 100）
     */
    public void setEQBandLevel(short band, short level)
            throws IllegalArgumentException, IllegalStateException, UnsupportedOperationException {
        mFakeEqualizer.setBandLevel(band, level);
        updateEqualizerSettings();
    }

    /**
     * 根据给定的预设设置均衡器。
     *
     * @param preset 要使用的新预设的索引。预设的索引范围为 [0，预设数量-1]
     */
    public void useEQPreset(short preset)
            throws IllegalArgumentException, IllegalStateException, UnsupportedOperationException {
        mFakeEqualizer.usePreset(preset);
        updateEqualizerSettings();
    }

    // *********************** BassBoost ********************

    /**
     * 获取低音增强效果的当前强度。
     *
     * @return 效果的强度。强度的有效范围是 [0，1000]，其中 0 表示最弱的效果，1000 表示最强的效果
     */
    public short getBBRoundedStrength()
            throws IllegalArgumentException, IllegalStateException, UnsupportedOperationException {
        return mFakeBassBoost.getRoundedStrength();
    }

    /**
     * 指示是否支持设置低音增强的强度。如果此方法返回 false，则仅支持一个强度，并且 setStrength() 方法始终会
     * 舍入到该值。
     *
     * @return 如果支持强度参数，则为 true；否则为 false
     */
    public boolean getBBStrengthSupported() {
        return mFakeBassBoost.getStrengthSupported();
    }

    /**
     * 设置低音增强效果的强度。strength 参数的范围是 [0, 1000]，其中，0 的强度最低，1000 的强度最高。如果具体
     * 的实现不支持使用千分率设置强度，则允许将给定强度舍入到最接近的支持值。您可以使用
     * {@link #getBBRoundedStrength()} 方法查询实际设置的值（可能是四舍五入的）。
     *
     * @param strength 低音增强效果的强度。强度的范围是 [0, 1000]，其中，0 的强度最低，1000 的强度最高
     */
    public void setBBStrength(short strength)
            throws IllegalArgumentException, IllegalStateException, UnsupportedOperationException {
        mFakeBassBoost.setStrength(strength);
        updateBassBoostSettings();
    }

    // *********************** Virtualizer ********************

    /**
     * 获取环绕声效果的当前强度。
     *
     * @return 环绕声效果的当前强度。强度的有效范围是 [0，1000]，其中 0 表示最弱的效果，1000 表示最强的效果
     */
    public short getVRRoundedStrength()
            throws IllegalArgumentException, IllegalStateException, UnsupportedOperationException {
        return mFakeVirtualizer.getRoundedStrength();
    }

    /**
     * 指示环绕声效果是否支持设置强度。如果此方法返回 false，则仅支持一种强度，并且 {@link #setVRStrength(short)}
     * 方法始终舍入为该值。
     *
     * @return 如果支持强度参数，则为 true；否则为 false
     */
    public boolean getVRStrengthSupported() {
        return mFakeVirtualizer.getStrengthSupported();
    }

    /**
     * 设置环绕声效果的强度。如果设备的实现不支持设置强度的精确度，则可以将给定强度四舍五入到最接近的支持值。可以
     * 使用 {@link #getVRRoundedStrength()} 方法查询实际设置的值（可能是四舍五入的）。
     *
     * @param strength 效果的强度。强度的有效范围是 [0，1000]，其中 0 表示最弱的效果，1000 表示最强的效果
     */
    public void setVRStrength(short strength)
            throws IllegalArgumentException, IllegalStateException, UnsupportedOperationException {
        mFakeVirtualizer.setStrength(strength);
        updateVirtualizerSettings();
    }

    // *********************** PresetReverb ********************

    /**
     * 获取预置混响效果当前使用的混响预设。
     *
     * @return 当前使用的预设
     */
    public short getPRPreset()
            throws IllegalArgumentException, IllegalStateException, UnsupportedOperationException {
        return mFakePresetReverb.getPreset();
    }

    /**
     * 设置预置混响效果要使用的预设。
     *
     * @param preset 使用的预设
     */
    public void setPRPreset(short preset)
            throws IllegalArgumentException, IllegalStateException, UnsupportedOperationException {
        mFakePresetReverb.setPreset(preset);
        updatePresetReverbSettings();
    }

    /**
     * 获取预置混响效果的预设的名称。
     *
     * @param preset 预置混响效果的预设的名称
     */
    public String getPRPresetName(@NonNull Resources res, int preset) {
        Preconditions.checkNotNull(res);
        switch (preset) {
            case PresetReverb.PRESET_NONE:
                return res.getString(R.string.snow_preset_none);
            case PresetReverb.PRESET_LARGEHALL:
                return res.getString(R.string.snow_preset_large_hall);
            case PresetReverb.PRESET_LARGEROOM:
                return res.getString(R.string.snow_preset_large_room);
            case PresetReverb.PRESET_MEDIUMHALL:
                return res.getString(R.string.snow_preset_medium_hall);
            case PresetReverb.PRESET_MEDIUMROOM:
                return res.getString(R.string.snow_preset_medium_room);
            case PresetReverb.PRESET_PLATE:
                return res.getString(R.string.snow_preset_plate);
            case PresetReverb.PRESET_SMALLROOM:
                return res.getString(R.string.snow_preset_small_room);
            default:
                return res.getString(R.string.snow_preset_unknown);
        }
    }

    @Override
    protected void finalize() throws Throwable {
        release();
        super.finalize();
    }
}
