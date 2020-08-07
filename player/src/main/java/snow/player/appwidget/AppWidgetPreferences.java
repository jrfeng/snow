package snow.player.appwidget;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.tencent.mmkv.MMKV;

import java.util.Map;
import java.util.Set;

import snow.player.PlayerService;

/**
 * 用于 PlayerService 与 AppWidget 之间的状态同步。支持跨进程访问。
 * <p>
 * PlayerService 会在修改 AppWidgetPreferences 后会发送 {@link #ACTION_PREFERENCE_CHANGED} 广播，可以让
 * 你的 AppWidgetProvider 监听此广播来同步刷新 AppWidget 的 UI。
 */
public class AppWidgetPreferences implements SharedPreferences {
    /**
     * PlayerService 会在修改 AppWidgetPreferences 后会发送此广播。
     * <p>
     * 该常量字符串的值为：{@code "snow.player.appwidget.action.PREFERENCE_CHANGED"}
     */
    public static final String ACTION_PREFERENCE_CHANGED = "snow.player.appwidget.action.PREFERENCE_CHANGED";
    private MMKV mMMKV;

    /**
     * 创建一个 AppWidgetPreferences 对象。
     *
     * @param context Context 对象
     * @param service 关联的 PlayerService 类的 Class 对象
     */
    public AppWidgetPreferences(@NonNull Context context, @NonNull Class<? extends PlayerService> service) {
        MMKV.initialize(context);
        mMMKV = MMKV.mmkvWithID("PlayerStateProvider:" + service.getName(), MMKV.MULTI_PROCESS_MODE);
    }

    @Override
    public Map<String, ?> getAll() {
        return mMMKV.getAll();
    }

    @Nullable
    @Override
    public String getString(String s, @Nullable String s1) {
        return mMMKV.getString(s, s1);
    }

    @Nullable
    @Override
    public Set<String> getStringSet(String s, @Nullable Set<String> set) {
        return mMMKV.getStringSet(s, set);
    }

    @Override
    public int getInt(String s, int i) {
        return mMMKV.getInt(s, i);
    }

    @Override
    public long getLong(String s, long l) {
        return mMMKV.getLong(s, l);
    }

    @Override
    public float getFloat(String s, float v) {
        return mMMKV.getFloat(s, v);
    }

    @Override
    public boolean getBoolean(String s, boolean b) {
        return mMMKV.getBoolean(s, b);
    }

    @Override
    public boolean contains(String s) {
        return mMMKV.contains(s);
    }

    @Override
    public Editor edit() {
        return mMMKV.edit();
    }

    /**
     * 注意！AppWidgetPreferences 不支持该功能！
     */
    @Override
    public void registerOnSharedPreferenceChangeListener(OnSharedPreferenceChangeListener onSharedPreferenceChangeListener) {
        mMMKV.registerOnSharedPreferenceChangeListener(onSharedPreferenceChangeListener);
    }

    /**
     * 注意！AppWidgetPreferences 不支持该功能！
     */
    @Override
    public void unregisterOnSharedPreferenceChangeListener(OnSharedPreferenceChangeListener onSharedPreferenceChangeListener) {
        mMMKV.registerOnSharedPreferenceChangeListener(onSharedPreferenceChangeListener);
    }
}
