package org.peercast.pecaport

import org.fourthline.cling.controlpoint.ControlPoint
import org.fourthline.cling.model.action.ActionException
import org.fourthline.cling.model.meta.RemoteService
import org.fourthline.cling.registry.Registry
import org.fourthline.cling.support.model.Connection
import org.fourthline.cling.support.model.PortMapping
import org.peercast.pecaport.cling.GetExternalIPFactory
import org.peercast.pecaport.cling.GetStatusInfoFactory
import org.peercast.pecaport.cling.PortMappingEntryGetFactory
import org.peercast.pecaport.cling.executeAwait
import org.slf4j.LoggerFactory

/**
 * 　WANPPPConnectionの状態
 * @author (c) 2015, T Yoshizawa
 * Dual licensed under the MIT or GPL licenses.
 */

/**
 * 各種Actionを実行し、外部IPアドレスや既存のマッピング状態を得る。
 */
class WanConnection private constructor(
        val registry: Registry,
        val service: RemoteService) : Comparable<WanConnection> {
    private val logger = LoggerFactory.getLogger(javaClass.name)

    var externalIp: String = ""
        private set

    var status: Connection.Status = Connection.Status.Disconnected
        private set

    private val mappings_ = ArrayList<PortMapping>()


    val mappings: List<PortMapping>
        get() = mappings_

    /**
     * ルーターに対して以下の順にActionを実行する。
     *
     *   GetExternalIP
     *   GetStatusInfo
     *   GetGenericPortMappingEntry(index=0)<br></br>
     * (取得に失敗しない限り・・)
     *   GetGenericPortMappingEntry(index=16)まで
     *
     */
    private suspend fun executeActions(controlPoint: ControlPoint) {
        try {
            externalIp = controlPoint.executeAwait(GetExternalIPFactory(service))
            logger.debug("Success GetExternalIP: $externalIp")
        } catch (e: ActionException) {
            logger.error("Fail GetExternalIP: ${e.message}")
        }

        try {
            val info = controlPoint.executeAwait(GetStatusInfoFactory(service))
            status = info.status
            logger.debug("Success GetStatusInfo: $info")
        } catch (e: ActionException) {
            logger.error("Fail GetStatusInfo: ${e.message}")
        }

        service.getAction("GetGenericPortMappingEntry")?.let {
            for (i in 0..15L) {
                try {
                    val m = controlPoint.executeAwait(PortMappingEntryGetFactory(service, i))
                    logger.debug("Success GetGenericPortMappingEntry: index=$i, $m")
                    mappings_ += m
                } catch (e: ActionException) {
                    if (i == 0L)
                        logger.error("Fail GetGenericPortMappingEntry: index=$i, ${e.message}")
                    break
                }
            }
        }
    }

    /**serviceId名で比較する */
    override fun compareTo(other: WanConnection): Int {
        return service.serviceId.toString().compareTo(other.service.serviceId.toString())
    }

    override fun toString(): String {
        return "WanConnection(service=$service, externalIp='$externalIp', status=$status, mappings=$mappings)"
    }

    companion object {
        private const val TAG = "WanConnection"

        /**no_throw*/
        suspend fun create(registry: Registry, service: RemoteService): WanConnection {
            return WanConnection(registry, service).also {
                it.executeActions(registry.upnpService.controlPoint)
            }
        }
    }


}