package snow.music.service;

import android.app.PendingIntent;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.core.content.res.ResourcesCompat;

import snow.music.R;
import snow.music.store.MusicStore;
import snow.music.util.FavoriteHelper;
import snow.music.util.MusicUtil;
import snow.player.HistoryRecorder;
import snow.player.PlayMode;
import snow.player.PlayerService;
import snow.player.annotation.PersistenceId;

@PersistenceId("AppPlayerService")
public class AppPlayerService extends PlayerService {
    private MusicStore mMusicStore;

    @Override
    public void onCreate() {
        super.onCreate();

        setMaxIDLETime(5);
        mMusicStore = MusicStore.getInstance();
    }

    @Nullable
    @Override
    protected NotificationView onCreateNotificationView() {
        return new AppNotificationView();
    }

    @Nullable
    @Override
    protected HistoryRecorder onCreateHistoryRecorder() {
        return musicItem -> mMusicStore.addHistory(MusicUtil.asMusic(musicItem));
    }

    private static class AppNotificationView extends MediaNotificationView {
        private static final String ACTION_TOGGLE_FAVORITE = "toggle_favorite";
        private static final String ACTION_SWITCH_PLAY_MODE = "switch_play_mode";

        private FavoriteHelper mFavoriteHelper;
        private PendingIntent mToggleFavorite;
        private PendingIntent mSwitchPlayMode;

        @Override
        protected void onInit(Context context) {
            super.onInit(context);

            mFavoriteHelper = new FavoriteHelper(favorite -> invalidate());

            mToggleFavorite = buildCustomAction(ACTION_TOGGLE_FAVORITE, (player, extras) ->
                    MusicStore.getInstance().toggleFavorite(MusicUtil.asMusic(getPlayingMusicItem())));

            mSwitchPlayMode = buildCustomAction(ACTION_SWITCH_PLAY_MODE, (player, extras) -> {
                switch (getPlayMode()) {
                    case SEQUENTIAL:
                        player.setPlayMode(PlayMode.LOOP);
                        break;
                    case LOOP:
                        player.setPlayMode(PlayMode.SHUFFLE);
                        break;
                    case SHUFFLE:
                        player.setPlayMode(PlayMode.SEQUENTIAL);
                        break;
                }
                invalidate();
            });
        }

        @Override
        protected void onRelease() {
            super.onRelease();
            mFavoriteHelper.release();
        }

        @Override
        protected void onBuildMediaStyle(androidx.media.app.NotificationCompat.MediaStyle mediaStyle) {
            mediaStyle.setShowActionsInCompactView(2, 3);
        }

        @Override
        protected void onBuildNotification(NotificationCompat.Builder builder) {
            addToggleFavorite(builder);

            // skip to previous, play pause, skip to next
            super.onBuildNotification(builder);

            addSwitchPlayMode(builder);
        }

        @NonNull
        @Override
        public Bitmap getDefaultIcon() {
            Context context = getContext();
            BitmapDrawable drawable = (BitmapDrawable) ResourcesCompat.getDrawable(
                    context.getResources(),
                    R.mipmap.ic_notif_default_icon,
                    context.getTheme());

            if (drawable == null) {
                throw new NullPointerException();
            }

            return drawable.getBitmap();
        }

        private void addToggleFavorite(NotificationCompat.Builder builder) {
            if (isExpire()) {
                mFavoriteHelper.setMusicItem(getPlayingMusicItem());
            }

            if (mFavoriteHelper.isFavorite()) {
                builder.addAction(R.drawable.ic_notif_favorite_true, "favorite", mToggleFavorite);
            } else {
                builder.addAction(R.drawable.ic_notif_favorite_false, "don't favorite", mToggleFavorite);
            }
        }

        private void addSwitchPlayMode(NotificationCompat.Builder builder) {
            switch (getPlayMode()) {
                case SEQUENTIAL:
                    builder.addAction(R.drawable.ic_notif_play_mode_sequential, "sequential", mSwitchPlayMode);
                    break;
                case LOOP:
                    builder.addAction(R.drawable.ic_notif_play_mode_loop, "sequential", mSwitchPlayMode);
                    break;
                case SHUFFLE:
                    builder.addAction(R.drawable.ic_notif_play_mode_shuffle, "sequential", mSwitchPlayMode);
                    break;
            }
        }
    }
}
