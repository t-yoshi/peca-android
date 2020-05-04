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
import androidx.appcompat.widget.SearchView
import androidx.core.content.edit
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import kotlinx.android.synthetic.main.yt_webview_fragment.*
import kotlinx.android.synthetic.main.yt_webview_fragment.view.*
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
        SearchView.OnQueryTextListener {

    private val appPrefs by inject<AppPreferences>()
    private val viewModel by sharedViewModel<PeerCastViewModel>()
    private val webViewPrefs: SharedPreferences by lazy(LazyThreadSafetyMode.NONE) {
        requireContext().getSharedPreferences("yt-webview", Context.MODE_PRIVATE)
    }
    private val activity get() = super.getActivity() as PeerCastActivity?

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
            activity?.progressValue = 0
        }

        private val RE_PAGES = """(index|channels|connections|settings|viewlog|notifications|rtmp|speedtest)\.html""".toRegex()

        override fun onPageFinished(view: WebView, url: String) {
            //Timber.d("onPageFinished: $url")
            activity?.run {
                progressValue = -1

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
            activity?.progressValue = newProgress
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
        view.vWebView.let { wv ->
            wv.webViewClient = webViewClient
            wv.webChromeClient = chromeClient
            wv.settings.javaScriptEnabled = true
            //wv.settings.domStorageEnabled = true

            if (savedInstanceState != null) {
                wv.restoreState(savedInstanceState)
            } else {
                viewModel.isServiceBoundLiveData.observe(viewLifecycleOwner, Observer { b ->
                    if (b) {
                        val path = listOf(
                                arguments?.getString(ARG_PATH),
                                webViewPrefs.getString(KEY_LAST_URL, null)
                                        ?.let(Uri::parse)?.path,
                                "/"
                        ).first { !it.isNullOrEmpty() }
                        wv.loadUrl("http://127.0.0.1:${appPrefs.port}$path")
                    }
                })
            }
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        activity?.collapsedAppBarUnlessEnoughHeight()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        vWebView?.saveState(outState)
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
        vWebView.onPause()
    }

    override fun onResume() {
        super.onResume()
        vWebView.onResume()
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.yt_webview_menu, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
//            R.id.menu_back -> view?.goBack()
//            R.id.menu_forward -> view?.goForward()
            R.id.menu_reload -> vWebView.reload()
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

    override fun onDestroyView() {
        super.onDestroyView()
        vWebView.destroy()
    }

    companion object {
        //最後に見たページを保存
        private const val KEY_LAST_URL = "last-url"

        /**(String)*/
        const val ARG_PATH = "path"
    }
}