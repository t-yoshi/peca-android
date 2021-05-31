package org.peercast.core

import android.annotation.SuppressLint
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.res.Configuration
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.view.*
import android.webkit.*
import android.widget.ProgressBar
import androidx.appcompat.widget.SearchView
import androidx.core.content.edit
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.sharedViewModel
import org.peercast.core.yt.CgiRequestHandler
import timber.log.Timber


/**
 *  YTのHTMLページから再生する。
 * @author (c) 2020, T Yoshizawa
 * @licenses Dual licensed under the MIT or GPL licenses.
 */
class YtWebViewFragment : Fragment(), PeerCastActivity.BackPressSupportFragment,
        PeerCastActivity.NestedScrollFragment,
        SearchView.OnQueryTextListener {

    private val appPrefs by inject<AppPreferences>()
    private val viewModel by sharedViewModel<PeerCastViewModel>()
    private val webViewPrefs: SharedPreferences by lazy(LazyThreadSafetyMode.NONE) {
        requireContext().getSharedPreferences("yt-webview", Context.MODE_PRIVATE)
    }
    private val activity get() = super.getActivity() as? PeerCastActivity?
    private var vWebView: WebView? = null

    private val webViewClient = object : WebViewClient() {
        private val requestHandler = CgiRequestHandler()

        override fun shouldInterceptRequest(view: WebView, request: WebResourceRequest): WebResourceResponse? {
            return requestHandler.shouldInterceptRequest(request)
        }

        private val RE_LOCAL_HOST = """(localhost|127\.0\.0\.1)""".toRegex()

        @Suppress("DEPRECATION")
        //NOTE: Android 6以下ではshouldOverrideUrlLoading(view, request)は機能しない
        override fun shouldOverrideUrlLoading(view: WebView, url_: String): Boolean {
            val url = Uri.parse(url_)
            if (url.host?.matches(RE_LOCAL_HOST) == true
                    && url.path?.startsWith("/pls/") != true)
                return false

            //外部サイト or プレイリストは外部アプリで開く
            try {
                startActivity(
                        Intent(Intent.ACTION_VIEW, url)
                )
            } catch (e: ActivityNotFoundException) {
                viewModel.notificationMessage.postValue(e.toString())
            }
            return true
        }

        override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
            setProgress(0)
        }

        private val RE_PAGES = """(index|channels|connections|settings|viewlog|notifications|rtmp|speedtest)\.html""".toRegex()

        override fun onPageFinished(view: WebView, url: String) {
            //Timber.d("onPageFinished: $url")
            setProgress(-1)
            activity?.run {
                val isPlayPage = "play.html" in url
                if (isPlayPage)
                    collapsedAppBarUnlessEnoughHeight()

                supportActionBar?.setDisplayHomeAsUpEnabled(isPlayPage)
                invalidateOptionsMenu()
            }

            if (RE_PAGES.find(url) != null) {
                webViewPrefs.edit {
                    putString(KEY_LAST_URL, url)
                }
            }
        }
    }

    private val chromeClient = object : WebChromeClient() {
        override fun onProgressChanged(view: WebView?, newProgress: Int) {
            setProgress(newProgress)
        }

        override fun onReceivedTitle(view: WebView, title: String) {
            activity?.supportActionBar?.title = title.substringBefore(" - ")
        }
    }

    init {
        arguments = Bundle().apply {
            putString(ARG_PATH, "")
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        setHasOptionsMenu(true)
        return inflater.inflate(R.layout.yt_webview_fragment, container, false)
    }

    @SuppressLint("SetJavaScriptEnabled")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        Timber.d("savedInstanceState=$savedInstanceState")
        view.findViewById<WebView>(R.id.vWebView).let { wv ->
            vWebView = wv
            wv.webViewClient = webViewClient
            wv.webChromeClient = chromeClient
            with(wv.settings) {
                javaScriptEnabled = true
                //domStorageEnabled = true
                mediaPlaybackRequiresUserGesture = false
            }
            if (savedInstanceState != null && savedInstanceState.getBoolean(STATE_IS_PLAYING)) {
                //再生時にだけstateから復元する
                wv.restoreState(savedInstanceState)
            } else {
                viewModel.isServiceBoundLiveData.observe(viewLifecycleOwner) { b ->
                    if (b) {
                        val lastUrl = webViewPrefs.getString(KEY_LAST_URL, null) ?: ""
                        val path = listOf(
                                arguments?.getString(ARG_PATH),
                                Uri.parse(lastUrl).path,
                                "/"
                        ).first { !it.isNullOrEmpty() }
                        wv.loadUrl("http://127.0.0.1:${appPrefs.port}$path")
                    }
                }
            }
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        activity?.collapsedAppBarUnlessEnoughHeight()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        vWebView?.let {
            it.saveState(outState)
            outState.putBoolean(STATE_IS_PLAYING, "play.html" in "${it.url?.toString()}")
        }
    }

    override fun onBackPressed(): Boolean {
        if (vWebView?.canGoBack() == true) {
            vWebView?.goBack()
            return true
        }
        return false
    }

    override fun onPause() {
        super.onPause()
        vWebView?.onPause()
    }

    override fun onResume() {
        super.onResume()
        vWebView?.onResume()
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.yt_webview_menu, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
//            R.id.menu_back -> view?.goBack()
//            R.id.menu_forward -> view?.goForward()
            R.id.menu_reload -> vWebView?.reload()
            else -> return false
        }
        return true
    }

    override fun onQueryTextChange(newText: String?): Boolean {
        vWebView?.findAllAsync(newText)
        return true
    }

    override fun onQueryTextSubmit(query: String?): Boolean {
        return true
    }

    private fun setProgress(value: Int) {
        activity?.findViewById<ProgressBar>(R.id.vProgress)?.let {
            it.progress = value
            it.isVisible = value in 1..99
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        vWebView?.destroy()
        vWebView = null
    }

    companion object {
        //最後に見たページを保存
        private const val KEY_LAST_URL = "last-url"
        private const val STATE_IS_PLAYING = "is-playing"

        /**(String)*/
        const val ARG_PATH = "path"
    }
}