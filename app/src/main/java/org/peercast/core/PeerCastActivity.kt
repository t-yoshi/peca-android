package org.peercast.core


import android.os.Bundle
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.updatePadding
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentTransaction
import kotlinx.android.synthetic.main.peercast_activity.*
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.peercast.core.databinding.PeercastActivityBinding
import timber.log.Timber

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

    private val fragmentCallback = object : FragmentManager.FragmentLifecycleCallbacks() {
        override fun onFragmentResumed(fm: FragmentManager, f: Fragment) {
            Timber.d("fragment => $f")
            when (f) {
                is YtWebViewFragment,
                is LogViewerFragment -> {
                    vFragContainer.updatePadding(0, 0, 0, 0)
                    viewModel.isExpandedAppBar.value =
                        resources.getBoolean(R.bool.is_portrait_enough_height)
                }
                else -> {
                    val h = resources.getDimension(R.dimen.activity_horizontal_margin).toInt()
                    val v = resources.getDimension(R.dimen.activity_vertical_margin).toInt()
                    vFragContainer.updatePadding(h, v, h, v)
                    viewModel.isExpandedAppBar.value = true
                    viewModel.progress.value = -1
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val binding = PeercastActivityBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val frag = when (appPrefs.isSimpleMode) {
            true -> PeerCastFragment()
            else -> YtWebViewFragment()
        }
        setSupportActionBar(binding.vToolbar)
        binding.vm = viewModel
        binding.lifecycleOwner = this

        supportFragmentManager.registerFragmentLifecycleCallbacks(fragmentCallback, true)

        supportFragmentManager.beginTransaction()
                .replace(R.id.vFragContainer, frag)
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
                PecaPortFragment().start()
            }

            R.id.menu_log -> {
                LogViewerFragment().start()
            }

            R.id.menu_settings -> {
                SettingFragment().start()
            }

            else -> return super.onOptionsItemSelected(item)
        }
        return true
    }

    private fun Fragment.start() {
        supportFragmentManager.beginTransaction()
                .addToBackStack(null)
                .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
                .replace(R.id.vFragContainer, this)
                .commit()
    }


    override fun onBackPressed() {
        val f = supportFragmentManager.findFragmentById(R.id.vFragContainer) as? BackPressSupportFragment
        if (f?.onBackPressed() == true)
            return
        super.onBackPressed()
    }

    override fun onDestroy() {
        super.onDestroy()
        supportFragmentManager.unregisterFragmentLifecycleCallbacks(fragmentCallback)
    }
}
