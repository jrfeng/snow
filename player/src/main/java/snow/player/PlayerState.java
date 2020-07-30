package snow.player;

import android.content.Context;
import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.common.base.Objects;
import com.google.common.base.Preconditions;

import snow.player.media.MusicItem;
import snow.player.util.ErrorUtil;

/**
 * 用于保存基本的播放器状态。
 */
class PlayerState implements Parcelable {
    private int playProgress;
    @Nullable
    private MusicItem musicItem;
    private int position;
    private PlayMode playMode;

    // no persistent
    private long playProgressUpdateTime;
    private PlaybackState playbackState;
    private int audioSessionId;
    private int bufferedProgress;
    private boolean stalled;
    private int errorCode;
    private String errorMessage;

    public PlayerState() {
        playProgress = 0;
        playProgressUpdateTime = 0;
        position = 0;
        playMode = PlayMode.SEQUENTIAL;

        playbackState = PlaybackState.UNKNOWN;
        audioSessionId = 0;
        bufferedProgress = 0;
        stalled = false;
        errorCode = ErrorUtil.NO_ERROR;
        errorMessage = "";
    }

    public PlayerState(PlayerState source) {
        playProgress = source.playProgress;
        playProgressUpdateTime = source.playProgressUpdateTime;
        if (source.musicItem != null) {
            musicItem = new MusicItem(source.musicItem);
        }
        position = source.position;
        playMode = source.playMode;

        playbackState = source.playbackState;
        audioSessionId = source.audioSessionId;
        bufferedProgress = source.bufferedProgress;
        stalled = source.stalled;
        errorCode = source.errorCode;
        errorMessage = source.errorMessage;
    }

    /**
     * 获取播放进度。
     *
     * @return 播放进度。
     */
    public int getPlayProgress() {
        return playProgress;
    }

    /**
     * 设置播放进度。
     *
     * @param playProgress 播放进度（小于 0 时，相当于设置为 0）。
     */
    public void setPlayProgress(int playProgress) {
        if (playProgress < 0) {
            this.playProgress = 0;
            return;
        }

        this.playProgress = playProgress;
    }


    /**
     * 获取上次播放进度的更新时间（单位：毫秒 ms）。
     *
     * @return 上次播放进度的更新时间。
     */
    public long getPlayProgressUpdateTime() {
        return playProgressUpdateTime;
    }

    /**
     * 设置上次播放进度的更新时间。
     *
     * @param updateTime 上次播放进度的更新时间。
     */
    public void setPlayProgressUpdateTime(long updateTime) {
        playProgressUpdateTime = updateTime;
    }

    /**
     * 获取当前播放歌曲的 MusicItem 对象。
     */
    @Nullable
    public MusicItem getMusicItem() {
        return musicItem;
    }

    /**
     * 设置当前播放歌曲的 MusicItem 对象。
     */
    public void setMusicItem(@Nullable MusicItem musicItem) {
        this.musicItem = musicItem;
    }

    /**
     * 获取播放队列的播放位置。
     *
     * @return 播放队列的播放位置。
     */
    public int getPosition() {
        return position;
    }

    /**
     * 设置播放队列的播放位置。
     *
     * @param position 播放队列的播放位置（小于 0 时相当于设置为 0）。
     */
    public void setPosition(int position) {
        if (position < 0) {
            this.position = 0;
            return;
        }

        this.position = position;
    }

    /**
     * 获取播放队列的播放模式。
     *
     * @return 播放队列的播放模式。
     * @see PlayMode
     */
    public PlayMode getPlayMode() {
        return playMode;
    }

    /**
     * 设置播放队列的播放模式。
     *
     * @param playMode 播放队列的播放模式。只能是这些值之一：{@link PlayMode#SEQUENTIAL},
     *                 {@link PlayMode#LOOP},
     *                 {@link PlayMode#SHUFFLE}
     * @see PlayMode
     */
    public void setPlayMode(@NonNull PlayMode playMode) {
        this.playMode = playMode;
    }

    /**
     * 获取播放状态。
     *
     * @return 返回当前播放状态。
     * @see PlaybackState
     */
    public PlaybackState getPlaybackState() {
        return playbackState;
    }

    /**
     * 设置播放状态。
     *
     * @param playbackState 要设置的播放状态。如果该参数的值不是 {@link PlaybackState#ERROR} 则会
     *                      清除错误码（重置为 {@link ErrorUtil#NO_ERROR}）与错误信息。
     *                      只能是这些值之一：{@link PlaybackState#UNKNOWN},
     *                      {@link PlaybackState#PREPARING},
     *                      {@link PlaybackState#PREPARED},
     *                      {@link PlaybackState#PLAYING},
     *                      {@link PlaybackState#PAUSED},
     *                      {@link PlaybackState#STOPPED},
     *                      {@link PlaybackState#ERROR}
     * @see PlaybackState
     */
    public void setPlaybackState(@NonNull PlaybackState playbackState) {
        Preconditions.checkNotNull(playbackState);
        this.playbackState = playbackState;

        if (playbackState != PlaybackState.ERROR) {
            errorCode = ErrorUtil.NO_ERROR;
            errorMessage = "";
        }
    }

    /**
     * 获取当前正在播放的音乐的 audio session id。
     * <p>
     * 注意！可能会返回 0 （API 21: {@link android.media.AudioManager#AUDIO_SESSION_ID_GENERATE}），表示当前音乐的
     * audio session id 不可用。
     */
    public int getAudioSessionId() {
        return audioSessionId;
    }

    /**
     * 设置当前音乐的 audio session id。
     *
     * @param audioSessionId 当前音乐的 audio session id。如果当前音乐的 audio session id 不可用，则可以
     *                       设为 0 （API 21: {@link android.media.AudioManager#AUDIO_SESSION_ID_GENERATE}）。
     */
    public void setAudioSessionId(int audioSessionId) {
        this.audioSessionId = audioSessionId;
    }

    /**
     * 获取当前音乐的缓存进度。百分比值，范围为 [0 ~ 100]。
     */
    public int getBufferedProgress() {
        return bufferedProgress;
    }

    /**
     * 设置当前音乐的缓存进度。百分比值，范围为 [0 ~ 100]。
     */
    public void setBufferedProgress(int bufferedProgress) {
        if (bufferedProgress < 0) {
            this.bufferedProgress = 0;
            return;
        }

        this.bufferedProgress = bufferedProgress;
    }

    /**
     * 获取 {@code stalled} 状态。
     *
     * <b>{@code stalled} 状态</b>：播放器的缓冲区中没有足够的数据支持播放器继续播放时的状态。
     *
     * @return {@code stalled} 状态。当播放器的缓冲区没有足够的数据支持继续播放时，则返回 true。
     */
    public boolean isStalled() {
        return stalled;
    }

    /**
     * 设置 {@code stalled} 状态。
     *
     * <b>{@code stalled} 状态</b>：播放器的缓冲区中没有足够的数据支持播放器继续播放时的状态。
     *
     * @param stalled {@code stalled} 状态
     */
    public void setStalled(boolean stalled) {
        this.stalled = stalled;
    }

    /**
     * 获取错误码。如果没有发生任何错误，则返回 {@link ErrorUtil#NO_ERROR}。
     *
     * @see ErrorUtil
     */
    public int getErrorCode() {
        return errorCode;
    }

    /**
     * 设置错误码。
     *
     * @param errorCode 错误码。预定义错误码，请查看 {@link ErrorUtil} 类。
     * @see ErrorUtil
     */
    public void setErrorCode(int errorCode) {
        this.errorCode = errorCode;
    }

    /**
     * 获取错误信息。
     */
    public String getErrorMessage() {
        return errorMessage;
    }

    /**
     * 设置错误信息（不能为 null）。
     *
     * @param errorMessage 错误信息（不能为 null）。
     * @see ErrorUtil#getErrorMessage(Context, int)
     */
    public void setErrorMessage(@NonNull String errorMessage) {
        Preconditions.checkNotNull(errorMessage);
        this.errorMessage = errorMessage;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PlayerState)) return false;

        PlayerState other = (PlayerState) o;

        return Objects.equal(playProgress, other.playProgress)
                && Objects.equal(playProgressUpdateTime, other.playProgressUpdateTime)
                && Objects.equal(musicItem, other.musicItem)
                && Objects.equal(position, other.position)
                && Objects.equal(playMode, other.playMode)
                && Objects.equal(playbackState, other.playbackState)
                && Objects.equal(audioSessionId, other.audioSessionId)
                && Objects.equal(bufferedProgress, other.bufferedProgress)
                && Objects.equal(stalled, other.stalled)
                && Objects.equal(errorCode, other.errorCode)
                && Objects.equal(errorMessage, other.errorMessage);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(playProgress,
                playProgressUpdateTime,
                musicItem,
                position,
                playMode,
                playbackState,
                audioSessionId,
                bufferedProgress,
                stalled,
                errorCode,
                errorMessage);
    }

    @NonNull
    @Override
    public String toString() {
        return "PlayerState{" +
                "playProgress=" + playProgress +
                ", playProgressUpdateTime=" + playProgressUpdateTime +
                ", musicItem=" + musicItem +
                ", position=" + position +
                ", playMode=" + playMode +
                ", playbackState=" + playbackState +
                ", audioSessionId=" + audioSessionId +
                ", bufferingPercent=" + bufferedProgress +
                ", stalled=" + stalled +
                ", errorCode=" + errorCode +
                ", errorMessage='" + errorMessage + '\'' +
                '}';
    }

    protected PlayerState(Parcel in) {
        playProgress = in.readInt();
        playProgressUpdateTime = in.readLong();
        musicItem = in.readParcelable(Thread.currentThread().getContextClassLoader());
        position = in.readInt();
        playMode = PlayMode.values()[in.readInt()];

        playbackState = PlaybackState.values()[in.readInt()];
        audioSessionId = in.readInt();
        bufferedProgress = in.readInt();
        stalled = in.readByte() != 0;
        errorCode = in.readInt();
        errorMessage = in.readString();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(playProgress);
        dest.writeLong(playProgressUpdateTime);
        dest.writeParcelable(musicItem, flags);
        dest.writeInt(position);
        dest.writeInt(playMode.ordinal());

        dest.writeInt(playbackState.ordinal());
        dest.writeInt(audioSessionId);
        dest.writeInt(bufferedProgress);
        dest.writeByte((byte) (stalled ? 1 : 0));
        dest.writeInt(errorCode);
        dest.writeString(errorMessage);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<PlayerState> CREATOR = new Creator<PlayerState>() {
        @Override
        public PlayerState createFromParcel(Parcel in) {
            return new PlayerState(in);
        }

        @Override
        public PlayerState[] newArray(int size) {
            return new PlayerState[size];
        }
    };
}
