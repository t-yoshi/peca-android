package org.peercast.pecaport

import android.content.Context
import android.preference.PreferenceManager
import androidx.core.content.edit
import org.fourthline.cling.model.ServiceReference

/**
 * @author (c) 2015, T Yoshizawa
 * Dual licensed under the MIT or GPL licenses.
 */

interface PecaPortPreferences {
    /**UPnPのデバッグモード */
    var isDebug: Boolean

    var selectedNetworkInterfaceName: String?
    var selectedWanServiceReference : ServiceReference?

}


class DefaultPecaPortPreferences(c: Context) : PecaPortPreferences {
    private val prefs = PreferenceManager.getDefaultSharedPreferences(c)!!

    /**UPnPのデバッグモード */
    override var isDebug: Boolean
        get() = prefs.getBoolean(PREF_DEBUG, false)
        set(value) {
            prefs.edit {
                putBoolean(PREF_DEBUG, value)
            }
        }


    override var selectedNetworkInterfaceName: String?
        get() = prefs.getString(PREF_SELECTED_NETWORK_INTERFACE_NAME, null)
        set(value) {
            prefs.edit {
                putString(PREF_SELECTED_NETWORK_INTERFACE_NAME, value)
            }
        }

    override var selectedWanServiceReference: ServiceReference?
        get() = prefs.getString(PREF_SELECTED_WAN_SERVICE_REFERENCE, null)?.let(::ServiceReference)?.let {
            if (it.udn != null && it.serviceId != null) it else null
        }
        set(value) {
            prefs.edit {
                if (value == null)
                    remove(PREF_SELECTED_WAN_SERVICE_REFERENCE)
                else
                    putString(PREF_SELECTED_WAN_SERVICE_REFERENCE, value.toString())
            }
        }

    companion object {
        private const val TAG = "PecaPortPreferences"
        private const val PREF_DEBUG = "pref_upnp_debug"
        private const val PREF_SELECTED_NETWORK_INTERFACE_NAME = "pref_upnp_selected_network_interface_name"
        private const val PREF_SELECTED_WAN_SERVICE_REFERENCE = "pref_upnp_selected_wan_service_reference"

    }

}
