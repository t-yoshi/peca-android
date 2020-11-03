package org.peercast.core


import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import androidx.annotation.LayoutRes
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.Fragment
import com.google.android.material.snackbar.Snackbar
import kotlinx.android.synthetic.main.peercast_activity_flexible.*
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
    @LayoutRes private var layoutId = 0
    private val fragmentInstanceStates = Bundle()

    /**BackPressイベントを受け取るFragment*/
    interface BackPressSupportFragment {
        fun onBackPressed(): Boolean
    }

    /**NestedScrollViewを含むFragment*/
    interface NestedScrollFragment


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (savedInstanceState == null) {
            //初回起動時
            initFragment(intent.getStringExtra(EXT_FRAGMENT_NAME))
        } else {
            //復帰時: Fragmentは自動で復元される
            setContentView(savedInstanceState.getInt(STATE_LAYOUT_ID))
            initActionBar()
            savedInstanceState.getBundle(STATE_FRAGMENT_INSTANCE_STATES)?.let(
                    fragmentInstanceStates::putAll
            )
        }

        viewModel.notificationMessage.value = ""
        viewModel.notificationMessage.observe(this ) { msg ->
            if (!msg.isNullOrBlank()) {
                Snackbar.make(findViewById(R.id.vContent), msg, Snackbar.LENGTH_SHORT).show()
            }
        }
    }

    private fun initFragment(fragName: String? = null) {
        val frag = when {
            !fragName.isNullOrEmpty() -> supportFragmentManager.fragmentFactory
                    .instantiate(classLoader, fragName)
            appPrefs.isSimpleMode -> PeerCastFragment()
            else -> YtWebViewFragment()
        }

        layoutId = when (frag) {
            is NestedScrollFragment -> {
                R.layout.peercast_activity_flexible
            }
            else -> {
                R.layout.peercast_activity_static
            }
        }
        setContentView(layoutId)
        collapsedAppBarUnlessEnoughHeight()
        initActionBar()

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

    private fun initActionBar(){
        findViewById<Toolbar>(R.id.vToolbar).let { bar ->
            setSupportActionBar(bar)
            bar.setOnMenuItemClickListener {
                val f = supportFragmentManager.findFragmentById(R.id.vFragContainer)
                f?.onOptionsItemSelected(it) == true || onOptionsItemSelected(it)
            }
        }

        supportActionBar?.run {
            title = intent.getStringExtra(EXT_FRAGMENT_TITLE)
                    ?: getString(R.string.app_name)
            setDisplayHomeAsUpEnabled(
                    intent.getBooleanExtra(EXT_FRAGMENT_HOME_ENABLED, false)
            )
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                onBackPressed()
            }

            R.id.menu_web_mode -> {
                appPrefs.isSimpleMode = false
                initFragment()
            }

            R.id.menu_list_mode -> {
                appPrefs.isSimpleMode = true
                initFragment()
            }

            R.id.menu_upnp_fragment -> {
                startFragment(PecaPortFragment::class.java, "UPnP")
            }

            R.id.menu_log -> {
                startFragment(LogViewerFragment::class.java, getString(R.string.t_view_log))
            }

//            R.id.menu_settings -> {
//                startFragment(SettingFragment::class.java, getString(R.string.menu_settings))
//            }

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

    /**ディスプレイの高さに余裕があるとき、AppBarをデフォルトで表示する*/
    fun collapsedAppBarUnlessEnoughHeight() {
        if (vAppBar == null) {
            Timber.e("not flexible layout")
            return
        }
        val expanded = vAppBar.height - vAppBar.bottom == 0
        if (expanded)
            vAppBar.setExpanded(
                    resources.getBoolean(R.bool.is_portrait_enough_height)
            )
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
        outState.putInt(STATE_LAYOUT_ID, layoutId)
    }

    companion object {
        private const val EXT_FRAGMENT_NAME = "fragment-name"
        private const val EXT_FRAGMENT_TITLE = "fragment-title"
        private const val EXT_FRAGMENT_HOME_ENABLED = "home-enabled"

        private const val STATE_FRAGMENT_INSTANCE_STATES = "state-fragment-instance-states"
        private const val STATE_LAYOUT_ID = "state-layout-id"
    }
}
