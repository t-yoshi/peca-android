package org.peercast.core.tv

import android.content.ActivityNotFoundException
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.text.Layout
import android.view.Gravity
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.leanback.app.BrowseSupportFragment
import androidx.leanback.widget.*
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.sharedViewModel
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.peercast.core.AppPreferences
import org.peercast.core.PeerCastViewModel
import org.peercast.core.R
import org.peercast.core.lib.LibPeerCast
import org.peercast.core.lib.rpc.YpChannel
import timber.log.Timber

/**
 * Loads [MainFragment].
 */
class PeerCastTvActivity : FragmentActivity() {
    private val viewModel by viewModel<PeerCastViewModel>()

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
        private val appPrefs by inject<AppPreferences>()
        private val viewModel by sharedViewModel<PeerCastViewModel>()
        private val rowsAdapter = ArrayObjectAdapter(ListRowPresenter())

        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)
            title = "Channels"

            loadYpChannels()
            headersState = HEADERS_DISABLED
            isHeadersTransitionOnBackEnabled = true
            adapter = rowsAdapter
            onItemViewClickedListener = this
        }

        private fun loadYpChannels() {
            val cardPresenter = CardPresenter2()
           // rowsAdapter.

            viewModel.executeRpcCommand { client ->
                //loading...
                val channels = LinkedHashMap<String, ArrayList<YpChannel>>()
                client.getYPChannels().forEach { ch ->
                    channels.getOrPut(
                        ch.yellowPage.removeSuffix("index.txt"),
                        { ArrayList() }
                    ).add(ch)
                }

                rowsAdapter.clear()
                channels.onEachIndexed { i, e ->
                    val listRowAdapter = ArrayObjectAdapter(cardPresenter)
                    val header = HeaderItem(i.toLong(), e.key)
                    listRowAdapter.addAll(0, e.value)
                    rowsAdapter.add(ListRow(header, listRowAdapter))
                }

                val gridHeader = HeaderItem(100L, "PREFERENCES")

                val mGridPresenter = GridItemPresenter()
                val gridRowAdapter = ArrayObjectAdapter(mGridPresenter)
                gridRowAdapter.add("reload")
                gridRowAdapter.add(getString(R.string.error_fragment))
                gridRowAdapter.add(resources.getString(R.string.personal_settings))
                rowsAdapter.add(ListRow(gridHeader, gridRowAdapter))


                Timber.d("->$channels")
                //adapter.notifyItemRangeChanged(0, 1)
            }
        }

        private inner class GridItemPresenter : Presenter() {
            private val GRID_ITEM_WIDTH = 128
            private val GRID_ITEM_HEIGHT = 128

            override fun onCreateViewHolder(parent: ViewGroup): ViewHolder {
                val inflater = LayoutInflater.from(parent.context)
                val view = inflater.inflate(R.layout.grid_item, parent, false) as ImageView


//                val view = TextView(parent.context)
//                val c = view.context
//                view.layoutParams = ViewGroup.LayoutParams(
//                    convertDpToPixel(c, GRID_ITEM_WIDTH),
//                    convertDpToPixel(c, GRID_ITEM_HEIGHT)
//                )
//                view.isFocusable = true
//                view.isFocusableInTouchMode = true
//                view.setBackgroundColor(ContextCompat.getColor(requireActivity(), R.color.default_background))
//                view.setTextColor(Color.WHITE)
//                view.gravity = Gravity.CENTER
                return ViewHolder(view)
            }

            override fun onBindViewHolder(viewHolder: ViewHolder, item: Any) {
                //(viewHolder.view as TextView).text = item as String
                (      viewHolder.view as ImageView)
                .setImageResource(R.drawable.ic_baseline_refresh_64)
            }

            override fun onUnbindViewHolder(viewHolder: ViewHolder) {
                (      viewHolder.view as ImageView)                    .setImageDrawable(null)
            }
        }

        override fun onItemClicked(
            itemViewHolder: Presenter.ViewHolder,
            item: Any,
            rowViewHolder: RowPresenter.ViewHolder,
            row: Row
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
            } else if (item == "reload"){
                loadYpChannels()
            }

        }
    }
}