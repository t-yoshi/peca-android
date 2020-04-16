package org.peercast.core


import android.os.Bundle
import android.view.MenuItem
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentTransaction
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.peercast.core.databinding.PeercastActivitySimpleBinding
import org.peercast.core.databinding.PeercastActivityWebviewBinding

/**
 * @author (c) 2014-2019, T Yoshizawa
 * @licenses Dual licensed under the MIT or GPL licenses.
 */
class PeerCastActivity : AppCompatActivity() {
    private val viewModel by viewModel<PeerCastViewModel>()
    private val appPrefs by inject<AppPreferences>()

    interface BackPressSupportFragment {
        fun onBackPressed(): Boolean
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val (binding, fragment) = when (appPrefs.isSimpleMode) {
            true -> {
                PeercastActivitySimpleBinding.inflate(layoutInflater) to
                        PeerCastFragment()
            }
            else -> {
                PeercastActivityWebviewBinding.inflate(layoutInflater) to
                        YtWebViewFragment()
            }
        }

        binding.setVariable(BR.vm, viewModel)
        binding.lifecycleOwner = this
        setContentView(binding.root)
        setSupportActionBar(binding.root.findViewById(R.id.vToolbar))

        supportFragmentManager.beginTransaction()
                .replace(R.id.vFragContainer, fragment)
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
                recreate()
            }

            R.id.menu_simple_mode -> {
                appPrefs.isSimpleMode = true
                recreate()
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

            else -> return super.onOptionsItemSelected(item)
        }
        return true
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
