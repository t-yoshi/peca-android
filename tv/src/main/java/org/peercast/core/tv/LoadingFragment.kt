package org.peercast.core.tv
/**
 * @author (c) 2014-2021, T Yoshizawa
 * @licenses Dual licensed under the MIT or GPL licenses.
 */
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.view.isInvisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.coroutineScope
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collect
import okhttp3.Request
import org.koin.androidx.viewmodel.ext.android.sharedViewModel
import org.peercast.core.lib.PeerCastRpcClient
import org.peercast.core.lib.internal.SquareUtils
import org.peercast.core.lib.internal.SquareUtils.runAwait
import timber.log.Timber
import java.io.IOException

class LoadingFragment : Fragment() {
    private val viewModel by sharedViewModel<TvViewModel>()
    private var job: Job? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        lifecycle.coroutineScope.launchWhenCreated {
            viewModel.rpcClient.collect { client ->
                job?.cancel()
                job = if (client == null) {
                    launch {
                        //10秒以内にサービスに接続できないなら
                        delay(10_000)
                        viewModel.showInfoToast("service could not be connected.")
                        finishFragment()
                    }
                } else {
                    lifecycle.coroutineScope.launchWhenResumed {
                        if (requireArguments().getBoolean(ARG_IS_FORCE_RELOAD))
                            cmdFetchFeeds()
                        loadYellowPages(client)
                        finishFragment()
                    }
                }
            }
        }
    }

    private suspend fun cmdFetchFeeds(){
        val u = "http://127.0.0.1:${viewModel.prefs.port}/admin?cmd=fetch_feeds"
        try {
            val req = Request.Builder().url(u).build()
            SquareUtils.okHttpClient.newCall(req).runAwait { res->
                Timber.i("fetch_feeds: ${res.code}")
            }
        } catch (e: IOException){
            Timber.e(e, "connect failed: $u")
        }
    }

    private suspend fun loadYellowPages(client: PeerCastRpcClient){
        kotlin.runCatching {
            client.getYPChannels()
        }.onFailure {
            Timber.e(it)
            viewModel.showInfoToast(it.message ?: "(null)", Toast.LENGTH_SHORT)
        }.onSuccess { channels ->
            val cmp = viewModel.bookmark.comparator()
            viewModel.ypChannelsFlow.value = withContext(Dispatchers.IO) {
                channels.sortedWith(cmp)
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

        fun start(fm: FragmentManager, isShowSpinner: Boolean = true, isForceReload: Boolean = false) {
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