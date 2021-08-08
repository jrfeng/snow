package snow.player;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.common.base.Preconditions;
import com.tencent.mmkv.MMKV;

import snow.player.audio.MusicItem;

/**
 * 用于对播放器的部分关键状态进行持久化。
 */
class PersistentPlayerState extends PlayerState {
    private static final String KEY_PLAY_PROGRESS = "play_progress";
    private static final String KEY_MUSIC_ITEM = "music_item";
    private static final String KEY_PLAY_POSITION = "position";
    private static final String KEY_PLAY_MODE = "play_mode";
    private static final String KEY_SPEED = "speed";
    private static final String KEY_DURATION = "duration";
    private static final String KEY_WAIT_PLAY_COMPLETE = "wait_play_complete";

    private final MMKV mMMKV;

    public PersistentPlayerState(@NonNull Context context, @NonNull String id) {
        Preconditions.checkNotNull(context);
        Preconditions.checkNotNull(id);

        MMKV.initialize(context);

        mMMKV = MMKV.mmkvWithID("PlayerState:" + id);

        super.setMusicItem(mMMKV.decodeParcelable(KEY_MUSIC_ITEM, MusicItem.class));
        super.setPlayPosition(mMMKV.decodeInt(KEY_PLAY_POSITION, 0));
        super.setPlayMode(PlayMode.getBySerialId(mMMKV.decodeInt(KEY_PLAY_MODE, 0)));
        super.setSpeed(mMMKV.getFloat(KEY_SPEED, 1.0F));
        super.setDuration(mMMKV.getInt(KEY_DURATION, 0));
        super.setWaitPlayComplete(mMMKV.getBoolean(KEY_WAIT_PLAY_COMPLETE, false));

        if (isForbidSeek()) {
            super.setPlayProgress(0);
            return;
        }

        super.setPlayProgress(mMMKV.decodeInt(KEY_PLAY_PROGRESS, 0));
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

        mMMKV.encode(KEY_PLAY_MODE, playMode.serialId);
    }

    @Override
    public void setSpeed(float speed) {
        super.setSpeed(speed);

        mMMKV.encode(KEY_SPEED, speed);
    }

    @Override
    public void setDuration(int duration) {
        super.setDuration(duration);

        mMMKV.encode(KEY_DURATION, duration);
    }

    @Override
    public void setWaitPlayComplete(boolean waitPlayComplete) {
        super.setWaitPlayComplete(waitPlayComplete);

        mMMKV.encode(KEY_WAIT_PLAY_COMPLETE, waitPlayComplete);
    }
}
