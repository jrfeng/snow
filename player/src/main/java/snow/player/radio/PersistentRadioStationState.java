package snow.player.radio;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.common.base.Preconditions;
import com.tencent.mmkv.MMKV;

import snow.player.media.MusicItem;

/**
 * 用于对 “电台” 状态进行持久化。
 */
public class PersistentRadioStationState extends RadioStationState {
    private static final String KEY_PLAY_PROGRESS = "play_progress";
    private static final String KEY_PLAY_PROGRESS_UPDATE_TIME = "play_progress_update_time";
    private static final String KEY_MUSIC_ITEM = "music_item";

    private static final String KEY_RADIO_STATION = "radio_station";

    private MMKV mMMKV;

    public PersistentRadioStationState(@NonNull Context context, @NonNull String id) {
        Preconditions.checkNotNull(context);
        Preconditions.checkNotNull(id);

        MMKV.initialize(context);

        mMMKV = MMKV.mmkvWithID(id);

        super.setPlayProgress(mMMKV.decodeInt(KEY_PLAY_PROGRESS, 0));
        super.setPlayProgressUpdateTime(mMMKV.decodeLong(KEY_PLAY_PROGRESS_UPDATE_TIME, System.currentTimeMillis()));
        super.setMusicItem(mMMKV.decodeParcelable(KEY_MUSIC_ITEM, MusicItem.class));

        super.setRadioStation(mMMKV.decodeParcelable(KEY_RADIO_STATION, RadioStation.class, new RadioStation()));
    }

    @Override
    public void setPlayProgress(int playProgress) {
        super.setPlayProgress(playProgress);

        mMMKV.encode(KEY_PLAY_PROGRESS, playProgress);
    }

    @Override
    public void setPlayProgressUpdateTime(long updateTime) {
        super.setPlayProgressUpdateTime(updateTime);

        mMMKV.encode(KEY_PLAY_PROGRESS_UPDATE_TIME, updateTime);
    }

    @Override
    public void setRadioStation(@NonNull RadioStation radioStation) {
        Preconditions.checkNotNull(radioStation);
        super.setRadioStation(radioStation);

        mMMKV.encode(KEY_RADIO_STATION, radioStation);
    }

    @Override
    public void setMusicItem(@Nullable MusicItem musicItem) {
        super.setMusicItem(musicItem);

        if (musicItem == null) {
            mMMKV.remove(KEY_MUSIC_ITEM);
            return;
        }

        mMMKV.encode(KEY_MUSIC_ITEM, musicItem);
    }

    public RadioStationState getRadioStationState() {
        return new RadioStationState(this);
    }
}
