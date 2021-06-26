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
import org.peercast.core.lib.internal.IPeerCastEndPoint


abstract class BasePeerCastWorker(
    c: Context, workerParams: WorkerParameters,
    private val peerCastUrl: Uri = DEFAULT_PEERCAST_URL,
    private val serviceTimeout: Long = DEFAULT_SERVICE_TIMEOUT,
) :
    CoroutineWorker(c, workerParams) {

    abstract suspend fun doWorkOnServiceConnected(client: PeerCastRpcClient): Result

    final override suspend fun doWork(): Result {
        if (peerCastUrl.host in listOf("localhost", "127.0.0.1")) {
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
            //ほかのPeerCastに接続する
            val h = requireNotNull(peerCastUrl.host)
            val p = peerCastUrl.port
            val client = PeerCastRpcClient(JsonRpcConnection(h, p))
            return doWorkOnServiceConnected(client)
        }
    }

    companion object {
        private const val TAG = "BasePeerCastWorker"

        private const val DEFAULT_SERVICE_TIMEOUT = 10_000L
        private val DEFAULT_PEERCAST_URL = Uri.parse("http://localhost:7144/")
    }


}