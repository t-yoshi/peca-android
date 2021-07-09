package org.peercast.core.tv

/**
 * @author (c) 2014-2021, T Yoshizawa
 * @licenses Dual licensed under the MIT or GPL licenses.
 */
import android.os.Bundle
import androidx.fragment.app.FragmentManager
import androidx.leanback.app.SearchSupportFragment
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import org.koin.androidx.viewmodel.ext.android.sharedViewModel

class SearchFragment : SearchSupportFragment(), SearchSupportFragment.SearchResultProvider {
    private val viewModel by sharedViewModel<TvViewModel>()
    private lateinit var adapterHelper: CardAdapterHelper.Searchable

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setSearchResultProvider(this)
        adapterHelper = CardAdapterHelper.Searchable()

        lifecycleScope.launchWhenStarted {
            viewModel.ypChannels.collect { channels ->
                adapterHelper.setChannel(channels)
            }
        }

        setOnItemViewClickedListener(CardEventHandler(parentFragmentManager))
    }

    override fun getResultsAdapter() = adapterHelper.adapter

    override fun onQueryTextChange(newQuery: String?): Boolean {
        lifecycleScope.launch {
            adapterHelper.applySearchQuery(newQuery)
        }
        return true
    }

    override fun onQueryTextSubmit(query: String?): Boolean {
        lifecycleScope.launch {
            adapterHelper.applySearchQuery(query)
        }
        return true
    }

    companion object {
        fun start(fm: FragmentManager) {
            val f = SearchFragment()
            fm.beginTransaction()
                .replace(android.R.id.content, f)
                .addToBackStack(null)
                .commit()
        }
    }
}