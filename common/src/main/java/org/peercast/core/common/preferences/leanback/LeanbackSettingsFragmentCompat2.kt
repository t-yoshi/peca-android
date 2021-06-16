package org.peercast.core.common.preferences.leanback

import androidx.leanback.preference.LeanbackSettingsFragmentCompat
import androidx.preference.EditTextPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import timber.log.Timber

/**
 * 現状、LeanbackEditTextPreferenceDialogFragmentCompatでonPreferenceChangeListenerが発火しない
 * */
abstract class LeanbackSettingsFragmentCompat2 : LeanbackSettingsFragmentCompat() {
    override fun onPreferenceDisplayDialog(
        caller: PreferenceFragmentCompat,
        pref: Preference?,
    ): Boolean {
        if (pref is EditTextPreference) {
            Timber.i("LeanbackEditTextPreferenceDialogFragmentCompat2")
            val f = LeanbackEditTextPreferenceDialogFragmentCompat2.newInstance(pref.getKey())
            f.setTargetFragment(caller, 0)
            startPreferenceFragment(f)
            return true
        }

        return super.onPreferenceDisplayDialog(caller, pref)
    }
}