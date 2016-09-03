package org.peercast.core;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.support.annotation.Nullable;
import android.util.Log;

import org.peercast.pecaport.PecaPortService;

import java.util.ArrayList;
import java.util.List;

/**
 * (c) 2016, T Yoshizawa
 * Dual licensed under the MIT or GPL licenses.
 */

class ServiceReceiver {
    private static final String TAG = "ServiceReceiver";

    private final PeerCastService mService;
    private final List<BroadcastReceiver> mRegisteredReceivers = new ArrayList<>(2);

    ServiceReceiver(PeerCastService s) {
        mService = s;
    }

    /**
     * Wifi or イーサネットに接続されたことを受信し、PecaPortServiceを呼び出す。
     */
    private class ConnectivityReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context c, Intent intent) {
            int port = mService.getRunningPort();
            ConnectivityManager manager = (ConnectivityManager) c.getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo info = manager.getActiveNetworkInfo();
            if (port > 0 && isWifiOrEthernetConnected(info)) {
                Log.i(TAG, "Connectivity changed: " + info);
                Intent portIntent = new Intent(c, PecaPortService.class);
                portIntent.putExtra("open", port);
                c.startService(portIntent);
            }
        }

        boolean isWifiOrEthernetConnected(@Nullable NetworkInfo info){
            if (info == null)
                return false;
            int type = info.getType();
            return info.isConnected() && (
                    type == ConnectivityManager.TYPE_WIFI ||
                    type == ConnectivityManager.TYPE_ETHERNET
            );
        }
    }

    private void registerReceiver(BroadcastReceiver receiver, String action) {
        mService.registerReceiver(receiver, new IntentFilter(action));
        mRegisteredReceivers.add(receiver);
    }


    /**
     * Wifi接続になったとき、ポートを開く。
     */
    void registerConnectivityReceiver() {
        registerReceiver(
                new ConnectivityReceiver(),
                ConnectivityManager.CONNECTIVITY_ACTION);
    }

    /**
     * 視聴せずリレーをキープしているものは切断。
     */
    private class ScreenOffReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            List<Channel> channels = Channel.fromNativeResult(mService.nativeGetChannels());
            for (Channel ch : channels) {
                if (ch.getLocalListeners() == 0 && ch.getLocalRelays() > 0) {
                    for (Servent svt : ch.getServents()) {
                        mService.nativeDisconnectServent(svt.getServent_ID());
                    }
                }
            }
        }
    }

    /**
     * スクリーン消灯した時、無駄なリレーを切断する。
     * */
    void registerScreenOffReceiver() {
        registerReceiver(
                new ScreenOffReceiver(),
                Intent.ACTION_SCREEN_OFF
        );
    }

    void unregisterAll() {
        for (BroadcastReceiver br : mRegisteredReceivers)
            mService.unregisterReceiver(br);
    }

}
