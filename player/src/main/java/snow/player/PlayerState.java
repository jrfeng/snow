package snow.player;

import android.content.Context;
import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.common.base.Objects;
import com.google.common.base.Preconditions;

import snow.player.media.MusicItem;

/**
 * 用于保存基本的播放器状态。
 */
public class PlayerState implements Parcelable {
    private int mPlayProgress;
    private long mPlayProgressUpdateTime;
    @Nullable
    private MusicItem mMusicItem;
    private int mPosition;
    private Player.PlayMode mPlayMode;

    // no persistent
    private Player.PlaybackState mPlaybackState;
    private int mAudioSessionId;
    private int mBufferingPercent;
    private long mBufferingPercentUpdateTime;
    private boolean mStalled;
    private int mErrorCode;
    private String mErrorMessage;

    public PlayerState() {
        mPlayProgress = 0;
        mPlayProgressUpdateTime = 0;
        mPosition = 0;
        mPlayMode = Player.PlayMode.SEQUENTIAL;

        mPlaybackState = Player.PlaybackState.UNKNOWN;
        mAudioSessionId = 0;
        mBufferingPercent = 0;
        mBufferingPercentUpdateTime = 0;
        mStalled = false;
        mErrorCode = Player.Error.NO_ERROR;
        mErrorMessage = "";
    }

    public PlayerState(PlayerState source) {
        mPlayProgress = source.mPlayProgress;
        mPlayProgressUpdateTime = source.mPlayProgressUpdateTime;
        if (source.mMusicItem != null) {
            mMusicItem = new MusicItem(source.mMusicItem);
        }
        mPosition = source.mPosition;
        mPlayMode = source.mPlayMode;

        mPlaybackState = source.mPlaybackState;
        mAudioSessionId = source.mAudioSessionId;
        mBufferingPercent = source.mBufferingPercent;
        mBufferingPercentUpdateTime = source.mBufferingPercentUpdateTime;
        mStalled = source.mStalled;
        mErrorCode = source.mErrorCode;
        mErrorMessage = source.mErrorMessage;
    }

    /**
     * 获取播放进度。
     *
     * @return 播放进度。
     */
    public int getPlayProgress() {
        return mPlayProgress;
    }

    /**
     * 设置播放进度。
     *
     * @param playProgress 播放进度（小于 0 时，相当于设置为 0）。
     */
    public void setPlayProgress(int playProgress) {
        if (playProgress < 0) {
            mPlayProgress = 0;
            return;
        }

        mPlayProgress = playProgress;
    }


    /**
     * 获取上次播放进度的更新时间（单位：毫秒 ms）。
     *
     * @return 上次播放进度的更新时间。
     */
    public long getPlayProgressUpdateTime() {
        return mPlayProgressUpdateTime;
    }

    /**
     * 设置上次播放进度的更新时间。
     *
     * @param updateTime 上次播放进度的更新时间。
     */
    public void setPlayProgressUpdateTime(long updateTime) {
        mPlayProgressUpdateTime = updateTime;
    }

    /**
     * 获取当前播放歌曲的 MusicItem 对象。
     */
    @Nullable
    public MusicItem getMusicItem() {
        return mMusicItem;
    }

    /**
     * 设置当前播放歌曲的 MusicItem 对象。
     */
    public void setMusicItem(@Nullable MusicItem musicItem) {
        mMusicItem = musicItem;
    }

    /**
     * 获取播放队列的播放位置。
     *
     * @return 播放队列的播放位置。
     */
    public int getPosition() {
        return mPosition;
    }

    /**
     * 设置播放队列的播放位置。
     *
     * @param position 播放队列的播放位置（小于 0 时相当于设置为 0）。
     */
    public void setPosition(int position) {
        if (position < 0) {
            mPosition = 0;
            return;
        }

        mPosition = position;
    }

    /**
     * 获取播放队列的播放模式。
     *
     * @return 播放队列的播放模式。
     * @see Player.PlayMode
     */
    public Player.PlayMode getPlayMode() {
        return mPlayMode;
    }

    /**
     * 设置播放队列的播放模式。
     *
     * @param playMode 播放队列的播放模式。只能是这些值之一：{@link Player.PlayMode#SEQUENTIAL},
     *                 {@link Player.PlayMode#LOOP},
     *                 {@link Player.PlayMode#SHUFFLE}
     * @see Player.PlayMode
     */
    public void setPlayMode(@NonNull Player.PlayMode playMode) {
        mPlayMode = playMode;
    }

    /**
     * 获取播放状态。
     *
     * @return 返回当前播放状态。
     * @see Player.PlaybackState
     */
    public Player.PlaybackState getPlaybackState() {
        return mPlaybackState;
    }

    /**
     * 设置播放状态。
     *
     * @param playbackState 要设置的播放状态。只能为这些值之一：{@link Player.PlaybackState#UNKNOWN},
     *                      {@link Player.PlaybackState#PREPARING},
     *                      {@link Player.PlaybackState#PREPARED},
     *                      {@link Player.PlaybackState#PLAYING},
     *                      {@link Player.PlaybackState#PAUSED},
     *                      {@link Player.PlaybackState#STOPPED},
     *                      {@link Player.PlaybackState#ERROR}
     * @see Player.PlaybackState
     */
    public void setPlaybackState(@NonNull Player.PlaybackState playbackState) {
        Preconditions.checkNotNull(playbackState);
        mPlaybackState = playbackState;

        if (playbackState != Player.PlaybackState.ERROR) {
            mErrorCode = Player.Error.NO_ERROR;
            mErrorMessage = "";
        }
    }

    /**
     * 获取当前正在播放的音乐的 audio session id。
     * <p>
     * 注意！可能会返回 0 （API 21: {@link android.media.AudioManager#AUDIO_SESSION_ID_GENERATE}），表示当前音乐的
     * audio session id 不可用。
     */
    public int getAudioSessionId() {
        return mAudioSessionId;
    }

    /**
     * 设置当前音乐的 audio session id。
     *
     * @param audioSessionId 当前音乐的 audio session id。如果当前音乐的 audio session id 不可用，则可以
     *                       设为 0 （API 21: {@link android.media.AudioManager#AUDIO_SESSION_ID_GENERATE}）。
     */
    public void setAudioSessionId(int audioSessionId) {
        mAudioSessionId = audioSessionId;
    }

    /**
     * 获取当前音乐的缓存进度。百分比值，范围为 [0 ~ 100]。
     */
    public int getBufferingPercent() {
        return mBufferingPercent;
    }

    /**
     * 设置当前音乐的缓存进度。百分比值，范围为 [0 ~ 100]。
     */
    public void setBufferingPercent(int bufferingPercent) {
        if (bufferingPercent < 0) {
            mBufferingPercent = 0;
            return;
        }

        if (bufferingPercent > 100) {
            mBufferingPercent = 100;
            return;
        }

        mBufferingPercent = bufferingPercent;
    }

    /**
     * 获取上一次缓存进度的更新时间（单位：毫秒 ms）。
     */
    public long getBufferingPercentUpdateTime() {
        return mBufferingPercentUpdateTime;
    }

    /**
     * 设置上一次缓存进度的更新时间（单位：毫秒 ms）。
     */
    public void setBufferingPercentUpdateTime(long bufferingPercentUpdateTime) {
        mBufferingPercentUpdateTime = bufferingPercentUpdateTime;
    }

    public boolean isStalled() {
        return mStalled;
    }

    public void setStalled(boolean stalled) {
        mStalled = stalled;
    }

    /**
     * 获取错误码。如果没有发生任何错误，则返回 {@link Player.Error#NO_ERROR}。
     *
     * @see Player.Error
     */
    public int getErrorCode() {
        return mErrorCode;
    }

    /**
     * 设置错误码。
     *
     * @param errorCode 错误码。预定义的错误码：{@link Player.Error#NO_ERROR},
     *                  {@link Player.Error#ONLY_WIFI_NETWORK},
     *                  {@link Player.Error#PLAYER_ERROR},
     *                  {@link Player.Error#NETWORK_UNAVAILABLE}
     * @see Player.Error
     */
    public void setErrorCode(int errorCode) {
        mErrorCode = errorCode;
    }

    /**
     * 获取错误信息。
     */
    public String getErrorMessage() {
        return mErrorMessage;
    }

    /**
     * 设置错误信息（不能为 null）。
     *
     * @param errorMessage 错误信息（不能为 null）。
     * @see Player.Error#getErrorMessage(Context, int)
     */
    public void setErrorMessage(@NonNull String errorMessage) {
        Preconditions.checkNotNull(errorMessage);
        mErrorMessage = errorMessage;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PlayerState)) return false;

        PlayerState other = (PlayerState) o;

        return Objects.equal(mPlayProgress, other.mPlayProgress)
                && Objects.equal(mPlayProgressUpdateTime, other.mPlayProgressUpdateTime)
                && Objects.equal(mMusicItem, other.mMusicItem)
                && Objects.equal(mPosition, other.mPosition)
                && Objects.equal(mPlayMode, other.mPlayMode)
                && Objects.equal(mPlaybackState, other.mPlaybackState)
                && Objects.equal(mAudioSessionId, other.mAudioSessionId)
                && Objects.equal(mBufferingPercent, other.mBufferingPercent)
                && Objects.equal(mBufferingPercentUpdateTime, other.mBufferingPercentUpdateTime)
                && Objects.equal(mStalled, other.mStalled)
                && Objects.equal(mErrorCode, other.mErrorCode)
                && Objects.equal(mErrorMessage, other.mErrorMessage);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(mPlayProgress,
                mPlayProgressUpdateTime,
                mMusicItem,
                mPosition,
                mPlayMode,
                mPlaybackState,
                mAudioSessionId,
                mBufferingPercent,
                mBufferingPercentUpdateTime,
                mStalled,
                mErrorCode,
                mErrorMessage);
    }

    protected PlayerState(Parcel in) {
        mPlayProgress = in.readInt();
        mPlayProgressUpdateTime = in.readLong();
        mMusicItem = in.readParcelable(Thread.currentThread().getContextClassLoader());
        mPosition = in.readInt();
        mPlayMode = Player.PlayMode.values()[in.readInt()];

        mPlaybackState = Player.PlaybackState.values()[in.readInt()];
        mAudioSessionId = in.readInt();
        mBufferingPercent = in.readInt();
        mBufferingPercentUpdateTime = in.readLong();
        mStalled = in.readByte() != 0;
        mErrorCode = in.readInt();
        mErrorMessage = in.readString();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(mPlayProgress);
        dest.writeLong(mPlayProgressUpdateTime);
        dest.writeParcelable(mMusicItem, flags);
        dest.writeInt(mPosition);
        dest.writeInt(mPlayMode.ordinal());

        dest.writeInt(mPlaybackState.ordinal());
        dest.writeInt(mAudioSessionId);
        dest.writeInt(mBufferingPercent);
        dest.writeLong(mBufferingPercentUpdateTime);
        dest.writeByte((byte) (mStalled ? 1 : 0));
        dest.writeInt(mErrorCode);
        dest.writeString(mErrorMessage);
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
