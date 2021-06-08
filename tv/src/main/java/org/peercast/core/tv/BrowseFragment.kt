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

class BrowseFragment : BrowseSupportFragment() {
    private val viewModel by sharedViewModel<TvViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        title = "PeerCast"

        headersState = HEADERS_DISABLED
        isHeadersTransitionOnBackEnabled = true

        val adapterHelper = CardAdapterHelper.Browse(parentFragmentManager)
        adapter = adapterHelper.adapter
        onItemViewClickedListener = adapterHelper

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