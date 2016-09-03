package org.peercast.core;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;

/**
 * (c) 2015, T Yoshizawa
 *  Dual licensed under the MIT or GPL licenses.
 */

public class AppPreferences {
    private static final String KEY_UPNP_ENABLED = "key_upnp_enabled";
    private static final String KEY_UPNP_CLOSE_ON_EXIT = "key_upnp_close_on_exit";
    private static final String KEY_POWER_SAVE_MODE = "key_power_save_mode";

    private final SharedPreferences mPreferences;

    private AppPreferences(Context c){
        mPreferences = PreferenceManager.getDefaultSharedPreferences(c);
    }

    static AppPreferences from(@NonNull Context c){
        return new AppPreferences(c);
    }

    public boolean isUPnPEnabled(){
        return mPreferences.getBoolean(KEY_UPNP_ENABLED, false);
    }

    public boolean isUPnPCloseOnExit(){
        return mPreferences.getBoolean(KEY_UPNP_CLOSE_ON_EXIT, false);
    }

    public void putUPnPEnabled(boolean b){
        mPreferences.edit().putBoolean(KEY_UPNP_ENABLED, b).apply();
    }

    public void putUPnPCloseOnExit(boolean b){
        mPreferences.edit().putBoolean(KEY_UPNP_CLOSE_ON_EXIT, b).apply();
    }

    public void putPowerSaveMode(boolean b){
        mPreferences.edit().putBoolean(KEY_POWER_SAVE_MODE, b).apply();
    }

    public boolean isPowerSaveMode(){
        return mPreferences.getBoolean(KEY_POWER_SAVE_MODE, true);
    }

}
