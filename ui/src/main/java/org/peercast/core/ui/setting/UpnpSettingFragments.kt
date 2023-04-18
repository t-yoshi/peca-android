package org.peercast.core.ui.setting

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.leanback.preference.LeanbackPreferenceFragmentCompat
import androidx.lifecycle.lifecycleScope
import androidx.preference.CheckBoxPreference
import androidx.preference.Preference
import androidx.preference.PreferenceCategory
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.SwitchPreference
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import org.peercast.core.common.upnp.UpnpManager
import timber.log.Timber
import java.io.IOException


private class UpnpSettingFragmentDelegate(fragment: PreferenceFragmentCompat) :
    BaseLoadableDelegate(fragment) {

    private val upnpManager by fragment.inject<UpnpManager>()
    private lateinit var catInfo: PreferenceCategory

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        super.onCreatePreferences(savedInstanceState, rootKey)
        val screen = fragment.preferenceScreen
        screen.title = "UPnP"

        val c = fragment.requireContext()

        SwitchPreference(c).also {
            it.title = "UPnP"
            it.isChecked = appPrefs.isUPnPEnabled
            it.setOnPreferenceChangeListener { _, v ->
                appPrefs.isUPnPEnabled = v as Boolean
                if (v) {
                    loadUpnpStatus()
                }
                true
            }
            screen.addPreference(it)
        }

        catInfo = PreferenceCategory(c)
        screen.addPreference(catInfo)

        if (appPrefs.isUPnPEnabled) {
            loadUpnpStatus()
        }
    }

    private fun loadUpnpStatus() {
        catInfo.removeAll()
        asyncExecute({
            upnpManager.getStatuses() to
                    upnpManager.getPortMaps()
        }, { (statuses, portMaps) ->
            portMaps.forEach { m ->
                CheckBoxPreference(catInfo.context).also {
                    it.title = "${m.protocol} ${m.internalClient}:${m.internalPort}"
                    it.summary = m.description
                    it.isChecked = m.enabled
                    catInfo.addPreference(it)
                    it.setOnPreferenceChangeListener { _, newValue ->
                        when (newValue as Boolean) {
                            true -> {
                                manipulatePort {
                                    upnpManager.addPort(m.internalPort)
                                }
                            }

                            else -> {
                                manipulatePort {
                                    upnpManager.removePort(m.internalPort)
                                }
                            }
                        }
                        true
                    }
                }
            }
            statuses.forEach { (title, r) ->
                Preference(catInfo.context).also {
                    it.title = "$title: $r"
                    catInfo.addPreference(it)
                }
            }
        }, { e ->
            Timber.w(e)
            showErrorMessage(e.message.toString())
        })
    }

    private fun manipulatePort(f: suspend () -> Unit) {
        fragment.lifecycleScope.launch {
            try {
                f()
            } catch (e: IOException) {
                Timber.w(e)
                showErrorMessage(e.message.toString())
            }
        }
    }

    private fun showErrorMessage(msg: CharSequence) {
        val p = catInfo.findPreference(KEY_ERROR) ?: Preference(catInfo.context).also {
            it.key = KEY_ERROR
            catInfo.addPreference(it)
        }
        p.title = msg
    }

    companion object {
        private const val KEY_ERROR = "_Key_Error"
    }
}


class UpnpSettingFragment : PreferenceFragmentCompat() {
    private val delegate = UpnpSettingFragmentDelegate(this)

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        delegate.onCreatePreferences(savedInstanceState, rootKey)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return delegate.wrapProgressView(
            super.onCreateView(inflater, container, savedInstanceState)
        )
    }

}

class TvUpnpSettingFragment : LeanbackPreferenceFragmentCompat() {
    private val delegate = UpnpSettingFragmentDelegate(this)

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        delegate.onCreatePreferences(savedInstanceState, rootKey)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return delegate.wrapProgressView(
            super.onCreateView(inflater, container, savedInstanceState),
        )
    }
}
