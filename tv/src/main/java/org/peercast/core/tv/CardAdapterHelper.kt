package org.peercast.core.tv

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ImageView
import androidx.leanback.widget.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.peercast.core.lib.rpc.YpChannel
import timber.log.Timber
import java.text.Normalizer
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.LinkedHashMap

internal sealed class CardAdapterHelper {
    protected val presenter = CardPresenter()
    val adapter = ArrayObjectAdapter(ListRowPresenter())

    protected fun addYpRows(channels: Collection<YpChannel>) {
        val tm = TreeMap<String, ArrayList<YpChannel>>()
        channels.forEach { ch ->
            tm.getOrPut(ch.ypHost) {
                ArrayList(128)
            }.add(ch)
        }
        tm.forEach { e ->
            val rowHeader = HeaderItem(e.key)
            val rowAdapter = ArrayObjectAdapter(presenter)
            rowAdapter.addAll(0, e.value)
            adapter.add(ListRow(rowHeader, rowAdapter))
        }
    }

    abstract suspend fun setChannel(channels: List<YpChannel>)

    class Browse : CardAdapterHelper() {
        override suspend fun setChannel(channels: List<YpChannel>) {
            adapter.clear()
            addYpRows(channels)
            addPrefRows()
        }

        private fun addPrefRows() {
            val gridHeader = HeaderItem(100L, "PREFERENCES")
            val mGridPresenter = GridItemPresenter()
            val gridRowAdapter = ArrayObjectAdapter(mGridPresenter)
            gridRowAdapter.add(R.drawable.ic_baseline_refresh_64)
            //gridRowAdapter.add(R.drawable.ic_baseline_open_in_browser_64)
            gridRowAdapter.add(R.drawable.ic_baseline_settings_64)
            adapter.add(ListRow(gridHeader, gridRowAdapter))
        }

        private class GridItemPresenter : Presenter() {
            override fun onCreateViewHolder(parent: ViewGroup): ViewHolder {
                val inflater = LayoutInflater.from(parent.context)
                val view = inflater.inflate(R.layout.grid_item, parent, false) as ImageView
                return ViewHolder(view)
            }

            override fun onBindViewHolder(viewHolder: ViewHolder, item: Any) {
                (viewHolder.view as ImageView)
                    .setImageResource(item as Int)
            }

            override fun onUnbindViewHolder(viewHolder: ViewHolder) {
                (viewHolder.view as ImageView).setImageDrawable(null)
            }
        }
    }

    class Searchable : CardAdapterHelper() {
        init {
            presenter.selectedColorRes = R.color.search_selected_background
        }

        private val channelWithNormalizedText = LinkedHashMap<YpChannel, String>(256)

        override suspend fun setChannel(channels: List<YpChannel>) {
            adapter.clear()
            channelWithNormalizedText.clear()

            channels.filter {
                it.isNotNilId
            }.also { playables ->
                addYpRows(playables)
            }.forEach { ch ->
                channelWithNormalizedText[ch] = with(ch) {
                    normalize("$name $genre $comment $description")
                }
            }
        }

        suspend fun applySearchQuery(query: String?) {
            adapter.clear()

            if (!query.isNullOrEmpty()) {
                val qs = query.split(RE_SPACE).map { normalize(it) }
                channelWithNormalizedText.entries.filter { e ->
                    withContext(Dispatchers.IO) {
                        qs.all { q ->
                            e.value.contains(q)
                        }
                    }
                }.map { it.key }
                    .also { Timber.d("-->>$it") }
            } else {
                channelWithNormalizedText.keys
            }.let {
                addYpRows(it)
            }
        }

        companion object {
            private val RE_SPACE = """\s+""".toRegex()

            private suspend fun normalize(s: String) = withContext(Dispatchers.IO) {
                Normalizer.normalize(s, Normalizer.Form.NFC).lowercase()
            }
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
