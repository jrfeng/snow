package snow.player;

import channel.helper.Channel;
import channel.helper.UseOrdinal;

/**
 * 睡眠定时器。
 *
 * @see PlayerClient#startSleepTimer(long)
 * @see PlayerClient#cancelSleepTimer()
 * @see PlayerClient#addOnSleepTimerStateChangeListener(SleepTimer.OnStateChangeListener)
 * @see PlayerClient#removeOnSleepTimerStateChangeListener(SleepTimer.OnStateChangeListener)
 */
@Channel
public interface SleepTimer {
    /**
     * 启动睡眠定时器。
     *
     * @param time   定时器的定时时间（单位：毫秒）。播放器会在经过 time 时间后暂停播放。
     * @param action 定时器的的时间到时要执行的操作。
     */
    void start(long time, @UseOrdinal TimeoutAction action);

    /**
     * 取消睡眠定时器。
     */
    void cancel();

    /**
     * 用于监听睡眠定时器的状态改变。
     * <p>
     * 当启动或者取消睡眠定时器时，该监听器会被调用。
     */
    @Channel
    interface OnStateChangeListener {
        /**
         * 当启动睡眠定时器时会调用该方法。
         * <p>
         * 使用当前的 System.currentTimeMillis() 减去 startTime 即可知道睡眠定时器已经走过的时间。
         *
         * @param time      睡眠定时器的定时时间
         * @param startTime 睡眠定时器的启动时间。使用当前的 System.currentTimeMillis()
         *                  减去 startTime 即可知道睡眠定时器已经走过的时间。
         * @param action    定时器的的时间到时要执行的操作。
         * @see #start(long, TimeoutAction)
         */
        void onStarted(long time, long startTime, @UseOrdinal TimeoutAction action);

        /**
         * 当睡眠定时器被取消时会调用该方法。
         *
         * @see #cancel()
         */
        void onCancelled();
    }

    /**
     * 睡眠定时器的时间到时要执行的操作。
     */
    enum TimeoutAction {
        /**
         * 当时间到时，暂停播放。
         */
        PAUSE,
        /**
         * 当时间到时，停止播放。
         */
        STOP,
        /**
         * 当时间到时，关闭 PlayerService。
         */
        SHUTDOWN
    }
}
