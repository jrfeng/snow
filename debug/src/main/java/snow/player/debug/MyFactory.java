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

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.SSLContext;

import okhttp3.ConnectionSpec;
import okhttp3.OkHttpClient;
import okhttp3.TlsVersion;
import snow.player.Inject;
import snow.player.PlayerService;
import snow.player.exo.ExoMusicPlayer;
import snow.player.media.MusicPlayer;

public class MyFactory extends PlayerService.ComponentFactory {
    private ProgressiveMediaSource.Factory mProgressiveMediaSourceFactory;
    private HlsMediaSource.Factory mHlsMediaSourceFactory;

    @Override
    public void init(Context context) {
        OkHttpClient client = getNewHttpClient();

        OkHttpDataSourceFactory httpDataSourceFactory =
                new OkHttpDataSourceFactory(client, Util.getUserAgent(context, context.getPackageName()));

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

    private OkHttpClient getNewHttpClient() {
        OkHttpClient.Builder client = new OkHttpClient.Builder()
                .followRedirects(true)
                .followSslRedirects(true)
                .retryOnConnectionFailure(true)
                .cache(null)
                .connectTimeout(10, TimeUnit.SECONDS)
                .writeTimeout(20, TimeUnit.SECONDS)
                .readTimeout(20, TimeUnit.SECONDS);

        return enableTls12OnPreLollipop(client).build();
    }

    public static OkHttpClient.Builder enableTls12OnPreLollipop(OkHttpClient.Builder client) {
        if (Build.VERSION.SDK_INT >= 16 && Build.VERSION.SDK_INT < 22) {
            try {
                SSLContext sc = SSLContext.getInstance("TLSv1.2");
                sc.init(null, null, null);
                client.sslSocketFactory(new Tls12SocketFactory(sc.getSocketFactory()));

                ConnectionSpec cs = new ConnectionSpec.Builder(ConnectionSpec.MODERN_TLS)
                        .tlsVersions(TlsVersion.TLS_1_2)
                        .build();

                List<ConnectionSpec> specs = new ArrayList<>();
                specs.add(cs);
                specs.add(ConnectionSpec.COMPATIBLE_TLS);
                specs.add(ConnectionSpec.CLEARTEXT);

                client.connectionSpecs(specs);
            } catch (Exception exc) {
                Log.e("OkHttpTLSCompat", "Error while setting TLS 1.2", exc);
            }
        }

        return client;
    }
}
