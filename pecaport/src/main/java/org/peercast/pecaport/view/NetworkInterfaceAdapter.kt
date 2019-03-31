package org.peercast.pecaport.view

import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.view.isInvisible
import org.peercast.pecaport.NetworkInterfaceInfo
import org.peercast.pecaport.R

class NetworkInterfaceAdapter(private val selectedItemPosition: ()->Int) : BaseSpinnerAdapter<NetworkInterfaceInfo>(
        R.layout.network_interface_item
) {

    override fun bindView(view: View, position : Int) {
        val item = getItem(position)
        val c = view.context

        val icon = view.findViewById<ImageView>(android.R.id.icon)
        val title = view.findViewById<TextView>(android.R.id.title)
        val text1 = view.findViewById<TextView?>(android.R.id.text1)

        when (item) {
            is NetworkInterfaceInfo.Wifi -> {
                icon.setImageResource(R.drawable.ic_wifi_36dp)
            }
            is NetworkInterfaceInfo.Ethernet -> {
                icon.setImageResource(R.drawable.ic_server_network_36dp)
            }
        }

        title.text = item.name
        icon.imageTintList = ContextCompat.getColorStateList(c, R.color.md_orange_500)
        text1?.text = item.ipAddress
        //icon1.isVisible = selectedItemPosition == position
    }

    override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
        return super.getDropDownView(position, convertView, parent).also {
            val icon1 = it.findViewById<ImageView>(android.R.id.icon1)
            icon1.isInvisible = selectedItemPosition() != position
        }
    }

}