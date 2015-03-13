package org.peercast.core;
/**
 * (c) 2013, T Yoshizawa
 *
 * Dual licensed under the MIT or GPL licenses.
 */

import android.os.Bundle;

public class Stats {

    private final Bundle mBundle;

    private Stats(Bundle b) {
        if (b == null)
            throw new NullPointerException();
        mBundle = b;
    }

    public int getInBytes() {
        return mBundle.getInt("in_bytes");
    }

    public int getOutBytes() {
        return mBundle.getInt("out_bytes");
    }

    public long getInTotalBytes() {
        return mBundle.getLong("in_total_bytes");
    }

    public long getOutTotalBytes() {
        return mBundle.getLong("out_total_bytes");
    }

    /**
     * @see PeerCastService#nativeGetStats()
     */
    public static Stats fromNativeResult(Bundle b) {
        return new Stats(b);
    }

    @Override
    public String toString() {
        String s = getClass().getSimpleName() + ": [";
        for (String k : mBundle.keySet()) {
            s += k + "=" + mBundle.get(k) + ", ";
        }
        return s + "]";
    }


}
