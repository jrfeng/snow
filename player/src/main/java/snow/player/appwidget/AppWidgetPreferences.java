package snow.player.appwidget;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.tencent.mmkv.MMKV;

import java.util.Map;
import java.util.Set;

import snow.player.PlayMode;
import snow.player.PlaybackState;
import snow.player.PlayerService;
import snow.player.media.MusicItem;

/**
 * 用于 PlayerService 与 AppWidget 之间的状态同步。支持跨进程访问。
 * <p>
 * 默认情况下，AppWidgetPreferences 会在被修改后会发送 {@link #ACTION_PREFERENCE_CHANGED} 广播，可以让
 * 你的 AppWidgetProvider 监听此广播来同步刷新 AppWidget 的 UI。
 */
public class AppWidgetPreferences implements SharedPreferences {
    /**
     * PlayerService 会在修改 AppWidgetPreferences 后会发送此广播。
     * <p>
     * 该常量字符串的值为：{@code "snow.player.appwidget.action.PREFERENCE_CHANGED"}
     */
    public static final String ACTION_PREFERENCE_CHANGED = "snow.player.appwidget.action.PREFERENCE_CHANGED";

    private static final String KEY_PLAYBACK_STATE = "playback_state";
    private static final String KEY_PLAYING_MUSIC_ITEM = "playing_music_item";
    private static final String KEY_PLAY_MODE = "play_mode";
    private static final String KEY_PLAY_PROGRESS = "play_progress";
    private static final String KEY_PLAY_PROGRESS_UPDATE_TIME = "play_progress_update_time";
    private static final String KEY_PREPARING = "preparing";
    private static final String KEY_STALLED = "stalled";
    private static final String KEY_ERROR_MESSAGE = "error_message";

    private Context mApplicationContext;
    private MMKV mMMKV;

    /**
     * 创建一个 AppWidgetPreferences 对象。
     *
     * @param context Context 对象
     * @param service 关联的 PlayerService 类的 Class 对象
     */
    public AppWidgetPreferences(@NonNull Context context, @NonNull Class<? extends PlayerService> service) {
        mApplicationContext = context.getApplicationContext();

        MMKV.initialize(context);
        mMMKV = MMKV.mmkvWithID("AppWidgetPreferences:" + service.getName(), MMKV.MULTI_PROCESS_MODE);
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
        return new Editor(mApplicationContext, mMMKV, false);
    }

    public Editor edit(boolean noNotify) {
        return new Editor(mApplicationContext, mMMKV, noNotify);
    }

    @NonNull
    public PlaybackState getPlaybackState() {
        return PlaybackState.values()[getInt(KEY_PLAYBACK_STATE, 0)];
    }

    @Nullable
    public MusicItem getPlayingMusicItem() {
        return mMMKV.decodeParcelable(KEY_PLAYING_MUSIC_ITEM, MusicItem.class);
    }

    @NonNull
    public PlayMode getPlayMode() {
        return PlayMode.values()[getInt(KEY_PLAY_MODE, 0)];
    }

    public int getPlayProgress() {
        return getInt(KEY_PLAY_PROGRESS, 0);
    }

    public long getPlayProgressUpdateTime() {
        return getLong(KEY_PLAY_PROGRESS_UPDATE_TIME, 0);
    }

    public boolean isPreparing() {
        return getBoolean(KEY_PREPARING, false);
    }

    public boolean isStalled() {
        return getBoolean(KEY_STALLED, false);
    }

    public String getErrorMessage() {
        return getString(KEY_ERROR_MESSAGE, "");
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

    public static class Editor implements SharedPreferences.Editor {
        private Context mApplicationContext;
        private MMKV mMMKV;
        private boolean mNoNotify;

        private Editor(Context context, MMKV mmkv, boolean noNotify) {
            mApplicationContext = context;
            mMMKV = mmkv;
            mNoNotify = noNotify;
        }

        @Override
        public Editor putString(String s, @Nullable String s1) {
            mMMKV.putString(s, s1);
            return this;
        }

        @Override
        public Editor putStringSet(String s, @Nullable Set<String> set) {
            mMMKV.putStringSet(s, set);
            return this;
        }

        @Override
        public Editor putInt(String s, int i) {
            mMMKV.putInt(s, i);
            return this;
        }

        @Override
        public Editor putLong(String s, long l) {
            mMMKV.putLong(s, l);
            return this;
        }

        @Override
        public Editor putFloat(String s, float v) {
            mMMKV.putFloat(s, v);
            return this;
        }

        @Override
        public Editor putBoolean(String s, boolean b) {
            mMMKV.putBoolean(s, b);
            return this;
        }

        @Override
        public Editor remove(String s) {
            mMMKV.remove(s);
            return this;
        }

        @Override
        public Editor clear() {
            mMMKV.clear();
            return this;
        }

        @Override
        public boolean commit() {
            // MMKV 不需要 commit
            notifyPreferenceChanged();
            return true;
        }

        @Override
        public void apply() {
            // MMKV 不需要 apply
            notifyPreferenceChanged();
        }

        private void notifyPreferenceChanged() {
            if (mNoNotify) {
                return;
            }

            Intent intent = new Intent(ACTION_PREFERENCE_CHANGED);
            mApplicationContext.sendBroadcast(intent);
        }

        public Editor setPlaybackState(@NonNull PlaybackState playbackState) {
            return putInt(KEY_PLAYBACK_STATE, playbackState.ordinal());
        }

        public Editor setPlayingMusicItem(@Nullable MusicItem musicItem) {
            if (musicItem == null) {
                mMMKV.remove(KEY_PLAYING_MUSIC_ITEM);
            } else {
                mMMKV.encode(KEY_PLAYING_MUSIC_ITEM, musicItem);
            }
            return this;
        }

        public Editor setPlayMode(@NonNull PlayMode playMode) {
            return putInt(KEY_PLAYBACK_STATE, playMode.ordinal());
        }

        public Editor setPlayProgress(int playProgress) {
            return putInt(KEY_PLAY_PROGRESS, playProgress);
        }

        public Editor setPlayProgressUpdateTime(long updateTime) {
            return putLong(KEY_PLAY_PROGRESS_UPDATE_TIME, updateTime);
        }

        public Editor setPreparing(boolean preparing) {
            return putBoolean(KEY_PREPARING, preparing);
        }

        public Editor setStalled(boolean stalled) {
            return putBoolean(KEY_STALLED, stalled);
        }

        public Editor setErrorMessage(String errorMessage) {
            putString(KEY_ERROR_MESSAGE, errorMessage);
            return this;
        }
    }
}
