package org.peercast.core.tv

/**
 * @author (c) 2014-2021, T Yoshizawa
 * @licenses Dual licensed under the MIT or GPL licenses.
 */
import android.os.Bundle
import androidx.fragment.app.FragmentActivity
import org.koin.androidx.viewmodel.ext.android.viewModel

class TvActivity : FragmentActivity() {
    private val viewModel by viewModel<TvViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(android.R.id.content, BrowseFragment())
                .commitNow()
        }
    }

    /**BackPressイベントを受け取るFragment*/
    interface BackPressSupportFragment {
        fun onBackPressed(): Boolean
    }

    override fun onBackPressed() {
        val m = supportFragmentManager
        val f = m.findFragmentById(android.R.id.content)
        if (f is BackPressSupportFragment && f.onBackPressed())
            return

        if (m.backStackEntryCount > 0)
            m.popBackStack()
        else
            super.onBackPressed()
    }

    override fun onStart() {
        super.onStart()
        viewModel.bindService()
    }

    override fun onStop() {
        super.onStop()
        viewModel.unbindService()
    }

}