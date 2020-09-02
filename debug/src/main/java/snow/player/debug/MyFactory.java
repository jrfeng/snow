package snow.player.debug;

import android.content.Context;
import android.net.Uri;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;

import com.google.android.exoplayer2.ext.okhttp.OkHttpDataSourceFactory;
import com.google.android.exoplayer2.source.ProgressiveMediaSource;
import com.google.android.exoplayer2.source.hls.HlsMediaSource;
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory;
import com.google.android.exoplayer2.util.Util;

import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import snow.player.Inject;
import snow.player.PlayerService;
import snow.player.exo.ExoMusicPlayer;
import snow.player.media.MusicPlayer;
import snow.player.util.OkHttpUtil;

public class MyFactory extends PlayerService.ComponentFactory {
    private ProgressiveMediaSource.Factory mProgressiveMediaSourceFactory;
    private HlsMediaSource.Factory mHlsMediaSourceFactory;

    @Override
    public void init(Context context) {
        OkHttpClient.Builder builder = new OkHttpClient.Builder()
                .followRedirects(true)
                .followSslRedirects(true)
                .retryOnConnectionFailure(true)
                .connectTimeout(10, TimeUnit.SECONDS)
                .writeTimeout(20, TimeUnit.SECONDS)
                .readTimeout(20, TimeUnit.SECONDS);

        OkHttpUtil.enableTls12OnPreLollipop(builder);

        OkHttpDataSourceFactory httpDataSourceFactory =
                new OkHttpDataSourceFactory(builder.build(), Util.getUserAgent(context, context.getPackageName()));

        DefaultDataSourceFactory dataSourceFactory = new DefaultDataSourceFactory(
                context, httpDataSourceFactory);

        mProgressiveMediaSourceFactory = new ProgressiveMediaSource.Factory(dataSourceFactory);
        mHlsMediaSourceFactory = new HlsMediaSource.Factory(dataSourceFactory);
    }

    @Inject
    @NonNull
    @Override
    public MusicPlayer createMusicPlayer(@NonNull Context context, @NonNull Uri uri) {
        String path = uri.getLastPathSegment();

        if (path != null && path.endsWith(".m3u8")) {
            return new ExoMusicPlayer(context, mHlsMediaSourceFactory, uri);
        }

        return new ExoMusicPlayer(context, mProgressiveMediaSourceFactory, uri);
    }
}
