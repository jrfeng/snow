package snow.player.debug;

import android.content.Context;
import android.net.Uri;

import androidx.annotation.NonNull;

import com.google.android.exoplayer2.ext.okhttp.OkHttpDataSourceFactory;
import com.google.android.exoplayer2.source.ProgressiveMediaSource;
import com.google.android.exoplayer2.source.hls.HlsMediaSource;
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory;
import com.google.android.exoplayer2.util.Util;

import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import snow.player.PlayerService;
import snow.player.exo.ExoMusicPlayer;
import snow.player.exo.util.OkHttpUtil;
import snow.player.audio.MusicItem;
import snow.player.audio.MusicPlayer;

public class MyPlayerService extends PlayerService {
    private ProgressiveMediaSource.Factory mProgressiveMediaSourceFactory;
    private HlsMediaSource.Factory mHlsMediaSourceFactory;

    @Override
    public void onCreate() {
        super.onCreate();

        setMaxIDLETime(10);
        setIgnoreAudioFocus(false);

        OkHttpClient.Builder builder = new OkHttpClient.Builder()
                .followRedirects(true)
                .followSslRedirects(true)
                .retryOnConnectionFailure(true)
                .connectTimeout(10, TimeUnit.SECONDS)
                .writeTimeout(20, TimeUnit.SECONDS)
                .readTimeout(20, TimeUnit.SECONDS);

        OkHttpUtil.enableTls12OnPreLollipop(builder, true);

        OkHttpDataSourceFactory httpDataSourceFactory =
                new OkHttpDataSourceFactory(builder.build(), Util.getUserAgent(this, this.getPackageName()));

        DefaultDataSourceFactory dataSourceFactory = new DefaultDataSourceFactory(
                this, httpDataSourceFactory);

        mProgressiveMediaSourceFactory = new ProgressiveMediaSource.Factory(dataSourceFactory);
        mHlsMediaSourceFactory = new HlsMediaSource.Factory(dataSourceFactory);
    }

    @NonNull
    @Override
    protected MusicPlayer onCreateMusicPlayer(@NonNull Context context, @NonNull MusicItem musicItem, @NonNull Uri uri) {
        String path = uri.getLastPathSegment();

        if (path != null && path.endsWith(".m3u8")) {
            return new ExoMusicPlayer(context, mHlsMediaSourceFactory, uri);
        }

        return new ExoMusicPlayer(context, mProgressiveMediaSourceFactory, uri);
    }
}
