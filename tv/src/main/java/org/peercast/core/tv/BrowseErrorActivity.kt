package org.peercast.core.tv

import android.os.Bundle
import android.os.Handler
import androidx.fragment.app.FragmentActivity

/**
 * BrowseErrorActivity shows how to use ErrorFragment.
 */
class BrowseErrorActivity : FragmentActivity() {

    private lateinit var mErrorFragment: ErrorFragment
    private lateinit var mSpinnerFragment: LoadingFragment

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_peer_cast_tv)
        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                .replace(R.id.main_browse_fragment, LoadingFragment())
                .commitNow()
        }
        testError()
    }

    private fun testError() {
        mErrorFragment = ErrorFragment()
        supportFragmentManager
            .beginTransaction()
            .add(R.id.main_browse_fragment, mErrorFragment)
            .commit()

        mSpinnerFragment = LoadingFragment()
        supportFragmentManager
            .beginTransaction()
            .add(R.id.main_browse_fragment, mSpinnerFragment)
            .commit()

        val handler = Handler()
        handler.postDelayed({
            supportFragmentManager
                .beginTransaction()
                .remove(mSpinnerFragment)
                .commit()
            mErrorFragment.setErrorContent()
        }, TIMER_DELAY)
    }

    companion object {
        private val TIMER_DELAY = 3000L
        private val SPINNER_WIDTH = 100
        private val SPINNER_HEIGHT = 100
    }
}