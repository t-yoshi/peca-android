package org.peercast.core.lib.app

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull
import org.peercast.core.lib.JsonRpcConnection
import org.peercast.core.lib.PeerCastController
import org.peercast.core.lib.PeerCastRpcClient


abstract class BasePeerCastWorker(
    c: Context, workerParams: WorkerParameters,
) : CoroutineWorker(c, workerParams) {


    abstract suspend fun doWorkOnServiceConnected(client: PeerCastRpcClient): Result

    final override suspend fun doWork(): Result {
        val pecaUrl = Uri.parse(inputData.getString(PEERCAST_URL) ?: "")
        val serviceTimeout = inputData.getLong(PEERCAST_SERVICE_TIMEOUT, DEFAULT_SERVICE_TIMEOUT)

        if (pecaUrl.host in listOf(null, "", "localhost", "127.0.0.1")) {
            //PeerCast fof Androidに接続する
            val controller = PeerCastController.from(applicationContext)
            val rpcClient = MutableStateFlow<PeerCastRpcClient?>(null)

            controller.eventListener = object : PeerCastController.ConnectEventListener {
                override fun onConnectService(controller: PeerCastController) {
                    rpcClient.value = PeerCastRpcClient(controller)
                }

                override fun onDisconnectService() {
                    rpcClient.value = null
                }
            }
            controller.bindService()

            val client = withTimeoutOrNull(serviceTimeout) {
                rpcClient.first { it != null }
            }

            if (client == null) {
                Log.e(TAG, "timeout: service could not be connected.")
                return Result.failure()
            }

            return try {
                doWorkOnServiceConnected(client)
            } finally {
                controller.bindService()
            }
        } else {
            //Lan内のPeerCastに接続する
            val h = requireNotNull(pecaUrl.host)
            val p = pecaUrl.port
            val client = PeerCastRpcClient(JsonRpcConnection(h, p))
            return doWorkOnServiceConnected(client)
        }
    }

    companion object {
        private const val TAG = "BasePeerCastWorker"

        const val PEERCAST_SERVICE_TIMEOUT = "peercast-service-timeout"
        const val PEERCAST_URL = "peercast-url"

        private const val DEFAULT_SERVICE_TIMEOUT = 10_000L
    }


}