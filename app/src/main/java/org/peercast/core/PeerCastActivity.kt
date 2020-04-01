package org.peercast.core


import android.net.Uri
import android.os.Bundle
import android.view.MenuItem
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentTransaction
import org.koin.android.ext.android.inject
import timber.log.Timber

/**
 * @author (c) 2014-2019, T Yoshizawa
 * @licenses Dual licensed under the MIT or GPL licenses.
 */
class PeerCastActivity : AppCompatActivity() {
    private val appPrefs by inject<AppPreferences>()

    interface BackPressSupportFragment {
        fun onBackPressed(): Boolean
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.peercast_activity)

        val f = when (appPrefs.isSimpleMode) {
            true -> PeerCastFragment()
            else -> YtWebViewFragment()
        }
        supportFragmentManager.beginTransaction()
                .replace(R.id.vFragContainer, f, f.javaClass.name)
                .commit()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                supportFragmentManager.let { m ->
                    if (m.backStackEntryCount > 0)
                        m.popBackStack()
                }
            }

            R.id.menu_yt -> {
                appPrefs.isSimpleMode = false
                switchMainFragment(false)
            }

            R.id.menu_simple_mode -> {
                appPrefs.isSimpleMode = true
                switchMainFragment(true)
            }

            R.id.menu_upnp_fragment -> {
                startFragment(PecaPortFragment())
            }

            R.id.menu_log -> {
                startFragment(LogViewerFragment())
            }

            R.id.menu_settings -> {
                startFragment(SettingFragment())
            }

            R.id.menu_html_settings -> {
                val path = getString(R.string.yt_settings_path)
                startFragment(YtWebViewFragment.create(path, false))
            }

            else -> return super.onOptionsItemSelected(item)
        }
        return true
    }

    private fun switchMainFragment(isSimpleMode: Boolean) {
        val f = if (isSimpleMode) {
            PeerCastFragment()
        } else {
            YtWebViewFragment.create()
        }
        supportFragmentManager.beginTransaction()
                .replace(R.id.vFragContainer, f)
                .commit()
    }

    private fun startFragment(frag: Fragment) {
        supportFragmentManager.beginTransaction()
                .addToBackStack(null)
                .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
                .replace(R.id.vFragContainer, frag)
                .commit()
    }

    fun showAlertDialog(title: Int, msg: String, onOkClick: () -> Unit = {}) {
        AlertDialog.Builder(this)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setTitle(title)
                .setMessage(msg)
                .setPositiveButton(android.R.string.ok) { dialog, _ ->
                    onOkClick()
                    dialog.dismiss()
                }
                .show()
    }

    override fun onBackPressed() {
        val f = supportFragmentManager.findFragmentById(R.id.vFragContainer) as? BackPressSupportFragment
        if (f != null && f.onBackPressed())
            return
        super.onBackPressed()
    }
}
