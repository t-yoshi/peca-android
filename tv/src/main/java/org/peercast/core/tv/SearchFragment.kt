package org.peercast.core.tv

import android.os.Bundle
import androidx.leanback.app.SearchSupportFragment
import androidx.leanback.widget.OnItemViewClickedListener
import androidx.leanback.widget.Presenter
import androidx.leanback.widget.Row
import androidx.leanback.widget.RowPresenter
import org.koin.androidx.viewmodel.ext.android.sharedViewModel
import org.peercast.core.lib.LibPeerCast
import org.peercast.core.lib.rpc.YpChannel

class SearchFragment : SearchSupportFragment(), SearchSupportFragment.SearchResultProvider,
    OnItemViewClickedListener {
    private val viewModel by sharedViewModel<TvViewModel>()
    private val cardAdapterModel = SearchableCardAdapterModel()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setSearchResultProvider(this)
        val channels = requireNotNull(
            arguments?.getParcelableArrayList<YpChannel>(ARG_YP_CHANNELS)
        )
        cardAdapterModel.channels = channels.filter { it.channelId != LibPeerCast.NIL_ID }
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
            item is YpChannel && item.channelId != LibPeerCast.NIL_ID -> {
                viewModel.startPlayer(this, item)
            }
        }
    }

    companion object {
        private const val ARG_YP_CHANNELS = "yp-channels" //

        fun newInstance(channels: List<YpChannel>): SearchFragment {
            return SearchFragment().also { f ->
                f.arguments = Bundle()
                f.requireArguments().putParcelableArrayList(
                    ARG_YP_CHANNELS,
                    ArrayList(channels)
                )
            }
        }
    }
}