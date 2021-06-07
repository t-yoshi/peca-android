package org.peercast.core.tv

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.coroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collect
import org.koin.androidx.viewmodel.ext.android.sharedViewModel
import org.peercast.core.lib.PeerCastRpcClient
import timber.log.Timber

class LoadingFragment : Fragment(), TvActivity.BackPressSupportFragment {
    private val viewModel by sharedViewModel<TvViewModel>()
    private var job: Job? = null
    private var client: PeerCastRpcClient? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        lifecycle.coroutineScope.launchWhenCreated {
            viewModel.rpcClientFlow.collect { client = it }
        }

        job = lifecycle.coroutineScope.launchWhenStarted {
            kotlin.runCatching {
                client?.getYPChannels() ?: throw IllegalStateException("client is null")
            }.onFailure {
                Timber.e(it)
                Toast.makeText(requireContext(), it.message, Toast.LENGTH_SHORT).show()
            }.onSuccess {
                viewModel.ypChannelsFlow.value = it
            }
            finish()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        return inflater.inflate(R.layout.tv_loading_fragment, container, false)
    }

    override fun onResume() {
        super.onResume()
        view?.findViewById<View>(android.R.id.progress)?.requestFocus()
    }

    private fun finish() {
        parentFragmentManager.beginTransaction()
            .remove(this)
            .commit()
    }

    override fun onBackPressed(): Boolean {
        job?.cancel()
        finish()
        return true
    }

    companion object {
        fun start(m: FragmentManager) {
            m.beginTransaction()
                .add(android.R.id.content, LoadingFragment())
                .commit()
        }
    }

}