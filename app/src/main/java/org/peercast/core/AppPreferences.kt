package org.peercast.core

import android.content.Context
import android.preference.PreferenceManager

/**
 * (c) 2015, T Yoshizawa
 * Dual licensed under the MIT or GPL licenses.
 */

interface AppPreferences {
    var isUPnPEnabled: Boolean
    var isUPnPCloseOnExit: Boolean
    var isPowerSaveMode: Boolean
}

class DefaultAppPreferences private constructor(c: Context) : AppPreferences {

    private val p = PreferenceManager.getDefaultSharedPreferences(c)

    override var isUPnPEnabled: Boolean
        get() = p.getBoolean(KEY_UPNP_ENABLED, false)
        set(value) {
            p.edit().putBoolean(KEY_UPNP_ENABLED, value).apply()
        }

    override var isUPnPCloseOnExit: Boolean
        get() = p.getBoolean(KEY_UPNP_CLOSE_ON_EXIT, false)
        set(value) {
            p.edit().putBoolean(KEY_UPNP_CLOSE_ON_EXIT, value).apply()
        }

    override var isPowerSaveMode: Boolean
        get() = p.getBoolean(KEY_POWER_SAVE_MODE, true)
        set(value) {
            p.edit().putBoolean(KEY_POWER_SAVE_MODE, value).apply()
        }

    companion object {
        private const val KEY_UPNP_ENABLED = "key_upnp_enabled"
        private const val KEY_UPNP_CLOSE_ON_EXIT = "key_upnp_close_on_exit"
        private const val KEY_POWER_SAVE_MODE = "key_power_save_mode"

        fun from(c: Context): AppPreferences {
            return DefaultAppPreferences(c)
        }
    }

}
