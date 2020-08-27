package snow.player.helper;

import android.content.Context;

import snow.player.PlayerClient;
import snow.player.PlayerService;

/**
 * {@link PlayerClient} 帮助类，用于避免创建多余的 {@link PlayerClient} 对象。
 * <p>
 * 该帮助类内部维护了一个计数器和一个 {@link PlayerClient} 对象，每当调用一次 {@link #getPlayerClient()}
 * 方法时，计数器会 +1；每当调用一次 {@link #repay()} 方法时，计数器会 -1。当计数器小于等于 0 时，会自动断开
 * 内部的 {@link PlayerClient} 的连接，并释放内部的 {@link PlayerClient} 对象。这样全局只需使用一个
 * {@link PlayerClient} 对象即可。
 * <p>
 * 可以为你的 Application 类增加一个 {@link PlayerClientHelper} 成员，程序的其他部分可以通过这个
 * {@link PlayerClientHelper} 成员共享同一个 {@link PlayerClient} 对象，避免创建多余的
 * {@link PlayerClient} 对象。
 */
public class PlayerClientHelper {
    private Context mApplicationContext;
    private Class<? extends PlayerService> mPlayerService;

    private PlayerClient mPlayerClient;
    private int mCount;

    /**
     * 创建一个 {@link PlayerClientHelper} 对象。
     *
     * @param context       Context 对象
     * @param playerService PlayerService 或者其子类的 Class 对象
     */
    public PlayerClientHelper(Context context, Class<? extends PlayerService> playerService) {
        mApplicationContext = context.getApplicationContext();
        mPlayerService = playerService;
    }

    /**
     * 获取 {@link PlayerClientHelper} 内部的 {@link PlayerClient} 对象。如果 {@link PlayerClient} 还
     * 没有连接，则会调用 {@link PlayerClient#connect()} 自动连接。
     *
     * @return 返回 {@link PlayerClientHelper} 内部的 {@link PlayerClient} 对象，并将计数器 +1。
     * 如果内部的 {@link PlayerClient} 对象不存在或者已被释放，则会创建一个新的 {@link PlayerClient} 对象，
     * 并将计数器设为 1
     */
    public synchronized PlayerClient getPlayerClient() {
        return getPlayerClient(true);
    }

    /**
     * 获取 {@link PlayerClientHelper} 内部的 {@link PlayerClient} 对象。
     *
     * @param autoConnect 是否自动连接 {@link PlayerClient}，如果为 true，如果 {@link PlayerClient} 还
     *                    没有连接，则会调用 {@link PlayerClient#connect()} 自动连接
     * @return 返回 {@link PlayerClientHelper} 内部的 {@link PlayerClient} 对象，并将计数器 +1。
     * 如果内部的 {@link PlayerClient} 对象不存在或者已被释放，则会创建一个新的 {@link PlayerClient} 对象，
     * 并将计数器设为 1
     */
    public synchronized PlayerClient getPlayerClient(boolean autoConnect) {
        if (mCount <= 0) {
            mPlayerClient = PlayerClient.newInstance(mApplicationContext, mPlayerService);
            mPlayerClient.connect();
            mCount = 1;
        } else {
            mCount += 1;
        }

        if (autoConnect && !mPlayerClient.isConnected()) {
            mPlayerClient.connect();
        }

        return mPlayerClient;
    }

    /**
     * 返还 {@link PlayerClient}，调用该方法后，计数器会 -1，当计数器小于等于 0 时，会自动断开内部的
     * {@link PlayerClient} 的连接，并释放掉内部的 {@link PlayerClient} 对象。
     */
    public synchronized void repay() {
        mCount -= 1;
        if (mCount <= 0) {
            mPlayerClient.disconnect();
            mPlayerClient = null;
        }
    }

    /**
     * 获取计数器的值。
     *
     * @return 计数器的值
     */
    public synchronized int getCount() {
        return mCount;
    }
}
