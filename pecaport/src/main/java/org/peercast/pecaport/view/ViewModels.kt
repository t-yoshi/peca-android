package org.peercast.pecaport.view

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import org.apache.commons.lang3.time.DurationFormatUtils
import org.fourthline.cling.support.model.PortMapping
import org.peercast.pecaport.NetworkInterfaceManager
import org.peercast.pecaport.R

class PecaPortViewModel(a: Application, networkInterfaceManager: NetworkInterfaceManager) : AndroidViewModel(a) {
    val networkInterfaces = networkInterfaceManager.allInterfaces
    val rooterName = MutableLiveData<CharSequence>()
    val rooterManufacturer = MutableLiveData<CharSequence>()
    val externalIp = MutableLiveData<CharSequence>()
}

class PortMappingViewModel  {
    var isPecaIconVisible = false

    var client = ""
        private set
    var port = ""
        private set
    var protocol = ""
        private set
    var description = ""
        private set
    var duration = ""
        private set
    var isEnabled = false
        private set
    //var isRemoveIconVisible = false

    fun setMapping(c: Context, m: PortMapping){
        client = m.internalClient
        port = if (m.externalPort == m.internalPort)
            m.externalPort.toString()
        else
            m.externalPort.toString() + "/" + m.internalPort

        protocol = m.protocol.name
        description = m.description


        duration = m.leaseDurationSeconds.value.let { sec ->
            when (sec) {
                0L -> c.getString(R.string.t_wan_duration_is_0)
                else -> {
                    DurationFormatUtils.formatDuration(sec * 1000, "dd'd' HH'h'", false)
                    //DateUtils.formatElapsedTime(sec)
                }
            }
        }
        isEnabled = m.isEnabled
    }

}

interface OnPortMappingHandler {
    fun onDeleteButtonClicked()
}