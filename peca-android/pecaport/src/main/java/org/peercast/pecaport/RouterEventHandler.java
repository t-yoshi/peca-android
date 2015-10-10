package org.peercast.pecaport;

import org.fourthline.cling.model.meta.RemoteDevice;

import java.util.Collection;
import java.util.Collections;
import java.util.logging.Logger;

/**
 * @author (c) 2015, T Yoshizawa
 * Dual licensed under the MIT or GPL licenses.
 * */
public class RouterEventHandler implements RouterDiscoverer.OnEventListener {

    private static final String TAG = "RouterEventHandler";
    private final Logger logger = Logger.getLogger(getClass().getName());

    private final PecaPortFragmentBase.ViewBinder mViewBinder;

    RouterEventHandler(PecaPortFragmentBase.ViewBinder viewBinder) {
        mViewBinder = viewBinder;
    }

    @Override
    public void onInternetGatewayDeviceAdded(RemoteDevice routerDevice) {
        logger.info(mViewBinder.getContext().getString(R.string.t_log_router_found) + routerDevice);

        //AndroidRouter localRouter = (AndroidRouter)service.getRouter();
        NetworkDeviceManager manager = NetworkDeviceManager.from(mViewBinder.getContext());
        mViewBinder.setNetworkInterfaceInfo(manager.getActiveInterface());

        mViewBinder.setWanConnections(Collections.<WanConnection>emptyList());

        mViewBinder.updateTextView(R.id.vRouterName, routerDevice.getDetails()
                .getModelDetails()
                .getModelName());

        mViewBinder.updateTextView(R.id.vRouterManufacturer, routerDevice.getDetails()
                .getManufacturerDetails()
                .getManufacturer());

    }

    @Override
    public void onWANPPPConnections(RemoteDevice device, Collection<WanConnection> wanConnections) {
        //Log.d(TAG, "" + Arrays.asList(wanConnection.getService().getActions()));
        mViewBinder.setWanConnections(wanConnections);
    }

    @Override
    public void onInternetGatewayDeviceRemoved(RemoteDevice device) {

    }


}
