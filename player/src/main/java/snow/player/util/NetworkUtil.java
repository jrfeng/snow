package snow.player.util;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;
import android.net.NetworkRequest;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;

import com.google.common.base.Preconditions;

import java.lang.ref.WeakReference;

@SuppressWarnings("deprecation")
public class NetworkUtil {
    private Context mApplicationContext;
    private WeakReference<OnNetworkStateChangeListener> mListenerWeakReference;

    private ConnectivityManager mConnectivityManager;
    private BroadcastReceiver mNetworkStateReceiver;
    private ConnectivityManager.NetworkCallback mNetworkCallback;

    private NetworkUtil(Context context, OnNetworkStateChangeListener listener) {
        mApplicationContext = context;
        mListenerWeakReference = new WeakReference<>(listener);

        mConnectivityManager = (ConnectivityManager) mApplicationContext.getSystemService(Context.CONNECTIVITY_SERVICE);

        mNetworkStateReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                OnNetworkStateChangeListener ls = mListenerWeakReference.get();
                if (ls == null) {
                    return;
                }

                ls.onNetworkStateChanged(networkAvailable(), isWifiNetwork());
            }
        };

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            mNetworkCallback = new ConnectivityManager.NetworkCallback() {
                @Override
                public void onLosing(@NonNull Network network, int maxMsToLive) {
                    OnNetworkStateChangeListener ls = mListenerWeakReference.get();
                    if (ls == null) {
                        return;
                    }

                    ls.onNetworkStateChanged(networkAvailable(), false);
                }

                @Override
                public void onAvailable(@NonNull Network network) {
                    OnNetworkStateChangeListener ls = mListenerWeakReference.get();
                    if (ls == null) {
                        return;
                    }

                    ls.onNetworkStateChanged(networkAvailable(), true);
                }
            };
        }
    }

    public static NetworkUtil newInstance(@NonNull Context context, @NonNull OnNetworkStateChangeListener listener) {
        Preconditions.checkNotNull(context);
        Preconditions.checkNotNull(listener);

        return new NetworkUtil(context, listener);
    }

    public boolean networkAvailable() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            return hasNetworkApi28();
        }

        return mConnectivityManager.getActiveNetworkInfo() != null;
    }

    @RequiresApi(Build.VERSION_CODES.P)
    private boolean hasNetworkApi28() {
        return mConnectivityManager.getActiveNetwork() != null;
    }

    public boolean isWifiNetwork() {
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.P) {
            return isWifiNetworkApi28();
        }

        NetworkInfo networkInfo = mConnectivityManager.getActiveNetworkInfo();
        if (networkInfo == null) {
            return false;
        }

        return networkInfo.getType() == ConnectivityManager.TYPE_WIFI;
    }

    @RequiresApi(Build.VERSION_CODES.P)
    private boolean isWifiNetworkApi28() {
        Network network = mConnectivityManager.getActiveNetwork();
        if (network == null) {
            return false;
        }

        NetworkCapabilities capabilities = mConnectivityManager.getNetworkCapabilities(network);
        if (capabilities == null) {
            return false;
        }

        return capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI);
    }

    @RequiresApi(Build.VERSION_CODES.P)
    private void subscribeNetworkStateApi28() {
        mConnectivityManager.registerNetworkCallback(
                new NetworkRequest.Builder()
                        .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
                        .build(),
                mNetworkCallback);
    }

    @RequiresApi(Build.VERSION_CODES.P)
    private void unsubscribeNetworkStateApi28() {
        mConnectivityManager.unregisterNetworkCallback(mNetworkCallback);
    }

    public void subscribeNetworkState() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            subscribeNetworkStateApi28();
            return;
        }

        IntentFilter intentFilter = new IntentFilter(ConnectivityManager.EXTRA_NO_CONNECTIVITY);
        intentFilter.addCategory(Intent.CATEGORY_DEFAULT);
        mApplicationContext.registerReceiver(mNetworkStateReceiver, intentFilter);
    }

    public void unsubscribeNetworkState() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            unsubscribeNetworkStateApi28();
            return;
        }

        mApplicationContext.unregisterReceiver(mNetworkStateReceiver);
    }

    public interface OnNetworkStateChangeListener {
        void onNetworkStateChanged(boolean connected, boolean wifiNetwork);
    }
}
