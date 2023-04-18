package org.peercast.core.ui.yt

import android.net.Uri
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import androidx.collection.LruCache
import timber.log.Timber
import java.io.ByteArrayInputStream
import java.io.FileNotFoundException
import java.io.IOException

class CgiRequestHandler {
    private data class LruKey(
        val fqdn: String,
        val category: String,
        val board_num: String,
    ) {
        constructor(u: Uri) : this(
            u.getQueryParameter("fqdn") ?: "",
            requireNotNull(u.getQueryParameter("category")) { "require category: $u" },
            u.getQueryParameter("board_num") ?: ""
        ) {
            if (fqdn.isEmpty() && board_num.isEmpty())
                throw IllegalArgumentException("it requires either fdqn or board_num: $u")
        }
    }

    private val clientCache = object : LruCache<LruKey, BaseBbsClient>(CLIENT_CACHE_SIZE) {
        override fun create(key: LruKey): BaseBbsClient {
            return createBbsClient(key.fqdn, key.category, key.board_num)
        }
    }

    fun shouldInterceptRequest(request: WebResourceRequest): WebResourceResponse? {
        val u = request.url
        val gr = RE_CGI_URL.find(u.toString())?.groupValues
        if (request.method != "GET" || gr == null)
            return null
        return try {
            val client = clientCache.get(LruKey(u))!!
            Timber.d("${request.url} : $client")
            when (gr[3]) {
                "board" -> {
                    client.boardCgi()
                }

                "thread" -> {
                    val id = requireNotNull(u.getQueryParameter("id")) { "require id: $u" }
                    val first = u.getQueryParameter("first")
                        ?.toIntOrNull()?.let { if (it > 1) it else null }
                        ?: 1
                    client.threadCgi(id, first)
                }

                "post" -> {
                    val id = requireNotNull(u.getQueryParameter("id")) { "require id: $u" }
                    val name = u.getQueryParameter("name") ?: ""
                    val mail = u.getQueryParameter("mail") ?: ""
                    val body = u.getQueryParameter("body") ?: ""
                    client.postCgi(id, name, mail, body)
                }

                else -> throw RuntimeException(gr[3])
            }.toWebResourceResponse()
        } catch (t: Throwable) {
            when (t) {
                is IllegalArgumentException -> {
                    Timber.e(t)
                    t.toWebResourceResponse(400, "Bad Request")
                }

                is FileNotFoundException -> {
                    Timber.w(t)
                    t.toWebResourceResponse(404, "Not Found")
                }

                is IOException -> {
                    Timber.w(t)
                    t.toWebResourceResponse(500, "Internal Error")
                }

                else -> {
                    Timber.wtf(t)
                    throw t
                }
            }
        }
    }

    companion object {
        private const val CLIENT_CACHE_SIZE = 5

        private val RE_CGI_URL =
            """https?://(localhost|127\.0\.0\.1)(:\d+)?/cgi-bin/(board|thread|post)\.cgi\?.+$""".toRegex()

        private fun JsonResult.toWebResourceResponse(): WebResourceResponse {
            return WebResourceResponse(
                "application/json",
                "utf8", toJson().byteInputStream()
            )
        }

        private fun Exception.toWebResourceResponse(
            code: Int,
            phrase: String,
        ): WebResourceResponse {
            return WebResourceResponse(
                "text/plain",
                "utf8", code, phrase, emptyMap(),
                ByteArrayInputStream(toString().toByteArray())
            )
        }
    }

}