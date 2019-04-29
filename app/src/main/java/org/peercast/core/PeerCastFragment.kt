package org.peercast.core


import android.content.ActivityNotFoundException
import android.os.Bundle
import android.view.*
import android.widget.ExpandableListView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import kotlinx.android.synthetic.main.peercast_fragment.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.sharedViewModel
import org.peercast.core.databinding.PeercastFragmentBinding
import org.peercast.core.lib.LibPeerCast
import org.peercast.core.lib.PeerCastController
import org.peercast.core.lib.PeerCastRpcClient
import timber.log.Timber
import kotlin.coroutines.CoroutineContext

/**
 * @author (c) 2014-2019, T Yoshizawa
 * @licenses Dual licensed under the MIT or GPL licenses.
 */
class PeerCastFragment : Fragment(), CoroutineScope {
    private val job = Job()
    private val viewModel by sharedViewModel<PeerCastViewModel>()
    private val controller by inject<PeerCastController>()
    private val activity: PeerCastActivity?
        get() = super.getActivity() as PeerCastActivity?
    private val appPrefs by inject<AppPreferences>()
    override val coroutineContext: CoroutineContext
        get() = job + Dispatchers.Main

    private val listAdapter: GuiListAdapter
        get() = vListChannel.expandableListAdapter as GuiListAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        viewModel.channels.observe(this, Observer { channels ->
            listAdapter.channels = channels
        })
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return PeercastFragmentBinding.inflate(inflater, container, false).also {
            it.viewModel = viewModel
            //FIX: NullPointerException: Attempt to invoke direct method 'void androidx.databinding.ViewDataBinding.handleFieldChange
            //FragmentのLifecycleOwnerはgetViewLifecycleOwnerを使おう
            //@see https://medium.com/@star_zero/fragment%E3%81%AElifecycleowner%E3%81%AFgetviewlifecycleowner%E3%82%92%E4%BD%BF%E3%81%8A%E3%81%86-3ab8b1d976ba
            it.lifecycleOwner = viewLifecycleOwner
            it.vListChannel.setAdapter(GuiListAdapter())
            registerForContextMenu(it.vListChannel)
        }.root
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        setHasOptionsMenu(true)
        activity?.supportActionBar?.let { ab ->
            ab.setDisplayHomeAsUpEnabled(false)
            ab.setTitle(R.string.app_name)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.peercast_menu, menu)
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
            menu.setHeaderTitle(ch.ch.info                    .name)
            //val mKeep = menu.findItem(R.id.menu_ch_keep)
            //mKeep.isChecked = ch.isStayConnected
        } else if (type == ExpandableListView.PACKED_POSITION_TYPE_CHILD) {
            // サーヴァント
            inflater.inflate(R.menu.servent_context_menu, menu)
            val svt = listAdapter.getChild(gPos, cPos)
            menu.setHeaderTitle(svt.remoteEndPoint.toString())
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.menu_remove_all_channel -> {
                listAdapter.channels.filter { ch ->
                    ch.ch.status.localRelays + ch.ch.status.localDirects == 0
                }.forEach { ch ->
                    //controller.disconnectChannel(ch.channel_id)r
                    launchRpc {
                        stopChannel(ch.ch.channelId)
                    }
                }
                true
            }

            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun <T> launchRpc(block: suspend PeerCastRpcClient.()->T) = launch {
        val rpcClient = when (controller.isConnected) {
            true -> PeerCastRpcClient(controller)
            else -> return@launch
        }
        runCatching { rpcClient.block() }.onFailure {
            Timber.e(it)
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
                Timber.i("Disconnect channel: $ch")
                launchRpc { stopChannel(ch.ch.channelId) }
                true
            }

//            R.id.menu_ch_keep -> {
//                Timber.i("Keep channel: $ch")
//                //controller.setChannelKeep(ch.channel_id, !item.isChecked)
//                true
//            }

            R.id.menu_ch_play -> {
                val intent = LibPeerCast.createStreamIntent(ch.ch.channelId, appPrefs.port)
                try {
                    showToast("${intent.data}")
                    startActivity(intent)
                } catch (e: ActivityNotFoundException) {
                    activity?.showAlertDialog(R.string.t_error, e.localizedMessage)
                }
                true
            }

            R.id.menu_ch_bump -> {
                Timber.i("Bump channel: $ch")
                launchRpc {
                    bumpChannel(ch.ch.channelId)
                }
                true
            }

            R.id.menu_svt_disconnect -> {
                //直下切断
                val conn = listAdapter.getChild(gPos, cPos)
                Timber.i("Disconnect connection: $conn")
                launchRpc {
                    stopChannelConnection(ch.ch.channelId ,conn.connectionId)
                }
                true
            }

            else -> super.onContextItemSelected(item)
        }
    }

    private fun showToast(msg: String) {
        Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
    }

    override fun onDestroy() {
        super.onDestroy()
        job.cancel()
    }

}
