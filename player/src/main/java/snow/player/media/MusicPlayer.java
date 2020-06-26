package snow.player.media;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.os.PowerManager;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

import com.google.common.base.Preconditions;

import java.io.IOException;

/**
 * 该类定义了音乐播放器的基本功能。可以通过继承该类来实现一个音乐播放器。
 */
public abstract class MusicPlayer {
    private static final String TAG = "MusicPlayer";

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
        if (wakeLockPermissionDenied()) {
            Log.w(TAG, "Forget to request 'android.permission.WAKE_LOCK' permission?");
            return;
        }

        if (mWakeLock != null && !mWakeLock.isHeld()) {
            mWakeLock.acquire(getDuration() + 5_000);
        }

        if (mWifiLock != null && !mWifiLock.isHeld()) {
            mWifiLock.acquire();
        }
    }

    private boolean wakeLockPermissionDenied() {
        return PackageManager.PERMISSION_DENIED ==
                ContextCompat.checkSelfPermission(getContext(), Manifest.permission.WAKE_LOCK);
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
    protected final Context getContext() {
        return mApplicationContext;
    }

    /**
     * 即可播放本地文件，也可以播放网络文件。
     *
     * @param uri 要播放的音乐的 mUri。
     */
    public abstract void setDataSource(Uri uri) throws IOException;

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
     * 获取当前音频文件的持续时间（单位：毫秒）。
     *
     * @return 当前音频文件的持续时间（单位：毫秒），如果持续时间未知，则返回 -1。
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
     * <p>
     * 该方法对 WakeLock 和 WifiLock 进行了处理，你的音乐播放器实现应该重写该方法以实现开始播放功能。
     */
    public void start() {
        requireWakeLock();
    }

    /**
     * 暂停播放。
     * <p>
     * 该方法对 WakeLock 和 WifiLock 进行了处理，你的音乐播放器实现应该重写该方法以实现暂停播放功能。
     */
    public void pause() {
        releaseWakeLock();
    }

    /**
     * 停止播放。
     * <p>
     * 该方法对 WakeLock 和 WifiLock 进行了处理，你的音乐播放器实现应该重写该方法以实现停止播放功能。
     */
    public void stop() {
        releaseWakeLock();
    }

    /**
     * 调整播放器的播放位置。
     *
     * @param pos 要调整到的播放位置。
     */
    public abstract void seekTo(int pos);

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
     * <p>
     * 该方法对 WakeLock 和 WifiLock 进行了处理，你的音乐播放器实现应该重写该方法以释放占用的资源。
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
        /**
         * 该方法会在音乐播放器准备完毕时被调用。
         *
         * @param mp 当前音乐播放器。
         */
        void onPrepared(MusicPlayer mp);
    }

    /**
     * 用于监听音乐播放器是否播放完毕。
     */
    public interface OnCompletionListener {
        /**
         * 该方法会在音乐播放完毕时被调用。
         *
         * @param mp 当前音乐播放器。
         */
        void onCompletion(MusicPlayer mp);
    }

    /**
     * 用于监听音乐播放器的播放进度是的调整完毕。
     */
    public interface OnSeekCompleteListener {
        /**
         * 该方法会在 seek 完成时被调用。
         *
         * @param mp 当前音乐播放器。
         */
        void onSeekComplete(MusicPlayer mp);
    }

    /**
     * 用于监听事件：进入缓冲区的数据变慢或停止并且播放缓冲区没有足够的数据继续播放。
     */
    public interface OnStalledListener {
        /**
         * 该方法会在播放的 stalled 状态改变时调用。
         *
         * @param stalled 如果缓冲区没有足够的数据继续播放，则该参数为 true，当缓冲区缓存了足够的数据可以继续
         *                播放时，该参数为 false。
         */
        void onStalled(boolean stalled);
    }

    /**
     * 用于监听音乐播放器的缓冲进度。
     */
    public interface OnBufferingUpdateListener {
        /**
         * 该方法会在缓存进度更新时调用。
         *
         * @param mp      当前音乐播放器。
         * @param percent 缓存进度，百分比值，范围为 [0, 100]
         */
        void onBufferingUpdate(MusicPlayer mp, int percent);
    }

    /**
     * 用于监听器音乐播放的错误状态。
     */
    public interface OnErrorListener {
        /**
         * 该方法会在错误发生时被调用。
         * <p>
         * 注意！当发生错误后，不允许再继续使用当前 MusicPlayer 对象，必须将其释放掉。如果需要继续播放，则
         * 需要创建一个新的 MusicPlayer 对象。
         *
         * @param mp        当前播放器。
         * @param errorCode 错误码。
         */
        void onError(MusicPlayer mp, int errorCode);
    }
}
