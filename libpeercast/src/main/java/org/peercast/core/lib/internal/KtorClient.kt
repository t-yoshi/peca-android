package org.peercast.core.lib.internal

import android.util.Log
import io.ktor.client.*
import io.ktor.client.engine.android.*
import io.ktor.client.features.logging.*
import org.peercast.core.lib.BuildConfig
import java.net.Proxy

internal val ktorHttpClient = HttpClient(Android) {
    expectSuccess = false
    engine {
        connectTimeout = 10_000
        socketTimeout = 10_000
        proxy = Proxy.NO_PROXY
    }
    if (BuildConfig.DEBUG) {
        install(Logging) {
            logger = LogcatLogger
            level = LogLevel.HEADERS
        }
    }
}

private object LogcatLogger : Logger {
    private const val TAG = "LibPeerCast"
    override fun log(message: String) {
        Log.d(TAG, message)
    }
}

