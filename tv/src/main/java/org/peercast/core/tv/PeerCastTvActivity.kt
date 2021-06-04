package org.peercast.core.tv

import android.content.ActivityNotFoundException
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ImageView
import androidx.fragment.app.FragmentActivity
import androidx.leanback.app.BrowseSupportFragment
import androidx.leanback.widget.*
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.sharedViewModel
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.peercast.core.lib.LibPeerCast
import org.peercast.core.lib.rpc.YpChannel
import timber.log.Timber

/**
 * Loads [MainFragment].
 */
class PeerCastTvActivity : FragmentActivity() {
    // private val viewModel by viewModel<PeerCastTvViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

//        setContentView(R.layout.activity_peer_cast_tv)
        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
//                .replace(android.R.id.content, YtWebViewFragment())
                .replace(android.R.id.content, BrowseFragment())
//                .replace(android.R.id.content, MainFragment())
                .commitNow()
        }
    }

    class BrowseFragment : BrowseSupportFragment(), OnItemViewClickedListener {
        private val appPrefs by inject<TvPreferences>()
        private val viewModel by sharedViewModel<PeerCastTvViewModel>()
        private val rowsAdapter = ArrayObjectAdapter(ListRowPresenter())

        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)


            title = "Channels"

            initRows()
            loadYpChannels()
            headersState = HEADERS_DISABLED
            isHeadersTransitionOnBackEnabled = true
            adapter = rowsAdapter
            onItemViewClickedListener = this
        }

        private val ypAdapterMap = HashMap<String, ArrayObjectAdapter>()
        private val cardPresenter = CardPresenter2()

        private fun initRows() {
            val gridHeader = HeaderItem(100L, "PREFERENCES")
            val mGridPresenter = GridItemPresenter()
            val gridRowAdapter = ArrayObjectAdapter(mGridPresenter)
            gridRowAdapter.add("reload")
            gridRowAdapter.add(getString(R.string.error_fragment))
            gridRowAdapter.add(resources.getString(R.string.personal_settings))
            rowsAdapter.add(ListRow(gridHeader, gridRowAdapter))
        }

        private fun putChannels(channels: List<YpChannel>) {
            ypAdapterMap.values.forEach { it.clear() }

            channels.forEach { ch ->
                val host = ch.yellowPage.replace("""^https?://""".toRegex(), "").removeSuffix("index.txt")
                ypAdapterMap.getOrPut(host) {
                    ArrayObjectAdapter(cardPresenter).also {
                        val n = ypAdapterMap.size
                        val header = HeaderItem(n.toLong(), host)
                        rowsAdapter.add(rowsAdapter.size() - 1, ListRow(header, it))
                    }
                }.add(ch)
            }
        }

        private fun loadYpChannels() {
            viewModel.executeRpcCommand { client ->
                //loading...
                val channels = client.getYPChannels()
                putChannels(channels)
                Timber.d("->$channels")
            }
        }

        private inner class GridItemPresenter : Presenter() {
            override fun onCreateViewHolder(parent: ViewGroup): ViewHolder {
                val inflater = LayoutInflater.from(parent.context)
                val view = inflater.inflate(R.layout.grid_item, parent, false) as ImageView
                view.setOnLongClickListener {
                    Timber.d("--->")
                    false
                }
                return ViewHolder(view)
            }

            override fun onBindViewHolder(viewHolder: ViewHolder, item: Any) {
                (viewHolder.view as ImageView)
                    .setImageResource(R.drawable.ic_baseline_refresh_64)
            }

            override fun onUnbindViewHolder(viewHolder: ViewHolder) {
                (viewHolder.view as ImageView).setImageDrawable(null)
            }
        }

        override fun onItemClicked(
            itemViewHolder: Presenter.ViewHolder,
            item: Any,
            rowViewHolder: RowPresenter.ViewHolder,
            row: Row,
        ) {
            if (item is YpChannel && item.channelId != NULL_ID) {
                Timber.i("item: $item")
                val i = LibPeerCast.createStreamIntent(item, appPrefs.port)
                Timber.i("start playing: ${i.data}")
                //i.setClass(requireContext(), PlaybackActivity::class.java)
                try {
                    startActivity(i)
                } catch (e: ActivityNotFoundException) {
                    Timber.e(e)
                }
            } else if (item is YpChannel && item.channelId == NULL_ID) {
                val i = Intent(requireContext(), DetailsActivity::class.java)
                i.putExtra(DetailsActivity.EX_YP_CHANNEL, item)
                startActivity(i)
            } else if (item == "reload") {
                loadYpChannels()
            }

        }
    }
}