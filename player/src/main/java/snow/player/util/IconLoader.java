package snow.player.util;

import android.graphics.Bitmap;
import android.widget.ImageView;

import androidx.annotation.NonNull;

import snow.player.audio.MusicItem;

/**
 * 用于帮助加载 {@link MusicItem} 对象的 Icon 图片。
 */
public interface IconLoader {
    /**
     * 加载 {@link MusicItem} 的图片，并将其设置给一个 ImageView 对象。
     *
     * @param musicItem {@link MusicItem} 对象，不能为 null
     * @param target    ImageView 对象，不能为 null
     */
    void loadIcon(@NonNull MusicItem musicItem, @NonNull ImageView target);

    /**
     * 加载 {@link MusicItem} 的图片，并在加载完成时调用 {@link Target} 回调。
     *
     * @param musicItem {@link MusicItem} 对象，不能为 null
     * @param target    {@link Target} 回调，不能为 null
     */
    void loadIcon(@NonNull MusicItem musicItem, @NonNull Target target);

    /**
     * 设置默认图片，该图片会在加载失败时返回。
     *
     * @param defaultIcon 默认图片，不能为 null
     */
    void setDefaultIcon(@NonNull Bitmap defaultIcon);

    /**
     * 对图片应用圆形裁剪。
     *
     * @param radius 圆形的半径。
     */
    void circleClip(int radius);

    /**
     * 对图片应用圆角矩形裁剪。
     *
     * @param topLeftRadius     左上角的圆角半径
     * @param topRightRadius    右上角的圆角半径
     * @param bottomRightRadius 右下角的圆角半径
     * @param bottomLeftRadius  左下角的圆角半径
     */
    void roundRectClip(int topLeftRadius, int topRightRadius, int bottomRightRadius, int bottomLeftRadius);

    /**
     * 取消加载。
     */
    void cancel();

    /**
     * 自定义目标。
     */
    interface Target {
        /**
         * 该方法会在加载完成时调用。
         *
         * @param bitmap 图片
         */
        void onIconLoaded(@NonNull Bitmap bitmap);

        /**
         * 图片的宽度。
         */
        int getWith();

        /**
         * 图片的高度。
         */
        int getHeight();
    }
}
