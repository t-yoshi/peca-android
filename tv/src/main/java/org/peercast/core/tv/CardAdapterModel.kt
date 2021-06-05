package org.peercast.core.tv

import androidx.leanback.widget.ArrayObjectAdapter
import androidx.leanback.widget.HeaderItem
import androidx.leanback.widget.ListRow
import androidx.leanback.widget.ListRowPresenter
import org.peercast.core.lib.rpc.YpChannel
import timber.log.Timber
import java.text.Normalizer
import java.util.*

open class CardAdapterModel {
    val adapter = ArrayObjectAdapter(ListRowPresenter())

    protected val presenter = CardPresenter2()
    protected val ypAdapters = TreeMap<String, ArrayObjectAdapter>()


    //Sp
    //Tp
    //Grid[refresh, preference]
    protected fun getOrCreateRowYpAdapter(ch: YpChannel): ArrayObjectAdapter {
        val host = ch.ypHost
        return ypAdapters.getOrPut(host) {
            ArrayObjectAdapter(presenter).also {
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

    open var channels = emptyList<YpChannel>()
        set(value) {
            field = value

            ypAdapters.values.forEach { it.clear() }
            value.forEach { ch ->
                getOrCreateRowYpAdapter(ch).add(ch)
            }
        }


    companion object {
        private val RE_HTTP = """^https?://""".toRegex()

        private val YpChannel.ypHost: String
            get() {
                return yellowPage
                    .replace(RE_HTTP, "")
                    .removeSuffix("index.txt")
            }

    }
}

class SearchableCardAdapterModel : CardAdapterModel(){
    //検索用
    private val normalizedText = HashMap<YpChannel, String>()
//    private val handler = Handler(Looper.getMainLooper())
//    private val worker = Handler(
//        HandlerThread("search").also { it.start() }.looper
//    )

    init {
        presenter.selectedColorRes = R.color.search_selected_background
    }

    override var channels: List<YpChannel>
        get() = super.channels
        set(value) {
            super.channels = value

            normalizedText.clear()
            value.forEach { ch ->
                with(ch) {
                    normalizedText[ch] = normalize("$name $genre $comment $description")
                }
            }
            Timber.d("-->$normalizedText")
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
            .also { Timber.d("-->>$it") }
        } else {
            channels
        }.forEach { ch ->
            getOrCreateRowYpAdapter(ch).add(ch)
        }
    }

    companion object {
        private val RE_SPACE = """\s+""".toRegex()

        private fun normalize(s: String) =
            Normalizer.normalize(s, Normalizer.Form.NFC).lowercase()
    }

}
