package org.peercast.core.tv

import android.content.ActivityNotFoundException
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ImageView
import androidx.leanback.app.BrowseSupportFragment
import androidx.leanback.widget.*
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.sharedViewModel
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.peercast.core.lib.LibPeerCast
import org.peercast.core.lib.rpc.YpChannel
import timber.log.Timber

class BrowseFragment : BrowseSupportFragment(), OnItemViewClickedListener {
    private val viewModel by sharedViewModel<PeerCastTvViewModel>()
    private val cardAdapterModel = CardAdapterModel()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        title = "PeerCast"
        initRows()
        loadYpChannels()
        headersState = HEADERS_DISABLED
        isHeadersTransitionOnBackEnabled = true
        adapter = cardAdapterModel.adapter
        onItemViewClickedListener = this

        setOnSearchClickedListener {
//            val i = Intent(it.context, SearchActivity::class.java)
//            i.putParcelableArrayListExtra(SearchActivity.EX_YP_CHANNELS, ArrayList(cardAdapterModel.channels))
//            startActivity(i)

            val f = SearchFragment.newInstance(cardAdapterModel.channels)
            parentFragmentManager.beginTransaction()
                .replace(android.R.id.content, f)
                .addToBackStack(null)
                .commit()
        }
    }

    private fun initRows() {
        val gridHeader = HeaderItem(100L, "PREFERENCES")
        val mGridPresenter = GridItemPresenter()
        val gridRowAdapter = ArrayObjectAdapter(mGridPresenter)
        gridRowAdapter.add("reload")
        gridRowAdapter.add(getString(R.string.error_fragment))
        gridRowAdapter.add(resources.getString(R.string.personal_settings))
        cardAdapterModel.adapter.add(ListRow(gridHeader, gridRowAdapter))
    }

    private fun loadYpChannels() {
        viewModel.executeRpcCommand { client ->
            //loading...
            cardAdapterModel.channels = client.getYPChannels()
            //viewModel.ypChannels = channels
            //putChannels(channels)
            Timber.d("$viewModel -->${viewModel.ypChannels}")
            //rowsAdapter.notifyArrayItemRangeChanged(0, rowsAdapter.size() - 1)

            setSelectedPosition(0, false)

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
            val i = LibPeerCast.createStreamIntent(item, viewModel.prefs.port)
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