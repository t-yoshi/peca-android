package org.peercast.core.tv

import android.os.Bundle
import androidx.leanback.app.SearchSupportFragment
import org.peercast.core.lib.rpc.YpChannel

class SearchFragment : SearchSupportFragment(), SearchSupportFragment.SearchResultProvider {
    private val cardAdapterModel = CardAdapterModel()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setSearchResultProvider(this)
        val channels = requireNotNull(
            arguments?.getParcelableArrayList<YpChannel>(ARG_YP_CHANNELS)
        )
        cardAdapterModel.channels = channels.filter { it.channelId != NULL_ID }
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

    companion object {
        private const val ARG_YP_CHANNELS = "yp-channels" //

        fun newInstance(channels: List<YpChannel>) : SearchFragment {
            return SearchFragment().also { f->
                f.arguments = Bundle()
                f.requireArguments().putParcelableArrayList(
                    ARG_YP_CHANNELS,
                    ArrayList(channels)
                )
            }
        }
    }
}