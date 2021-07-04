package org.peercast.core.tv.yp

import android.content.Context
import android.widget.Toast
import androidx.work.WorkerParameters
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.peercast.core.lib.PeerCastRpcClient
import org.peercast.core.lib.app.BaseClientWorker
import timber.log.Timber
import java.io.IOException


class YpLoadingWorker(c: Context, workerParams: WorkerParameters) :
    BaseClientWorker(c, workerParams), KoinComponent {

    private val bookmark by inject<Bookmark>()
    private val ypChannels by inject<YpChannelsFlow>()

    override suspend fun doWorkOnServiceConnected(client: PeerCastRpcClient): Result {
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
        }
    }

    private fun showInfoToast(text: CharSequence) {
        Toast.makeText(applicationContext, text, Toast.LENGTH_SHORT).show()
    }


}