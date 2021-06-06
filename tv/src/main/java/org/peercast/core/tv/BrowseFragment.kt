package org.peercast.core.tv

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ImageView
import androidx.leanback.app.BrowseSupportFragment
import androidx.leanback.widget.*
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import org.koin.androidx.viewmodel.ext.android.sharedViewModel
import org.peercast.core.lib.LibPeerCast
import org.peercast.core.lib.rpc.YpChannel
import timber.log.Timber

class BrowseFragment : BrowseSupportFragment(), OnItemViewClickedListener {
    private val viewModel by sharedViewModel<TvViewModel>()
    private val cardAdapterModel = CardAdapterModel()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        title = "PeerCast"
        initRows()

        headersState = HEADERS_DISABLED
        isHeadersTransitionOnBackEnabled = true
        adapter = cardAdapterModel.adapter
        onItemViewClickedListener = this

        setOnSearchClickedListener {
            val f = SearchFragment.newInstance(cardAdapterModel.channels)
            parentFragmentManager.beginTransaction()
                .replace(android.R.id.content, f)
                .addToBackStack(null)
                .commit()
        }

        lifecycleScope.launchWhenStarted {
            viewModel.ypChannelsFlow.collect {
                cardAdapterModel.channels = it
                launch {
                    delay(1000)
                    setSelectedPosition(0, false)
                }

            }
        }
        startLoading()
    }

    private fun initRows() {
        val gridHeader = HeaderItem(100L, "PREFERENCES")
        val mGridPresenter = GridItemPresenter()
        val gridRowAdapter = ArrayObjectAdapter(mGridPresenter)
        gridRowAdapter.add(R.drawable.ic_baseline_refresh_64)
        gridRowAdapter.add(R.drawable.ic_baseline_open_in_browser_64)
        gridRowAdapter.add(R.drawable.ic_baseline_settings_64)
        cardAdapterModel.adapter.add(ListRow(gridHeader, gridRowAdapter))
    }

    private fun startLoading() {
        parentFragmentManager
            .beginTransaction()
            .add(android.R.id.content, LoadingFragment())
            .commit()
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
                .setImageResource(item as Int)
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
        when {
            item is YpChannel && item.channelId != LibPeerCast.NIL_ID -> {
                viewModel.startPlayer(this, item)
            }
            item is YpChannel -> {
                val i = Intent(requireContext(), DetailsActivity::class.java)
                i.putExtra(DetailsActivity.EX_YP_CHANNEL, item)
                startActivity(i)
            }
            item == R.drawable.ic_baseline_refresh_64 -> {
                startLoading()
            }
            item == R.drawable.ic_baseline_open_in_browser_64 -> {

            }
            item == R.drawable.ic_baseline_settings_64 -> {

            }
        }
    }
}