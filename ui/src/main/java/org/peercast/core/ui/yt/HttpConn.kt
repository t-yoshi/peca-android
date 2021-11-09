package org.peercast.core.ui.yt

import androidx.core.text.parseAsHtml
import okhttp3.ConnectionSpec
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.TlsVersion
import okhttp3.logging.HttpLoggingInterceptor
import java.io.IOException
import java.io.Reader
import java.nio.charset.Charset
import java.util.concurrent.TimeUnit

private const val HTTP_CONNECT_TIMEOUT = 15L
private const val HTTP_RW_TIMEOUT = 30L
private val CONNECTION_SPECS = listOf(
    ConnectionSpec.CLEARTEXT,
    ConnectionSpec.Builder(ConnectionSpec.MODERN_TLS)
        .tlsVersions(TlsVersion.TLS_1_2)
        .build()
)

private val okHttpClient = OkHttpClient.Builder()
    .connectionSpecs(CONNECTION_SPECS)
    .connectTimeout(HTTP_CONNECT_TIMEOUT, TimeUnit.SECONDS)
    .readTimeout(HTTP_RW_TIMEOUT, TimeUnit.SECONDS)
    .writeTimeout(HTTP_RW_TIMEOUT, TimeUnit.SECONDS)
    .also { b ->
        if (org.peercast.core.ui.BuildConfig.DEBUG) {
            b.addNetworkInterceptor(HttpLoggingInterceptor().also {
                it.level = HttpLoggingInterceptor.Level.HEADERS
            })
        }
    }
    .build()

private val RE_REMOVE_TAG = """(?is)<(script|style)[ >].+?</\1>""".toRegex()


private fun CharSequence.stripHtml(): CharSequence {
    return RE_REMOVE_TAG.replace(this, "")
        .parseAsHtml().toString()
}

private val RE_SPACE = """[\s　]+""".toRegex()

internal fun httpPost(req: Request): CharSequence {
    val res = okHttpClient.newCall(req).execute()
    val body = res.body ?: throw IOException("body returned null.")
    val cs = body.contentType()?.charset() ?: Charsets.UTF_8
    val ret = body.byteStream().reader(cs).readText()

    return ret.stripHtml().trim().replace(RE_SPACE, " ")
}


internal fun httpGet(url: String, charset: Charset? = null): Reader {
    require(url.startsWith("http:") || url.startsWith("https:"))

    val req = Request.Builder()
        .url(url)
        .build()
    val body = okHttpClient.newCall(req).execute().body ?: throw IOException("body returned null.")
    val cs = charset ?: body.contentType()?.charset() ?: Charsets.UTF_8
    return body.byteStream().bufferedReader(cs)
}


