package org.peercast.core;
/**
 * (c) 2013, T Yoshizawa
 *
 * Dual licensed under the MIT or GPL licenses.
 */

import android.os.Bundle;
import android.support.annotation.NonNull;

import org.apache.commons.lang3.builder.ToStringBuilder;

public class Servent {

    private final Bundle mBundle;

    /**
     * @see Channel#getServents()
     */
    Servent(@NonNull Bundle b) {
        mBundle = b;
    }

    public int getServent_ID() {
        return mBundle.getInt("servent_id");
    }

    public boolean isRelay() {
        return mBundle.getBoolean("relay");
    }

    public boolean isFirewalled() {
        return mBundle.getBoolean("firewalled");
    }

    public boolean isSetInfoFlg() {
        return mBundle.getBoolean("infoFlg");
    }


    public int getNumRelays() {
        return mBundle.getInt("numRelays");
    }

    public String getHost() {
        return mBundle.getString("host");
    }

    public int getPort() {
        return mBundle.getInt("port");
    }

    public int getTotalListeners() {
        return mBundle.getInt("totalListeners");
    }

    public int getTotalRelays() {
        return mBundle.getInt("totalRelays");
    }

    @Override
    public String toString() {
        ToStringBuilder sb = new ToStringBuilder(this);
        for (String k : mBundle.keySet()) {
            sb.append(k, mBundle.get(k));
        }
        return sb.toString();
    }

    public String getVersion() {
        return mBundle.getString("version");
    }


}
