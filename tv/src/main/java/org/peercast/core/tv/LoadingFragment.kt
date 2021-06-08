package org.peercast.core.tv

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.coroutineScope
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collect
import org.koin.androidx.viewmodel.ext.android.sharedViewModel
import timber.log.Timber

class LoadingFragment : Fragment(), TvActivity.BackPressSupportFragment {
    private val viewModel by sharedViewModel<TvViewModel>()
    private var job: Job? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        lifecycle.coroutineScope.launchWhenCreated {
            viewModel.rpcClient.collect { client ->
                job?.cancel()
                if (client == null) {
                    job = launch {
                        //10秒以内にサービスに接続できないなら
                        delay(10_000)
                        finish()
                    }
                } else {
                    job = lifecycle.coroutineScope.launchWhenResumed {
                        kotlin.runCatching {
                            client.getYPChannels()
                        }.onFailure {
                            Timber.e(it)
                            Toast.makeText(requireContext(), it.message, Toast.LENGTH_SHORT).show()
                        }.onSuccess { channels ->
                            val bmAll = Bookmark(requireContext()).all()
                            viewModel.ypChannelsFlow.value = withContext(Dispatchers.IO){
                                channels.sortedWith { c1, c2 ->
                                    (c2.channelId in bmAll).compareTo(c1.channelId in bmAll)
                                }
                            }
                        }
                        finish()
                    }
                }
            }
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

    override fun onDestroy() {
        super.onDestroy()
        job?.cancel()
    }

    private fun finish() {
        parentFragmentManager.beginTransaction()
            .remove(this)
            .commit()
    }

    override fun onBackPressed(): Boolean {
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