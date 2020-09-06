package snow.player.util;

import android.os.Build;
import android.util.Log;

import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import okhttp3.ConnectionSpec;
import okhttp3.OkHttpClient;
import okhttp3.TlsVersion;

/**
 * OkHttp 工具，用于在大于等于 API level 16 小于 API level 22 的 Android 版本中启用 TLS v1.2。
 * <p>
 * 要使用此工具，你的项目需要添加对 OKHttp 的依赖，本项目不对外提供 OkHttp 依赖。
 */
public final class OkHttpUtil {

    private OkHttpUtil() {
        throw new AssertionError();
    }

    public static void enableTls12OnPreLollipop(OkHttpClient.Builder builder) {
        enableTls12OnPreLollipop(builder, false);
    }

    /**
     * Warning! Trust all certificate is unsafe!
     */
    public static void enableTls12OnPreLollipop(OkHttpClient.Builder builder, boolean trustAllCertificate) {
        if (Build.VERSION.SDK_INT >= 16 && Build.VERSION.SDK_INT < 22) {
            try {
                SSLContext sc = SSLContext.getInstance("TLSv1.2");

                if (trustAllCertificate) {
                    TrustManager[] trustAllCerts = getUnsafeTrustManager();
                    sc.init(null, trustAllCerts, new java.security.SecureRandom());
                    builder.sslSocketFactory(new Tls12SocketFactory(sc.getSocketFactory()), (X509TrustManager) trustAllCerts[0]);
                } else {
                    sc.init(null, null, null);
                    builder.sslSocketFactory(new Tls12SocketFactory(sc.getSocketFactory()));
                }

                ConnectionSpec cs = new ConnectionSpec.Builder(ConnectionSpec.MODERN_TLS)
                        .tlsVersions(TlsVersion.TLS_1_2)
                        .build();

                List<ConnectionSpec> specs = new ArrayList<>();
                specs.add(cs);
                specs.add(ConnectionSpec.COMPATIBLE_TLS);
                specs.add(ConnectionSpec.CLEARTEXT);

                builder.connectionSpecs(specs);
            } catch (Exception exc) {
                Log.e("OkHttpUtil", "Error while setting TLS 1.2", exc);
            }
        }
    }

    /**
     * Warning! This is unsafe!
     */
    private static TrustManager[] getUnsafeTrustManager() {
        return new TrustManager[]{new X509TrustManager() {
            public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                return new java.security.cert.X509Certificate[]{};
            }

            public void checkClientTrusted(X509Certificate[] chain, String authType) {
            }

            public void checkServerTrusted(X509Certificate[] chain, String authType) {
            }
        }};
    }

    /**
     * Enables TLS v1.2 when creating SSLSockets.
     * <p/>
     * For some reason, android supports TLS v1.2 from API 16, but enables it by
     * default only from API 20.
     *
     * @link https://developer.android.com/reference/javax/net/ssl/SSLSocket.html
     * @see SSLSocketFactory
     */
    private static class Tls12SocketFactory extends SSLSocketFactory {
        private static final String[] TLS_V12_ONLY = {"TLSv1.2"};

        final SSLSocketFactory delegate;

        public Tls12SocketFactory(SSLSocketFactory base) {
            this.delegate = base;
        }

        @Override
        public String[] getDefaultCipherSuites() {
            return delegate.getDefaultCipherSuites();
        }

        @Override
        public String[] getSupportedCipherSuites() {
            return delegate.getSupportedCipherSuites();
        }

        @Override
        public Socket createSocket(Socket s, String host, int port, boolean autoClose) throws IOException {
            return patch(delegate.createSocket(s, host, port, autoClose));
        }

        @Override
        public Socket createSocket(String host, int port) throws IOException {
            return patch(delegate.createSocket(host, port));
        }

        @Override
        public Socket createSocket(String host, int port, InetAddress localHost, int localPort) throws IOException {
            return patch(delegate.createSocket(host, port, localHost, localPort));
        }

        @Override
        public Socket createSocket(InetAddress host, int port) throws IOException {
            return patch(delegate.createSocket(host, port));
        }

        @Override
        public Socket createSocket(InetAddress address, int port, InetAddress localAddress, int localPort) throws IOException {
            return patch(delegate.createSocket(address, port, localAddress, localPort));
        }

        private Socket patch(Socket s) {
            if (s instanceof SSLSocket) {
                ((SSLSocket) s).setEnabledProtocols(TLS_V12_ONLY);
            }
            return s;
        }
    }
}
