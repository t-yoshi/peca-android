package org.peercast.core

/**
 * @author (c) 2014-2019, T Yoshizawa
 * @licenses Dual licensed under the MIT or GPL licenses.
 */


import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseExpandableListAdapter
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.DrawableRes
import kotlinx.coroutines.*
import org.peercast.core.lib.rpc.Channel
import org.peercast.core.lib.rpc.ChannelConnection
import org.peercast.core.lib.rpc.ConnectionStatus
import java.net.InetAddress
import java.net.UnknownHostException

class GuiListAdapter : BaseExpandableListAdapter() {
    var channels = emptyList<ActiveChannel>()
        set(channels) {
            field = channels
            notifyDataSetChanged()
        }

    override fun getGroupView(pos: Int, isExpanded: Boolean, convertView: View?, parent: ViewGroup): View {
        val v = convertView
                ?: LayoutInflater.from(parent.context).inflate(R.layout.ch_item_g, parent, false)
        val ch = getGroup(pos)

        v.findViewById<ImageView>(R.id.vChIcon).setImageResource(getChannelStatusIcon(ch))
        v.findViewById<TextView>(R.id.vChName).text = ch.ch.info.name

        v.findViewById<TextView>(R.id.vChRelays).text = "%d/%d  -  [%d/%d]".format(
                ch.ch.status.totalDirects,
                ch.ch.status.totalRelays,
                ch.ch.status.localDirects,
                ch.ch.status.localRelays)
        v.findViewById<TextView>(R.id.vChBitrate).text = "% 5d kbps".format(ch.ch.info.bitrate)

        return v
    }

    @DrawableRes
    private fun getChannelStatusIcon(ch: ActiveChannel): Int {
        return when (ch.ch.status.status) {
            ConnectionStatus.IDLE,
            ConnectionStatus.Idle ->
                R.drawable.ic_st_idle

            ConnectionStatus.Searching,
            ConnectionStatus.SEARCH,
            ConnectionStatus.CONNECT -> R.drawable.ic_st_connect

            ConnectionStatus.Receiving,
            ConnectionStatus.RECEIVE -> {
//                val nowTimeSec = (System.currentTimeMillis() / 1000).toInt()
//
//                if (ch.skipCount > 2 && ch.lastSkipTime + 120 > nowTimeSec) {
//                    R.drawable.ic_st_conn_ok_skip
//                } else {
                R.drawable.ic_st_conn_ok
//                }
            }

            ConnectionStatus.BROADCAST -> R.drawable.ic_st_broad_ok

            ConnectionStatus.Error,
            ConnectionStatus.ERROR ->
                R.drawable.ic_st_error

            else -> R.drawable.ic_st_idle
        }
    }

    @DrawableRes
    private fun getServentStatusIcon(ch: Channel, conn: ChannelConnection): Int {
        if (ch.status.status !in listOf<ConnectionStatus>(
                        ConnectionStatus.Receiving,
                        ConnectionStatus.RECEIVE))
            return R.drawable.ic_empty
//
//        val nowTimeSec = (System.currentTimeMillis() / 1000).toInt()
//        return if (ch.skipCount > 2 && ch.lastSkipTime + 120 > nowTimeSec) {
//            when {
//                svt.isRelay -> R.drawable.ic_st_conn_ok_skip
//                svt.numRelays > 0 -> R.drawable.ic_st_conn_full_skip
//                else -> R.drawable.ic_st_conn_over_skip
//            }
//        } else {
//            when {
//                svt.isRelay -> R.drawable.ic_st_conn_ok
//                svt.numRelays > 0 -> R.drawable.ic_st_conn_full
//                else -> R.drawable.ic_st_conn_over
//            }
//        }
        return when {
            conn.localRelays ?: 0 > 0 ->
                R.drawable.ic_st_conn_full
            else -> R.drawable.ic_st_conn_ok
        }
    }

    override fun getChild(gPos: Int, cPos: Int): ChannelConnection {
        return getGroup(gPos).connections[cPos]
    }

    override fun getChildId(gPos: Int, cPos: Int): Long = 0

    override fun getChildView(gPos: Int, cPos: Int, isLastChild: Boolean, convertView: View?, parent: ViewGroup): View {
        val c = parent.context
        val v = convertView ?: LayoutInflater.from(c).inflate(R.layout.ch_item_c, parent, false)
        val ch = getGroup(gPos)
        val svt = getChild(gPos, cPos)

        v.findViewById<ImageView>(R.id.vSvtIcon).setImageResource(getServentStatusIcon(ch.ch, svt))
        // (Version) 0/0 123.0.0.0(hostname)
        v.findViewById<TextView>(R.id.vSvtVersion).text = "(%s)".format(svt.agentName)
        v.findViewById<TextView>(R.id.vSvtRelays).text = "%d/%d".format(svt.localDirects, svt.localRelays)
        v.findViewById<TextView>(R.id.vSvtHost).text = "%s (%s)".format(
                svt.remoteEndPoint, svt.remoteEndPoint?.let {
            getHostName(it.host)
        })
        return v
    }

    override fun getChildrenCount(gPos: Int): Int {
        return getGroup(gPos).connections.size
    }

    override fun getGroup(pos: Int): ActiveChannel {
        return channels[pos]
    }

    override fun getGroupCount(): Int = channels.size

    override fun getGroupId(pos: Int): Long = 0

    override fun isChildSelectable(gPos: Int, cPos: Int): Boolean = true

    override fun hasStableIds(): Boolean = true


    private val hostNames = HashMap<String, String>()

    /**
     * ipから逆引きしてホスト名を得る。
     */
    private fun getHostName(ip: String): String {
        return hostNames.getOrPut(ip) {
            GlobalScope.launch(Dispatchers.Main) {
                hostNames[ip] = withContext(Dispatchers.Default) {
                    try {
                        InetAddress.getByName(ip).hostName
                    } catch (e: UnknownHostException) {
                        "unknown host"
                    }
                }
                notifyDataSetChanged()
            }
            "lookup..."
        }
    }

}