package org.peercast.core.tv

import android.os.Bundle
import androidx.fragment.app.FragmentActivity
import timber.log.Timber

/**
 * Loads [MainFragment].
 */
class PeerCastTvActivity : FragmentActivity() {
    // private val viewModel by viewModel<PeerCastTvViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

//        setContentView(R.layout.activity_peer_cast_tv)
        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
//                .replace(android.R.id.content, YtWebViewFragment())
                .replace( android.R.id.content, BrowseFragment())
//                .replace(android.R.id.content, MainFragment())
//                .addToBackStack(null)

                .commitNow()
        }
    }

    override fun onBackPressed() {
        with(supportFragmentManager){
           if (backStackEntryCount > 0)
               popBackStack()
           else
               super.onBackPressed()
        }
    }

}