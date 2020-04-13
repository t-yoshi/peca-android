package org.peercast.core

import android.annotation.SuppressLint
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Bundle
import android.view.*
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.appcompat.widget.SearchView
import androidx.core.content.edit
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.sharedViewModel
import org.peercast.core.yt.CgiRequestHandler
import timber.log.Timber
import kotlin.coroutines.CoroutineContext


/** YTのHTMLページから再生する。
 * @author (c) 2020, T Yoshizawa
 * @licenses Dual licensed under the MIT or GPL licenses.
 */
class YtWebViewFragment : Fragment(), PeerCastActivity.BackPressSupportFragment,
        SearchView.OnQueryTextListener {
    private val appPrefs by inject<AppPreferences>()
    private val viewModel by sharedViewModel<PeerCastViewModel>()
    private val webViewPrefs: SharedPreferences by lazy(LazyThreadSafetyMode.NONE) {
        context!!.getSharedPreferences("yt-webview", Context.MODE_PRIVATE)
    }

    private val webViewClient = object : WebViewClient() {
        private val requestHandler = CgiRequestHandler()

        override fun shouldInterceptRequest(view: WebView, request: WebResourceRequest): WebResourceResponse? {
            return requestHandler.shouldInterceptRequest(request)
        }

        private val RE_LOCAL_HOST = """https?://(localhost|127\.0\.0\.1)(:\d+)?/.*$""".toRegex()

        @Suppress("DEPRECATION")
        //NOTE: Android 6以下ではshouldOverrideUrlLoading(view, request)は機能しない
        override fun shouldOverrideUrlLoading(view: WebView, url: String): Boolean {
            if (url.matches(RE_LOCAL_HOST))
                return false

            //外部サイトはブラウザで開く
            try {
                startActivity(
                        Intent(Intent.ACTION_VIEW, Uri.parse(url))
                )
            } catch (e: ActivityNotFoundException){
                Timber.w(e)
            }
            return true
        }

        private val RE_PAGES = """(index|channels|connections|settings|viewlog|notifications|rtmp|speedtest)\.html""".toRegex()

        override fun onPageFinished(view: WebView, url: String?) {
            //Timber.d("onPageFinished: $url")
            activity?.let { a ->
                a.actionBar?.title = view.title
                a.invalidateOptionsMenu()
            }
            if (url != null && RE_PAGES.find(url) != null) {
                webViewPrefs.edit {
                    putString(KEY_LAST_URL, url)
                }
            }
        }
    }

    private val view: WebView? get() = super.getView() as WebView?


    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        setHasOptionsMenu(arguments?.getBoolean(ARG_HAS_OPTION_MENU) != false)

        return WebView(inflater.context).also { wv ->
            wv.webViewClient = webViewClient
            wv.settings.javaScriptEnabled = true
            wv.settings.domStorageEnabled = true

            if (savedInstanceState == null) {
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
            } else {
                wv.restoreState(savedInstanceState)
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        view?.saveState(outState)
    }

    override fun onBackPressed(): Boolean {
        if (view?.canGoBack() == true) {
            view?.goBack()
            return true
        }
        return false
    }

    override fun onPause() {
        super.onPause()
        view?.onPause()
    }

    override fun onResume() {
        super.onResume()
        view?.onResume()
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.yt_webview_menu, menu)
        (menu.findItem(R.id.menu_search).actionView as SearchView)
                .setOnQueryTextListener(this)
    }

    override fun onPrepareOptionsMenu(menu: Menu) {
        val reSearchVisibleUrl = """/(channels|play)\.html""".toRegex()
        if (view?.url?.let { reSearchVisibleUrl.find(it) } != null) {
            menu.findItem(R.id.menu_search).isVisible = true
            menu.findItem(R.id.menu_reload).setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER)
        } else {
            menu.findItem(R.id.menu_search).isVisible = false
            menu.findItem(R.id.menu_reload).setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM)
        }
//        menu.findItem(R.id.menu_forward).isEnabled = view?.canGoForward() == true
//        menu.findItem(R.id.menu_back).isEnabled = view?.canGoBack() == true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
//            R.id.menu_back -> view?.goBack()
//            R.id.menu_forward -> view?.goForward()
            R.id.menu_reload -> view?.reload()
            else -> return super.onOptionsItemSelected(item)
        }
        return true
    }

    override fun onQueryTextChange(newText: String?): Boolean {
        view?.findAllAsync(newText)
        return true
    }

    override fun onQueryTextSubmit(query: String?): Boolean {
        return true
    }

    companion object {
        fun create(path: String = "", hasOptionsMenu: Boolean = true): YtWebViewFragment {
            return YtWebViewFragment().also { f ->
                f.arguments = Bundle().also { b ->
                    b.putString(ARG_PATH, path)
                    b.putBoolean(ARG_HAS_OPTION_MENU, hasOptionsMenu)
                }
            }
        }

        //最後に見たページを保存
        private const val KEY_LAST_URL = "last-url"

        private const val ARG_PATH = "path"
        private const val ARG_HAS_OPTION_MENU = "hasOptionsMenu"
    }
}