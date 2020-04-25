package org.peercast.core


import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isGone
import androidx.core.view.updatePadding
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import com.google.android.material.snackbar.Snackbar
import kotlinx.android.synthetic.main.peercast_activity.*
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel

/**
 * @author (c) 2014-2020, T Yoshizawa
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

        setContentView(R.layout.peercast_activity)

        val fragName: String? = intent.getStringExtra(EXT_FRAGMENT_NAME)
        val frag = when {
            !fragName.isNullOrEmpty() -> supportFragmentManager.fragmentFactory
                    .instantiate(classLoader, fragName)
            appPrefs.isSimpleMode -> PeerCastFragment()
            else -> YtWebViewFragment()
        }

        when (frag) {
            is YtWebViewFragment,
            is LogViewerFragment -> {
                vAppBar.setExpanded(
                        resources.getBoolean(R.bool.is_portrait_enough_height)
                )
                vFragContainer.updatePadding(0, 0, 0,0)
            }
            else -> {
                vAppBar.setExpanded(true)
            }
        }

        setSupportActionBar(vToolbar)
        vToolbar.setOnMenuItemClickListener {
            frag.onOptionsItemSelected(it) || onOptionsItemSelected(it)
        }

        supportActionBar?.run {
            title = intent.getStringExtra(EXT_FRAGMENT_TITLE)
                    ?: getString(R.string.app_name)
            setDisplayHomeAsUpEnabled(
                    intent.getBooleanExtra(EXT_FRAGMENT_HOME_ENABLED, false)
            )
        }

        supportFragmentManager.beginTransaction()
                .replace(R.id.vFragContainer, frag)
                .commit()

        viewModel.notificationMessage.value = ""
        viewModel.notificationMessage.observe(this, Observer { msg ->
            if (!msg.isNullOrBlank()) {
                Snackbar.make(vCoordinator, msg, Snackbar.LENGTH_SHORT).show()
            }
        })
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                onBackPressed()
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
                startFragment(PecaPortFragment::class.java, "UPnP")
            }

            R.id.menu_log -> {
                startFragment(LogViewerFragment::class.java, getString(R.string.t_view_log))
            }

            R.id.menu_settings -> {
                startFragment(SettingFragment::class.java, getString(R.string.menu_settings))
            }

            else -> super.onOptionsItemSelected(item)
        }
        return true
    }

    private fun <T : Fragment> startFragment(f: Class<T>, title: String? = null) {
        val i = Intent()
                .setClass(this, javaClass)
                .putExtra(EXT_FRAGMENT_NAME, f.name)
                .putExtra(EXT_FRAGMENT_TITLE, title)
                .putExtra(EXT_FRAGMENT_HOME_ENABLED, true)
        startActivity(i)
    }

    fun collapsedAppBarUnlessEnoughHeight() {
        val expanded = vAppBar.height - vAppBar.bottom == 0
        if (expanded)
            vAppBar.setExpanded(
                    resources.getBoolean(R.bool.is_portrait_enough_height)
            )
    }

    var progressValue: Int
        get() = vProgress.progress
        set(value) {
            vProgress.progress = value
            vProgress.isGone = value < 0
        }

    override fun onBackPressed() {
        val f = supportFragmentManager.findFragmentById(R.id.vFragContainer) as? BackPressSupportFragment
        if (f?.onBackPressed() == true)
            return
        super.onBackPressed()
    }

    companion object {
        private const val EXT_FRAGMENT_NAME = "fragment-name"
        private const val EXT_FRAGMENT_TITLE = "fragment-title"
        private const val EXT_FRAGMENT_HOME_ENABLED = "home-enabled"
    }
}
