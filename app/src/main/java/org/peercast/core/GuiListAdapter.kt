package org.peercast.core

/**
 * (c) 2014, T Yoshizawa
 *
 * Dual licensed under the MIT or GPL licenses.
 */

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseExpandableListAdapter
import androidx.annotation.DrawableRes
import kotlinx.android.synthetic.main.ch_item_c.view.*
import kotlinx.android.synthetic.main.ch_item_g.view.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.InetAddress
import java.net.UnknownHostException

class GuiListAdapter : BaseExpandableListAdapter() {
    var channels = emptyList<Channel>()
        set(channels) {
            field = channels
            notifyDataSetChanged()
        }

    override fun getGroupView(pos: Int, isExpanded: Boolean, convertView: View?, parent: ViewGroup): View {
        val v = convertView ?:
            LayoutInflater.from(parent.context).inflate(R.layout.ch_item_g, parent, false)
        val ch = getGroup(pos)

        v.vChIcon.setImageResource(getChannelStatusIcon(ch))
        v.vChName.text = ch.info.name
        v.vChRelays.text = "%d/%d  -  [%d/%d]".format(ch.totalListeners, ch.totalRelays, ch.localListeners, ch.localRelays)
        v.vChBitrate.text = "% 5d kbps".format(ch.info.bitrate)

        return v
    }

    @DrawableRes
    private fun getChannelStatusIcon(ch: Channel): Int {
        return when (ch.status) {
            Channel.S_IDLE -> R.drawable.ic_st_idle

            Channel.S_SEARCHING,
            Channel.S_CONNECTING -> R.drawable.ic_st_connect

            Channel.S_RECEIVING -> {
                val nowTimeSec = (System.currentTimeMillis() / 1000).toInt()

                if (ch.skipCount > 2 && ch.lastSkipTime + 120 > nowTimeSec) {
                    R.drawable.ic_st_conn_ok_skip
                } else {
                    R.drawable.ic_st_conn_ok
                }
            }

            Channel.S_BROADCASTING -> R.drawable.ic_st_broad_ok

            Channel.S_ERROR ->
                // if (ch && ch->bumped)
                // img = img_connect;
                R.drawable.ic_st_error

            else -> R.drawable.ic_st_idle
        }
    }

    @DrawableRes
    private fun getServentStatusIcon(ch: Channel, svt: Servent): Int {
        if (ch.status != Channel.S_RECEIVING)
            return R.drawable.ic_empty

        val nowTimeSec = (System.currentTimeMillis() / 1000).toInt()
        return if (ch.skipCount > 2 && ch.lastSkipTime + 120 > nowTimeSec) {
            when {
                svt.isRelay -> R.drawable.ic_st_conn_ok_skip
                svt.numRelays > 0 -> R.drawable.ic_st_conn_full_skip
                else -> R.drawable.ic_st_conn_over_skip
            }
        } else {
            when {
                svt.isRelay -> R.drawable.ic_st_conn_ok
                svt.numRelays > 0 -> R.drawable.ic_st_conn_full
                else -> R.drawable.ic_st_conn_over
            }
        }
    }

    override fun getChild(gPos: Int, cPos: Int): Servent {
        return getGroup(gPos).servents[cPos]
    }

    override fun getChildId(gPos: Int, cPos: Int): Long = 0

    override fun getChildView(gPos: Int, cPos: Int, isLastChild: Boolean, convertView: View?, parent: ViewGroup): View {
        val c = parent.context
        val v = convertView ?: LayoutInflater.from(c).inflate(R.layout.ch_item_c, parent, false)
        val ch = getGroup(gPos)
        val svt = getChild(gPos, cPos)

        v.vSvtIcon.setImageResource(getServentStatusIcon(ch, svt))
        // (Version) 0/0 123.0.0.0(hostname)
        v.vSvtVersion.text = "(%s)".format(svt.version)
        v.vSvtRelays.text = "%d/%d".format(svt.totalListeners, svt.totalRelays)
        v.vSvtHost.text = "%s (%s)".format(svt.host, getHostName(svt.host))
        return v
    }

    override fun getChildrenCount(gPos: Int): Int {
        return getGroup(gPos).servents.size
    }

    override fun getGroup(pos: Int): Channel {
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
                hostNames[ip] = withContext (Dispatchers.Default) {
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