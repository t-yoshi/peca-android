package org.peercast.core.lib.internal

import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.suspendCancellableCoroutine
import okhttp3.*
import okhttp3.logging.HttpLoggingInterceptor
import org.peercast.core.lib.BuildConfig
import org.peercast.core.lib.rpc.EndPointAdapter
import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException


object SquareUtils {
    private const val HTTP_CONNECT_TIMEOUT = 10L
    private const val HTTP_RW_TIMEOUT = 20L
    private val connectionSpecs = listOf(
            ConnectionSpec.CLEARTEXT,
            ConnectionSpec.Builder(ConnectionSpec.MODERN_TLS)
                    .tlsVersions(TlsVersion.TLS_1_2)
                    .build()
    )

    val okHttpClient = OkHttpClient.Builder()
            .connectTimeout(HTTP_CONNECT_TIMEOUT, TimeUnit.SECONDS)
            .connectionSpecs(connectionSpecs)
            .readTimeout(HTTP_RW_TIMEOUT, TimeUnit.SECONDS)
            .writeTimeout(HTTP_RW_TIMEOUT, TimeUnit.SECONDS)

            .also {
                if (BuildConfig.DEBUG) {
                    it.addNetworkInterceptor(HttpLoggingInterceptor().also {
                        it.level = HttpLoggingInterceptor.Level.HEADERS
                    })
                }
            }
            .build()

    val moshi: Moshi = Moshi.Builder()
            .add(NullSafeAdapter)
            .add(EndPointAdapter)
            .add(KotlinJsonAdapterFactory())
            .build()

    /**Callback#onResponse内でfを実行し、その結果を返す*/
    suspend fun <T> Call.runAwait(f: (Response) -> T): T {
        return suspendCancellableCoroutine { continuation ->
            enqueue(object : Callback {
                override fun onResponse(call: Call, response: Response) {
                    kotlin.runCatching {
                        response.use { f(it) }
                    }
                            .onSuccess<T>(continuation::resume)
                            .onFailure(::onFailure)
                }

                private fun onFailure(t: Throwable) {
                    if (continuation.isCancelled)
                        return
                    continuation.resumeWithException(t)
                }

                override fun onFailure(call: Call, e: IOException) {
                    onFailure(e)
                }
            })

            continuation.invokeOnCancellation {
                try {
                    cancel()
                } catch (ex: Throwable) {
                }
            }
        }
    }

}