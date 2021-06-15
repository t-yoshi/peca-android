package org.peercast.core.tv
/**
 * @author (c) 2014-2021, T Yoshizawa
 * @licenses Dual licensed under the MIT or GPL licenses.
 */
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
import androidx.core.content.FileProvider
import androidx.fragment.app.FragmentManager
import androidx.leanback.app.ErrorSupportFragment
import org.koin.androidx.viewmodel.ext.android.sharedViewModel
import org.peercast.core.lib.LibPeerCast.toStreamIntent
import org.peercast.core.lib.rpc.YpChannel
import timber.log.Timber
import java.io.File
import java.io.IOException

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

        finishFragment()
    }

    private fun createLocalPlayList(): Uri {
        try {
            val c = requireContext()
            val f = File(c.cacheDir, "pls/${ypChannel.channelId}.pls")
            val now = System.currentTimeMillis()
            f.parentFile?.let { p ->
                if (!p.exists())
                    p.mkdirs()
                p.listFiles { f ->
                    f.lastModified() < now - 7 * 24 * 60 * 60_000
                }?.forEach { f ->
                    f.delete()
                }
            }
            //プレイリストにURLを5つ並べて再接続できるようにする
            val stream = ypChannel.toStreamIntent(viewModel.prefs.port).dataString!!
            f.printWriter().use { p ->
                for (i in 0..4) {
                    p.println("$stream#${now / 1000 + i}")
                }
            }
            return FileProvider.getUriForFile(c, "org.peercast.core.fileprovider", f)
        } catch (e: IOException) {
            Timber.e(e)
        }
        return Uri.EMPTY
    }

    private fun startVlcPlayer() {
        val i = Intent(Intent.ACTION_VIEW, createLocalPlayList())
        Timber.i("start vlc player: ${i.data}")
        i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
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
            finishFragment()
        }
    }

    private fun startMxPlayer() {
        val i = ypChannel.toStreamIntent(viewModel.prefs.port)

        Timber.i("start player: ${i.data}")
        try {
            startActivity(i)
            finishFragment()
        } catch (e: RuntimeException) {
            //Timber.w(e)
            initPromptToInstallVlcPlayer()
        }
    }

    private fun initPromptToInstallVlcPlayer() {
        message = getString(R.string.please_install_vlc_player)
        if (requireContext().isFireTv) {
            buttonText = getString(android.R.string.ok)
            buttonClickListener = View.OnClickListener {
                finishFragment()
            }
        } else {
            buttonText = getString(R.string.google_play)
            buttonClickListener = View.OnClickListener {
                val u = Uri.parse("market://details?id=" + VLC_PLAYER_ACTIVITY.packageName)
                try {
                    startActivity(Intent(Intent.ACTION_VIEW, u))
                } catch (e: RuntimeException) {
                    viewModel.showInfoToast("$e")
                }
                finishFragment()
            }
        }
    }

    private fun startPlayer() {
        //return initPromptToInstallVlcPlayer()
        if (hasVlcPlayerInstalled())
            startVlcPlayer()
        else
            startMxPlayer()
    }

    private fun hasVlcPlayerInstalled(): Boolean {
        return requireContext().packageManager.getInstalledApplications(0).any {
            it.packageName == VLC_PLAYER_ACTIVITY.packageName
        }
    }

    override fun onResume() {
        super.onResume()
        view?.findViewById<View>(androidx.leanback.R.id.button)?.requestFocus()
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