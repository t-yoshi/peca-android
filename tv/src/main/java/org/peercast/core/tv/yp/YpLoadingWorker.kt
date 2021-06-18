package org.peercast.core.tv.yp

import android.content.Context
import android.widget.Toast
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.peercast.core.lib.PeerCastController
import org.peercast.core.lib.PeerCastRpcClient
import timber.log.Timber
import java.io.IOException


class YpLoadingWorker(c: Context, workerParams: WorkerParameters) :
    CoroutineWorker(c, workerParams), KoinComponent {

    private val bookmark by inject<Bookmark>()
    private val ypChannels by inject<YpChannelsFlow>()

    override suspend fun doWork(): Result {
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

        val client = withTimeoutOrNull(10_000) {
            rpcClient.first { it != null }
        }

        if (client == null) {
            Timber.e("timeout: service could not be connected.")
            return Result.failure()
        }

        return try {
            val channels = client.getYPChannels()
            val cmp = bookmark.comparator()
            ypChannels.value = withContext(Dispatchers.IO) {
                channels.sortedWith(cmp)
            }
            Result.success()
        } catch (e: IOException) {
            Timber.e(e)
            showInfoToast(e.message ?: "(null)")
            Result.failure()
        } finally {
            controller.bindService()
        }
    }

    private fun showInfoToast(text: CharSequence) {
        Toast.makeText(applicationContext, text, Toast.LENGTH_SHORT).show()
    }


}