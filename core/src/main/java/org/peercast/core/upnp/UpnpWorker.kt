package org.peercast.core.upnp

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.peercast.core.common.upnp.UpnpManager
import timber.log.Timber
import java.io.IOException

class UpnpWorker(appContext: Context, params: WorkerParameters) :
    CoroutineWorker(appContext, params), KoinComponent {
    private val upnpManager by inject<UpnpManager>()

    override suspend fun doWork(): Result {
        val port = inputData.getInt(PARAM_PORT, 7144)
        if (port !in 1025..65532) {
            Timber.e("invalid port: %d", port)
            return Result.failure()
        }

        val f = when (inputData.getInt(PARAM_OPERATION, -1)) {
            OPERATION_OPEN -> upnpManager::addPort
            OPERATION_CLOSE -> upnpManager::removePort
            else -> throw IllegalArgumentException()
        }

        try {
            f(port)
            return Result.success()
        } catch (e: IOException) {
            Timber.d(e)
        } catch (t: Throwable) {
            //NOTE: 例外が起きても[androidx.work.impl.WorkerWrapper]内で
            //キャッチされるだけ。補足しにくいので注意。
            Timber.e(t, "An exception happened in UpnpWorker.")
        }
        return Result.failure()
    }

    companion object {
        /* open | close*/
        private const val PARAM_OPERATION = "operation"
        private const val OPERATION_OPEN = 1
        private const val OPERATION_CLOSE = 2

        private const val PARAM_PORT = "port"

        private const val TAG_WORKER = "UpnpWorker"

        fun openPort(c: Context, port: Int) {
            enqueueOneTimeWorkRequest(c, OPERATION_OPEN, port)
        }

        fun closePort(c: Context, port: Int) {
            enqueueOneTimeWorkRequest(c, OPERATION_CLOSE, port)
        }

        private fun enqueueOneTimeWorkRequest(c: Context, operation: Int, port: Int) {
            val req = OneTimeWorkRequest.Builder(UpnpWorker::class.java)
                .addTag(TAG_WORKER)
                .setInputData(
                    Data.Builder()
                        .putInt(PARAM_OPERATION, operation)
                        .putInt(PARAM_PORT, port)
                        .build()
                )
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .build()
                )
                .build()

            WorkManager.getInstance(c).run {
                cancelAllWorkByTag(TAG_WORKER)
                enqueue(req)
            }
        }

    }

}