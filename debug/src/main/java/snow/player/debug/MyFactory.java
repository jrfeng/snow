package snow.player.debug;

import android.content.Context;
import android.net.Uri;

import androidx.annotation.NonNull;

import com.google.android.exoplayer2.source.ProgressiveMediaSource;
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory;
import com.google.android.exoplayer2.upstream.DefaultHttpDataSource;
import com.google.android.exoplayer2.upstream.DefaultHttpDataSourceFactory;
import com.google.android.exoplayer2.util.Util;

import snow.player.Inject;
import snow.player.PlayerService;
import snow.player.exo.ExoMusicPlayer;
import snow.player.media.MusicPlayer;

public class MyFactory extends PlayerService.ComponentFactory {
    private ProgressiveMediaSource.Factory mMediaSourceFactory;

    @Override
    public void init(Context context) {
        DefaultHttpDataSourceFactory httpDataSourceFactory =
                new DefaultHttpDataSourceFactory(
                        Util.getUserAgent(context, context.getPackageName()),
                        null,
                        DefaultHttpDataSource.DEFAULT_CONNECT_TIMEOUT_MILLIS,
                        DefaultHttpDataSource.DEFAULT_READ_TIMEOUT_MILLIS,
                        true);

        DefaultDataSourceFactory dataSourceFactory = new DefaultDataSourceFactory(
                context, httpDataSourceFactory);

        mMediaSourceFactory = new ProgressiveMediaSource.Factory(dataSourceFactory);
    }

    @Inject
    @NonNull
    @Override
    public MusicPlayer createMusicPlayer(@NonNull Context context, @NonNull Uri uri) {
        return new ExoMusicPlayer(context, mMediaSourceFactory, uri);
    }
}
