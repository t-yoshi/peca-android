package org.peercast.core.tv

import android.os.Handler
import android.os.HandlerThread
import android.os.Looper
import androidx.leanback.widget.ArrayObjectAdapter
import androidx.leanback.widget.HeaderItem
import androidx.leanback.widget.ListRow
import androidx.leanback.widget.ListRowPresenter
import org.peercast.core.lib.rpc.YpChannel
import java.text.Normalizer
import java.util.*

class CardAdapterModel {
    val adapter = ArrayObjectAdapter(ListRowPresenter())

    private val ypAdapters = TreeMap<String, ArrayObjectAdapter>()
    private val cardPresenter = CardPresenter2()

    //検索用
    private val normalizedText = HashMap<YpChannel, String>()
//    private val handler = Handler(Looper.getMainLooper())
//    private val worker = Handler(
//        HandlerThread("search").also { it.start() }.looper
//    )

    //Sp
    //Tp
    //Grid[refresh, preference]
    private fun getOrCreateRowYpAdapter(host: String): ArrayObjectAdapter {
        return ypAdapters.getOrPut(host) {
            ArrayObjectAdapter(cardPresenter).also {
                val header = YpHeaderItem(host)
                //Timber.d("-->" + adapter.unmodifiableList<Any>())
                val n = adapter.unmodifiableList<Any>().indexOfLast { r ->
                    r is ListRow && r.headerItem is YpHeaderItem
                }
                adapter.add(n + 1, ListRow(header, it))
            }
        }
    }

    private class YpHeaderItem(name: String) : HeaderItem(name)

    var channels = emptyList<YpChannel>()
        set(value) {
            field = value
            normalizedText.clear()
            ypAdapters.values.forEach { it.clear() }

            value.forEach { ch ->
                getOrCreateRowYpAdapter(ch.ypHost).add(ch)
                with(ch) {
                    normalizedText[ch] = normalize("$name $genre $comment $description")
                }
            }
        }


    fun applySearchQuery(query: String?) {
        ypAdapters.values.forEach { it.clear() }

        if (!query.isNullOrEmpty()) {
            val qs = query.split(RE_SPACE).map(::normalize)
            normalizedText.entries.filter { e ->
                qs.all { q ->
                    e.value.contains(q)
                }
            }.map { it.key }
        } else {
            channels
        }.forEach { ch ->
            getOrCreateRowYpAdapter(ch.ypHost).add(ch)
        }
    }

    companion object {
        private val RE_SPACE = """\s+""".toRegex()
        private val RE_HTTP = """^https?://""".toRegex()

        private fun normalize(s: String) =
            Normalizer.normalize(s, Normalizer.Form.NFKD).lowercase()

        private val YpChannel.ypHost: String
            get() {
                return yellowPage
                    .replace(RE_HTTP, "")
                    .removeSuffix("index.txt")
            }

    }

}