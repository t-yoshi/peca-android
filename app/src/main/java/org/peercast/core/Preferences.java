package org.peercast.core;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

/**
 * (c) 2015, T Yoshizawa
 *  Dual licensed under the MIT or GPL licenses.
 */

public class Preferences {
    private final SharedPreferences mPreferences;

    private static final String KEY_UPNP_ENABLED = "key_upnp_enabled";
    private static final String KEY_UPNP_CLOSE_ON_EXIT = "key_upnp_close_on_exit";

    private Preferences(Context c){
        mPreferences = PreferenceManager.getDefaultSharedPreferences(c);
    }

    static Preferences from(Context c){
        return new Preferences(c);
    }

    public boolean isUpnpEnabled(){
        return mPreferences.getBoolean(KEY_UPNP_ENABLED, false);
    }

    public boolean isUpnpCloseOnExit(){
        return mPreferences.getBoolean(KEY_UPNP_CLOSE_ON_EXIT, false);
    }


    public void putUpnpEnabled(boolean b){
        mPreferences.edit().putBoolean(KEY_UPNP_ENABLED, b).apply();
    }

    public void putUpnpCloseOnExit(boolean b){
        mPreferences.edit().putBoolean(KEY_UPNP_CLOSE_ON_EXIT, b).apply();
    }

}
