package org.peercast.core.tv

import org.peercast.core.lib.rpc.YpChannel
import timber.log.Timber
import java.text.Normalizer
import java.util.*

class SearchableCardAdapterModel : CardAdapterModel() {
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