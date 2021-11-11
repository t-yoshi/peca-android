package org.peercast.core.tv.setting

/**
 * @author (c) 2014-2021, T Yoshizawa
 * @licenses Dual licensed under the MIT or GPL licenses.
 */
import androidx.core.os.bundleOf
import androidx.fragment.app.FragmentManager
import androidx.preference.Preference
import androidx.preference.PreferenceDialogFragmentCompat
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.PreferenceScreen
import org.peercast.core.tv.setting.leanback.LeanbackSettingsFragmentCompat2
import timber.log.Timber


class SettingFragment : LeanbackSettingsFragmentCompat2() {

    override fun onPreferenceStartInitialScreen() {
        startPreferenceFragment(SettingInitialFragment())
    }

    override fun onPreferenceStartFragment(
        caller: PreferenceFragmentCompat?,
        pref: Preference,
    ): Boolean {
        val f = childFragmentManager.fragmentFactory.instantiate(
            requireActivity().classLoader, pref.fragment
        )
        f.arguments = pref.extras
        Timber.d("-->$f")

        when (f) {
            is PreferenceFragmentCompat,
            is PreferenceDialogFragmentCompat -> {
                startPreferenceFragment(f)
            }
            else -> {
                startImmersiveFragment(f)
            }
        }

        return true
    }

    override fun onPreferenceStartScreen(
        caller: PreferenceFragmentCompat?,
        pref: PreferenceScreen,
    ): Boolean {
        val f = SettingInitialFragment()
        f.arguments = bundleOf(
            PreferenceFragmentCompat.ARG_PREFERENCE_ROOT to pref.key
        )
        startPreferenceFragment(f)
        return true
    }

    companion object {
        fun start(fm: FragmentManager) {
            val f = SettingFragment()
            fm.beginTransaction()
                .replace(android.R.id.content, f)
                .addToBackStack(null)
                .commit()
        }
    }


}