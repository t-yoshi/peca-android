package org.peercast.core.yt

import com.squareup.moshi.JsonClass
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import org.peercast.core.lib.internal.SquareUtils
import org.unbescape.html.HtmlEscape
import timber.log.Timber
import java.io.IOException
import java.nio.charset.Charset

abstract class BaseBbsReader(private val charset: Charset) {
    /**@throws IOException*/
    abstract fun boardCgi(): JSONObject

    /**@throws IOException*/
    abstract fun threadCgi(id: String, first: Int = 1): JSONObject

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
        val body = SquareUtils.okHttpClient.newCall(req).execute().body
                ?: throw IOException("body is null")
        return body.byteStream().reader(charset).useLines {
            convert(it).toList()
        }
    }

    interface JSONize {
        fun toJSONObject(): JSONObject
    }

    data class Thread(val id: String,
                      val title: String,
                      var last: Int) : JSONize {
        override fun toJSONObject(): JSONObject {
            return JSONObject().apply {
                put("id", id)
                put("title", title)
                put("last", last)
            }
        }
    }

    data class Post(val no: Int,
                    val name: String,
                    val mail: String,
                    val date: String,
                    val body: String) : JSONize {
        override fun toJSONObject(): JSONObject {
            return JSONObject().apply {
                put("no", no)
                put("name", name)
                put("mail", mail)
                put("date", date)
                put("body", body)
            }
        }
    }
}

private fun Collection<BaseBbsReader.JSONize>.toJSONArray(): JSONArray {
    return JSONArray().also { ja ->
        forEachIndexed { i, js -> ja.put(i, js.toJSONObject()) }
    }
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

    override fun boardCgi(): JSONObject {
        return JSONObject().apply {
            put("threads", parseSubject().also {
                threads = it
            }.toJSONArray())
            put("status", "ok")
            put("title", title)
            put("category", category)
            put("board_num", board_num)
        }
    }

    override fun threadCgi(id: String, first: Int): JSONObject {
        require(first >= 1)

        val thread = (threads ?: parseSubject()).firstOrNull {
            it.id == id
        } ?: throw IOException("id `$id` is missing")

        val u = "https://jbbs.shitaraba.net/bbs/rawmode.cgi/$category/$board_num/$id/$first-"
        val posts = parsePosts(u)
        posts.lastOrNull()?.let {
            thread.last = it.no
        }
        return JSONObject(thread.toJSONObject(), arrayOf("id", "title", "last")).apply {
            put("status", "ok")
            put("posts", posts.toJSONArray())
        }
    }

    companion object {
        private val RE_SUBJECT_LINE = """^(\d+)\.cgi,(.+)\((\d+)\)$""".toRegex()
    }

}

private class ZeroChannelReader(val fdqn: String, val category: String)
    : BaseBbsReader(Charset.forName("shift-jis")) {

    private var title: String? = null
    private var threads: List<Thread>? = null

    private fun parseSubject(): List<Thread> {
        if (title == null) {
            title = parseSetting("http://$fdqn/$category/SETTING.TXT")
                    .getOrElse("BBS_TITLE") { "$fdqn/$category" }
        }

        return open("http://$fdqn/$category/subject.txt") {
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

    override fun boardCgi(): JSONObject {
        return JSONObject().apply {
            put("threads", parseSubject().also {
                threads = it
            }.toJSONArray())
            put("status", "ok")
            put("title", title)
            put("category", category)
            put("board_num", "")
        }
    }

    override fun threadCgi(id: String, first: Int): JSONObject {
        require(first >= 1)

        val thread = (threads ?: parseSubject()).firstOrNull {
            it.id == id
        } ?: throw IOException("id `$id` is missing")

        val u = "http://$fdqn/$category/dat/$id.dat"
        val posts = parsePosts(u).drop(first - 1)
        posts.lastOrNull()?.let {
            thread.last = it.no
        }
        return JSONObject(thread.toJSONObject(), arrayOf("id", "title", "last")).apply {
            put("status", "ok")
            put("posts", posts.toJSONArray())
        }
    }

    companion object {
        private val RE_SUBJECT_LINE = """^(\d+)\.dat<>(.+)\((\d+)\)$""".toRegex()
    }
}


fun createBbsReader(fdqn: String, category: String, board_num: String): BaseBbsReader {
    return if (fdqn.isEmpty() || fdqn == "jbbs.shitaraba.net")
        ShitarabaReader(category, board_num)
    else
        ZeroChannelReader(fdqn, category)
}
