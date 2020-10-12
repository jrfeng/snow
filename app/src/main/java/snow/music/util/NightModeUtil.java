package snow.music.util;

import android.content.Context;

import androidx.appcompat.app.AppCompatDelegate;

import com.tencent.mmkv.MMKV;

/**
 * 夜间模式工具。
 * <p>
 * 该工具对 AppCompatDelegate.setDefaultNightMode(int) 方法进行了封装，用于修改和持久化夜间模式配置。
 */
public class NightModeUtil {
    private static final String KEY_MODE = "mode";

    /**
     * 应用上次设置的夜间模式。
     * <p>
     * 可以在 Application 的 onCreate 方法中调用该方法来恢复上次设置的夜间模式。默认的模式为
     * {@link Mode#NIGHT_FOLLOW_SYSTEM}，即 AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
     */
    public static void applyNightMode(Context context) {
        MMKV mmkv = MMKV.mmkvWithID(getMMapId(context));
        Mode mode = Mode.getModeById(mmkv.decodeInt(KEY_MODE, 0));
        AppCompatDelegate.setDefaultNightMode(mode.getModeValue());
    }

    /**
     * 设置夜间模式。
     *
     * @param context Context 对象
     * @param mode    要设置的模式
     * @see Mode
     */
    public static void setDefaultNightMode(Context context, Mode mode) {
        MMKV mmkv = MMKV.mmkvWithID(getMMapId(context));
        mmkv.encode(KEY_MODE, mode.id);
        AppCompatDelegate.setDefaultNightMode(mode.getModeValue());
    }

    private static String getMMapId(Context context) {
        return context.getPackageName() + ".NIGHT_MODE";
    }

    /**
     * 夜间模式。
     */
    public enum Mode {
        /**
         * 跟随系统。
         * <p>
         * 该值被映射为 AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
         */
        NIGHT_FOLLOW_SYSTEM(0),
        /**
         * 禁用夜间模式。
         * <p>
         * 该值被映射为：AppCompatDelegate.MODE_NIGHT_NO
         */
        NIGHT_NO(1),
        /**
         * 启用夜间模式。
         * <p>
         * 该值被映射为：AppCompatDelegate.MODE_NIGHT_YES
         */
        NIGHT_YES(2),
        /**
         * 当启用系统的 “省电模式” 时，启用夜间模式。
         * <p>
         * 该值被映射为：AppCompatDelegate.MODE_NIGHT_AUTO_BATTERY
         */
        NIGHT_AUTO_BATTERY(3);

        private int id;

        Mode(int id) {
            this.id = id;
        }

        private static Mode getModeById(int id) {
            switch (id) {
                case 1:
                    return NIGHT_NO;
                case 2:
                    return NIGHT_YES;
                case 3:
                    return NIGHT_AUTO_BATTERY;
                default:
                    return NIGHT_FOLLOW_SYSTEM;
            }
        }

        private int getModeValue() {
            switch (this) {
                case NIGHT_NO:
                    return AppCompatDelegate.MODE_NIGHT_NO;
                case NIGHT_YES:
                    return AppCompatDelegate.MODE_NIGHT_YES;
                case NIGHT_AUTO_BATTERY:
                    return AppCompatDelegate.MODE_NIGHT_AUTO_BATTERY;
                default:
                    return AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM;
            }
        }
    }

}
