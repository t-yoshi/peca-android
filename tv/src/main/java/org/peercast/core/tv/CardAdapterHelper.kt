package org.peercast.core.tv

import androidx.leanback.widget.ArrayObjectAdapter
import androidx.leanback.widget.HeaderItem
import androidx.leanback.widget.ListRow
import androidx.leanback.widget.ListRowPresenter
import org.peercast.core.lib.rpc.YpChannel
import java.util.*
import kotlin.collections.ArrayList

open class CardAdapterHelper {
    protected val presenter = CardPresenter()
    val adapter = ArrayObjectAdapter(ListRowPresenter())

    protected val ypAdapters = HashMap<String, ArrayObjectAdapter>()


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
            ypAdapters.values.forEach { it.clear() }
            val tm = TreeMap<String, ArrayList<YpChannel>>()
            value.forEach { ch->
                tm.getOrPut(ch.ypHost){
                    ArrayList()
                }.add(ch)
            }
            field = tm.values.flatten()
            field.forEach { ch ->
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

