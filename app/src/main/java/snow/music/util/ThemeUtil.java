package snow.music.util;

import android.content.res.Resources;
import android.util.TypedValue;

import androidx.annotation.AttrRes;
import androidx.annotation.ColorInt;

public final class ThemeUtil {

    private ThemeUtil() {
        throw new AssertionError();
    }

    @ColorInt
    public static int getThemeColor(Resources.Theme theme, @AttrRes int attrId) {
        TypedValue outValue = new TypedValue();
        theme.resolveAttribute(attrId, outValue, true);
        return outValue.data;
    }
}
