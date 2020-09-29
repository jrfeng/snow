package snow.player.audio;

import snow.player.helper.VolumeEaseHelper;

/**
 * 该类已经对 {@link VolumeEaseHelper} 进行了封装，用于减少模板代码。
 */
public abstract class AbstractMusicPlayer implements MusicPlayer {
    private final VolumeEaseHelper mVolumeEaseHelper;

    public AbstractMusicPlayer() {
        mVolumeEaseHelper = new VolumeEaseHelper(this, new VolumeEaseHelper.Callback() {
            @Override
            public void start() {
                startEx();
            }

            @Override
            public void pause() {
                pauseEx();
            }
        });
    }

    @Override
    public final void start() {
        mVolumeEaseHelper.start();
    }

    /**
     * 开始播放。
     */
    public abstract void startEx();

    @Override
    public final void pause() {
        mVolumeEaseHelper.pause();
    }

    /**
     * 暂停播放。
     */
    public abstract void pauseEx();

    @Override
    public final void stop() {
        mVolumeEaseHelper.cancel();
        stopEx();
    }

    /**
     * 停止播放。
     */
    public abstract void stopEx();

    @Override
    public final void release() {
        mVolumeEaseHelper.cancel();
        releaseEx();
    }

    /**
     * 释放音乐播放器。
     */
    public abstract void releaseEx();

    @Override
    public void quiet() {
        mVolumeEaseHelper.quiet();
    }

    @Override
    public void dismissQuiet() {
        mVolumeEaseHelper.dismissQuiet();
    }
}
