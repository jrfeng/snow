package snow.player.audio;

import android.content.Context;
import android.content.res.Resources;

import snow.player.R;

/**
 * 预定义错误码。
 */
public final class ErrorCode {
    /**
     * 没有发生任何错误（默认值）。
     */
    public static final int NO_ERROR = 0;
    /**
     * 仅允许 Wifi 网络下联网。
     */
    public static final int ONLY_WIFI_NETWORK = 1;
    /**
     * 播放器错误。
     */
    public static final int PLAYER_ERROR = 2;
    /**
     * 网络错误。
     */
    public static final int NETWORK_ERROR = 3;
    /**
     * 文件未找到。
     */
    public static final int FILE_NOT_FOUND = 4;
    /**
     * 数据加载失败。
     */
    public static final int DATA_LOAD_FAILED = 5;
    /**
     * 获取播放链接失败。
     */
    public static final int GET_URL_FAILED = 6;
    /**
     * 内存不足。
     */
    public static final int OUT_OF_MEMORY = 7;
    /**
     * 未知错误。
     */
    public static final int UNKNOWN_ERROR = 8;
    /**
     * 在准备 MusicItem 时出错。
     * <p>
     * 如果发生了该错误，请检查你的 {@code PlayerService} 的 {@code onPrepareMusicItem(MusicItem, SoundQuality, AsyncResult)} 方法。
     */
    public static final int PREPARE_MUSIC_ITEM_ERROR = 9;

    private ErrorCode() {
        throw new AssertionError();
    }

    /**
     * 根据错误码获取对应的错误信息。
     *
     * @param context   Context 对象
     * @param errorCode 错误码
     * @return 错误码对应的错误信息
     */
    public static String getErrorMessage(Context context, int errorCode) {
        Resources res = context.getResources();

        switch (errorCode) {
            case NO_ERROR:
                return res.getString(R.string.snow_error_no_error);
            case ONLY_WIFI_NETWORK:
                return res.getString(R.string.snow_error_only_wifi_network);
            case PLAYER_ERROR:
                return res.getString(R.string.snow_error_player_error);
            case NETWORK_ERROR:
                return res.getString(R.string.snow_error_network_error);
            case FILE_NOT_FOUND:
                return res.getString(R.string.snow_error_file_not_found);
            case DATA_LOAD_FAILED:
                return res.getString(R.string.snow_error_data_load_failed);
            case GET_URL_FAILED:
                return res.getString(R.string.snow_error_get_url_failed);
            case OUT_OF_MEMORY:
                return res.getString(R.string.snow_error_out_of_memory);
            case UNKNOWN_ERROR:
                return res.getString(R.string.snow_error_unknown_error);
            case PREPARE_MUSIC_ITEM_ERROR:
                return res.getString(R.string.snow_error_unknown_error);
            default:
                return res.getString(R.string.snow_error_unknown_error);
        }
    }
}
