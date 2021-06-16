package org.peercast.pecaport

import android.content.Context
import android.os.Build
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import kotlinx.coroutines.withTimeoutOrNull
import org.fourthline.cling.model.action.ActionException
import org.fourthline.cling.model.meta.RemoteService
import org.fourthline.cling.support.model.PortMapping
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.peercast.pecaport.cling.PortMappingAddFactory
import org.peercast.pecaport.cling.PortMappingDeleteFactory
import org.peercast.pecaport.cling.executeAwait
import org.slf4j.LoggerFactory

/**
 * (c) 2019, T Yoshizawa
 * Dual licensed under the MIT or GPL licenses.
 */
class PecaPortWorker(appContext: Context, params: WorkerParameters)
    : CoroutineWorker(appContext, params), KoinComponent {
    private val prefs by inject<PecaPortPreferences>()
    private val logger = LoggerFactory.getLogger(javaClass.name)
    private val networkInterfaceManager by inject<NetworkInterfaceManager>()

    private inner class Params {
        val port = inputData.getInt(PARAM_PORT, 7144).also {
            if (it !in 1025..65535)
                throw IllegalArgumentException("Invalid port: $it")
        }

        val isDelete = inputData.getBoolean(PARAM_DELETE, false)

        val networkInterface = prefs.selectedNetworkInterfaceName?.let {
            networkInterfaceManager.findByName(it)
        } ?: throw IllegalArgumentException("NetworkInterface: not selected")

        val wanReference = prefs.selectedWanServiceReference
                ?: throw IllegalArgumentException("WanReference: not selected")

        override fun toString() =
                "(port=$port, isDelete=$isDelete, networkInterface=$networkInterface, wanReference=$wanReference)"
    }


    override suspend fun doWork(): Result {
        val params = try {
            Params()
        } catch (e: IllegalArgumentException) {
            logger.error(e.message)
            return Result.failure()
        }
        logger.info("Start PecaPortWorker: $params")

        val controller = UpnpServiceController(applicationContext)
        controller.bindService()

        try {
            val discovered = withTimeoutOrNull(5_000) {
                controller.discoveredLiveData.await()
            }
            val wanService = discovered?.registry?.getService(params.wanReference) as? RemoteService
            if (wanService == null) {
                logger.error("WanService not found: '${params.wanReference}'")
                return Result.failure()
            }

            val controlPoint = discovered.registry.upnpService.controlPoint
            val wanConnection = WanConnection.create(discovered.registry, wanService)
            //logger.debug("wanConnection=$wanConnection")

            val alreadyMapping = wanConnection.mappings.firstOrNull { m ->
                m.internalClient == params.networkInterface.ipAddress &&
                        m.externalPort.value.toInt() == params.port &&
                        m.protocol == PortMapping.Protocol.TCP
            }

            //ポートを閉じて終了する。
            if (params.isDelete) {
                try {
                    alreadyMapping?.let {
                        logger.debug("Try PortMappingDelete: $it")
                        controlPoint.executeAwait(PortMappingDeleteFactory(wanService, it))
                        logger.info("Success PortMappingDelete: $it")
                    }
                } catch (e: ActionException) {
                    logger.error(e.message, e)
                    return Result.failure()
                }
                return Result.success()
            }

            if (alreadyMapping != null) {
                logger.info("Already mapped: ${params.networkInterface.ipAddress}:${params.port} exit.")
                return Result.success()
            }

            //同じポートでクライアントが違う場合、閉じる。
            val isWrongClient = wanConnection.mappings.any { m ->
                m.externalPort.value.toInt() == params.port &&
                        m.protocol == PortMapping.Protocol.TCP
            }

            if (isWrongClient) {
                val m = PortMapping(params.port, null, PortMapping.Protocol.TCP)
                logger.debug("Try PortMappingDelete: $m")
                try {
                    controlPoint.executeAwait(PortMappingDeleteFactory(wanService, m))
                    logger.info("Success PortMappingDelete: $m")
                } catch (e:ActionException){
                    logger.warn(e.message, e)
                }
            }

            //ポートを開く。
            try {
                val m = PortMapping(params.port,
                        params.networkInterface.ipAddress,
                        PortMapping.Protocol.TCP, DESCRIPTION)
                logger.debug("Try PortMappingAdd: $m")
                controlPoint.executeAwait(PortMappingAddFactory(wanService, m))
                logger.info("Success PortMappingAdd: $m")
            } catch (e: ActionException) {
                logger.error("${e.errorCode}: ${e.message}", e)
                return Result.failure()
            }

            return Result.success()
        } finally {
            controller.unbindService()
        }
    }


    companion object {
        /**開くポート番号 (Int)*/
        const val PARAM_PORT = "port"
        /**ポートを閉じる (Boolean)*/
        const val PARAM_DELETE = "delete"

        private val DESCRIPTION = "PeerCast(${Build.MODEL.take(16)})"
    }
}