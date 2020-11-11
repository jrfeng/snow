package snow.music.util;

import android.content.res.Resources;

import androidx.annotation.DimenRes;
import androidx.annotation.NonNull;

import com.google.common.base.Preconditions;

/**
 * 尺寸工具。
 */
public final class DimenUtil {
    private DimenUtil() {
        throw new AssertionError();
    }

    /**
     * 将 dimen 资源转换成 px 像素值。
     *
     * @param resources Resources 对象
     * @param dimenRes  dimen 资源
     * @return dimen 资源对应的 px 像素
     */
    public static int getDimenPx(@NonNull Resources resources, @DimenRes int dimenRes) {
        Preconditions.checkNotNull(resources);
        return resources.getDimensionPixelSize(dimenRes);
    }
}
