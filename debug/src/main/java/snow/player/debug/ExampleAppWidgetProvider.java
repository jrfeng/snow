package snow.player.debug;

import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.text.TextUtils;
import android.widget.RemoteViews;

import snow.player.appwidget.AppWidgetPlayerState;
import snow.player.audio.MusicItem;

public class ExampleAppWidgetProvider extends AppWidgetProvider {

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        super.onUpdate(context, appWidgetManager, appWidgetIds);

        for (int appWidgetId : appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId);
        }
    }

    private void updateAppWidget(Context context, AppWidgetManager appWidgetManager, int appWidgetId) {
        RemoteViews remoteViews = new RemoteViews(context.getPackageName(), R.layout.example_appwidget);

        AppWidgetPlayerState state = AppWidgetPlayerState.getPlayerState(context, MyPlayerService.class);
        if (state == null) {
            return;
        }

        MusicItem musicItem = state.getPlayingMusicItem();
        if (musicItem == null) {
            return;
        }

        String title = musicItem.getTitle();
        if (TextUtils.isEmpty(title)) {
            title = "未知歌曲名";
        }

        remoteViews.setTextViewText(R.id.tv_song_name, title);
        remoteViews.setTextViewText(R.id.tv_playback_state, state.getPlaybackState().toString());

        appWidgetManager.updateAppWidget(appWidgetId, remoteViews);
    }
}
