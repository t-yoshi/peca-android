package org.peercast.core.lib.app

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull
import org.peercast.core.lib.PeerCastController
import org.peercast.core.lib.PeerCastRpcClient


abstract class BasePeerCastWorker(
    c: Context, workerParams: WorkerParameters,
    private val serviceTimeout: Long = 10_000L,
) :
    CoroutineWorker(c, workerParams) {

    abstract suspend fun doWorkOnServiceConnected(client: PeerCastRpcClient): Result

    final override suspend fun doWork(): Result {
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
    }

    companion object {
        private const val TAG = "BasePeerCastWorker"
    }


}