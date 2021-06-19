package org.peercast.core.tv
/**
 * @author (c) 2014-2021, T Yoshizawa
 * @licenses Dual licensed under the MIT or GPL licenses.
 */
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

        if (!requireContext().isFireTv) {
            setOnSearchClickedListener {
                SearchFragment.start(parentFragmentManager)
            }
        }

        lifecycleScope.launchWhenResumed {
            viewModel.ypChannels.collect { channels ->
                //Timber.d("-->$it")
                adapterHelper.setChannel(channels)
                //val n = channels.count { it.isNotNilId }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        LoadingFragment.start(
            parentFragmentManager,
            //サービスに接続するまで時間がかかるので読込中を表示する
            viewModel.rpcClient.value == null
        )
    }
}