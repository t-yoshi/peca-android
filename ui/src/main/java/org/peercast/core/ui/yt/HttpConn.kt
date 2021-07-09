package org.peercast.core.ui.yt

import org.peercast.core.lib.BuildConfig
import timber.log.Timber
import java.io.Reader
import java.net.HttpURLConnection
import java.net.Proxy
import java.net.URL
import java.nio.charset.Charset

fun httpGet(url: String, charset: Charset): Reader {
    require(url.startsWith("http:") || url.startsWith("https:"))

    val conn = URL(url).openConnection(Proxy.NO_PROXY) as HttpURLConnection
    return conn.let {
        it.connectTimeout = 15_000
        it.readTimeout = 30_000
        it.requestMethod = "GET"
        it.connect()
        if (BuildConfig.DEBUG)
            it.debugLog()

        it.inputStream.buffered().bufferedReader(charset)
    }
}

private fun HttpURLConnection.debugLog() {
    Timber.d("$url")
    headerFields.forEach { e ->
        Timber.d("  ${e.key}: ${e.value}")
    }
}

