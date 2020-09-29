package snow.player.helper;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;

import androidx.annotation.NonNull;

import com.google.common.base.Preconditions;

import snow.player.audio.MusicPlayer;

/**
 * 用于帮助实现 “渐隐播放” 功能。
 * <p>
 * <b>使用步骤：</b>
 * <ol>
 *     <li>创建一个 {@link VolumeEaseHelper} 对象；</li>
 *     <li>将 {@link MusicPlayer#start()} 与 {@link MusicPlayer#pause()} 方法分别代理给
 *     {@link #start()} 与 {@link #pause()} 方法，并在
 *     {@link Callback} 中实现真正的 {@link MusicPlayer#start()} 与 {@link MusicPlayer#pause()} 逻辑；</li>
 *     <li>（可选）将 {@link MusicPlayer#quiet()} 与 {@link MusicPlayer#dismissQuiet()} 方法分别代理给
 *     {@link #quiet()} 与 {@link #dismissQuiet()} 方法；</li>
 *     <li>最后，还需分别在 {@link MusicPlayer#stop()} 与 {@link MusicPlayer#release()} 方法中调用
 *     {@link #cancel()} 方法，否则会有内存泄露的风险。</li>
 * </ol>
 * <p>
 * <b>例：</b>
 * <pre>
 * public class MyMusicPlayer implements MusicPlayer {
 *     ...
 *     private VolumeEaseHelper mVolumeEaseHelper;
 *
 *     public MediaMusicPlayer() {
 *         ...
 *         mVolumeEaseHelper = new VolumeEaseHelper(this, new VolumeEaseHelper.Callback() {
 *             &#64;Override
 *             public void start() {
 *                 // 在该方法中实现真正的 start  逻辑
 *                 mMediaPlayer.start();
 *             }
 *
 *             &#64;Override
 *             public void pause() {
 *                 // 在该方法中实现真正的 pause 逻辑
 *                 mMediaPlayer.pause();
 *             }
 *         });
 *     }
 *
 *     &#64;Override
 *     public void start() {
 *         // 将 start 方法代理给 VolumeEaseHelper 对象的 start 方法
 *         mVolumeEaseHelper.start();
 *     }
 *
 *     &#64;Override
 *     public void pause() {
 *         // 将 pause 方法代理给 VolumeEaseHelper 对象的 pause 方法
 *         mVolumeEaseHelper.pause();
 *     }
 *
 *     &#64;Override
 *     public void stop() {
 *         // 在 stop 方法中调用 VolumeEaseHelper 对象的 cancel 方法
 *         mVolumeEaseHelper.cancel();
 *         ...
 *     }
 *
 *     &#64;Override
 *     public synchronized void release() {
 *         // 在 release 方法中调用 VolumeEaseHelper 对象的 cancel 方法
 *         mVolumeEaseHelper.cancel();
 *         ...
 *     }
 *
 *     &#64;Override
 *     public void quiet() {
 *         // 可选：将 quiet 方法代理给 VolumeEaseHelper 对象的 quiet 方法
 *         mVolumeEaseHelper.quiet();
 *     }
 *
 *     &#64;Override
 *     public void dismissQuiet() {
 *         // 可选：将 dismissQuiet 方法代理给 VolumeEaseHelper 对象的 dismissQuiet 方法
 *         mVolumeEaseHelper.dismissQuiet();
 *     }
 * }
 * </pre>
 * <p>
 * 如果你觉得这很繁琐，可以继承 {@link snow.player.audio.AbstractMusicPlayer} 类，该类已对
 * {@link VolumeEaseHelper} 进行了封装，可以减少模板代码。
 *
 * @see snow.player.audio.AbstractMusicPlayer
 */
public class VolumeEaseHelper {
    private final MusicPlayer mMusicPlayer;
    private final Callback mCallback;

    private ObjectAnimator mStartVolumeAnimator;
    private ObjectAnimator mPauseVolumeAnimator;
    private ObjectAnimator mDismissQuietVolumeAnimator;

    private boolean mQuiet;

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

        if (mQuiet) {
            mCallback.pause();
            return;
        }

        mPauseVolumeAnimator.start();
    }

    public void quiet() {
        mQuiet = true;
        mMusicPlayer.setVolume(0.2F, 0.2F);
    }

    public void dismissQuiet() {
        mQuiet = false;
        if (mPauseVolumeAnimator.isStarted()) {
            // 避免和 pause 冲突
            return;
        }

        cancel();
        mDismissQuietVolumeAnimator.start();
    }

    private void initVolumeAnimator() {
        long volumeAnimDuration = 400L;

        mStartVolumeAnimator = ObjectAnimator.ofFloat(this, "volume", 0.0F, 1.0F);
        mStartVolumeAnimator.setDuration(1000L);
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

        mDismissQuietVolumeAnimator = ObjectAnimator.ofFloat(this, "volume", 0.2F, 1.0F);
        mDismissQuietVolumeAnimator.setDuration(volumeAnimDuration);
        mDismissQuietVolumeAnimator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationCancel(Animator animation) {
                setVolume(1.0F);
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

        if (mDismissQuietVolumeAnimator.isStarted()) {
            mDismissQuietVolumeAnimator.cancel();
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
