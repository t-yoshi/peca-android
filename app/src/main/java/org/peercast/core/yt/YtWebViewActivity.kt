package org.peercast.core.yt

import android.os.Bundle
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.viewModel
import org.peercast.core.AppPreferences
import org.peercast.core.PeerCastViewModel
import timber.log.Timber

class YtWebViewActivity : AppCompatActivity() {
    private val appPrefs by inject<AppPreferences>()
    private val viewModel by viewModel<PeerCastViewModel>()

    private val webViewClient = object : WebViewClient() {
        override fun shouldInterceptRequest(view: WebView, request: WebResourceRequest): WebResourceResponse? {
            Timber.d("${request.url}")
            //WebResourceResponse()
            return null
        }
    }

    // http://127.0.0.1:7145/cgi-bin/board.cgi?category=game&board_num=48946

    private lateinit var webView: WebView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        webView = WebView(this).also {
            it.webViewClient = webViewClient
            it.settings.javaScriptEnabled = true

            viewModel.isBoundService.observe(this, Observer { b ->
                if (it.url.isNullOrEmpty() && b) {
                    //v.loadUrl("http://www.google.com/")
                    it.loadUrl("http://127.0.0.1:${appPrefs.port}/")
                }
            })
        }
        setContentView(webView)
    }

    override fun onBackPressed() {
        if (webView.canGoBack())
            webView.goBack()
        else
            super.onBackPressed()
    }

}