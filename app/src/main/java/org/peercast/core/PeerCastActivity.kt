package org.peercast.core


import android.os.Bundle
import android.view.MenuItem
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentTransaction

/**
 * (c) 2015, T Yoshizawa
 * Dual licensed under the MIT or GPL licenses.
 */
class PeerCastActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.peercast_activity)

        supportFragmentManager.beginTransaction()
                .replace(R.id.vFragContainer, PeerCastFragment(), "PeerCastFragment")
                .commit()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home ->{
                supportFragmentManager.let { m->
                    if (m.backStackEntryCount > 0)
                        m.popBackStack()
                }
                true
            }

            R.id.menu_upnp_fragment -> {
                startFragment(PecaPortFragment())
                true
            }

            R.id.menu_log -> {
                startFragment(LogViewerFragment())
                true
            }

            R.id.menu_settings -> {
                startFragment(SettingFragment())
                true
            }

            else -> super.onOptionsItemSelected(item)
        }
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

    companion object {
        private const val TAG = "PeerCastActivity"
    }
}
