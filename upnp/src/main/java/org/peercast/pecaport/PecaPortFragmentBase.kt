package org.peercast.pecaport

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Spinner
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.fourthline.cling.model.action.ActionException
import org.fourthline.cling.support.model.PortMapping
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.sharedViewModel
import org.peercast.pecaport.cling.PortMappingDeleteFactory
import org.peercast.pecaport.cling.executeAwait
import org.peercast.pecaport.databinding.PecaportFragmentBinding
import org.peercast.pecaport.databinding.PecaportFragmentBindingImpl
import org.peercast.pecaport.view.*
import org.slf4j.LoggerFactory
import kotlin.coroutines.CoroutineContext


/**
 * @author (c) 2015, T Yoshizawa
 * Dual licensed under the MIT or GPL licenses.
 */
abstract class PecaPortFragmentBase : Fragment() {

    private val logger = LoggerFactory.getLogger(javaClass.name)

    private val prefs by inject<PecaPortPreferences>()
    private val viewModel by sharedViewModel<PecaPortViewModel>()

    private var upnpServiceController: UpnpServiceController? = null
    private var isUpnpEnabled = false
    private val wanAdapter = WanConnectionAdapter()
    private lateinit var binding: PecaportFragmentBinding

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return PecaportFragmentBindingImpl.inflate(inflater, container, false).let {
            it.viewModel = viewModel
            it.lifecycleOwner = viewLifecycleOwner
            binding = it
            it.root
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.vNetworkInterfaces.also {
            val adapter = NetworkInterfaceAdapter { it.selectedItemPosition }
            it.onItemSelectedListener { position, _ ->
                val ni = adapter.items[position]
                logger.info("Selected NetworkInterface: ${ni.name}")
                prefs.selectedNetworkInterfaceName = ni.name
            }
            adapter.items = viewModel.networkInterfaces
            it.adapter = adapter
            it.setSelection(
                    adapter.items.indexOfFirst { it.name == prefs.selectedNetworkInterfaceName },
                    false
            )
        }

        binding.rooterInfo.vWan.also {
            it.onItemSelectedListener { position, _ ->
                adapter.getItem(position).let { conn ->
                    conn as WanConnection
                    logger.info("Selected Wan: ${conn.service.reference}")
                    viewModel.externalIp.value = conn.externalIp

                    binding.vMappingEntries.setPortMappings(conn.mappings) { b, m ->
                        b.vm = PortMappingViewModel().also { vm ->
                            vm.setMapping(context, m)
                        }
                        b.handler = DeleteButtonHandler(conn, m)
                    }
                    prefs.selectedWanServiceReference = conn.service.reference
                }
            }
            it.adapter = wanAdapter
        }
    }

    private inner class DeleteButtonHandler(
            private val conn: WanConnection,
            private val m: PortMapping
    ) : OnPortMappingHandler {
        override fun onDeleteButtonClicked() {
            AlertDialog.Builder(context!!)
                    .setIcon(R.drawable.ic_delete_36dp)
                    .setTitle(R.string.t_delete)
                    .setMessage(getString(org.peercast.pecaport.R.string.t_do_you_want_to_delete,
                            m.description, m.internalClient
                    ))
                    .setPositiveButton(android.R.string.ok) { _, _ ->
                        doDelete()
                    }
                    .setNegativeButton(android.R.string.cancel, null)
                    .show()
        }

        private fun doDelete() = lifecycleScope.launch {
            try {
                logger.info("try PortMappingDelete: ${conn.service}, $m")
                conn.registry.upnpService.controlPoint.executeAwait(
                        PortMappingDeleteFactory(conn.service, m)
                )

                upnpServiceController?.let {
                    it.unbindService()
                    it.bindService()
                }
            } catch (e: ActionException) {
                logger.error("${e.errorCode}: ${e.message}", e)
                AlertDialog.Builder(context!!)
                        .setIcon(android.R.drawable.stat_notify_error)
                        .setMessage("${e.errorCode}: ${e.message}")
                        .show()
            }
        }
    }

    protected fun startDiscoverer() {
        if (upnpServiceController != null)
            return
        isUpnpEnabled = true
        upnpServiceController = UpnpServiceController(requireContext()).also { con ->
            con.bindService()
            con.discoveredLiveData.observe(this, Observer { res ->
                res?.let { onDiscovered(it) }
            })
        }
    }

    private fun onDiscovered(discovered: UpnpServiceController.DiscoveredResult) = lifecycleScope.launch {
        discovered.device.details.let {
            viewModel.rooterManufacturer.value = it.manufacturerDetails.manufacturer
            viewModel.rooterName.value = it.friendlyName
        }
        wanAdapter.items = discovered.wanConnections.map {
            WanConnection.create(discovered.registry, it)
        }

        binding.rooterInfo.vWan.setSelection(
                wanAdapter.items.indexOfFirst { it.service.reference == prefs.selectedWanServiceReference },
                false)
    }

    override fun onPause() {
        super.onPause()
        upnpServiceController?.unbindService()
        upnpServiceController = null
    }

    override fun onResume() {
        super.onResume()
        if (isUpnpEnabled)
            startDiscoverer()
    }

}