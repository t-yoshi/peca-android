package org.peercast.core.tv

import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Intent
import android.os.Bundle
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultCallback
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import org.koin.androidx.viewmodel.ext.android.sharedViewModel
import org.peercast.core.lib.rpc.YpChannel
import org.peercast.core.lib.toPlayListIntent
import org.peercast.core.lib.toStreamIntent
import timber.log.Timber

class PlayerLauncherFragment : Fragment(), ActivityResultCallback<ActivityResult> {
    private val viewModel by sharedViewModel<TvViewModel>()
    private lateinit var ypChannel: YpChannel
    private lateinit var activityLauncher: ActivityResultLauncher<Intent>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        activityLauncher =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult(),this)
        ypChannel = requireNotNull(
            requireArguments().getParcelable(ARG_YP_CHANNEL)
        )

        startPlayer()
    }

    override fun onActivityResult(result: ActivityResult?) {
        Timber.d("-> $result ${result?.data?.extras?.keySet()}")
        val extras = result?.data?.extras ?: Bundle.EMPTY
        extras.keySet().forEach {
            Timber.d(" -> $it: ${extras[it]}")
        }

        finish()
    }

    private fun startVlcPlayer() {
        val i = ypChannel.toPlayListIntent(viewModel.prefs.port)

        Timber.i("start vlc player: ${i.data}")
        //@see https://wiki.videolan.org/Android_Player_Intents/
        //@see https://code.videolan.org/videolan/vlc-android/-/blob/master/application/vlc-android/src/org/videolan/vlc/gui/video/VideoPlayerActivity.kt
        i.component = ComponentName(
            "org.videolan.vlc",
            "org.videolan.vlc.gui.video.VideoPlayerActivity"
        )
        //i.`package` = "org.videolan.vlc"
        i.putExtra("title", ypChannel.name)

        try {
            activityLauncher.launch(i)
        } catch (e: ActivityNotFoundException) {
            finish()
            Timber.e(e)
        }
    }

    private fun startPlayer() {
        return startVlcPlayer()

        val i = ypChannel.toStreamIntent(viewModel.prefs.port)

        Timber.i("start player: ${i.data}")
        //@see https://wiki.videolan.org/Android_Player_Intents/
        //@see https://code.videolan.org/videolan/vlc-android/-/blob/master/application/vlc-android/src/org/videolan/vlc/gui/video/VideoPlayerActivity.kt
        //i.component = ComponentName("org.videolan.vlc", "org.videolan.vlc.gui.video.VideoPlayerActivity")
        try {
            //Timber.d("-> ${i.data} ${i.extras?.keySet()?.toList()}")
            startActivity(i)
        } catch (e: ActivityNotFoundException) {
            viewModel.showInfoToast("Please install VLC Player")
            Timber.w(e)
        }
        finish()
    }


    private fun finish() {
        parentFragmentManager.beginTransaction()
            .remove(this)
            .commit()
    }


    companion object {
        private const val ARG_YP_CHANNEL = "yp-channel"

        fun start(fm: FragmentManager, ypChannel: YpChannel) {
            val f = PlayerLauncherFragment()
            f.arguments = Bundle(1).also {
                it.putParcelable(ARG_YP_CHANNEL, ypChannel)
            }
            fm.beginTransaction()
                .add(android.R.id.content, f)
                .commit()
        }

    }
}