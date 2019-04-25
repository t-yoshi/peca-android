package org.peercast.core.yt

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.sharedViewModel
import org.peercast.core.AppPreferences
import org.peercast.core.PeerCastViewModel
import timber.log.Timber

class WebViewFragment : Fragment() {
    private val appPrefs by inject<AppPreferences>()
    private val viewModel by sharedViewModel<PeerCastViewModel>()

    private val webViewClient = object : WebViewClient(){
        override fun shouldInterceptRequest(view: WebView, request: WebResourceRequest): WebResourceResponse? {
            Timber.d("${request.url}")
            //WebResourceResponse()
            return null
        }
     }

    // http://127.0.0.1:7145/cgi-bin/board.cgi?category=game&board_num=48946

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel.channels
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return WebView(inflater.context).also { v->
            v.webViewClient = webViewClient
            v.settings.javaScriptEnabled = true

            viewModel.isBoundService.observe(viewLifecycleOwner, Observer { b->
                if (v.url.isNullOrEmpty() && b){
                    //v.loadUrl("http://www.google.com/")
                    v.loadUrl("http://127.0.0.1:${appPrefs.port}/")
                }
            })
        }
    }


}