package org.peercast.core.ui

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.view.*
import android.webkit.*
import androidx.appcompat.widget.SearchView
import androidx.core.content.edit
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.filterNotNull
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.sharedViewModel
import org.peercast.core.common.AppPreferences
import org.peercast.core.ui.databinding.WebViewFragmentBinding
import org.peercast.core.ui.yt.CgiRequestHandler


/**
 *  YTのHTMLページから再生する。
 * @author (c) 2020, T Yoshizawa
 * @licenses Dual licensed under the MIT or GPL licenses.
 */
class WebViewFragment : Fragment(), PeerCastActivity.FragmentCallback,
    SearchView.OnQueryTextListener {

    private val appPrefs by inject<AppPreferences>()
    private val viewModel by sharedViewModel<UiViewModel>()
    private lateinit var webViewPrefs: SharedPreferences
    private var lastVisitedPath = ""
    private val progress = MutableStateFlow(0)
    private lateinit var binding: WebViewFragmentBinding
    private val activity get() = super.getActivity() as PeerCastActivity?

    private val wvClient = object : WebViewClient() {
        private val requestHandler = CgiRequestHandler()

        override fun shouldInterceptRequest(
            view: WebView,
            request: WebResourceRequest,
        ): WebResourceResponse? {
            return requestHandler.shouldInterceptRequest(request)
        }

        private val RE_LOCAL_HOST = """(localhost|127\.0\.0\.1)""".toRegex()

        @Suppress("DEPRECATION")
        //NOTE: Android 6以下ではshouldOverrideUrlLoading(view, request)は機能しない
        override fun shouldOverrideUrlLoading(view: WebView, url_: String): Boolean {
            val url = Uri.parse(url_)
            if (url.host?.matches(RE_LOCAL_HOST) == true
                && url.path?.startsWith("/pls/") != true
            )
                return false

            //外部サイト or プレイリストは外部アプリで開く
            try {
                startActivity(
                    Intent(Intent.ACTION_VIEW, url)
                )
            } catch (e: RuntimeException) {
                viewModel.notificationMessage.value = e.toString()
            }
            return true
        }

        override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
            progress.value = 0
        }

        private val RE_PAGES =
            """(index|channels|connections|settings|viewlog|notifications|rtmp|speedtest)\.html""".toRegex()

        override fun onPageFinished(view: WebView, url: String) {
            //Timber.d("onPageFinished: $url")
            progress.value = 0

            activity?.run {
                when ("play.html" in url) {
                    true -> collapseAppBar()
                    else -> expandAppBar()
                }
                invalidateOptionsMenu()
            }

            if (RE_PAGES.find(url) != null) {
                if (Uri.parse(url).path == lastVisitedPath) {
                    view.clearHistory()
                }

                webViewPrefs.edit {
                    putString(KEY_LAST_PATH, Uri.parse(url).path)
                }
            }
        }

        override fun onReceivedError(
            view: WebView,
            errorCode: Int,
            description: String,
            failingUrl: String,
        ) {
            //ポート変更後
            if (errorCode == ERROR_CONNECT && "settings.html" in failingUrl) {
                view.loadUrl("http://127.0.0.1:${appPrefs.port}/")
            }
        }
    }

    private val chClient = object : WebChromeClient() {
        override fun onProgressChanged(view: WebView?, newProgress: Int) {
            progress.value = newProgress
        }

        override fun onReceivedTitle(view: WebView, title: String) {
            activity?.title = title.substringBefore(" - ")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        webViewPrefs = requireContext().getSharedPreferences("yt-webview", Context.MODE_PRIVATE)
        setHasOptionsMenu(true)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        return WebViewFragmentBinding.inflate(inflater, container, false).let {
            binding = it
            it.progress = progress
            it.lifecycleOwner = viewLifecycleOwner
            it.root
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        with(binding.vWebView) {
            webViewClient = wvClient
            webChromeClient = chClient
            with(settings) {
                javaScriptEnabled = true
                mediaPlaybackRequiresUserGesture = false
            }
            if (savedInstanceState != null && savedInstanceState.getBoolean(STATE_IS_PLAYING)) {
                //再生時にだけstateから復元する
                restoreState(savedInstanceState)
            } else {
                lifecycleScope.launchWhenCreated {
                    viewModel.rpcClient.filterNotNull().collect {
                        lastVisitedPath = webViewPrefs.getString(KEY_LAST_PATH, null) ?: "/"
                        loadUrl("http://127.0.0.1:${appPrefs.port}$lastVisitedPath")
                    }
                }
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        binding.vWebView.let {
            it.saveState(outState)
            outState.putBoolean(STATE_IS_PLAYING, "play.html" in "${it.url}")
        }
    }

    override fun onPause() {
        super.onPause()
        binding.vWebView.onPause()
    }

    override fun onResume() {
        super.onResume()
        binding.vWebView.onResume()
    }

    override fun onBackPressed(): Boolean {
        if (binding.vWebView.canGoBack()) {
            binding.vWebView.goBack()
            return true
        }
        return false
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.webview_menu, menu)
        val sv = menu.findItem(R.id.menu_search)?.actionView as? SearchView
        sv?.setOnQueryTextListener(this)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
//            R.id.menu_back -> view?.goBack()
//            R.id.menu_forward -> view?.goForward()
            R.id.menu_reload -> {
                binding.vWebView.run {
                    val u = Uri.parse("$url")
                    loadUrl("http://127.0.0.1:${appPrefs.port}${u.path}")
                }
            }
            else -> return false
        }
        return true
    }

    override fun onQueryTextChange(newText: String): Boolean {
        binding.vWebView.findAllAsync(newText)
        return true
    }

    override fun onQueryTextSubmit(query: String?): Boolean {
        return true
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding.vWebView.destroy()
    }

    companion object {
        //最後に見たページを保存
        private const val KEY_LAST_PATH = "last-path"
        private const val STATE_IS_PLAYING = "is-playing"
    }
}