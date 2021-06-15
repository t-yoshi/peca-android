package org.peercast.core.tv
/**
 * @author (c) 2014-2021, T Yoshizawa
 * @licenses Dual licensed under the MIT or GPL licenses.
 */
import androidx.leanback.widget.ArrayObjectAdapter
import androidx.leanback.widget.HeaderItem
import androidx.leanback.widget.ListRow
import androidx.leanback.widget.ListRowPresenter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.peercast.core.lib.rpc.YpChannel
import java.text.Normalizer
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.LinkedHashMap

sealed class CardAdapterHelper {
    protected abstract val presenter: CardPresenter
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
        override val presenter = CardPresenter(R.color.default_selected_background)
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

    }

    class Searchable : CardAdapterHelper() {
        override val presenter = CardPresenter(R.color.search_selected_background)

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
                //    .also { Timber.d("-->>$it") }
            } else {
                channelWithNormalizedText.keys
            }.let {
                addYpRows(it)
            }
        }

        companion object {
            private val RE_SPACE = """\s+""".toRegex()

            private suspend fun normalize(s: String) = withContext(Dispatchers.IO) {
                Normalizer.normalize(s, Normalizer.Form.NFKC).toHiragana().lowercase()
            }

            //全角カタカナ -> 全角ひらがな
            private fun String.toHiragana(): String {
                val b = StringBuilder(this.length)
                this.forEach { c ->
                    when (c) {
                        in CharRange('ァ', 'ヶ') -> c + 'あ'.code - 'ア'.code
                        else -> c
                    }.let(b::append)
                }
                return b.toString()
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

