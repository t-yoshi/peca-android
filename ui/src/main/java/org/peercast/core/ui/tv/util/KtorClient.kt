package org.peercast.core.ui.tv.util

import io.ktor.client.HttpClient
import io.ktor.client.engine.android.Android
import io.ktor.client.features.logging.LogLevel
import io.ktor.client.features.logging.Logger
import io.ktor.client.features.logging.Logging
import org.peercast.core.lib.BuildConfig
import timber.log.Timber
import java.net.Proxy

internal val ktorHttpClient = HttpClient(Android) {
    expectSuccess = false
    engine {
        connectTimeout = 10_000
        socketTimeout = 10_000
        proxy = Proxy.NO_PROXY
    }
    install(Logging) {
        logger = LogcatLogger
        level = LogLevel.HEADERS
    }
}

private object LogcatLogger : Logger {
    override fun log(message: String) {
        if (BuildConfig.DEBUG) {
            Timber.d(message)
        }
    }
}

