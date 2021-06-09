package org.peercast.core.yt

import android.net.Uri
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import androidx.collection.LruCache
import okio.Buffer
import timber.log.Timber
import java.io.ByteArrayInputStream
import java.io.IOException

class CgiRequestHandler {
    private data class LruKey(
            val fqdn: String,
            val category: String,
            val board_num: String
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

    private val readerCache = object : LruCache<LruKey, BaseBbsReader>(READER_CACHE_SIZE) {
        override fun create(key: LruKey): BaseBbsReader {
            return createBbsReader(key.fqdn, key.category, key.board_num)
        }
    }

    fun shouldInterceptRequest(request: WebResourceRequest): WebResourceResponse? {
        val u = request.url
        val gr = RE_CGI_URL.find(u.toString())?.groupValues
        if (request.method != "GET" || gr == null)
            return null
        return try {
            val reader = readerCache.get(LruKey(u))!!
            Timber.d("${request.url} : $reader")
            when (gr[3]) {
                "board" -> {
                    reader.boardCgi()
                }
                "thread" -> {
                    val id = requireNotNull(u.getQueryParameter("id")) { "require id: $u" }
                    val first = u.getQueryParameter("first")
                            ?.toIntOrNull()?.let { if (it > 1) it else null }
                            ?: 1
                    reader.threadCgi(id, first)
                }
                else -> throw RuntimeException(gr[3])
            }.toWebResourceResponse()
        } catch (t: Throwable) {
            when (t) {
                is IllegalArgumentException -> {
                    //Timber.w(t)
                    t.toWebResourceResponse(400, "Bad Request")
                }
                is IOException -> {
                    Timber.w(t)
                    t.toWebResourceResponse(500, "Internal Error")
                }
                else -> throw t
            }
        }
    }

    companion object {
        private const val READER_CACHE_SIZE = 5

        private val RE_CGI_URL = """https?://(localhost|127\.0\.0\.1)(:\d+)?/cgi-bin/(board|thread)\.cgi\?.+$""".toRegex()

        private fun JsonResult.toWebResourceResponse(): WebResourceResponse {
            val b = Buffer()
            toJson(b)
            return WebResourceResponse(
                    "application/json",
                    "utf8", b.inputStream()
            )
        }

        private fun Exception.toWebResourceResponse(code: Int, phrase: String): WebResourceResponse {
            return WebResourceResponse(
                    "text/plain",
                    "utf8", code, phrase, emptyMap(),
                    ByteArrayInputStream(toString().toByteArray())
            )
        }
    }

}