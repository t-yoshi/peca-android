package org.peercast.core.tv

import android.os.Bundle
import androidx.fragment.app.FragmentActivity
import androidx.leanback.app.BrowseSupportFragment
import androidx.leanback.widget.ArrayObjectAdapter
import androidx.leanback.widget.HeaderItem
import androidx.leanback.widget.ListRow
import androidx.leanback.widget.ListRowPresenter
import okhttp3.*
import okhttp3.internal.notifyAll
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.sharedViewModel
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.peercast.core.AppPreferences
import org.peercast.core.PeerCastViewModel
import org.peercast.core.R
import org.peercast.core.YtWebViewFragment
import org.peercast.core.lib.internal.SquareUtils
import org.peercast.core.lib.rpc.YpChannel
import timber.log.Timber
import java.io.IOException
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.LinkedHashMap

/**
 * Loads [MainFragment].
 */
class PeerCastTvActivity : FragmentActivity() {
    private val viewModel by viewModel<PeerCastViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
//        setContentView(R.layout.activity_peer_cast_tv)
        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
//                .replace(android.R.id.content, YtWebViewFragment())
                .replace(android.R.id.content, BrowseFragment())
//                .replace(android.R.id.content, MainFragment())
                .commitNow()
        }
    }

    class BrowseFragment : BrowseSupportFragment(){
        private val appPrefs by inject<AppPreferences>()
        private val viewModel by sharedViewModel<PeerCastViewModel>()
        private val rowsAdapter = ArrayObjectAdapter(ListRowPresenter())

        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)
            title = "Channels"

            loadYpChannels()
            headersState = HEADERS_DISABLED
            isHeadersTransitionOnBackEnabled = true
            adapter = rowsAdapter
        }

        private fun loadYpChannels(){
            val cardPresenter = CardPresenter2()
            viewModel.executeRpcCommand { client->
                rowsAdapter.clear()

                val channels = LinkedHashMap<String, ArrayList<YpChannel>>()
                client.getYPChannels().forEach { ch->
                    channels.getOrPut(
                        ch.yellowPage.removeSuffix("index.txt"),
                        {ArrayList()}
                    ).add(ch)
                }

                channels.onEachIndexed { i,e->
                    val listRowAdapter = ArrayObjectAdapter(cardPresenter)
                    val header = HeaderItem(i.toLong(), e.key)
                    listRowAdapter.addAll(0, e.value)
                    rowsAdapter.add(ListRow(header, listRowAdapter))
                }

                Timber.d("->$channels")
                //adapter.notifyItemRangeChanged(0, 1)
            }

//            val u = "http://127.0.0.1:${appPrefs.port}"
//            val req = Request.Builder().url(u).build()
//            SquareUtils.okHttpClient.newCall(req).enqueue(object : Callback {
//                override fun onResponse(call: Call, response: Response) {
//
//                }
//
//                override fun onFailure(call: Call, e: IOException) {
//
//                }
//            })
        }


    }
}