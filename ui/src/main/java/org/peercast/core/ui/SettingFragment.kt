package org.peercast.core.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.preference.PreferenceFragmentCompat
import androidx.recyclerview.widget.RecyclerView
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.sharedViewModel
import org.peercast.core.common.AppPreferences
import org.peercast.core.common.preferences.SettingFragmentDelegate


/**
 * Json-RPCのgetSettingsを実行し、Preference化する。
 * @author (c) 2019, T Yoshizawa
 * @licenses Dual licensed under the MIT or GPL licenses.
 */
class SettingFragment : PreferenceFragmentCompat() {
    private val appPrefs by inject<AppPreferences>()
    private val viewModel by sharedViewModel<UiViewModel>()
    private val delegate by lazy { SettingFragmentDelegate(this, viewModel, appPrefs) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        delegate.onCreatePreferences(savedInstanceState, rootKey)
    }

    override fun onCreateRecyclerView(
        inflater: LayoutInflater?,
        parent: ViewGroup?,
        savedInstanceState: Bundle?,
    ): RecyclerView {
        return super.onCreateRecyclerView(inflater, parent, savedInstanceState).also {
            it.isNestedScrollingEnabled = false
        }
    }


}
