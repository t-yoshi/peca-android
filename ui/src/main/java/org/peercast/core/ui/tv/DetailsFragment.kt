package org.peercast.core.ui.tv

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
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentManager
import androidx.leanback.app.DetailsSupportFragment
import androidx.leanback.app.DetailsSupportFragmentBackgroundController
import androidx.leanback.widget.*
import androidx.lifecycle.lifecycleScope
import io.ktor.client.request.*
import io.ktor.client.statement.*
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.koin.androidx.viewmodel.ext.android.sharedViewModel
import org.peercast.core.lib.LibPeerCast.toPlayListIntent
import org.peercast.core.lib.rpc.YpChannel
import org.peercast.core.ui.R
import org.peercast.core.ui.tv.util.isNotNilId
import org.peercast.core.ui.tv.util.ktorHttpClient
import org.peercast.core.ui.tv.util.unescapeHtml
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
    private var preloadJob: Job? = null
    private var hasAlreadyPlayed = false
    private lateinit var playerLauncher: PlayerLauncher

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        detailsBackground = DetailsSupportFragmentBackgroundController(this)

        ypChannel = requireNotNull(
            requireArguments().getParcelable(ARG_YP_CHANNEL)
        )
        hasAlreadyPlayed = savedInstanceState?.getBoolean(STATE_ALREADY_PLAYED, false) ?: false
        playerLauncher = PlayerLauncher(this, ypChannel)

        Timber.i("hasAlreadyPlayed=$hasAlreadyPlayed -> $ypChannel")

        adapter = ArrayObjectAdapter(presenterSelector)

        setupDetailsOverviewRowPresenter()
        setupDetailsOverviewRow()
        setAdapter(adapter)


        //val bm = BitmapFactory.decodeResource(resources, R.drawable.megaphone)
        //detailsBackground.coverBitmap = bm
        detailsBackground.enableParallax()

        if (!hasAlreadyPlayed && ypChannel.isNotNilId)
            startAutoPlay()
    }

    private fun startAutoPlay() {
        val streamUrl = ypChannel.toPlayListIntent(viewModel.config.port).dataString!!

        val playStartET = SystemClock.elapsedRealtime() + AUTO_PLAY_WAIT_MSEC

        //プレーヤー起動前に少しストリームを読み込む
        preloadJob = lifecycleScope.launch {
            val code = try {
                val res = ktorHttpClient.get<HttpResponse>(streamUrl)
                //res.readBytes(1)
                res.status.value
            } catch (e: IOException) {
                Timber.w(e, "preload connect failed: $streamUrl")
                //viewModel.sendErrorToast(e)
                502
            }
            Timber.d("preload connect: code=$code")
            delay(playStartET - SystemClock.elapsedRealtime())
            if (code != 404 && preloadJob?.isCancelled != true) {
                hasAlreadyPlayed = true
                playerLauncher.startPlayer()
            }
        }
    }


    private fun getBookmarkLabel(): String {
        return when (ypChannel in viewModel.bookmark) {
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
            ContextCompat.getColor(requireActivity(), R.color.tv_default_background)

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
                hasAlreadyPlayed = true
                playerLauncher.startPlayer()
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
                    Timber.e(e, "Couldn't launch browser")
                    viewModel.sendErrorToast(e)
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

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean(STATE_ALREADY_PLAYED, hasAlreadyPlayed)
    }

    override fun onDestroy() {
        super.onDestroy()
        preloadJob?.cancel()
    }

    companion object {
        private const val ID_PLAY = 0L
        private const val ID_BOOKMARK = 1L
        private const val ID_CONTACT = 2L

        private const val AUTO_PLAY_WAIT_MSEC = 4000

        private const val ARG_YP_CHANNEL = "yp-channel"
        private const val STATE_ALREADY_PLAYED = "already-played"

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