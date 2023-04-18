package org.peercast.core.ui


import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.databinding.BindingAdapter
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.commit
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.peercast.core.common.isTvMode
import org.peercast.core.ui.databinding.PeerCastActivityBinding
import org.peercast.core.ui.setting.SettingFragment
import org.peercast.core.ui.tv.TvActivity

/**
 * @author (c) 2014-2020, T Yoshizawa
 * @licenses Dual licensed under the MIT or GPL licenses.
 */
class PeerCastActivity : AppCompatActivity(),
    PreferenceFragmentCompat.OnPreferenceStartFragmentCallback {
    private val viewModel by viewModel<UiViewModel>()
    private lateinit var binding: PeerCastActivityBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        //FIRE TV
        if (isTvMode && intent.action == Intent.ACTION_MAIN) {
            val i = Intent(this, TvActivity::class.java)
            i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
            startActivity(i)
            finish()
            return
        }

        supportFragmentManager.registerFragmentLifecycleCallbacks(object :
            FragmentManager.FragmentLifecycleCallbacks() {
            override fun onFragmentResumed(fm: FragmentManager, f: Fragment) {
                viewModel.scrollable.value = f is WebViewFragment
                supportActionBar?.setDisplayHomeAsUpEnabled(f !is WebViewFragment)
            }
        }, false)

        binding = DataBindingUtil.setContentView(this, R.layout.peer_cast_activity)
        binding.vm = viewModel
        binding.lifecycleOwner = this

        setSupportActionBar(binding.vToolbar)
        binding.vToolbar.setOnMenuItemClickListener {
            val f = supportFragmentManager.findFragmentById(R.id.vFragContainer)
            f?.onOptionsItemSelected(it) == true || onOptionsItemSelected(it)
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.notificationMessage.filter { it.isNotBlank() }.collect { msg ->
                        Snackbar.make(binding.root, msg, Snackbar.LENGTH_SHORT).show()
                    }
                }

                launch {
                    viewModel.title.collect {
                        title = it.ifEmpty { getString(R.string.app_name) }
                    }
                }

                launch {
                    viewModel.expandAppBar.collect {
                        binding.vAppBar.setExpanded(it)
                        invalidateOptionsMenu()
                    }
                }
            }
        }

        if (intent.getBooleanExtra(EX_IS_INVISIBLE, false)) {
            moveTaskToBack(true)
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                onBackPressed()
            }

            R.id.menu_setting -> {
                startFragment(SettingFragment())
            }

            else -> super.onOptionsItemSelected(item)
        }
        return true
    }

    private fun startFragment(f: Fragment) {
        supportFragmentManager.commit {
            addToBackStack(null)
            replace(R.id.vFragContainer, f)
        }
    }

    override fun onPreferenceStartFragment(
        caller: PreferenceFragmentCompat,
        pref: Preference
    ): Boolean {
        val f = supportFragmentManager.fragmentFactory.instantiate(
            classLoader,
            pref.fragment ?: return false
        )
        startFragment(f)
        return true
    }

    override fun onStart() {
        super.onStart()
        viewModel.bindService()
    }

    override fun onStop() {
        super.onStop()
        viewModel.unbindService()
    }

    companion object {
        /**表示せず、すぐにバックスタックに送る。*/
        const val EX_IS_INVISIBLE = "is-invisible"

        @JvmStatic
        @BindingAdapter("bindScrollable")
        fun setToolbarScrollable(view: Toolbar, enabled: Boolean) {
            val p = view.layoutParams as AppBarLayout.LayoutParams
            p.scrollFlags = when (enabled) {
                true -> AppBarLayout.LayoutParams.SCROLL_FLAG_ENTER_ALWAYS or
                        AppBarLayout.LayoutParams.SCROLL_FLAG_SCROLL or
                        AppBarLayout.LayoutParams.SCROLL_FLAG_SNAP

                else -> AppBarLayout.LayoutParams.SCROLL_FLAG_NO_SCROLL
            }
        }

    }
}
