package org.peercast.pecaport.view


import android.view.View
import android.widget.TextView
import androidx.annotation.StringRes
import org.fourthline.cling.support.model.Connection
import org.peercast.pecaport.R
import org.peercast.pecaport.WanConnection

/**
 * 複数の接続先があるルーターに対応する、Spinner用のアダプタ
 *
 * @author (c) 2015, T Yoshizawa
 * Dual licensed under the MIT or GPL licenses.
 */
class WanConnectionAdapter : BaseSpinnerAdapter<WanConnection>(
        R.layout.wan_connection_item,
        R.layout.wan_connection_dropdown_item) {

    override fun bindView(view: View, position: Int) {
        val wan = getItem(position)
        val text1 = view.findViewById<TextView>(android.R.id.text1)
        val text2 = view.findViewById<TextView>(android.R.id.text2)

        text1.text = wan.service.serviceId.id
        text2.setText(wan.status.toStringRes())
        text2.isEnabled = wan.status == Connection.Status.Connected
    }

    @StringRes
    private fun Connection.Status.toStringRes(): Int = when (this) {
        Connection.Status.Connected -> R.string.t_status_connected
        Connection.Status.Connecting -> R.string.t_status_connecting
        Connection.Status.Disconnected -> R.string.t_status_disconnected
        Connection.Status.Disconnecting -> R.string.t_status_disconnecting
        Connection.Status.PendingDisconnect -> R.string.t_status_pending_disconnect
        Connection.Status.Unconfigured -> R.string.t_status_unconfigured
        else -> R.string.t_unknown
    }
}

