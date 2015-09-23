package org.peercast.pecaport;

import org.fourthline.cling.UpnpServiceConfiguration;
import org.fourthline.cling.android.AndroidUpnpServiceConfiguration;
import org.fourthline.cling.android.AndroidUpnpServiceImpl;

/**
 * Registry更新周期 1秒 -> 5秒
 * @author (c) 2015, T Yoshizawa
 * Dual licensed under the MIT or GPL licenses.
 * */
public class UpnpInternalService extends AndroidUpnpServiceImpl {

    @Override
    protected UpnpServiceConfiguration createConfiguration() {
        return new AndroidUpnpServiceConfiguration() {
            @Override
            public int getRegistryMaintenanceIntervalMillis() {
                return 5000;
            }
        };
    }

}
