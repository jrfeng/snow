package snow.player.media;

import android.content.Context;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.os.PowerManager;

import androidx.annotation.NonNull;

import com.google.common.base.Preconditions;

import java.io.IOException;

/**
 * 该类定义了音乐播放器的基本功能。可以通过继承该类来实现一个音乐播放器。
 */
public abstract class MusicPlayer {
    private Context mApplicationContext;

    private PowerManager.WakeLock mWakeLock;
    private WifiManager.WifiLock mWifiLock;

    public MusicPlayer(@NonNull Context context) {
        Preconditions.checkNotNull(context);
        mApplicationContext = context.getApplicationContext();

        initWakeLock();
    }

    private void initWakeLock() {
        PowerManager pm = (PowerManager) mApplicationContext.getSystemService(Context.POWER_SERVICE);
        WifiManager wm = (WifiManager) mApplicationContext.getApplicationContext().getSystemService(Context.WIFI_SERVICE);

        String tag = "player:MusicPlayer";

        if (pm != null) {
            mWakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, tag);
            mWakeLock.setReferenceCounted(false);
        }

        if (wm != null) {
            mWifiLock = wm.createWifiLock(WifiManager.WIFI_MODE_FULL, tag);
            mWifiLock.setReferenceCounted(false);
        }
    }

    /**
     * 申请 WakeLock 与 WifiLock
     */
    protected final void requireWakeLock() {
        if (mWakeLock != null && !mWakeLock.isHeld()) {
            mWakeLock.acquire(getDuration() + 5_000);
        }

        if (mWifiLock != null && !mWifiLock.isHeld()) {
            mWifiLock.acquire();
        }
    }

    /**
     * 释放 WakeLock 与 WifiLock
     */
    protected final void releaseWakeLock() {
        if (mWakeLock != null && mWakeLock.isHeld()) {
            mWakeLock.release();
        }

        if (mWifiLock != null && mWifiLock.isHeld()) {
            mWifiLock.release();
        }
    }

    /**
     * 获取 Context 对象。
     *
     * @return Application Context
     */
    protected Context getContext() {
        return mApplicationContext;
    }

    /**
     * 即可播放本地文件，也可以播放网络文件。
     *
     * @param context Context 对象
     * @param uri     要播放的音乐的 mUri。
     */
    public abstract void setDataSource(Context context, Uri uri) throws IOException;

    /**
     * 准备当前音乐播放器。
     *
     * @throws IOException 当准备失败（如：无法读取文件）时会抛出该异常。
     */
    public abstract void prepare() throws IOException;

    /**
     * 以异步的方式准备当前音乐播放器。
     */
    public abstract void prepareAsync();

    /**
     * 设置是否循环播放。
     *
     * @param looping 是否循环播放（默认为 false）。
     */
    public abstract void setLooping(boolean looping);

    /**
     * 判断是否循环播放（默认为 false）。
     *
     * @return 是否循环播放（默认为 false）。
     */
    public abstract boolean isLooping();

    /**
     * 判断是否正在播放。
     *
     * @return 播放器是否正在播放音乐
     */
    public abstract boolean isPlaying();

    /**
     * 获取当前音频文件的持续时间。
     *
     * @return 当前音频文件的持续时间。
     */
    public abstract int getDuration();

    /**
     * 获取音频文件的当前播放位置。
     *
     * @return 音频文件的当前播放位置。
     */
    public abstract int getCurrentPosition();

    /**
     * 开始播放。
     */
    public void start() {
        requireWakeLock();
    }

    /**
     * 暂停播放。
     */
    public void pause() {
        releaseWakeLock();
    }

    /**
     * 停止播放。
     */
    public void stop() {
        releaseWakeLock();
    }

    /**
     * 调整播放器的播放位置。
     *
     * @param pos 要调整到的播放位置。
     */
    public abstract void seekTo(long pos);

    /**
     * 设置音量为当前音量的百分比，范围：0.0 ~ 1.0
     *
     * @param leftVolume  左声道的音量百分比。
     * @param rightVolume 右声道的音量百分比。
     */
    public abstract void setVolume(float leftVolume, float rightVolume);

    /**
     * 临时降低音量。
     */
    public abstract void volumeDuck();

    /**
     * 从临时降低的音量中恢复原来的音量。
     */
    public abstract void volumeRestore();

    /**
     * 释放音乐播放器。注意！一旦调用该方法，就不能再调用 MusicPlayer 对象的任何方法，否则会发生不可预见的错误。
     */
    public void release() {
        releaseWakeLock();
    }

    /**
     * 获取音频会话 ID。如果失败，则返回 0。
     *
     * @return 音频会话 ID。如果失败，则返回 0。
     */
    public abstract int getAudioSessionId();

    /**
     * 设置一个 OnPreparedListener 监听器。
     */
    public abstract void setOnPreparedListener(OnPreparedListener listener);

    /**
     * 设置一个 OnCompletionListener 监听器。
     */
    public abstract void setOnCompletionListener(OnCompletionListener listener);

    /**
     * 设置一个 OnSeekCompleteListener 监听器。
     */
    public abstract void setOnSeekCompleteListener(OnSeekCompleteListener listener);

    /**
     * 设置一个 OnStalledListener 监听器。
     */
    public abstract void setOnStalledListener(OnStalledListener listener);

    /**
     * 设置一个 OnBufferingUpdateListener 监听器。
     */
    public abstract void setOnBufferingUpdateListener(OnBufferingUpdateListener listener);

    /**
     * 设置一个 OnErrorListener 监听器。
     */
    public abstract void setOnErrorListener(OnErrorListener listener);

    /**
     * 用于监听音乐播放器是否准备完毕。
     */
    public interface OnPreparedListener {
        void onPrepared(MusicPlayer mp);
    }

    /**
     * 用于监听音乐播放器是否播放完毕。
     */
    public interface OnCompletionListener {
        void onCompletion(MusicPlayer mp);
    }

    /**
     * 用于监听音乐播放器的播放进度是的调整完毕。
     */
    public interface OnSeekCompleteListener {
        void onSeekComplete(MusicPlayer mp);
    }

    /**
     * 用于监听事件：进入缓冲区的数据变慢或停止并且播放缓冲区没有足够的数据继续播放。
     */
    public interface OnStalledListener {
        void onStalled(boolean stalled);
    }

    /**
     * 用于监听音乐播放器的缓冲进度。
     */
    public interface OnBufferingUpdateListener {
        void onBufferingUpdate(MusicPlayer mp, int percent);
    }

    /**
     * 用于监听器音乐播放的错误状态。
     */
    public interface OnErrorListener {
        void onError(MusicPlayer mp, int errorCode);
    }
}
