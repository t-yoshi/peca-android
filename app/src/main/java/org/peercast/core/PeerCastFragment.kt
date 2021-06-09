package org.peercast.core


import android.content.ActivityNotFoundException
import android.os.Bundle
import android.view.*
import android.widget.ExpandableListView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.sharedViewModel
import org.peercast.core.databinding.PeercastFragmentBinding
import org.peercast.core.lib.LibPeerCast
import org.peercast.core.lib.PeerCastRpcClient
import org.peercast.core.preferences.AppPreferences
import timber.log.Timber

/**
 * @author (c) 2014-2019, T Yoshizawa
 * @licenses Dual licensed under the MIT or GPL licenses.
 */
class PeerCastFragment : Fragment() {
    private val viewModel by sharedViewModel<AppViewModel>()
    private val appPrefs by inject<AppPreferences>()
    private val listAdapter = GuiListAdapter()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        viewLifecycleOwner.lifecycleScope.launchWhenStarted {
            while (isActive){
                listAdapter.channels = viewModel.getActiveChannels()
                delay(8_000)
            }
        }

        return PeercastFragmentBinding.inflate(inflater, container, false).also {
            it.viewModel = viewModel
            it.lifecycleOwner = viewLifecycleOwner
            it.vListChannel.setAdapter(listAdapter)
            registerForContextMenu(it.vListChannel)
        }.root
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        setHasOptionsMenu(true)
        (activity as AppCompatActivity).supportActionBar?.let{ ab ->
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
            menu.setHeaderTitle(ch.ch.info.name)
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
        when (item.itemId) {
            R.id.menu_remove_all_channel -> {
                listAdapter.channels.filter { ch ->
                    ch.ch.status.localRelays + ch.ch.status.localDirects == 0
                }.forEach { ch ->
                    executeRpcCommand {
                        stopChannel(ch.ch.channelId)
                    }
                }

            }

            else -> return false
        }
        return true
    }

    private fun executeRpcCommand(f: suspend PeerCastRpcClient.()->Unit){
        lifecycleScope.launchWhenStarted {
            val client = viewModel.rpcClient.value
            if (client == null){
                Timber.w("service not connected")
                return@launchWhenStarted
            }
            kotlin.runCatching {
                client.f()
            }.onFailure {
                Timber.e(it)
            }
        }
    }

    override fun onContextItemSelected(item: MenuItem): Boolean {
        val info = item.menuInfo as ExpandableListView.ExpandableListContextMenuInfo
        val type = ExpandableListView.getPackedPositionType(info.packedPosition)
        val gPos = ExpandableListView.getPackedPositionGroup(info.packedPosition)
        val cPos = ExpandableListView.getPackedPositionChild(info.packedPosition)

        if (listAdapter.groupCount <= gPos)
            return false
        val ch = listAdapter.getGroup(gPos)

        return when (item.itemId) {

            R.id.menu_ch_disconnect -> {
                Timber.i("Disconnect channel: $ch")
                executeRpcCommand { stopChannel(ch.ch.channelId) }
                true
            }

            R.id.menu_ch_play -> {
                val intent = LibPeerCast.createStreamIntent(ch.ch.channelId, appPrefs.port)
                try {
                    startActivity(intent)
                } catch (e: ActivityNotFoundException) {
                    Snackbar.make(requireView(), e.toString(), Snackbar.LENGTH_LONG).also {
                        val color = ContextCompat.getColor(it.context, R.color.md_red_700)
                        it.setTextColor(color)
                    }
                }
                true
            }

            R.id.menu_ch_bump -> {
                Timber.i("Bump channel: $ch")
                executeRpcCommand {
                    bumpChannel(ch.ch.channelId)
                }
                true
            }

            R.id.menu_svt_disconnect -> {
                //直下切断
                val conn = listAdapter.getChild(gPos, cPos)
                Timber.i("Disconnect connection: $conn")
                executeRpcCommand {
                    stopChannelConnection(ch.ch.channelId, conn.connectionId)
                }
                true
            }

            else -> super.onContextItemSelected(item)
        }
    }
}
