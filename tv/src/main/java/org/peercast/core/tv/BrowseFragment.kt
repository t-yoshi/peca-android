package org.peercast.core.tv

import android.os.Bundle
import androidx.leanback.app.BrowseSupportFragment
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.flow.collect
import org.koin.androidx.viewmodel.ext.android.sharedViewModel

class BrowseFragment : BrowseSupportFragment() {
    private val viewModel by sharedViewModel<TvViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        title = "PeerCast"

        headersState = HEADERS_DISABLED
        isHeadersTransitionOnBackEnabled = true

        val adapterHelper = CardAdapterHelper.Browse()
        adapter = adapterHelper.adapter
        onItemViewClickedListener = CardEventHandler(parentFragmentManager)

        setOnSearchClickedListener {
            parentFragmentManager.beginTransaction()
                .replace(android.R.id.content, SearchFragment())
                .addToBackStack(null)
                .commit()
        }

        lifecycleScope.launchWhenResumed {
            viewModel.ypChannelsFlow.collect { channels ->
                //Timber.d("-->$it")
                adapterHelper.setChannel(channels)
                //val n = channels.count { it.isNotNilId }
            }
        }

        LoadingFragment.start(parentFragmentManager)
    }
}