package org.peercast.core


import android.content.ActivityNotFoundException
import android.os.Bundle
import android.view.*
import android.widget.ExpandableListView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import kotlinx.android.synthetic.main.peercast_fragment.*
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.sharedViewModel
import timber.log.Timber

/**
 * (c) 2015, T Yoshizawa
 * Dual licensed under the MIT or GPL licenses.
 */
class PeerCastFragment : Fragment() {

    private val viewModel by sharedViewModel<PeerCastViewModel>()
    private val controller by inject<PeerCastController>()
    private val activity: PeerCastActivity?
        get() = super.getActivity() as PeerCastActivity?
    private var runningPort = 0

    private val listAdapter : GuiListAdapter
        get() = vListChannel.expandableListAdapter as GuiListAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        viewModel.serviceResultLiveData.observe(this, Observer {
            listAdapter.channels = it.channels
            vBandwidth?.text = getString(
                    R.string.status_format,
                    it.stats.inBytes / 1000f * 8,
                    it.stats.outBytes / 1000f * 8,
                    it.props.port)
            runningPort = it.props.port
        })
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.peercast_fragment, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        vListChannel.setAdapter(GuiListAdapter())
        registerForContextMenu(vListChannel)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        setHasOptionsMenu(true)
        activity?.supportActionBar?.let { ab->
            ab.setDisplayHomeAsUpEnabled(false)
            ab.setTitle(R.string.app_name)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.peercast_menu, menu)
    }


    override fun onPrepareOptionsMenu(menu: Menu) {
        menu.findItem(R.id.menu_settings).isEnabled = runningPort > 0
    }

    override fun onCreateContextMenu(menu: ContextMenu, v: View, menuInfo: ContextMenu.ContextMenuInfo?) {
        val info = menuInfo as? ExpandableListView.ExpandableListContextMenuInfo ?: return

        val type = ExpandableListView.getPackedPositionType(info.packedPosition)
        val gPos = ExpandableListView.getPackedPositionGroup(info.packedPosition)
        val cPos = ExpandableListView.getPackedPositionChild(info.packedPosition)
        val inflater = MenuInflater(v.context)

        if (type == ExpandableListView.PACKED_POSITION_TYPE_GROUP) {
            // チャンネル
            inflater.inflate(R.menu.channel_context_menu, menu)
            val ch = listAdapter.getGroup(gPos)
            menu.setHeaderTitle(ch.info
                    .name)
            val mKeep = menu.findItem(R.id.menu_ch_keep)
            mKeep.isChecked = ch.isStayConnected
        } else if (type == ExpandableListView.PACKED_POSITION_TYPE_CHILD) {
            // サーヴァント
            inflater.inflate(R.menu.servent_context_menu, menu)
            val svt = listAdapter.getChild(gPos, cPos)
            menu.setHeaderTitle(svt.host)
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.menu_remove_all_channel -> {
                listAdapter.channels.filter { ch->
                    ch.localRelays + ch.localListeners == 0
                }.forEach { ch->
                    controller.disconnectChannel(ch.channel_id)
                }
                true
            }

            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onContextItemSelected(item: MenuItem): Boolean {
        val info = item.menuInfo as ExpandableListView.ExpandableListContextMenuInfo
        val type = ExpandableListView.getPackedPositionType(info.packedPosition)
        val gPos = ExpandableListView.getPackedPositionGroup(info.packedPosition)
        val cPos = ExpandableListView.getPackedPositionChild(info.packedPosition)

        val ch = listAdapter.getGroup(gPos)

        return when (item.itemId) {

            R.id.menu_ch_disconnect -> {
                Timber.i( "Disconnect channel: $ch")
                controller.disconnectChannel(ch.channel_id)
                true
            }

            R.id.menu_ch_keep -> {
                Timber.i( "Keep channel: $ch")
                controller.setChannelKeep(ch.channel_id, !item.isChecked)
                true
            }

            R.id.menu_ch_play -> {
                val intent = ch.info.createIntent(runningPort)
                try {
                    showToast("${intent.data}")
                    startActivity(intent)
                } catch (e: ActivityNotFoundException) {
                    activity?.showAlertDialog(R.string.t_error, e.localizedMessage)
                }
                true
            }

            R.id.menu_ch_bump -> {
                Timber.i( "Bump channel: $ch")
                controller.bumpChannel(ch.channel_id)
                true
            }

            R.id.menu_svt_disconnect -> {
                //直下切断
                val svt = listAdapter.getChild(gPos, cPos)
                Timber.i( "Disconnect servent: $svt")
                controller.disconnectServent(svt.servent_id)
                true
            }

            else -> super.onContextItemSelected(item)
        }
    }

    private fun showToast(msg: String) {
        Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
    }


}
