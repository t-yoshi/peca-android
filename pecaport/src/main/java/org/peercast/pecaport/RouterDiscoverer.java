package org.peercast.pecaport;

import android.content.ComponentName;
import android.content.Context;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.util.Log;

import org.fourthline.cling.android.AndroidUpnpService;
import org.fourthline.cling.model.message.header.RootDeviceHeader;
import org.fourthline.cling.model.meta.LocalDevice;
import org.fourthline.cling.model.meta.RemoteDevice;
import org.fourthline.cling.model.meta.RemoteService;
import org.fourthline.cling.model.types.UDADeviceType;
import org.fourthline.cling.model.types.UDAServiceType;
import org.fourthline.cling.registry.DefaultRegistryListener;
import org.fourthline.cling.registry.Registry;
import org.fourthline.cling.registry.RegistryListener;

import java.util.Collection;
import java.util.TreeSet;
import java.util.logging.Logger;

/**
 * @author (c) 2015, T Yoshizawa
 *         Dual licensed under the MIT or GPL licenses.
 */
public class RouterDiscoverer implements ServiceConnection {
    private static final String TAG = "RouterDiscoverer";
    private static final UDADeviceType InternetGatewayDevice = new UDADeviceType("InternetGatewayDevice");

    private final Logger logger = Logger.getLogger(getClass().getName());
    private AndroidUpnpService mUpnpService;
    private OnEventListener mEventObserver;

    private final RegistryListener mRegistryListener = new DefaultRegistryListener() {
        @Override
        public void remoteDeviceDiscoveryStarted(Registry registry, RemoteDevice device) {
            logger.fine("remoteDeviceDiscoveryStarted: " + registry + "," + device);
        }

        @Override
        synchronized public void remoteDeviceAdded(Registry registry, RemoteDevice device) {
            logger.fine("remoteDeviceAdded: " + registry + "," + device);

            if (device.isRoot() && device.getType().equals(InternetGatewayDevice)) {
                mEventObserver.onInternetGatewayDeviceAdded(device);

                RemoteService[] services = device.findServices(new UDAServiceType("WANPPPConnection"));
                if (services.length == 0)
                    services = device.findServices(new UDAServiceType("WANIPConnection"));

                Collection<WanConnection> wanConnections = new TreeSet<>(WanConnection.CMP_SERVICE_ID);
                for (RemoteService service : services) {
                    WanConnection connection = new WanConnection(mUpnpService.getControlPoint(), service);
                    wanConnections.add(connection);
                }
                mEventObserver.onWANPPPConnections(device, wanConnections);
            }
        }


        @Override
        synchronized public void remoteDeviceRemoved(Registry registry, RemoteDevice device) {
            logger.fine("remoteDeviceRemoved: " + registry + "," + device);
            if (device.isRoot() && device.getType().equals(InternetGatewayDevice)) {
                mEventObserver.onInternetGatewayDeviceRemoved(device);
            }
        }

        @Override
        public void localDeviceAdded(Registry registry, LocalDevice device) {
            logger.fine("localDeviceAdded: " + registry + "," + device);
        }

        @Override
        public void localDeviceRemoved(Registry registry, LocalDevice device) {
            logger.fine("localDeviceRemoved: " + registry + "," + device);
        }

    };

    public RouterDiscoverer(Context c, OnEventListener observer) {
        mEventObserver = observer;
    }

    @Override
    public void onServiceConnected(ComponentName className, IBinder service) {
        mUpnpService = (AndroidUpnpService) service;
        logger.fine("onServiceConnected: " + mUpnpService);

        Registry registry = mUpnpService.getRegistry();
        registry.removeAllRemoteDevices();
        registry.addListener(mRegistryListener);

        mUpnpService.getControlPoint()
                .search(new RootDeviceHeader(), 3);
    }

    /**
     * サービスがOSにKillされた場合にだけ呼ばれる。<br>
     * NOTE: メモリリークを防ぐためにunbindService()の前に手動で呼ぶこと。
     */
    @Override
    public void onServiceDisconnected(ComponentName unused) {
        synchronized (mRegistryListener) {
            if (mUpnpService == null)
                return;
            logger.fine("onServiceDisconnected()");
            mUpnpService.getRegistry().removeListener(mRegistryListener);
            mUpnpService = null;
        }
    }

    public boolean isConnected() {
        return mUpnpService != null;
    }

    public void research() {
        if (!isConnected()) {
            Log.e(TAG, "not connected");
            return;
        }
        mUpnpService.getRegistry().removeAllRemoteDevices();
        mUpnpService.getControlPoint()
                .search(new RootDeviceHeader());
    }

    public interface OnEventListener {
        /**
         * ルーターが見つかったとき呼ばれる。
         */
        void onInternetGatewayDeviceAdded(RemoteDevice device);

        /**
         * WANPPPConnectionまたはWANIPConnectionが見つかったとき呼ばれる。
         **/
        void onWANPPPConnections(RemoteDevice device, Collection<WanConnection> wanConnections);

        /**
         *
         */
        void onInternetGatewayDeviceRemoved(RemoteDevice device);

    }

}
