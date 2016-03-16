package org.peercast.pecaport;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import org.fourthline.cling.controlpoint.ControlPoint;
import org.fourthline.cling.model.action.ActionInvocation;
import org.fourthline.cling.model.message.UpnpResponse;
import org.fourthline.cling.model.meta.RemoteService;
import org.fourthline.cling.support.igd.callback.GetExternalIP;
import org.fourthline.cling.support.igd.callback.GetStatusInfo;
import org.fourthline.cling.support.model.Connection;
import org.fourthline.cling.support.model.PortMapping;
import org.peercast.pecaport.action.GetGenericPortMappingEntry;
import org.peercast.pecaport.util.LazyTask;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.TreeMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Logger;

/**
 * 　WANPPPConnectionの状態
 * @author (c) 2015, T Yoshizawa
 * Dual licensed under the MIT or GPL licenses.
 */
public class WanConnection  {
    private static final String TAG = "WanConnection";
    private static final int EXECUTE_ACTION_THREADS = 2;
    private final Logger logger = Logger.getLogger(getClass().getName());

    private final RemoteService mService;
    private String mExternalIp = null;
    private Connection.Status mStatus = Connection.Status.Disconnected;
    private Map<Integer, PortMapping> mMappings =
            Collections.synchronizedMap(new TreeMap<Integer, PortMapping>());

    private final PortManipulator mPortManipulator;

    /**
     * 各種Actionを同期で実行し、プロパティをセットする。
     */
    WanConnection(@NonNull ControlPoint controlPoint,
                  @NonNull RemoteService service) throws RuntimeException {
        mService = service;
        mPortManipulator = new PortManipulator(controlPoint, mService);
        executeActionsSync(controlPoint);
    }

    public RemoteService getService() {
        return mService;
    }

    @Nullable
    public String getExternalIp() {
        return mExternalIp;
    }


    /**
     * ルーターに対して以下の順にActionを実行する。(スレッド=2)
     * <ol>
     * <li>GetExternalIP</li>
     * <li>GetStatusInfo</li>
     * <li>GetGenericPortMappingEntry(index=0)<br>
     * (取得に失敗しない限り・・)</li>
     * <li>GetGenericPortMappingEntry(index=16)まで</li>
     * </ol>
     */
    private void executeActionsSync(ControlPoint controlPoint) {
        Queue<LazyTask<Void>> taskQueue = new LinkedList<>();
        final AtomicBoolean isBreak = new AtomicBoolean();

        taskQueue.add(LazyTask.wrapExecute(controlPoint, new GetExternalIP(mService) {
            @Override
            protected void success(String externalIPAddress) {
                logger.finest("GetExternalIP Success: " + externalIPAddress);
                mExternalIp = externalIPAddress;
            }

            @Override
            public void failure(ActionInvocation invocation, UpnpResponse operation, String defaultMsg) {
                logger.severe("GetExternalIP Failure: " + defaultMsg);
            }
        }));

        taskQueue.add(LazyTask.wrapExecute(controlPoint, new GetStatusInfo(mService) {
            @Override
            protected void success(Connection.StatusInfo statusInfo) {
                logger.finest("GetStatusInfo Success: " + statusInfo.getStatus().name());
                mStatus = statusInfo.getStatus();
            }

            @Override
            public void failure(ActionInvocation invocation, UpnpResponse operation, String defaultMsg) {
                logger.severe("GetStatusInfo Failure: " + defaultMsg);
            }
        }));

        if (mService.getAction("GetGenericPortMappingEntry") != null) {
            for (int i = 0; i < 16; i++) {
                final int index = i;

                taskQueue.add(LazyTask.wrapExecute(controlPoint, new GetGenericPortMappingEntry(mService, index) {
                            @Override
                            protected void success(PortMapping portMapping) {
                                logger.finest("Success GetGenericPortMappingEntry: index=" + index + ", " + portMapping);
                                mMappings.put(index, portMapping);
                            }

                            @Override
                            public void failure(ActionInvocation invocation, UpnpResponse operation, String defaultMsg) {
                                //invocation.getFailure().printStackTrace();
                                logger.finest("Fail GetGenericPortMappingEntry: index=" + index + ", " + defaultMsg);
                                isBreak.set(true);
                            }
                        }
                ));
            }
        }

        while (!taskQueue.isEmpty() && !isBreak.get()) {
            for (Future<?> f : LazyTask.lazyExecute(EXECUTE_ACTION_THREADS, taskQueue)) {
                try {
                    f.get();
                } catch (ExecutionException e) {
                    throw new RuntimeException(e.getCause());
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        }
        //同期にもどる
    }


    public Connection.Status getStatus() {
        return mStatus;
    }


    public List<PortMapping> getMappingEntries() {
        return new ArrayList<>(mMappings.values());
    }


    public PortManipulator getManipulator() {
        return mPortManipulator;
    }

    /**serviceId名で比較する*/
    public static final Comparator<WanConnection> CMP_SERVICE_ID = new Comparator<WanConnection>() {
        @Override
        public int compare(WanConnection lhs, WanConnection rhs) {
            return lhs.getService().getServiceId().toString().compareTo(
                    rhs.getService().getServiceId().toString());
        }
    };


}
