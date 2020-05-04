package org.peercast.core


import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isInvisible
import androidx.core.view.isVisible
import androidx.core.view.updatePadding
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import com.google.android.material.snackbar.Snackbar
import kotlinx.android.synthetic.main.peercast_activity.*
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel
import timber.log.Timber

/**
 * @author (c) 2014-2020, T Yoshizawa
 * @licenses Dual licensed under the MIT or GPL licenses.
 */
class PeerCastActivity : AppCompatActivity() {
    private val viewModel by viewModel<PeerCastViewModel>()
    private val appPrefs by inject<AppPreferences>()
    private val fragmentInstanceStates = Bundle()

    interface BackPressSupportFragment {
        fun onBackPressed(): Boolean
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.peercast_activity)

        setSupportActionBar(vToolbar)
        vToolbar.setOnMenuItemClickListener {
            val f = supportFragmentManager.findFragmentById(R.id.vFragContainer)
            f?.onOptionsItemSelected(it) == true || onOptionsItemSelected(it)
        }

        supportActionBar?.run {
            title = intent.getStringExtra(EXT_FRAGMENT_TITLE)
                    ?: getString(R.string.app_name)
            setDisplayHomeAsUpEnabled(
                    intent.getBooleanExtra(EXT_FRAGMENT_HOME_ENABLED, false)
            )
        }

        //savedInstanceState != null のときはFragmentが復元される
        if (savedInstanceState == null) {
            initFragment(intent.getStringExtra(EXT_FRAGMENT_NAME))
        } else {
            savedInstanceState.getBundle(STATE_FRAGMENT_INSTANCE_STATES)?.let(
                    fragmentInstanceStates::putAll
            )
        }

        viewModel.notificationMessage.value = ""
        viewModel.notificationMessage.observe(this, Observer { msg ->
            if (!msg.isNullOrBlank()) {
                Snackbar.make(vCoordinator, msg, Snackbar.LENGTH_SHORT).show()
            }
        })
    }

    private fun initFragment(fragName: String? = null) {
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
                vFragContainer.updatePadding(0, 0, 0, 0)
            }
            else -> {
                vAppBar.setExpanded(true)
                val pv = resources.getDimension(R.dimen.activity_vertical_margin).toInt()
                val ph = resources.getDimension(R.dimen.activity_horizontal_margin).toInt()
                vFragContainer.updatePadding(ph, pv, ph, pv)
            }
        }

        with(supportFragmentManager) {
            // PeerCastFragment <-> YtWebViewFragment
            //  の切り替え時に状態を保存する。
            if (fragName == null) {
                findFragmentById(R.id.vFragContainer)?.let { f ->
                    fragmentInstanceStates.putParcelable(
                            f.javaClass.name,
                            saveFragmentInstanceState(f)
                    )
                }
                frag.setInitialSavedState(
                        fragmentInstanceStates.getParcelable(frag.javaClass.name)
                )
            }

            beginTransaction()
                    .replace(R.id.vFragContainer, frag)
                    .commit()
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                onBackPressed()
            }

            R.id.menu_yt -> {
                appPrefs.isSimpleMode = false
                initFragment()
            }

            R.id.menu_simple_mode -> {
                appPrefs.isSimpleMode = true
                initFragment()
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
            vProgress.isVisible = value in 1 .. 99
        }

    override fun onBackPressed() {
        val f = supportFragmentManager.findFragmentById(R.id.vFragContainer) as? BackPressSupportFragment
        if (f?.onBackPressed() == true)
            return
        super.onBackPressed()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBundle(STATE_FRAGMENT_INSTANCE_STATES, fragmentInstanceStates)
    }

    companion object {
        private const val EXT_FRAGMENT_NAME = "fragment-name"
        private const val EXT_FRAGMENT_TITLE = "fragment-title"
        private const val EXT_FRAGMENT_HOME_ENABLED = "home-enabled"
        private const val STATE_FRAGMENT_INSTANCE_STATES = "state-fragment-instance-states"
    }
}
