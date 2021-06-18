package org.peercast.core.tv
/**
 * @author (c) 2014-2021, T Yoshizawa
 * @licenses Dual licensed under the MIT or GPL licenses.
 */
import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.SystemClock
import android.view.KeyEvent
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentManager
import androidx.leanback.app.DetailsSupportFragment
import androidx.leanback.app.DetailsSupportFragmentBackgroundController
import androidx.leanback.widget.*
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import okhttp3.*
import okhttp3.internal.closeQuietly
import org.koin.androidx.viewmodel.ext.android.sharedViewModel
import org.peercast.core.lib.LibPeerCast.toStreamIntent
import org.peercast.core.lib.internal.SquareUtils
import org.peercast.core.lib.internal.SquareUtils.runAwait
import org.peercast.core.lib.rpc.YpChannel
import timber.log.Timber
import java.io.IOException
import java.lang.RuntimeException

class DetailsFragment : DetailsSupportFragment(), OnActionClickedListener,
    BaseOnItemViewSelectedListener<Any> {
    private val viewModel by sharedViewModel<TvViewModel>()
    private lateinit var detailsBackground: DetailsSupportFragmentBackgroundController
    private lateinit var ypChannel: YpChannel
    private lateinit var adapter: ArrayObjectAdapter
    private val presenterSelector = ClassPresenterSelector()
    private val actionAdapter = ArrayObjectAdapter()
    private var preloadJob: Job? = null

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


        //val bm = BitmapFactory.decodeResource(resources, R.drawable.megaphone)
        //detailsBackground.coverBitmap = bm
        detailsBackground
        detailsBackground.enableParallax()

        startAutoPlay()
    }

    private fun startAutoPlay() {
        if (ypChannel.isNilId || preloadJob != null)
            return

        val streamUrl = ypChannel.toStreamIntent(viewModel.prefs.port).dataString!!
        val req = Request.Builder().url(streamUrl)
            .cacheControl(CacheControl.FORCE_NETWORK).build()

        val playStartET = SystemClock.elapsedRealtime() + AUTO_PLAY_WAIT_MSEC

        val call = SquareUtils.okHttpClient.newCall(req)

        //プレーヤー起動前に少しストリームを読み込む
        preloadJob = lifecycleScope.launch {
            var retry = 2
            var err: String? = null
            while (--retry > 0) {
                Timber.d("retry to connect @$retry")
                try {
                    call.clone().runAwait { res ->
                        res.peekBody(1)
                    }
                    delay(playStartET - SystemClock.elapsedRealtime())
                    if (preloadJob?.isCancelled != true) {
                        PlayerLauncherFragment.start(parentFragmentManager, ypChannel)
                    }
                    return@launch
                } catch (e: IOException) {
                    Timber.w(e, "preload failed")
                    err = e.message
                }
            }

            if (err != null)
                viewModel.showInfoToast(err)
        }
    }


    private fun getBookmarkLabel(): String {
        return when (viewModel.bookmark.exists(ypChannel)) {
            true -> "Unbookmark"
            else -> "Bookmark"
        }
    }

    private fun setupDetailsOverviewRow() {
        val icon: Int
        if (ypChannel.isNotNilId) {
            actionAdapter.add(
                Action(ID_PLAY, "Play")
            )
            actionAdapter.add(
                Action(ID_BOOKMARK, getBookmarkLabel()).also {
                    it.addKeyCode(KeyEvent.KEYCODE_BOOKMARK)
                }
            )
            icon = R.drawable.ic_baseline_ondemand_video_96
        } else {
            icon = R.drawable.ic_baseline_speaker_notes_96
        }

        actionAdapter.add(
            Action(ID_CONTACT, "Contact")
        )

        val row = DetailsOverviewRow(ypChannel)
        row.actionsAdapter = actionAdapter
        val img = ContextCompat.getDrawable(requireContext(), icon)
        img?.setTint(ContextCompat.getColor(requireContext(), R.color.md_grey_400))
        row.imageDrawable = img
        row.isImageScaleUpAllowed = true

        adapter.add(row)
    }


    private fun setupDetailsOverviewRowPresenter() {
        val detailsPresenter = FullWidthDetailsOverviewRowPresenter(DetailsDescriptionPresenter())

        detailsPresenter.backgroundColor =
            ContextCompat.getColor(requireActivity(), R.color.default_background)

        setOnItemViewSelectedListener(this)

        detailsPresenter.onActionClickedListener = this
        presenterSelector.addClassPresenter(DetailsOverviewRow::class.java, detailsPresenter)
    }

    override fun onItemSelected(
        itemViewHolder: Presenter.ViewHolder?,
        item: Any?,
        rowViewHolder: RowPresenter.ViewHolder?,
        row: Any?,
    ) {
        //Timber.d("-->$item")
        if (preloadJob?.isActive == true && item is Action && item.id != ID_PLAY) {
            Timber.i("Cancel preloading for automatic playing")
            preloadJob?.cancel()
        }
    }

    override fun onActionClicked(action: Action) {
        Timber.d("-->$action")
        when (action.id) {
            ID_PLAY -> {
                preloadJob?.cancel()
                PlayerLauncherFragment.start(parentFragmentManager, ypChannel)
            }
            ID_BOOKMARK -> {
                viewModel.bookmark.toggle(ypChannel)
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
                } catch (e: RuntimeException) {
                    Timber.e(e, "it couldn't launch browser")
                    viewModel.showInfoToast(e.message ?: "(null)", Toast.LENGTH_LONG)
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
        preloadJob?.cancel()
    }

    companion object {
        private const val ID_PLAY = 0L
        private const val ID_BOOKMARK = 1L
        private const val ID_CONTACT = 2L

        private const val AUTO_PLAY_WAIT_MSEC = 5000

        private const val ARG_YP_CHANNEL = "yp-channel"

        fun start(fm: FragmentManager, ypChannel: YpChannel) {
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