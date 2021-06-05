package org.peercast.core.tv

import android.os.Bundle
import android.view.View
import androidx.fragment.app.FragmentActivity
import androidx.leanback.app.SearchSupportFragment
import org.peercast.core.lib.rpc.YpChannel

class SearchActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(android.R.id.content, Fragment())
                .commitNow()
        }
    }

    class Fragment : SearchSupportFragment(), SearchSupportFragment.SearchResultProvider {
        //private val viewModel by viewModel<PeerCastTvViewModel>()
        private val cardAdapterModel = CardAdapterModel()
        //private val delayedLoad = SearchRunnable()

        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)
            setSearchResultProvider(this)
        }

        override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
            super.onViewCreated(view, savedInstanceState)
            val channels = requireActivity().intent.getParcelableArrayListExtra<YpChannel>(EX_YP_CHANNELS)
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
    }

    companion object {
        const val EX_YP_CHANNELS = "yp-channels" //
    }


}