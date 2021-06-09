package org.peercast.core.tv

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.leanback.preference.LeanbackPreferenceFragmentCompat
import androidx.preference.*
import org.koin.androidx.viewmodel.ext.android.sharedViewModel
import org.peercast.core.preferences.SettingFragmentDelegate
import org.peercast.core.preferences.leanback.LeanbackSettingsFragmentCompat2
import timber.log.Timber


class SettingFragment : LeanbackSettingsFragmentCompat2() {

    class DemoFragment : LeanbackPreferenceFragmentCompat() {
        private val viewModel by sharedViewModel<TvViewModel>()
        private val delegate by lazy {
            SettingFragmentDelegate(this, viewModel, viewModel.prefs)
        }

        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)

        }

        override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?,
        ): View? {
            delegate.toString()
            return super.onCreateView(inflater, container, savedInstanceState)
        }

        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            // Load the preferences from an XML resource
            //setPreferencesFromResource(R.xml.preferences, rootKey)
            delegate.onCreatePreferences(savedInstanceState, rootKey)
        }
    }

    override fun onPreferenceStartInitialScreen() {
        startPreferenceFragment(DemoFragment())
        //startImmersiveFragment(DemoFragment())
    }

    override fun onPreferenceStartFragment(
        caller: PreferenceFragmentCompat?,
        pref: Preference,
    ): Boolean {


        val args = pref.extras
        val f: Fragment = childFragmentManager.fragmentFactory.instantiate(
            requireActivity().classLoader, pref.fragment)
        f.setArguments(args)
        f.setTargetFragment(caller, 0)
        Timber.d("-->$f")

        if (f is PreferenceFragmentCompat
            ||
            f is PreferenceDialogFragmentCompat
        ) {
            startPreferenceFragment(f)
            //startImmersiveFragment(f)
        } else {
            startImmersiveFragment(f)
        }
        return true
    }


    override fun onPreferenceStartScreen(
        caller: PreferenceFragmentCompat?,
        pref: PreferenceScreen,
    ): Boolean {
        val fragment: Fragment = DemoFragment()
        val args = Bundle(1)
        args.putString(PreferenceFragmentCompat.ARG_PREFERENCE_ROOT, pref.key)
        fragment.setArguments(args)
        startPreferenceFragment(fragment)
        //startImmersiveFragment(fragment)
        return true
    }

//    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
//        delegate.onCreatePreferences(savedInstanceState, rootKey)
//    }

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