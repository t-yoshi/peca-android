package org.peercast.core.tv

import android.os.Bundle
import androidx.fragment.app.FragmentActivity
import androidx.leanback.app.BrowseSupportFragment
import androidx.leanback.widget.ArrayObjectAdapter
import androidx.leanback.widget.HeaderItem
import androidx.leanback.widget.ListRow
import androidx.leanback.widget.ListRowPresenter
import okhttp3.*
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.sharedViewModel
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.peercast.core.AppPreferences
import org.peercast.core.PeerCastViewModel
import org.peercast.core.R
import org.peercast.core.lib.internal.SquareUtils
import timber.log.Timber
import java.io.IOException

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
                .replace(android.R.id.content, BrowseFragment())
                .commitNow()
        }
    }

    class BrowseFragment : BrowseSupportFragment(){
        private val appPrefs by inject<AppPreferences>()
        private val viewModel by sharedViewModel<PeerCastViewModel>()

        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)
            title = "Channels"
            val rowsAdapter = ArrayObjectAdapter(ListRowPresenter())
            val cardPresenter = CardPresenter()

            val listRowAdapter = ArrayObjectAdapter(cardPresenter)
            val header1 = HeaderItem(0, "SP")
            rowsAdapter.add(ListRow(header1, listRowAdapter))
            val header2 = HeaderItem(0, "TP")
            rowsAdapter.add(ListRow(header2, listRowAdapter))
            loadYpChannels()
            adapter = rowsAdapter
        }

        private fun loadYpChannels(){
            viewModel.executeRpcCommand {
                Timber.d("->"+ it.getYPChannels());
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