package org.peercast.core.ui

/**
 * @author (c) 2014-2021, T Yoshizawa
 * @licenses Dual licensed under the MIT or GPL licenses.
 */
import android.os.Bundle
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.commit
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.peercast.core.ui.setting.SettingFragment

class SettingActivity : AppCompatActivity(),
    PreferenceFragmentCompat.OnPreferenceStartFragmentCallback {

    private val viewModel by viewModel<UiViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (savedInstanceState == null) {
            supportFragmentManager.commit {
                add(android.R.id.content, SettingFragment())
            }
        }
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }

    override fun onStart() {
        super.onStart()
        viewModel.bindService()
    }

    override fun onStop() {
        super.onStop()
        viewModel.unbindService()
    }

    override fun onPreferenceStartFragment(
        caller: PreferenceFragmentCompat,
        pref: Preference
    ): Boolean {
        val fm = supportFragmentManager
        val f = fm.fragmentFactory.instantiate(classLoader, pref.fragment ?: return false)
        fm.commit {
            addToBackStack(null)
            replace(android.R.id.content, f)
        }
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                val fm = supportFragmentManager
                if (fm.backStackEntryCount > 0)
                    fm.popBackStack()
                else
                    finish()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

}
