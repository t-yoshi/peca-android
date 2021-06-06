package org.peercast.core.tv

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.coroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import org.koin.androidx.viewmodel.ext.android.sharedViewModel

class LoadingFragment : Fragment(), TvActivity.BackPressSupportFragment {
    private val viewModel by sharedViewModel<TvViewModel>()
    private var job: Job? = null

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

        job = lifecycle.coroutineScope.launchWhenStarted {
            viewModel.rpcClientFlow.collect { client->
                if (client != null){
                    delay(2_000)
                    viewModel.ypChannelsFlow.value = client.getYPChannels()
                    finish()
                }
            }
        }
    }

    private fun finish(){
        parentFragmentManager.beginTransaction()
            .remove(this)
            .commit()
    }

    override fun onPause() {
        super.onPause()
        job?.cancel()
    }

    override fun onBackPressed(): Boolean {
        val j = job
        if (j != null && j.isActive) {
            j.cancel()
            finish()
            return true
        }
        return false
    }

}