package org.peercast.core.tv

import android.content.Intent
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.leanback.app.DetailsSupportFragment
import androidx.leanback.app.DetailsSupportFragmentBackgroundController
import androidx.leanback.widget.*
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.SimpleTarget
import com.bumptech.glide.request.transition.Transition
import org.peercast.core.R
import org.peercast.core.lib.rpc.YpChannel
import timber.log.Timber
import java.util.*

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
        private lateinit var adapter: ArrayObjectAdapter
        private val presenterSelector = ClassPresenterSelector()

        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)

            detailsBackground = DetailsSupportFragmentBackgroundController(this)

            ypChannel = requireActivity().intent.getParcelableExtra(EX_YP_CHANNEL)
            Timber.i("->$ypChannel")
            adapter = ArrayObjectAdapter(presenterSelector)
            //    setupDetailsOverviewRow()
            //    setupDetailsOverviewRowPresenter()
            //    setupRelatedMovieListRow()
            setupDetailsOverviewRowPresenter()
            setupDetailsOverviewRow()
            setAdapter(adapter)
            //     initializeBackground(mSelectedMovie)
            //     onItemViewClickedListener = ItemViewClickedListener()
        }

//        private fun setupRelatedMovieListRow() {
//            val subcategories = arrayOf(getString(R.string.related_movies))
//            val list = MovieList.list
//
//            Collections.shuffle(list)
//            val listRowAdapter = ArrayObjectAdapter(CardPresenter())
//            for (j in 0 until VideoDetailsFragment.NUM_COLS) {
//                listRowAdapter.add(list[j % 5])
//            }
//
//            val header = HeaderItem(0, subcategories[0])
//            //mAdapter.add(ListRow(header, listRowAdapter))
//            //mPresenterSelector.addClassPresenter(ListRow::class.java, ListRowPresenter())
//        }

        private fun setupDetailsOverviewRow() {
            //Log.d(VideoDetailsFragment.TAG, "doInBackground: " + mSelectedMovie?.toString())
            val row = DetailsOverviewRow(ypChannel)
            //row.imageDrawable = ContextCompat.getDrawable(requireActivity(), R.drawable.default_background)
//            val width = convertDpToPixel(requireActivity(), VideoDetailsFragment.DETAIL_THUMB_WIDTH)
//            val height = convertDpToPixel(requireActivity(),
//                VideoDetailsFragment.DETAIL_THUMB_HEIGHT)
//            Glide.with(requireActivity())
//                .load(mSelectedMovie?.cardImageUrl)
//                .centerCrop()
//                .error(R.drawable.default_background)
//                .into<SimpleTarget<Drawable>>(object : SimpleTarget<Drawable>(width, height) {
//                    override fun onResourceReady(
//                        drawable: Drawable,
//                        transition: Transition<in Drawable>?
//                    ) {
//                        Log.d(VideoDetailsFragment.TAG, "details overview card image url ready: " + drawable)
//                        row.imageDrawable = drawable
//                        mAdapter.notifyArrayItemRangeChanged(0, mAdapter.size())
//                    }
//                })

            val actionAdapter = ArrayObjectAdapter()

            actionAdapter.add(
                Action(
                    0L,
                    "Contact",
                )
            )
            actionAdapter.add(
                Action(
                    2L,
                    resources.getString(R.string.buy_1),
                    resources.getString(R.string.buy_2)
                )
            )

            row.actionsAdapter = actionAdapter

            adapter.add(row)
        }



        private fun setupDetailsOverviewRowPresenter() {
            // Set detail background.
            val detailsPresenter = FullWidthDetailsOverviewRowPresenter(DetailsDescriptionPresenter())
            detailsPresenter.backgroundColor =
                ContextCompat.getColor(requireActivity(), R.color.selected_background)

            // Hook up transition element.
            val sharedElementHelper = FullWidthDetailsOverviewSharedElementHelper()
            sharedElementHelper.setSharedElementEnterTransition(
                activity, DetailsActivity.SHARED_ELEMENT_NAME
            )
            detailsPresenter.setListener(sharedElementHelper)
            detailsPresenter.isParticipatingEntranceTransition = true

//            detailsPresenter.onActionClickedListener = OnActionClickedListener { action ->
//                if (action.id == VideoDetailsFragment.ACTION_WATCH_TRAILER) {
//                    val intent = Intent(requireActivity(), PlaybackActivity::class.java)
//                    intent.putExtra(DetailsActivity.MOVIE, mSelectedMovie)
//                    startActivity(intent)
//                } else {
//                    Toast.makeText(requireActivity(), action.toString(), Toast.LENGTH_SHORT).show()
//                }
//            }
            presenterSelector.addClassPresenter(DetailsOverviewRow::class.java, detailsPresenter)
            adapter.notifyArrayItemRangeChanged(0,1)
        }



        class DetailsDescriptionPresenter : AbstractDetailsDescriptionPresenter() {

            override fun onBindDescription(
                viewHolder: ViewHolder,
                item: Any,
            ) {
                val ypChannel = item as YpChannel

                viewHolder.title.text = ypChannel.name
                viewHolder.subtitle.text = ypChannel.comment
                viewHolder.body.text = ypChannel.description
            }
        }
    }

    companion object {
        const val EX_YP_CHANNEL = "yp-channel"
        const val SHARED_ELEMENT_NAME = "hero"
        const val MOVIE = "Movie"
    }
}