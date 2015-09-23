package org.peercast.pecaport;

import android.app.Service;
import android.content.Intent;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.widget.Toast;

import org.apache.commons.lang3.StringUtils;
import org.fourthline.cling.model.meta.RemoteDevice;
import org.fourthline.cling.model.meta.RemoteService;
import org.fourthline.cling.support.model.Connection;
import org.fourthline.cling.support.model.PortMapping;

import java.util.Collection;
import java.util.Queue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.Logger;


/**
 * PeerCastの開始、終了時にポートを操作する。
 * @author (c) 2015, T Yoshizawa
 * Dual licensed under the MIT or GPL licenses.
 */
public class PecaPortService extends Service implements
        RouterDiscoverer.OnEventListener,
        PortManipulator.OnResultListener {

    private static final String TAG = "PecaPortService";
    private final Logger logger = Logger.getLogger(getClass().getName());


    private ServiceHandler mServiceHandler;
    private Handler mUiThreadHandler;

    private RouterDiscoverer mRouterDiscoverer;
    private NetworkInterfaceInfo mActiveNicInfo;
    private Queue<PecaPortServiceTask> mTasks = new LinkedBlockingQueue<>();


    public static final String DESCRIPTION =
            String.format("PeerCast(%s)", StringUtils.substring(Build.MODEL, 0, 16));


    private static final int MSG_STOP_SELF = 0xD1E;

    private final class ServiceHandler extends Handler {
        public ServiceHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            if (msg.what == MSG_STOP_SELF) {
                stopSelf();
                return;
            }
            onHandleIntent((Intent) msg.obj);
        }
    }


    @Override
    public void onCreate() {
        super.onCreate();
        logger.info(getString(R.string.t_log_starting) + "PecaPortService");

        HandlerThread thread = new HandlerThread(TAG);
        thread.start();

        mServiceHandler = new ServiceHandler(thread.getLooper());
        mUiThreadHandler = new Handler(Looper.getMainLooper());
        mRouterDiscoverer = new RouterDiscoverer(getApplicationContext(), this);

        NetworkDeviceManager nicManager = NetworkDeviceManager.from(this);
        mActiveNicInfo = nicManager.getActiveInterface();
    }

    private void onHandleIntent(Intent intent) {
        if (mActiveNicInfo == null) {
            logger.severe("No Active Network.");
            mServiceHandler.sendEmptyMessage(MSG_STOP_SELF);
            return;
        }
        String clientIp = mActiveNicInfo.getPrivateAddress().getHostAddress();
        if (intent.hasExtra("open")) {
            int port = intent.getIntExtra("open", 0);
            Log.i(TAG, "open=" + port);
            mTasks.add(new PecaPortServiceTask.DeleteOnDifferentClient(clientIp, port));
            mTasks.add(new PecaPortServiceTask.Add(clientIp, port));
        } else {
            int port = intent.getIntExtra("close", 0);
            Log.i(TAG, "close=" + port);
            mTasks.add(new PecaPortServiceTask.Delete(clientIp, port));
        }

        getApplicationContext().bindService(
                new Intent(this, UpnpInternalService.class),
                mRouterDiscoverer, BIND_AUTO_CREATE
        );

        //(次のインテントが来なければ)10秒後にstopSelf
        mServiceHandler.removeMessages(MSG_STOP_SELF);
        mServiceHandler.sendEmptyMessageDelayed(MSG_STOP_SELF, 10 * 1000);
    }


    @Override
    public void onStart(Intent intent, int startId) {
        if (intent == null)
            return;
        Message msg = mServiceHandler.obtainMessage();
        msg.arg1 = startId;
        msg.obj = intent;
        mServiceHandler.sendMessage(msg);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        onStart(intent, startId);
        return START_NOT_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        if (mRouterDiscoverer.isConnected()) {
            mRouterDiscoverer.onServiceDisconnected(null);
            getApplicationContext().unbindService(mRouterDiscoverer);
        }
        mServiceHandler.getLooper().quit();
        logger.info(getString(R.string.t_log_finished) + "PecaPortService");
    }


    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }


    @Override
    public void onInternetGatewayDeviceAdded(RemoteDevice device) {
        logger.info(getString(R.string.t_log_router_found) + device);
    }

    @Override
    public void onWANPPPConnections(RemoteDevice device, Collection<WanConnection> wanConnections) {
        for (WanConnection conn : wanConnections) {
            onWANConnection(conn);
        }
    }

    private void onWANConnection(WanConnection wanConnection) {
        String serviceId = wanConnection.getService().getServiceId().getId();

        if (wanConnection.getExternalIp() == null ||
                wanConnection.getStatus() != Connection.Status.Connected) {
            logger.finest("Disconnected: " + serviceId);
            return;
        }

        PecaPortPreferences pref = PecaPortPreferences.from(this);
        NetworkIdentity identity = new NetworkIdentity(this, mActiveNicInfo, wanConnection.getService());
        if (pref.getAllDisabledNetworks().contains(identity)) {
            logger.finest(String.format("Denied: serviceId=%s. ", serviceId));
            return;
        }

        while (!mTasks.isEmpty()) {
            PecaPortServiceTask task = mTasks.poll();

            if (!task.needExecute(wanConnection.getMappingEntries()))
                continue;

            logger.info(getString(R.string.t_log_fmt_try_mapping,
                    task.method, serviceId, task.port));

            Future f = task.execute(wanConnection.getManipulator(), this);
            try {
                f.get();
            } catch (InterruptedException e) {
                Log.w(TAG, "! Interrupted !", e);
            } catch (ExecutionException e) {
                throw new RuntimeException(e.getCause());
            }
        }
    }

    @Override
    public void onInternetGatewayDeviceRemoved(RemoteDevice device) {

    }

    @Override
    public void onSuccess(Method m, RemoteService service, PortMapping mapping) {
        String msg = getString(R.string.t_log_success) +
                String.format("\n%sPortMapping %s", m, mapping);
        logger.info(msg.replace("\n", ""));
        showToast(msg);
    }

    @Override
    public void onFailure(Method m, RemoteService service, PortMapping mapping, String errMsg) {
        String msg = getString(R.string.t_log_failed) +
                String.format("\n%sPortMapping %s %s", m, mapping, errMsg);
        logger.severe(msg.replace("\n", ""));
        showToast(msg);
    }


    private void showToast(final CharSequence text) {
        mUiThreadHandler.post(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(getApplicationContext(), text, Toast.LENGTH_SHORT).show();
            }
        });
    }

}
