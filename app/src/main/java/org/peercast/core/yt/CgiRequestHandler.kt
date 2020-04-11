package org.peercast.core.yt

import android.net.Uri
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import androidx.collection.LruCache
import org.json.JSONObject
import timber.log.Timber
import java.io.ByteArrayInputStream
import java.io.IOException

class CgiRequestHandler {
    private data class LruKey(
            val fdqn: String,
            val category: String,
            val board_num: String
    ) {
        constructor(u: Uri) : this(
                u.getQueryParameter("fdqn") ?: "",
                requireNotNull(u.getQueryParameter("category")) { "category is missing" },
                requireNotNull(u.getQueryParameter("board_num")) { "board_num is missing" }
        )
    }

    private val readerCache = object : LruCache<LruKey, BaseBbsReader>(READER_CACHE_SIZE) {
        override fun create(key: LruKey): BaseBbsReader {
            return createBbsReader(key.fdqn, key.category, key.board_num)
        }
    }

    fun shouldInterceptRequest(request: WebResourceRequest): WebResourceResponse? {
        val u = request.url
        val gr = RE_CGI_URL.find(u.toString())?.groupValues
        if (request.method != "GET" || gr == null)
            return null
        //Timber.d("--> ${request.url}")
        return try {
            val reader = readerCache.get(LruKey(u))!!
            when (gr[3]) {
                "board" -> {
                    reader.boardCgi()
                }
                "thread" -> {
                    val id = requireNotNull(u.getQueryParameter("id")) { "id is missing" }
                    val first = u.getQueryParameter("first")
                            ?.toIntOrNull()?.let { if (it > 1) it else null }
                            ?: 1
                    reader.threadCgi(id, first)
                }
                else -> throw RuntimeException()
            }.toWebResourceResponse()
        } catch (t: Throwable) {
            when (t) {
                is IllegalArgumentException -> {
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

        private fun JSONObject.toWebResourceResponse(): WebResourceResponse {
            return WebResourceResponse(
                    "application/json",
                    "utf8", ByteArrayInputStream(toString().toByteArray())
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