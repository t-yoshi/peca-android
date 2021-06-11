package org.peercast.core.tv

import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultCallback
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.FragmentManager
import androidx.leanback.app.ErrorSupportFragment
import org.koin.androidx.viewmodel.ext.android.sharedViewModel
import org.peercast.core.lib.LibPeerCast.toPlayListIntent
import org.peercast.core.lib.LibPeerCast.toStreamIntent
import org.peercast.core.lib.rpc.YpChannel
import timber.log.Timber
import java.lang.RuntimeException

class PlayerLauncherFragment : ErrorSupportFragment(), ActivityResultCallback<ActivityResult> {
    private val viewModel by sharedViewModel<TvViewModel>()
    private lateinit var ypChannel: YpChannel
    private lateinit var activityLauncher: ActivityResultLauncher<Intent>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        activityLauncher =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult(), this)
        ypChannel = requireNotNull(
            requireArguments().getParcelable(ARG_YP_CHANNEL)
        )
        //透明
        setDefaultBackground(true)

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
            Timber.e(e)
            viewModel.showInfoToast("$e")
            finish()
        }
    }

    private fun startMxPlayer(){
        val i = ypChannel.toStreamIntent(viewModel.prefs.port)

        Timber.i("start player: ${i.data}")
        try {
            startActivity(i)
            finish()
        } catch (e: RuntimeException) {
            //Timber.w(e)
            initPromptToInstallVlcPlayer()
        }
    }

    private fun initPromptToInstallVlcPlayer() {
        message = getString(R.string.please_install_vlc_player)
        buttonText = getString(R.string.google_play)
        buttonClickListener = View.OnClickListener {
            val u = Uri.parse("market://details?id=" + VLC_PLAYER_ACTIVITY.packageName)
            try {
                startActivity(Intent(Intent.ACTION_VIEW, u))
            } catch (e: RuntimeException){
                viewModel.showInfoToast("$e")
            }
            finish()
        }
    }

    private fun startPlayer() {
        //return initPromptToInstallVlcPlayer()
        if (hasVlcPlayerInstalled())
            startVlcPlayer()
        else
            startMxPlayer()
    }

    private fun hasVlcPlayerInstalled() : Boolean {
        return requireContext().packageManager.getInstalledApplications(0).any {
            it.packageName == VLC_PLAYER_ACTIVITY.packageName
        }
    }

    override fun onResume() {
        super.onResume()
        view?.findViewById<View>(androidx.leanback.R.id.button)?.requestFocus()
    }

    private fun finish() {
        parentFragmentManager.beginTransaction()
            .remove(this)
            .commit()
    }


    companion object {
        private const val ARG_YP_CHANNEL = "yp-channel"
        private val VLC_PLAYER_ACTIVITY = ComponentName(
            "org.videolan.vlc",
            "org.videolan.vlc.gui.video.VideoPlayerActivity"
        )

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