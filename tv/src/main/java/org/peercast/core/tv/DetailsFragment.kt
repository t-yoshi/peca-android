package org.peercast.core.tv

import android.annotation.SuppressLint
import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.SystemClock
import android.view.KeyEvent
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.leanback.app.DetailsSupportFragment
import androidx.leanback.app.DetailsSupportFragmentBackgroundController
import androidx.leanback.widget.*
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import okhttp3.Call
import okhttp3.Callback
import okhttp3.Request
import okhttp3.Response
import org.koin.androidx.viewmodel.ext.android.sharedViewModel
import org.peercast.core.lib.internal.SquareUtils
import org.peercast.core.lib.isNilId
import org.peercast.core.lib.isNotNilId
import org.peercast.core.lib.rpc.YpChannel
import org.peercast.core.lib.toStreamIntent
import org.unbescape.html.HtmlEscape
import timber.log.Timber
import java.io.IOException

class DetailsFragment : DetailsSupportFragment(), OnActionClickedListener,
    BaseOnItemViewSelectedListener<Any> {
    private val viewModel by sharedViewModel<TvViewModel>()
    private lateinit var detailsBackground: DetailsSupportFragmentBackgroundController
    private lateinit var ypChannel: YpChannel
    private lateinit var adapter: ArrayObjectAdapter
    private val presenterSelector = ClassPresenterSelector()
    private val actionAdapter = ArrayObjectAdapter()
    private val bookmark by lazy { Bookmark(requireContext()) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        detailsBackground = DetailsSupportFragmentBackgroundController(this)

        ypChannel = requireNotNull(
            requireArguments().getParcelable(ARG_YP_CHANNEL)
        )

        //Timber.i("->$ypChannel")
        adapter = ArrayObjectAdapter(presenterSelector)

        setupDetailsOverviewRowPresenter()
        setupDetailsOverviewRow()
        setAdapter(adapter)

        startAutoPlay()
    }

    private var preloadCall: Call? = null

    private fun startAutoPlay() {
        if (ypChannel.isNilId || preloadCall != null)
            return

        val i = ypChannel.toStreamIntent(viewModel.prefs.port)
        val req = Request.Builder().url(i.dataString!!).build()

        val playStartET = SystemClock.elapsedRealtime() + AUTO_PLAY_WAIT_MSEC

        preloadCall = SquareUtils.okHttpClient.newCall(req).also {
            it.enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    Timber.w(e)
                    lifecycleScope.launch {
                        viewModel.showInfoToast(e.toString())
                    }
                }

                override fun onResponse(call: Call, response: Response) {
                    lifecycleScope.launch {
                        delay(playStartET - SystemClock.elapsedRealtime())
                        if (preloadCall?.isCanceled() != true)
                            viewModel.startPlayer(this@DetailsFragment, ypChannel)
                    }
                }
            })
        }
    }

    private fun getBookmarkLabel() : String {
        return when (bookmark.exists(ypChannel)){
            true -> "Unbookmark"
            else -> "Bookmark"
        }
    }

    private fun setupDetailsOverviewRow() {

        if (ypChannel.isNotNilId) {
            actionAdapter.add(
                Action(ID_PLAY, "Play")
            )
            actionAdapter.add(
                Action(ID_BOOKMARK, getBookmarkLabel()).also {
                    it.addKeyCode(KeyEvent.KEYCODE_BOOKMARK)
                }
            )
        }

        actionAdapter.add(
            Action(ID_CONTACT, "Contact")
        )

        val row = DetailsOverviewRow(ypChannel)
        row.actionsAdapter = actionAdapter
        adapter.add(row)
    }


    private fun setupDetailsOverviewRowPresenter() {
        // Set detail background.
        val detailsPresenter = FullWidthDetailsOverviewRowPresenter(DetailsDescriptionPresenter())

        detailsPresenter.backgroundColor =
            ContextCompat.getColor(requireActivity(), R.color.default_background)

        // Hook up transition element.
//        val sharedElementHelper = FullWidthDetailsOverviewSharedElementHelper()
//        sharedElementHelper.setSharedElementEnterTransition(
//            activity, DetailsActivity.SHARED_ELEMENT_NAME
//        )
//        detailsPresenter.setListener(sharedElementHelper)
//        detailsPresenter.isParticipatingEntranceTransition = true

        setOnItemViewSelectedListener(this)

        detailsPresenter.onActionClickedListener = this
        presenterSelector.addClassPresenter(DetailsOverviewRow::class.java, detailsPresenter)
        //adapter.notifyArrayItemRangeChanged(0, 1)
    }

    override fun onItemSelected(
        itemViewHolder: Presenter.ViewHolder?,
        item: Any?,
        rowViewHolder: RowPresenter.ViewHolder?,
        row: Any?,
    ) {
        //Timber.d("-->$item")
        if (preloadCall?.isCanceled() != true && item is Action && item.id != ID_PLAY) {
            Timber.i("Cancel preloading for automatic playing")
            preloadCall?.cancel()
        }
    }

    override fun onActionClicked(action: Action) {
        Timber.d("-->$action")
        when (action.id) {
            ID_PLAY -> {
                preloadCall?.cancel()
                viewModel.startPlayer(this, ypChannel)
            }
            ID_BOOKMARK -> {
                bookmark.toggle(ypChannel)
                action.label1 = getBookmarkLabel()
                actionAdapter.notifyArrayItemRangeChanged(1, 1)
            }
            ID_CONTACT -> {
                try {
                    val u = Uri.parse(ypChannel.contactUrl)
                    if (u != Uri.EMPTY) {
                        val i = Intent(Intent.ACTION_VIEW, u)
                        startActivity(i)
                    }
                } catch (e: ActivityNotFoundException) {
                    Timber.e(e)
                    Toast.makeText(requireContext(), e.message, Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private class DetailsDescriptionPresenter : AbstractDetailsDescriptionPresenter() {
        @SuppressLint("SetTextI18n")
        override fun onBindDescription(
            viewHolder: ViewHolder,
            item: Any,
        ) {
            with(item as YpChannel) {
                viewHolder.title.text = name.unescapeHtml()
                viewHolder.subtitle.text = genre.unescapeHtml()
                viewHolder.body.text =
                    "$comment $description".unescapeHtml().trim() +
                            " ($listeners/$relays) $contentType"
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        preloadCall?.cancel()
    }

    companion object {
        private const val ID_PLAY = 0L
        private const val ID_BOOKMARK = 1L
        private const val ID_CONTACT = 2L

        private const val AUTO_PLAY_WAIT_MSEC = 5000

        private const val ARG_YP_CHANNEL = "yp-channel"

        fun start(fm: FragmentManager, ypChannel: YpChannel){
            val f = DetailsFragment()
            f.arguments = Bundle().also {
                it.putParcelable(ARG_YP_CHANNEL, ypChannel)
            }
            fm.beginTransaction()
                .replace(android.R.id.content, f)
                .addToBackStack(null)
                .commit()
        }

    }
}