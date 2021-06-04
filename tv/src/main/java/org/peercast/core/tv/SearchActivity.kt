package org.peercast.core.tv

import android.os.Bundle
import androidx.fragment.app.FragmentActivity
import androidx.leanback.app.SearchSupportFragment
import androidx.leanback.widget.*
import org.koin.androidx.viewmodel.ext.android.sharedViewModel
import org.koin.androidx.viewmodel.ext.android.viewModel
import timber.log.Timber

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
        private val viewModel by viewModel<PeerCastTvViewModel>()
        private val rowsAdapter = ArrayObjectAdapter(ListRowPresenter())
        //private val delayedLoad = SearchRunnable()

        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)
            setSearchResultProvider(this)
        }

        override fun getResultsAdapter() = rowsAdapter

        override fun onQueryTextChange(newQuery: String?): Boolean {
            rowsAdapter.clear()
            if (!newQuery.isNullOrEmpty()){
                val a = ArrayObjectAdapter(CardPresenter2())
                a.addAll(0, viewModel.searchChannel(newQuery))
                Timber.d("$viewModel -->${viewModel.ypChannels}")
                rowsAdapter.add(ListRow(HeaderItem(""), a))
            }

            return true
        }

        override fun onQueryTextSubmit(query: String?): Boolean {
            onQueryTextChange(query)
            return true
        }
    }


}