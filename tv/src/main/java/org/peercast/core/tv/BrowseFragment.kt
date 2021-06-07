package org.peercast.core.tv

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
import org.peercast.core.lib.isNotNilId
import org.peercast.core.lib.rpc.YpChannel

class BrowseFragment : BrowseSupportFragment(), OnItemViewClickedListener {
    private val viewModel by sharedViewModel<TvViewModel>()
    private val cardAdapterHelper = CardAdapterHelper()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        title = "PeerCast"
        initRows()

        headersState = HEADERS_DISABLED
        isHeadersTransitionOnBackEnabled = true
        adapter = cardAdapterHelper.adapter
        onItemViewClickedListener = this
//        setOnItemViewSelectedListener()

        setOnSearchClickedListener {
            parentFragmentManager.beginTransaction()
                .replace(android.R.id.content, SearchFragment())
                .addToBackStack(null)
                .commit()
        }

        lifecycleScope.launchWhenResumed {
            viewModel.ypChannelsFlow.collect { channels ->
                //Timber.d("-->$it")
                cardAdapterHelper.channels = channels
                launch {
                    if (channels.isNotEmpty()) {
                        delay(100)
                        setSelectedPosition(0, true)
                    }
                    val n = channels.count { it.isNotNilId }

                }

            }
        }

        LoadingFragment.start(parentFragmentManager)
    }

    private fun initRows() {
        val gridHeader = HeaderItem(100L, "PREFERENCES")
        val mGridPresenter = GridItemPresenter()
        val gridRowAdapter = ArrayObjectAdapter(mGridPresenter)
        gridRowAdapter.add(R.drawable.ic_baseline_refresh_64)
        gridRowAdapter.add(R.drawable.ic_baseline_open_in_browser_64)
        gridRowAdapter.add(R.drawable.ic_baseline_settings_64)
        cardAdapterHelper.adapter.add(ListRow(gridHeader, gridRowAdapter))
    }

    private inner class GridItemPresenter : Presenter() {
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

    override fun onItemClicked(
        itemViewHolder: Presenter.ViewHolder,
        item: Any,
        rowViewHolder: RowPresenter.ViewHolder,
        row: Row,
    ) {
        when (item) {
            is YpChannel -> {
                DetailsFragment.start(parentFragmentManager, item)
            }
            R.drawable.ic_baseline_refresh_64 -> {
                LoadingFragment.start(parentFragmentManager)
            }
            R.drawable.ic_baseline_open_in_browser_64 -> {

            }
            R.drawable.ic_baseline_settings_64 -> {

            }
        }
    }
}