package snow.player.helper;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;

import androidx.annotation.NonNull;

import com.google.common.base.Preconditions;

import snow.player.media.MusicPlayer;

/**
 * 用于帮助实现 “渐隐播放” 功能。
 * <p>
 * 使用步骤：
 * <ol>
 *     <li>创建一个 {@link VolumeEaseHelper} 对象</li>
 *     <li>将 {@link MusicPlayer} 的 {@link MusicPlayer#start()} 与 {@link MusicPlayer#pause()}
 *     方法分别代理给 {@link VolumeEaseHelper} 的 {@link VolumeEaseHelper#start()} 与
 *     {@link VolumeEaseHelper#pause()} 方法，并在 {@link Callback} 中实现真正的
 *     {@link MusicPlayer#start()} 与 {@link MusicPlayer#pause()} 方法。</li>
 *     <li>最后，分别在 {@link MusicPlayer} 的 {@link MusicPlayer#stop()} 与
 *     {@link MusicPlayer#release()} ()} 方法中调用 {@link VolumeEaseHelper} 对象的
 *     {@link VolumeEaseHelper#cancel()} 方法。</li>
 * </ol>
 */
public class VolumeEaseHelper {
    private MusicPlayer mMusicPlayer;
    private Callback mCallback;

    private ObjectAnimator mStartVolumeAnimator;
    private ObjectAnimator mPauseVolumeAnimator;

    /**
     * 创建一个 {@link VolumeEaseHelper} 对象。
     *
     * @param musicPlayer MusicPlayer 对象，不能为 null
     * @param callback    回调接口，不能为 null。请在该回调接口中实现正在的 start 与 pause 逻辑。
     */
    public VolumeEaseHelper(@NonNull MusicPlayer musicPlayer, @NonNull Callback callback) {
        Preconditions.checkNotNull(musicPlayer);
        Preconditions.checkNotNull(callback);

        mMusicPlayer = musicPlayer;
        mCallback = callback;

        initVolumeAnimator();
    }

    public void start() {
        cancel();
        setVolume(0.0F);
        mCallback.start();
        mStartVolumeAnimator.start();
    }

    public void pause() {
        cancel();
        mPauseVolumeAnimator.start();
    }

    private void initVolumeAnimator() {
        long volumeAnimDuration = 400L;

        mStartVolumeAnimator = ObjectAnimator.ofFloat(this, "volume", 0.0F, 1.0F);
        mStartVolumeAnimator.setDuration(volumeAnimDuration);
        mStartVolumeAnimator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationCancel(Animator animation) {
                setVolume(1.0F);
            }
        });

        mPauseVolumeAnimator = ObjectAnimator.ofFloat(this, "volume", 1.0F, 0.0F);
        mPauseVolumeAnimator.setDuration(volumeAnimDuration);
        mPauseVolumeAnimator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationCancel(Animator animation) {
                mCallback.pause();
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                mCallback.pause();
            }
        });
    }

    /**
     * Volume Ease 辅助方法。
     */
    public void setVolume(float volume) {
        mMusicPlayer.setVolume(volume, volume);
    }

    public void cancel() {
        if (mStartVolumeAnimator.isStarted()) {
            mStartVolumeAnimator.cancel();
        }

        if (mPauseVolumeAnimator.isStarted()) {
            mPauseVolumeAnimator.cancel();
        }
    }

    /**
     * 回调接口，请在该接口中实现真正的 start 与 pause 逻辑。
     */
    public interface Callback {
        void start();

        void pause();
    }
}
