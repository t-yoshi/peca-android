package org.peercast.core.tv

import android.os.Bundle
import androidx.leanback.app.SearchSupportFragment
import androidx.leanback.widget.OnItemViewClickedListener
import androidx.leanback.widget.Presenter
import androidx.leanback.widget.Row
import androidx.leanback.widget.RowPresenter
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.flow.collect
import org.koin.androidx.viewmodel.ext.android.sharedViewModel
import org.peercast.core.lib.isNotNilId
import org.peercast.core.lib.rpc.YpChannel

class SearchFragment : SearchSupportFragment(), SearchSupportFragment.SearchResultProvider,
    OnItemViewClickedListener {
    private val viewModel by sharedViewModel<TvViewModel>()
    private val cardAdapterModel = SearchableCardAdapterModel()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setSearchResultProvider(this)

        lifecycleScope.launchWhenStarted {
            viewModel.ypChannelsFlow.collect { channels ->
                cardAdapterModel.channels = channels.filter { it.isNotNilId }
            }
        }

        setOnItemViewClickedListener(this)
    }

    override fun getResultsAdapter() = cardAdapterModel.adapter

    override fun onQueryTextChange(newQuery: String?): Boolean {
        cardAdapterModel.applySearchQuery(newQuery)
        return true
    }

    override fun onQueryTextSubmit(query: String?): Boolean {
        cardAdapterModel.applySearchQuery(query)
        return true
    }

    override fun onItemClicked(
        itemViewHolder: Presenter.ViewHolder?,
        item: Any?,
        rowViewHolder: RowPresenter.ViewHolder?,
        row: Row?,
    ) {
        when {
            item is YpChannel && item.isNotNilId -> {
                viewModel.startPlayer(this, item)
            }
        }
    }
}