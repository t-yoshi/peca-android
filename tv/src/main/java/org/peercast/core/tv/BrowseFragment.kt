package org.peercast.core.tv

import android.os.Bundle
import android.os.SystemClock
import androidx.leanback.app.BrowseSupportFragment
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.flow.collect
import org.koin.androidx.viewmodel.ext.android.sharedViewModel
import org.peercast.core.lib.rpc.YpChannel
import timber.log.Timber

class BrowseFragment : BrowseSupportFragment() {
    private val viewModel by sharedViewModel<TvViewModel>()
    private var nextAutoReloadRT = 0L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        nextAutoReloadRT = savedInstanceState?.getLong(STATE_NEXT_AUTO_RELOAD_RT) ?: 0L

        title = "PeerCast"

        headersState = HEADERS_DISABLED
        isHeadersTransitionOnBackEnabled = true

        val adapterHelper = CardAdapterHelper.Browse()
        adapter = adapterHelper.adapter
        onItemViewClickedListener = CardEventHandler(parentFragmentManager)

        setOnSearchClickedListener {
            SearchFragment.start(parentFragmentManager)
        }

        lifecycleScope.launchWhenResumed {
            viewModel.ypChannelsFlow.collect { channels ->
                //Timber.d("-->$it")
                if (channels !== emptyList<YpChannel>()) {
                    nextAutoReloadRT = SystemClock.elapsedRealtime() + AUTO_RELOAD_INTERVAL_MS
                }
                adapterHelper.setChannel(channels)
                //val n = channels.count { it.isNotNilId }
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putLong(STATE_NEXT_AUTO_RELOAD_RT, nextAutoReloadRT)
    }

    override fun onResume() {
        super.onResume()

        if (nextAutoReloadRT < SystemClock.elapsedRealtime()) {
            LoadingFragment.start(
                parentFragmentManager,
                //サービスに接続するまで時間がかかるので読込中を表示する
                viewModel.rpcClient.value == null
            )
        }
    }

    companion object {
        private const val AUTO_RELOAD_INTERVAL_MS = 3 * 60_000
        private const val STATE_NEXT_AUTO_RELOAD_RT = "BrowseFragment#nextAutoReloadRT"
    }
}