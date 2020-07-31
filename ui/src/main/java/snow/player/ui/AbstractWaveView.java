package snow.player.ui;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Canvas;
import android.media.audiofx.Equalizer;
import android.media.audiofx.Visualizer;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.core.app.ActivityCompat;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleObserver;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.OnLifecycleEvent;

/**
 * 用于显示音频波形。
 * <p>
 * 波形捕获功能基于 {@code android.media.audiofx.Visualizer} 实现, 需要申请
 * {@code android.permission.RECORD_AUDIO} 权限, 否则波形捕获功能无法使用。
 * <p>
 * 你需要调用 {@link #setEnabled(boolean)} 方法并传入 true 来启用波形捕获功能。当波形捕获功能启动后，如果该
 * View 嵌入的 Activity 具有生命周期感知功能（实现了 LifecycleOwner 接口）, 那么该类会自动在 onStop 时暂停
 * 波形捕获功能, 并在 onStart 时重新启动波形捕获功能。类似的, 当该 View 变得不可见（GONE 或者 INVISIBLE）时,
 * 会自动暂停波形捕获功能, 并在 View 重新变得可见（View.VISIBLE）时重新启动波形捕获功能。
 */
public abstract class AbstractWaveView extends View implements LifecycleObserver {
    private static final String TAG = "WaveView";

    @Nullable
    private Visualizer mVisualizer;
    private int mCaptureSize = getMinCaptureSize();
    private int mCaptureRate = getMaxCaptureRate();

    // 避免因系统音量大小的改变影响到波形（这个 Equalizer 什么也不会做）
    private Equalizer mEqualizer;

    private byte[] mWaveform;
    private int mSamplingRate;

    private boolean mEnable;

    @Nullable
    private LifecycleOwner mLifecycleOwner;

    public AbstractWaveView(Context context) {
        super(context);
    }

    public AbstractWaveView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public AbstractWaveView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @RequiresApi(21)
    public AbstractWaveView(Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    /**
     * 设置要捕获波形的音频的 AudioSessionId。
     * <p>
     * 需要调用 {@link #setEnabled(boolean)} 方法并传入 true 来启动波形捕获功能。
     * <p>
     * 如果该 View 嵌入的那个 Activity 具有生命周期感知功能（实现了 LifecyclerOwner 接口）, 那么该类会自动
     * 在 ON_STOP 事件发生时暂停波形捕获功能, 并在 ON_START 事件发生时重新启动波形捕获功能。否则, 你需要手动
     * 处理相关的 Activity 生命周期事件。
     *
     * @param audioSessionId 正在播放的音频的 AudioSessionId。
     */
    public void setAudioSessionId(int audioSessionId) {
        if (getContext() instanceof LifecycleOwner) {
            setAudioSessionId(audioSessionId, (LifecycleOwner) getContext());
            return;
        }

        setAudioSessionId(audioSessionId, null);
    }

    /**
     * 设置要捕获波形的音频的 AudioSessionId, 以及相关的 LifecycleOwner。
     * <p>
     * 要启动波形捕获功能, 还需要调用 {@link #setEnabled(boolean)} 方法并传入 true。如果波形捕获功能已启动,
     * 那么就不需要再次调用 setEnabled(true)。
     *
     * @param audioSessionId 正在播放的音频的 AudioSessionId。
     * @param lifecycleOwner 要监听的 LifecycleOwner。如果该参数不为 null, 那么会在 ON_STOP 事件发生时暂
     *                       停波形的捕获, 并在 ON_START 事件发生时恢复波形的捕获。并且会在 ON_DESTROY 事
     *                       件发生时调用 {@link #release()} 方法释放掉占用的资源。如果该参数为 null, 则
     *                       需要你自己手动处理相关的 Activity 生命周期事件。
     */
    public void setAudioSessionId(int audioSessionId, @Nullable LifecycleOwner lifecycleOwner) {
        releaseVisualizer();

        if (audioSessionId < 0) {
            throw new IllegalArgumentException("audio session id must >= 0");
        }

        if (noRecordAudioPermission()) {
            Log.e(TAG, "need permission: android.permission.RECORD_AUDIO");
            return;
        }

        if (audioSessionId == 0 && noModifyAudioSettingsPermission()) {
            Log.e(TAG, "audio session id is 0, need permission: android.permission.MODIFY_AUDIO_SETTINGS");
            return;
        }

        if (lifecycleOwner != null) {
            mLifecycleOwner = lifecycleOwner;
            initLifecycleObserver();
        }

        mVisualizer = new Visualizer(audioSessionId);
        mVisualizer.setCaptureSize(getCaptureSize());
        onVisualizerCreated(mVisualizer);
        mVisualizer.setDataCaptureListener(new Visualizer.OnDataCaptureListener() {
            @Override
            public void onWaveFormDataCapture(Visualizer visualizer, byte[] waveform, int samplingRate) {
                mWaveform = waveform;
                mSamplingRate = samplingRate;
                postInvalidate();
            }

            @Override
            public void onFftDataCapture(Visualizer visualizer, byte[] fft, int samplingRate) {
                // ignore
            }
        }, getCaptureRate(), true, false);

        // 避免因音量大小的改变影响到波形
        mEqualizer = new Equalizer(-1, audioSessionId);

        if (mEnable) {
            startVisualizer();
        }
    }

    /**
     * 启动或暂停波形捕获功能。
     * <p>
     * 建议在开始播放时启动波形捕获, 在暂停播放时也暂停波形捕获。
     * <p>
     * 注意！如果你的应用没有 android.permission.RECORD_AUDIO 权限, 那么调用该方法并传入 true 是无效的。
     *
     * @param enabled 该参数为 true 时启动波形捕获功能, 该参数为 false 时暂停波形捕获功能。
     */
    public void setEnabled(boolean enabled) {
        if (enabled && noRecordAudioPermission()) {
            Log.e(TAG, "need permission: android.permission.RECORD_AUDIO");
            return;
        }

        mEnable = enabled;

        if (mEnable) {
            startVisualizer();
        } else {
            pauseVisualizer();
        }
    }

    /**
     * 判断波形捕获功能是否已启动。
     */
    public boolean isEnable() {
        return mEnable;
    }

    /**
     * 获取能够捕获的最小波形数组的大小。
     */
    public static int getMinCaptureSize() {
        return Visualizer.getCaptureSizeRange()[0];
    }

    /**
     * 获取能够捕获的最大波形数组的大小。
     */
    public static int getMaxCaptureSize() {
        return Visualizer.getCaptureSizeRange()[1];
    }

    /**
     * 获取最大的波形捕获率（一般为 20000）。
     */
    public static int getMaxCaptureRate() {
        return Visualizer.getMaxCaptureRate();
    }

    /**
     * 获取当前设置的要捕获的波形数组的大小。
     */
    public final int getCaptureSize() {
        return mCaptureSize;
    }

    /**
     * 设置要捕获的波形数组的大小。该值必须是 {@link #getMinCaptureSize()} 至 {@link #getMaxCaptureSize()}
     * 范围内的 2 的幂。
     */
    public final void setCaptureSize(int captureSize) {
        mCaptureSize = captureSize;

        if (mCaptureSize < getMinCaptureSize()) {
            mCaptureSize = getMinCaptureSize();
        }

        if (mCaptureSize > getMaxCaptureSize()) {
            mCaptureSize = getMaxCaptureSize();
        }
    }

    /**
     * 获取当前设置的波形捕获率（默认使用最大的波形捕获率）。
     */
    public final int getCaptureRate() {
        return mCaptureRate;
    }

    /**
     * 设置当前的波形捕获率。最大值为 {@link #getMaxCaptureRate()}, 值越大, 捕获波形的频率越快。默认使用最
     * 大的波形捕获率。
     *
     * @see #getMaxCaptureRate()
     */
    public final void setCaptureRate(int captureRate) {
        mCaptureRate = captureRate;

        if (mCaptureRate > getMaxCaptureRate()) {
            mCaptureRate = getMaxCaptureRate();
        }
    }

    /**
     * 手动释放掉占用的资源。
     * <p>
     * 建议在停止播放后调用该方法释放占用的资源。调用该方法后, 如果你需要重新启动波形捕获功能, 必须再次调用
     * {@link #setAudioSessionId(int)} 方法设置一个正确的 Audio Session Id, 并调用 setEnabled(true) 启
     * 动波形捕获功能。
     * <p>
     * 当 View 从窗口中移除时会自动调用该方法。
     */
    public void release() {
        mEnable = false;
        releaseVisualizer();
    }

    /**
     * LifecyclerOwner 相关的回调方法。
     */
    @OnLifecycleEvent(Lifecycle.Event.ON_START)
    public void onStart() {
        if (mEnable) {
            startVisualizer();
        }
    }

    /**
     * LifecyclerOwner 相关的回调方法。
     */
    @OnLifecycleEvent(Lifecycle.Event.ON_STOP)
    public void onStop() {
        pauseVisualizer();
    }

    /**
     * LifecyclerOwner 相关的回调方法。
     */
    @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
    public void onDesctroy() {
        release();
    }

    // ************************************protected*****************************************

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();

        if (mLifecycleOwner != null) {
            mLifecycleOwner.getLifecycle().removeObserver(this);
        }

        release();
    }

    @Override
    protected void onVisibilityChanged(@NonNull View changedView, int visibility) {
        super.onVisibilityChanged(changedView, visibility);

        if (visibility != VISIBLE) {
            pauseVisualizer();
            return;
        }

        if (mEnable) {
            startVisualizer();
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (mWaveform == null) {
            return;
        }

        onDrawWave(canvas, mWaveform, mSamplingRate);
    }

    /**
     * 该方法会在 Visualizer 对象创建后调用, 如果你不满意默认的 Visualizer 配置, 可以重写该方法对
     * Visualizer 进行自定义配置。
     */
    protected void onVisualizerCreated(@NonNull Visualizer visualizer) {
    }

    /**
     * 重写该方法以绘制波形图像。
     *
     * @param canvas       当前的 Canvas 对象。
     * @param waveform     波形数据。
     * @param samplingRate 波形的采样率。
     */
    protected abstract void onDrawWave(Canvas canvas, @NonNull byte[] waveform, int samplingRate);

    // **************************************private*****************************************

    private boolean noRecordAudioPermission() {
        return noPermission(Manifest.permission.RECORD_AUDIO);
    }

    private boolean noModifyAudioSettingsPermission() {
        return noPermission(Manifest.permission.MODIFY_AUDIO_SETTINGS);
    }

    private boolean noPermission(String permission) {
        int result = ActivityCompat.checkSelfPermission(getContext(), permission);
        return result == PackageManager.PERMISSION_DENIED;
    }

    private void initLifecycleObserver() {
        if (mLifecycleOwner == null) {
            return;
        }

        mLifecycleOwner.getLifecycle().addObserver(this);
    }

    private void startVisualizer() {
        if (mVisualizer != null) {
            mVisualizer.setEnabled(true);
            mEqualizer.setEnabled(true);
        }
    }

    private void pauseVisualizer() {
        if (mVisualizer != null) {
            mVisualizer.setEnabled(false);
            mEqualizer.setEnabled(false);
        }
    }

    private void releaseVisualizer() {
        if (mVisualizer != null) {
            mVisualizer.release();
            mEqualizer.release();
            mVisualizer = null;
            mEqualizer = null;
        }
    }
}
