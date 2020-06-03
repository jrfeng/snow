package snow.player;

import android.os.Parcel;
import android.os.Parcelable;

import snow.player.playlist.PlaylistPlayer;

/**
 * 用于存储播放队列的状态。
 */
class PlaylistState extends PlayerState implements Parcelable {
    private int mPosition;
    private int mPlayMode;

    PlaylistState() {
        mPosition = 0;
        mPlayMode = PlaylistPlayer.PlayMode.SEQUENTIAL;
    }

    /**
     * 获取播放队列的播放位置。
     *
     * @return 播放队列的播放位置。
     */
    int getPosition() {
        return mPosition;
    }

    /**
     * 设置播放队列的播放位置。
     *
     * @param position 播放队列的播放位置（小于 0 时相当于设置为 0）。
     */
    void setPosition(int position) {
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
     * @see PlaylistPlayer.PlayMode
     */
    int getPlayMode() {
        return mPlayMode;
    }

    /**
     * 设置播放队列的播放模式。
     *
     * @param playMode 播放队列的播放模式。只能是这些值之一：{@link PlaylistPlayer.PlayMode#SEQUENTIAL},
     *                 {@link PlaylistPlayer.PlayMode#LOOP},
     *                 {@link PlaylistPlayer.PlayMode#SHUFFLE}
     * @see PlaylistPlayer.PlayMode
     */
    void setPlayMode(int playMode) {
        mPlayMode = playMode;
    }

    protected PlaylistState(Parcel in) {
        super(in);
        mPosition = in.readInt();
        mPlayMode = in.readInt();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        super.writeToParcel(dest, flags);
        dest.writeInt(mPosition);
        dest.writeInt(mPlayMode);
    }

    public static final Creator<PlaylistState> CREATOR = new Creator<PlaylistState>() {
        @Override
        public PlaylistState createFromParcel(Parcel in) {
            return new PlaylistState(in);
        }

        @Override
        public PlaylistState[] newArray(int size) {
            return new PlaylistState[size];
        }
    };
}
