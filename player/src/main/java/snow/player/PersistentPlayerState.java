package snow.player;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.common.base.Preconditions;
import com.tencent.mmkv.MMKV;

import snow.player.media.MusicItem;

class PersistentPlayerState extends PlayerState {
    private static final String KEY_PLAY_PROGRESS = "play_progress";
    private static final String KEY_PLAY_PROGRESS_UPDATE_TIME = "play_progress_update_time";
    private static final String KEY_MUSIC_ITEM = "music_item";
    private static final String KEY_POSITION = "position";
    private static final String KEY_PLAY_MODE = "play_mode";

    private MMKV mMMKV;

    public PersistentPlayerState(@NonNull Context context, @NonNull String id) {
        Preconditions.checkNotNull(context);
        Preconditions.checkNotNull(id);

        MMKV.initialize(context);

        mMMKV = MMKV.mmkvWithID(id);

        super.setPlayProgress(mMMKV.decodeInt(KEY_PLAY_PROGRESS, 0));
        super.setPlayProgressUpdateTime(mMMKV.decodeLong(KEY_PLAY_PROGRESS_UPDATE_TIME, System.currentTimeMillis()));
        super.setMusicItem(mMMKV.decodeParcelable(KEY_MUSIC_ITEM, MusicItem.class));

        super.setPosition(mMMKV.decodeInt(KEY_POSITION, 0));
        super.setPlayMode(Player.PlayMode.values()[mMMKV.decodeInt(KEY_PLAY_MODE, 0)]);
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
    public void setMusicItem(@Nullable MusicItem musicItem) {
        super.setMusicItem(musicItem);

        if (musicItem == null) {
            mMMKV.remove(KEY_MUSIC_ITEM);
            return;
        }

        mMMKV.encode(KEY_MUSIC_ITEM, musicItem);
    }

    @Override
    public void setPosition(int position) {
        super.setPosition(position);

        mMMKV.encode(KEY_POSITION, position);
    }

    @Override
    public void setPlayMode(@NonNull Player.PlayMode playMode) {
        super.setPlayMode(playMode);

        mMMKV.encode(KEY_PLAY_MODE, playMode.ordinal());
    }
}
