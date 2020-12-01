package snow.music.activity.player;

import android.animation.ObjectAnimator;
import android.os.Build;
import android.view.View;
import android.view.animation.LinearInterpolator;

import androidx.annotation.NonNull;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleObserver;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.OnLifecycleEvent;

import com.google.common.base.Preconditions;

import snow.player.PlayerClient;
import snow.player.lifecycle.PlayerViewModel;

/**
 * 管理布局中 {@link snow.music.R.id#ivDisk } 的动画。
 */
public class AlbumIconAnimManager implements LifecycleObserver {
    private View mTarget;
    private LifecycleOwner mLifecycleOwner;
    private PlayerViewModel mPlayerViewModel;

    private ObjectAnimator mDiskRotateAnimator;
    private long mDiskAnimPlayTime;
    private boolean mActivityStopped;
    private boolean mRunning;

    public AlbumIconAnimManager(@NonNull View target,
                                @NonNull LifecycleOwner lifecycleOwner,
                                @NonNull PlayerViewModel viewModel) {
        Preconditions.checkNotNull(target);
        Preconditions.checkNotNull(lifecycleOwner);
        Preconditions.checkNotNull(viewModel);

        mTarget = target;
        mLifecycleOwner = lifecycleOwner;
        mPlayerViewModel = viewModel;
        initDiskRotateAnim();

        lifecycleOwner.getLifecycle().addObserver(this);
    }

    /**
     * 重置动画。
     */
    public void reset() {
        mDiskRotateAnimator.cancel();
        mTarget.setRotation(0);
        initDiskRotateAnim();
        resumeAnim();
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_START)
    public void onStart() {
        mActivityStopped = false;
        resumeAnim();
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_STOP)
    public void onStop() {
        mActivityStopped = true;
        pauseAnim();
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
    public void onDestroy() {
        mDiskRotateAnimator.cancel();
    }

    private void initDiskRotateAnim() {
        mRunning = false;
        mDiskRotateAnimator = ObjectAnimator.ofFloat(mTarget, "rotation", 0, 360);
        mDiskRotateAnimator.setDuration(20_000);
        mDiskRotateAnimator.setRepeatCount(-1);
        mDiskRotateAnimator.setRepeatMode(ObjectAnimator.RESTART);
        mDiskRotateAnimator.setInterpolator(new LinearInterpolator());

        mPlayerViewModel.getPlayingNoStalled()
                .observe(mLifecycleOwner, playingNoStalled -> {
                    if (playingNoStalled) {
                        resumeAnim();
                    } else {
                        pauseAnim();
                    }
                });
    }

    private void pauseAnim() {
        if (!mRunning) {
            return;
        }

        mRunning = false;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            mDiskRotateAnimator.pause();
            return;
        }

        mDiskAnimPlayTime = mDiskRotateAnimator.getCurrentPlayTime();
        mDiskRotateAnimator.cancel();
    }

    private void resumeAnim() {
        if (mRunning || !shouldStartAnim()) {
            return;
        }

        mRunning = true;

        if (!mDiskRotateAnimator.isStarted()) {
            mDiskRotateAnimator.start();
            return;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            mDiskRotateAnimator.resume();
            return;
        }

        mDiskRotateAnimator.start();
        mDiskRotateAnimator.setCurrentPlayTime(mDiskAnimPlayTime);
    }

    private boolean shouldStartAnim() {
        PlayerClient playerClient = mPlayerViewModel.getPlayerClient();
        return !mActivityStopped && playerClient.isPlaying() && !playerClient.isPreparing() && !playerClient.isStalled();
    }
}
