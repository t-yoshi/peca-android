package org.peercast.core.tv

import android.net.Uri
import android.os.Bundle
import androidx.leanback.app.VideoSupportFragment
import androidx.leanback.app.VideoSupportFragmentGlueHost
import androidx.leanback.media.MediaPlayerAdapter
import androidx.leanback.media.PlaybackTransportControlGlue
import androidx.leanback.widget.PlaybackControlsRow
import timber.log.Timber

/** Handles video playback with media controls. */
class PlaybackVideoFragment : VideoSupportFragment() {

    private lateinit var mTransportControlGlue: PlaybackTransportControlGlue<MediaPlayerAdapter>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        //val (_, title, description, _, _, videoUrl) =
        //    activity?.intent?.getSerializableExtra(DetailsActivity.MOVIE) as Movie

        val glueHost = VideoSupportFragmentGlueHost(this@PlaybackVideoFragment)
        val playerAdapter = MediaPlayerAdapter(activity)
        playerAdapter.setRepeatAction(PlaybackControlsRow.RepeatAction.INDEX_NONE)

        mTransportControlGlue = PlaybackTransportControlGlue(requireActivity(), playerAdapter)
        mTransportControlGlue.host = glueHost
       // mTransportControlGlue.title = title
       // mTransportControlGlue.subtitle = description
        mTransportControlGlue.playWhenPrepared()

        val u = Uri.parse("${activity?.intent?.data}.flv")
        playerAdapter.setDataSource(u)
        Timber.d("->$u")
        playerAdapter.play()
    }

    override fun onPause() {
        super.onPause()
        mTransportControlGlue.pause()
    }
}