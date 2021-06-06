package org.peercast.core.tv

import android.os.Bundle
import androidx.fragment.app.FragmentActivity

@Deprecated("")
class SearchActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(android.R.id.content, SearchFragment())
                .commitNow()
        }
    }

    companion object {
        const val EX_YP_CHANNELS = "yp-channels" //
    }


}