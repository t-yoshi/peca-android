package org.peercast.core.tv

import android.os.Bundle
import androidx.fragment.app.FragmentActivity
import androidx.leanback.app.DetailsSupportFragment
import androidx.leanback.app.DetailsSupportFragmentBackgroundController
import androidx.leanback.widget.ArrayObjectAdapter
import androidx.leanback.widget.ClassPresenterSelector
import org.peercast.core.R
import org.peercast.core.lib.rpc.YpChannel

/**
 * Details activity class that loads [VideoDetailsFragment] class.
 */
class DetailsActivity : FragmentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_details)
        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(android.R.id.content, DetailsFragment())
                .commitNow();
        }
    }

    class DetailsFragment : DetailsSupportFragment() {
        private lateinit var detailsBackground: DetailsSupportFragmentBackgroundController
        private lateinit var ypChannel: YpChannel

        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)

            detailsBackground = DetailsSupportFragmentBackgroundController(this)

            ypChannel = requireActivity().intent.getParcelableExtra(EX_YP_CHANNEL)
            val mPresenterSelector = ClassPresenterSelector()
            val mAdapter = ArrayObjectAdapter(mPresenterSelector)
            //    setupDetailsOverviewRow()
            //    setupDetailsOverviewRowPresenter()
            //    setupRelatedMovieListRow()
            adapter = mAdapter
            //     initializeBackground(mSelectedMovie)
            //     onItemViewClickedListener = ItemViewClickedListener()
        }
    }

    companion object {
        const val EX_YP_CHANNEL = "yp-channel"
        const val SHARED_ELEMENT_NAME = "hero"
        const val MOVIE = "Movie"
    }
}