package snow.player;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.common.base.Preconditions;
import com.tencent.mmkv.MMKV;

import snow.player.media.MusicItem;

/**
 * 用于对播放器的部分关键状态进行持久化。
 */
class PersistentPlayerState extends PlayerState {
    private static final String KEY_PLAY_PROGRESS = "play_progress";
    private static final String KEY_MUSIC_ITEM = "music_item";
    private static final String KEY_PLAY_POSITION = "position";
    private static final String KEY_PLAY_MODE = "play_mode";

    private MMKV mMMKV;

    public PersistentPlayerState(@NonNull Context context, @NonNull String id) {
        Preconditions.checkNotNull(context);
        Preconditions.checkNotNull(id);

        MMKV.initialize(context);

        mMMKV = MMKV.mmkvWithID("PlayerState:" + id);

        super.setPlayProgress(mMMKV.decodeInt(KEY_PLAY_PROGRESS, 0));
        super.setMusicItem(mMMKV.decodeParcelable(KEY_MUSIC_ITEM, MusicItem.class));
        super.setPlayPosition(mMMKV.decodeInt(KEY_PLAY_POSITION, 0));
        super.setPlayMode(PlayMode.values()[mMMKV.decodeInt(KEY_PLAY_MODE, 0)]);
    }

    @Override
    public void setPlayProgress(int playProgress) {
        super.setPlayProgress(playProgress);

        if (isForbidSeek()) {
            mMMKV.encode(KEY_PLAY_PROGRESS, 0);
            return;
        }

        mMMKV.encode(KEY_PLAY_PROGRESS, playProgress);
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
    public void setPlayPosition(int playPosition) {
        super.setPlayPosition(playPosition);

        mMMKV.encode(KEY_PLAY_POSITION, playPosition);
    }

    @Override
    public void setPlayMode(@NonNull PlayMode playMode) {
        super.setPlayMode(playMode);

        mMMKV.encode(KEY_PLAY_MODE, playMode.ordinal());
    }
}
