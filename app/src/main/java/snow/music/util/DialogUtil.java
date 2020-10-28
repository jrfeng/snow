package snow.music.util;

import android.app.Dialog;
import android.graphics.drawable.Drawable;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.StyleRes;

import com.google.common.base.Preconditions;

/**
 * Dialog 工具类。
 */
public final class DialogUtil {
    private static final String TAG = "DialogUtil";

    private DialogUtil() {
        throw new AssertionError();
    }

    /**
     * 设置 Dialog 内容的 Gravity
     *
     * @param dialog  Dialog 对象，不能为 null
     * @param gravity Gravity 常量，具体请参考 {@code android.view.Gravity} 类
     */
    public static void setGravity(@NonNull Dialog dialog, int gravity) {
        Preconditions.checkNotNull(dialog);

        Window window = dialog.getWindow();
        if (window == null) {
            Log.e(TAG, "The Window of dialog is null.");
            return;
        }

        window.setGravity(gravity);
    }

    /**
     * 设置 Dialog 内容的宽度。
     *
     * @param dialog Dialog 对象，不能为 null
     * @param width  宽度值
     */
    public static void setWith(@NonNull Dialog dialog, int width) {
        Preconditions.checkNotNull(dialog);

        Window window = dialog.getWindow();
        if (window == null) {
            Log.e(TAG, "The Window of dialog is null.");
            return;
        }

        View decorView = window.getDecorView();
        decorView.setPadding(0, decorView.getTop(), 0, decorView.getBottom());

        WindowManager.LayoutParams lp = window.getAttributes();
        lp.width = width;
        window.setAttributes(lp);
    }

    /**
     * 设置 Dialog 内容的高度。
     *
     * @param dialog Dialog 对象，不能为 null
     * @param height 高度值
     */
    public static void setHeight(@NonNull Dialog dialog, int height) {
        Preconditions.checkNotNull(dialog);

        Window window = dialog.getWindow();
        if (window == null) {
            Log.e(TAG, "The Window of dialog is null.");
            return;
        }

        View decorView = window.getDecorView();
        decorView.setPadding(decorView.getLeft(), 0, decorView.getRight(), 0);

        WindowManager.LayoutParams lp = window.getAttributes();
        lp.height = height;
        window.setAttributes(lp);
    }

    /**
     * 设置 Dialog 的进入/退出动画。
     * <p>
     * 进入退出动画的一个 style 资源，例如：
     * <p>
     * <pre>
     * {@code
     * <style name="OptionMenuAnim">
     *     <item name="android:windowEnterAnimation">@anim/enter_from_bottom</item>
     *     <item name="android:windowExitAnimation">@anim/exit_from_bottom</item>
     * </style>
     * }
     * </pre>
     * <p>
     * 其中，{@code android:windowEnterAnimation} 与 {@code android:windowExitAnimation} 是两个动画资源，
     * 例如：<p>
     * 动画资源：{@code @anim/enter_from_bottom}<br>
     * <pre>
     * {@code
     * <?xml version="1.0" encoding="utf-8"?>
     * <set xmlns:android="http://schemas.android.com/apk/res/android">
     *     <translate
     *         android:duration="200"
     *         android:fromYDelta="100%"
     *         android:interpolator="@android:anim/accelerate_interpolator"
     *         android:toYDelta="0%"/>
     * </set>
     * }
     * </pre>
     * 动画资源：{@code @anim/exit_from_bottom}<br>
     * <pre>
     * {@code
     * <?xml version="1.0" encoding="utf-8"?>
     * <set xmlns:android="http://schemas.android.com/apk/res/android">
     *     <translate
     *         android:duration="100"
     *         android:fromYDelta="0%"
     *         android:interpolator="@android:anim/decelerate_interpolator"
     *         android:toYDelta="100%"/>
     * </set>
     * }
     * </pre>
     *
     * @param dialog   Dialog 对象，不能为 null
     * @param styleRes style 资源
     */
    public static void setAnimations(@NonNull Dialog dialog, @StyleRes int styleRes) {
        Preconditions.checkNotNull(dialog);

        Window window = dialog.getWindow();
        if (window == null) {
            Log.e(TAG, "The Window of dialog is null.");
            return;
        }

        window.setWindowAnimations(styleRes);
    }

    /**
     * 设置 Dialog 的背景。
     *
     * @param dialog   Dialog 对象，不能为 null
     * @param drawable Drawable 对象，不能为 null
     */
    public static void setBackgroundDrawable(@NonNull Dialog dialog, @NonNull Drawable drawable) {
        Preconditions.checkNotNull(dialog);
        Preconditions.checkNotNull(drawable);

        Window window = dialog.getWindow();
        if (window == null) {
            Log.e(TAG, "The Window of dialog is null.");
            return;
        }

        window.setBackgroundDrawable(drawable);
    }

    /**
     * 设置 Dialog 的背景。
     *
     * @param dialog     Dialog 对象，不能为 null
     * @param drawableId Drawable 资源的 ID
     */
    public static void setBackgroundDrawableResource(@NonNull Dialog dialog, @DrawableRes int drawableId) {
        Preconditions.checkNotNull(dialog);

        Window window = dialog.getWindow();
        if (window == null) {
            Log.e(TAG, "The Window of dialog is null.");
            return;
        }

        window.setBackgroundDrawableResource(drawableId);
    }
}
