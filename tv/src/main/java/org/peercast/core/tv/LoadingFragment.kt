package org.peercast.core.tv

/**
 * @author (c) 2014-2021, T Yoshizawa
 * @licenses Dual licensed under the MIT or GPL licenses.
 */
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isInvisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.coroutineScope
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkManager
import io.ktor.client.request.*
import io.ktor.client.statement.*
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull
import org.koin.androidx.viewmodel.ext.android.sharedViewModel
import org.peercast.core.tv.util.finishFragment
import org.peercast.core.tv.util.ktorHttpClient
import org.peercast.core.tv.yp.YpChannelsFlow
import org.peercast.core.tv.yp.YpLoadingWorker
import timber.log.Timber
import java.io.IOException

class LoadingFragment : Fragment() {
    private val viewModel by sharedViewModel<TvViewModel>()
    private var job: Job? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        WorkManager.getInstance(requireContext()).beginUniqueWork(
            "yp_loading_work", ExistingWorkPolicy.KEEP,
            OneTimeWorkRequest.Builder(YpLoadingWorker::class.java)
                .build()
        ).enqueue()

        lifecycle.coroutineScope.launchWhenCreated {
            var timeout = 10_000L
            if (requireArguments().getBoolean(ARG_IS_FORCE_RELOAD)) {
                cmdFetchFeeds()
                timeout += 5_000L
            }

            val isTimeout = withTimeoutOrNull(timeout) {
                viewModel.ypChannels.first { it !== YpChannelsFlow.INIT_LIST }
            } == null

            if (isTimeout) {
                viewModel.showInfoToast("yellwpages loading failed.")
            }

            finishFragment()
        }
    }

    private suspend fun cmdFetchFeeds() {
        val u = "http://127.0.0.1:${viewModel.prefs.port}/admin?cmd=fetch_feeds"
        try {
            val res = ktorHttpClient.get<HttpResponse>(u)
            Timber.i("fetch_feeds: ${res.status}")
        } catch (e: IOException) {
            Timber.e(e, "connect failed: $u")
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        return inflater.inflate(R.layout.tv_loading_fragment, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        view.isInvisible = !requireArguments().getBoolean(ARG_SHOW_SPINNER)
    }

    override fun onResume() {
        super.onResume()
        view?.findViewById<View>(android.R.id.progress)?.requestFocus()
    }

    override fun onDestroy() {
        super.onDestroy()
        job?.cancel()
    }

    companion object {
        private const val ARG_SHOW_SPINNER = "show-spinner"
        private const val ARG_IS_FORCE_RELOAD = "force-reload"

        fun start(
            fm: FragmentManager,
            isShowSpinner: Boolean = true,
            isForceReload: Boolean = false,
        ) {
            val f = LoadingFragment()
            f.arguments = Bundle(2).also {
                it.putBoolean(ARG_SHOW_SPINNER, isShowSpinner)
                it.putBoolean(ARG_IS_FORCE_RELOAD, isForceReload)
            }
            fm.beginTransaction()
                .add(android.R.id.content, f)
                .commit()
        }
    }

}