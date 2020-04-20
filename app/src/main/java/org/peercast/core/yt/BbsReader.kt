package org.peercast.core.yt

import com.squareup.moshi.JsonClass
import okhttp3.Request
import okio.BufferedSink
import org.peercast.core.lib.internal.SquareUtils
import org.unbescape.html.HtmlEscape
import timber.log.Timber
import java.io.IOException
import java.nio.charset.Charset

abstract class JsonResult {
    @Transient
    private val adapter = SquareUtils.moshi.adapter(javaClass)
            .indent("\t")

    fun toJson(sink: BufferedSink) {
        adapter.toJson(sink, this)
    }
}

abstract class BaseBbsReader(private val charset: Charset) {
    /**@throws IOException*/
    abstract fun boardCgi(): JsonResult

    /**@throws IOException*/
    abstract fun threadCgi(id: String, first: Int = 1): JsonResult

    protected fun parseSetting(u: String): Map<String, String> {
        return open(u) { seq ->
            seq.mapIndexedNotNull { i, line ->
                val a = line.split("=", limit = 2)
                if (a.size == 2) {
                    a[0] to a[1]
                } else {
                    Timber.w("${i + 1}: $line")
                    null
                }
            }
        }.toMap()
    }

    /**@throws IOException*/
    protected fun <T> open(u: String, convert: (Sequence<String>) -> Sequence<T>): List<T> {
        require(u.matches("^https?://.+".toRegex()))
        val req = Request.Builder()
                .url(u)
                .build()
        val res = SquareUtils.okHttpClient.newCall(req).execute()
        if (!res.isSuccessful)
            throw IOException("${res.code} ${res.message}")
        val body = res.body ?: throw IOException("body is null")
        return body.byteStream().reader(charset).useLines {
            convert(it).toList()
        }
    }

    @JsonClass(generateAdapter = true)
    data class Thread(val id: String,
                      val title: String,
                      var last: Int)

    @JsonClass(generateAdapter = true)
    data class Post(val no: Int,
                    val name: String,
                    val mail: String,
                    val date: String,
                    val body: String)

    @JsonClass(generateAdapter = true)
    data class BoardJsonResult(
            val status: String,
            val threads: List<Thread>,
            val title: String,
            val category: String,
            val board_num: String
    ) : JsonResult()

    @JsonClass(generateAdapter = true)
    data class ThreadJsonResult(
            val status: String,
            val id: String,
            val title: String,
            val last: Int,
            val posts: List<Post>
    ) : JsonResult()

}

private class ShitarabaReader(val category: String, val board_num: String)
    : BaseBbsReader(Charset.forName("euc-jp")) {

    private var threads: List<Thread>? = null
    private var title: String? = null

    private fun parseSubject(): List<Thread> {
        if (title == null) {
            title = parseSetting("https://jbbs.shitaraba.net/bbs/api/setting.cgi/$category/$board_num/")
                    .getOrElse("BBS_TITLE") { "$category/$board_num" }
        }

        return open("https://jbbs.shitaraba.net/$category/$board_num/subject.txt") {
            it.mapIndexedNotNull { i, line ->
                val g = RE_SUBJECT_LINE.matchEntire(line)?.groupValues
                if (g != null) {
                    Thread(g[1], HtmlEscape.unescapeHtml(g[2]), g[3].toInt())
                } else {
                    Timber.w("${i + 1}: $line")
                    null
                }
            }
        }
    }

    private fun parsePosts(u: String): List<Post> {
        return open(u) {
            it.mapIndexedNotNull { i, line ->
                val a = line.split("<>", limit = 6)
                if (a.size >= 6) {
                    Post(a[0].toInt(), a[1], a[2], a[3], a[4])
                } else {
                    Timber.w("${i + 1}: $line")
                    null
                }
            }
        }
    }

    override fun boardCgi(): BoardJsonResult {
        return BoardJsonResult(
                "ok",
                parseSubject().also {
                    threads = it
                },
                title ?: "title", category, board_num)
    }

    override fun threadCgi(id: String, first: Int): JsonResult {
        require(first >= 1)

        val thread = (threads ?: parseSubject()).firstOrNull {
            it.id == id
        } ?: throw IOException("id `$id` is missing")

        val u = "https://jbbs.shitaraba.net/bbs/rawmode.cgi/$category/$board_num/$id/$first-"
        val posts = parsePosts(u)
        posts.lastOrNull()?.let {
            thread.last = it.no
        }
        return ThreadJsonResult(
                "ok", thread.id, thread.title, thread.last, posts
        )
    }

    companion object {
        private val RE_SUBJECT_LINE = """^(\d+)\.cgi,(.+)\((\d+)\)$""".toRegex()
    }

}

private class ZeroChannelReader(val fqdn: String, val category: String)
    : BaseBbsReader(Charset.forName("shift-jis")) {

    private var title: String? = null
    private var threads: List<Thread>? = null

    private fun parseSubject(): List<Thread> {
        if (title == null) {
            title = parseSetting("http://$fqdn/$category/SETTING.TXT")
                    .getOrElse("BBS_TITLE") { "$fqdn/$category" }
        }

        return open("http://$fqdn/$category/subject.txt") {
            it.mapIndexedNotNull { i, line ->
                val g = RE_SUBJECT_LINE.matchEntire(line)?.groupValues
                if (g != null) {
                    Thread(g[1], HtmlEscape.unescapeHtml(g[2]).trim(), g[3].toInt())
                } else {
                    Timber.w("${i + 1}: $line")
                    null
                }
            }
        }
    }

    private fun parsePosts(u: String): List<Post> {
        return open(u) {
            it.mapIndexedNotNull { i, line ->
                val a = line.split("<>")
                if (a.size >= 4) {
                    Post(i + 1, a[0], a[1], a[2], a[3].trimStart())
                } else {
                    Timber.w("${i + 1}: $line")
                    null
                }
            }
        }
    }

    override fun boardCgi(): JsonResult {
        return BoardJsonResult(
                "ok",
                parseSubject().also {
                    threads = it
                },
                title ?: "title", category, ""
        )
    }

    override fun threadCgi(id: String, first: Int): JsonResult {
        require(first >= 1)

        val thread = (threads ?: parseSubject()).firstOrNull {
            it.id == id
        } ?: throw IOException("id `$id` is missing")

        val u = "http://$fqdn/$category/dat/$id.dat"
        val posts = parsePosts(u).drop(first - 1)
        posts.lastOrNull()?.let {
            thread.last = it.no
        }
        return ThreadJsonResult(
                "ok", thread.id, thread.title, thread.last, posts
        )
    }

    companion object {
        private val RE_SUBJECT_LINE = """^(\d+)\.dat<>(.+)\((\d+)\)$""".toRegex()
    }
}


fun createBbsReader(fqdn: String, category: String, board_num: String): BaseBbsReader {
    return if (fqdn.isEmpty() || fqdn == "jbbs.shitaraba.net")
        ShitarabaReader(category, board_num)
    else
        ZeroChannelReader(fqdn, category)
}
