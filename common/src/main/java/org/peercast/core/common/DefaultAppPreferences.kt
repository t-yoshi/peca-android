package org.peercast.core.common

import android.app.Application
import androidx.core.content.edit
import androidx.preference.PreferenceManager

internal class DefaultAppPreferences(a: Application) : AppPreferences {

    private val prefs = PreferenceManager.getDefaultSharedPreferences(a)

    override var isUPnPEnabled: Boolean
        get() = prefs.getBoolean(KEY_UPNP_ENABLED, false)
        set(value) {
            prefs.edit {
                putBoolean(KEY_UPNP_ENABLED, value)
            }
        }

    companion object {
        private const val KEY_UPNP_ENABLED = "key_upnp_enabled"
    }

}

