package org.peercast.core.tv

import android.os.Bundle
import androidx.fragment.app.FragmentActivity
import org.peercast.core.R

/**
 * Loads [MainFragment].
 */
class PeerCastTvActivity : FragmentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_peer_cast_tv)
        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                .replace(R.id.main_browse_fragment, MainFragment())
                .commitNow()
        }
    }
}