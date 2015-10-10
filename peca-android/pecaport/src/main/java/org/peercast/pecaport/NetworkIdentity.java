package org.peercast.pecaport;

import android.content.Context;
import android.support.annotation.NonNull;

import org.fourthline.cling.model.meta.RemoteService;

/**
 *
 * 現在の接続(Wifi等)と、ルーターのUDN、接続名でIDを作成する。
 * @author (c) 2015, T Yoshizawa
 * Dual licensed under the MIT or GPL licenses.
 */
public class NetworkIdentity {

    private static final String TAG = "NetworkIdentity";
    private final String mIdentity;

    public NetworkIdentity(@NonNull Context c, @NonNull NetworkInterfaceInfo nicInfo, @NonNull RemoteService service) {
        String nicName = nicInfo.getDisplayName(c);
        String routerUdn = service.getDevice().getIdentity().getUdn().getIdentifierString();
        String serviceId = service.getServiceId().getId();
        mIdentity = c.getString(R.string.t_fmt_network_identity,
           nicName, routerUdn, serviceId
        );
    }

    NetworkIdentity(@NonNull String identity)  {
        mIdentity = identity;
    }


    @Override
    public int hashCode() {
        return mIdentity.hashCode();
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass())
            return false;
        return mIdentity.equals(((NetworkIdentity) o).mIdentity);
    }

    @Override
    public String toString() {
        return mIdentity;
    }
}
