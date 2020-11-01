package snow.music.util;

import android.content.res.Resources;

import androidx.annotation.DimenRes;
import androidx.annotation.NonNull;

import com.google.common.base.Preconditions;

public final class DimenUtil {
    private DimenUtil() {
        throw new AssertionError();
    }

    public static int getDimenPx(@NonNull Resources resources, @DimenRes int dimenRes) {
        Preconditions.checkNotNull(resources);
        return resources.getDimensionPixelSize(dimenRes);
    }
}
