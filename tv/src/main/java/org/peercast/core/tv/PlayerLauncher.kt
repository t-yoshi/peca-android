package org.peercast.core.tv

/**
 * @author (c) 2014-2021, T Yoshizawa
 * @licenses Dual licensed under the MIT or GPL licenses.
 */
import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.leanback.app.ErrorSupportFragment
import org.koin.androidx.viewmodel.ext.android.getSharedViewModel
import org.koin.androidx.viewmodel.ext.android.sharedViewModel
import org.peercast.core.common.isFireTv
import org.peercast.core.lib.LibPeerCast.toStreamIntent
import org.peercast.core.lib.internal.ServiceIntents
import org.peercast.core.lib.rpc.YpChannel
import timber.log.Timber
import java.io.File
import java.io.IOException

class PlayerLauncher(private val f: Fragment, private val ypChannel: YpChannel) {

    private val viewModel = f.getSharedViewModel<TvViewModel>()
    private val c = f.requireContext()
    private val launcher = f.registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { r ->
        Timber.d("-> $r ${r?.data?.extras?.keySet()}")
        val extras = r?.data?.extras ?: Bundle.EMPTY
        extras.keySet().forEach {
            Timber.d(" -> $it: ${extras[it]}")
        }
    }

    private val listCreator = VlcPlayListCreator(c)

    private fun startVlcPlayer() {
        val u = listCreator.create(ypChannel, viewModel.prefs.port)
        val i = Intent(Intent.ACTION_VIEW, u)
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
            launcher.launch(i)
            viewModel.bookmark.incrementPlayedCount(ypChannel)
        } catch (e: ActivityNotFoundException) {
            Timber.e(e)
            viewModel.showInfoToast("$e")
        }
    }

    private fun startMxPlayer() {
        val i = ypChannel.toStreamIntent(viewModel.prefs.port)

        Timber.i("start player: ${i.data}")
        try {
            f.startActivity(i)
            viewModel.bookmark.incrementPlayedCount(ypChannel)
        } catch (e: RuntimeException) {
            //Timber.w(e)
            PromptToInstallVlcPlayerFragment.start(f.parentFragmentManager)
        }
    }

    class PromptToInstallVlcPlayerFragment : ErrorSupportFragment() {
        private val viewModel by sharedViewModel<TvViewModel>()

        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)

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

        companion object {
            fun start(fm: FragmentManager) {
                fm.beginTransaction()
                    .replace(android.R.id.content, PromptToInstallVlcPlayerFragment())
                    .commit()
            }
        }

    }

    fun startPlayer() {
        //return PromptToInstallVlcPlayerFragment.start(f.parentFragmentManager)

        if (hasInstalledVlcPlayer())
            startVlcPlayer()
        else
            startMxPlayer()
    }

    private fun hasInstalledVlcPlayer(): Boolean {
        return try {
            c.packageManager.getApplicationInfo(VLC_PLAYER_ACTIVITY.packageName, 0)
            true
        } catch (e: PackageManager.NameNotFoundException) {
            false
        }
    }

    private class VlcPlayListCreator(private val c: Context) {
        val plsDir = File(c.cacheDir, "pls/")

        init {
            val now = System.currentTimeMillis()
            plsDir.run {
                if (!exists())
                    mkdirs()
                //古いものを削除
                listFiles { f ->
                    f.lastModified() < now - 7 * 24 * 60 * 60_000
                }?.forEach { f ->
                    f.delete()
                }
            }
        }

        /**プレイリストにURLを5つ並べて再接続できるようにする*/
        fun create(ypChannel: YpChannel, port: Int): Uri {
            val stream = ypChannel.toStreamIntent(port).dataString!!
            val f = File(plsDir, "${ypChannel.name.fileNameEscape()}.pls")

            val now = System.currentTimeMillis()
            return try {
                f.printWriter().use { p ->
                    for (i in 0..4) {
                        p.println("$stream?v=${now / 1000 + i}")
                    }
                }
                FileProvider.getUriForFile(c, "org.peercast.core.fileprovider", f).also {
//                    c.grantUriPermission(VLC_PLAYER_ACTIVITY.packageName,
//                        it, Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
            } catch (e: IOException) {
                Uri.EMPTY
            }
        }

        companion object {
            private fun String.fileNameEscape() = replace("""\\W""".toRegex(), "_")
        }
    }

    companion object {
        private val VLC_PLAYER_ACTIVITY = ComponentName(
            "org.videolan.vlc",
            "org.videolan.vlc.gui.video.VideoPlayerActivity"
        )

    }
}