package org.peercast.core.ui.setting

import android.content.ComponentName
import android.content.Context
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import android.text.InputType
import androidx.core.os.bundleOf
import androidx.fragment.app.FragmentManager
import androidx.leanback.preference.LeanbackPreferenceFragmentCompat
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.preference.EditTextPreference
import androidx.preference.Preference
import androidx.preference.PreferenceDialogFragmentCompat
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.PreferenceScreen
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.getSharedViewModel
import org.koin.androidx.viewmodel.ext.android.sharedViewModel
import org.peercast.core.IPeerCastService
import org.peercast.core.common.AppPreferences
import org.peercast.core.lib.PeerCastRpcClient
import org.peercast.core.lib.app.BaseClientViewModel
import org.peercast.core.lib.internal.ServiceIntents
import org.peercast.core.lib.rpc.Settings
import org.peercast.core.lib.rpc.io.JsonRpcException
import org.peercast.core.ui.R
import org.peercast.core.ui.UiViewModel
import org.peercast.core.ui.leanback.LeanbackEditTextPreferenceDialogFragmentCompat2
import org.peercast.core.ui.leanback.LeanbackSettingsFragmentCompat2
import org.peercast.core.ui.tv.TvViewModel
import timber.log.Timber
import kotlin.reflect.KProperty0


/**
 * Json-RPCのgetSettingsを実行し、Preference化する。
 * @author (c) 2021, T Yoshizawa
 * @licenses Dual licensed under the MIT or GPL licenses.
 */
private class SettingFragmentDelegate(
    private val fragment: PreferenceFragmentCompat,
    private val viewModel: BaseClientViewModel
) {
    private val appPrefs by fragment.inject<AppPreferences>()
    private var service: IPeerCastService? = null
    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName, service_: IBinder) {
            service = IPeerCastService.Stub.asInterface(service_).also {
                initPortEditTextPreference(it)
            }
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            service = null
        }
    }

    private lateinit var rpcClient: PeerCastRpcClient

    init {
        fragment.lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onResume(owner: LifecycleOwner) {
                updatePreferences()
            }
        })
    }

    fun onCreatePreferences() {
        fragment.setPreferencesFromResource(R.xml.prefs, null)

        fragment.requireContext().bindService(
            ServiceIntents.SERVICE4_INTENT, serviceConnection, Context.BIND_AUTO_CREATE
        )

        fragment.lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onDestroy(owner: LifecycleOwner) {
                if (service != null)
                    fragment.requireContext().unbindService(serviceConnection)
                service = null
            }
        })

        fragment.lifecycleScope.launch {
            viewModel.rpcClient.filterNotNull().collect { c ->
                rpcClient = c
                executeRpcCommand {
                    loadSettings(it)
                }
            }
        }
    }

    private fun updatePreferences() {
        checkNotNull(fragment.findPreference("_key_YellowPage")).let { p ->
            p.fragment = when (fragment) {
                is LeanbackPreferenceFragmentCompat -> TvYpSettingFragment::class.java
                else -> YpSettingFragment::class.java
            }.name
        }

        checkNotNull(fragment.findPreference("_key_UPnP")).let { p ->
            p.fragment = when (fragment) {
                is LeanbackPreferenceFragmentCompat -> TvUpnpSettingFragment::class.java
                else -> UpnpSettingFragment::class.java
            }.name
            p.summary = when (appPrefs.isUPnPEnabled) {
                true -> "on"
                else -> "off"
            }
        }
    }

    private fun initPortEditTextPreference(service: IPeerCastService) {
        val oldPort = kotlin.runCatching { service.port }.getOrNull() ?: return
        (fragment.findPreference<EditTextPreference>("_key_Port")!!).let { p ->
            p.text = oldPort.toString()
            p.summary = oldPort.toString()
            p.setEditInputType(INPUT_TYPE_NUMBER_SIGNED)
            p.isEnabled = true

            p.setOnPreferenceChangeListener { _, newValue ->
                //Timber.d("-->$newValue")
                if (newValue != p.summary) {
                    val n = newValue.toString().toInt()
                    if (n in 1025..65532) {
                        return@setOnPreferenceChangeListener kotlin.runCatching {
                            service.port = n
                            p.summary = service.port.toString()
                        }.isSuccess
                    }
                }
                false
            }
        }
    }

    private suspend fun loadSettings(client: PeerCastRpcClient) {
        val settings = try {
            client.getSettings()
        } catch (e: JsonRpcException) {
            Timber.e(e)
            return
        }

        Timber.d("--> $settings")
        setPrefsValue(settings::maxDirects) { settings.copy(maxDirects = it) }
        setPrefsValue(settings::maxDirectsPerChannel) { settings.copy(maxDirectsPerChannel = it) }
        setPrefsValue(settings::maxRelays) { settings.copy(maxRelays = it) }
        setPrefsValue(settings::maxRelaysPerChannel) { settings.copy(maxRelaysPerChannel = it) }
        setPrefsValue(settings::maxUpstreamRate) { settings.copy(maxUpstreamRate = it) }
    }

    private fun setPrefsValue(prop: KProperty0<Int>, assigned: (Int) -> Settings) {
        val p = fragment.preferenceScreen.findPreference<EditTextPreference>("_key_${prop.name}")
            ?: return

        p.text = prop.get().toString()
        p.summary = p.text
        p.setEditInputType(INPUT_TYPE_NUMBER_SIGNED)

        p.setOnPreferenceChangeListener { _, newValue ->
            newValue as String
            if (newValue == "" || p.text == newValue)
                return@setOnPreferenceChangeListener false

            fragment.lifecycleScope.launch {
                try {
                    executeRpcCommand {
                        it.setSettings(assigned(newValue.toInt()))
                    }
                    p.summary = newValue
                } catch (e: JsonRpcException) {
                    Timber.e(e)
                }
            }
            true
        }
    }

    private fun executeRpcCommand(f: suspend (PeerCastRpcClient) -> Unit) {
        fragment.lifecycleScope.launchWhenResumed {
            kotlin.runCatching {
                f(rpcClient)
            }.onFailure(Timber::e)
        }
    }

    companion object {
        private const val INPUT_TYPE_NUMBER_SIGNED =
            InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_SIGNED

        private fun EditTextPreference.setEditInputType(type: Int) {
            extras.putInt(
                LeanbackEditTextPreferenceDialogFragmentCompat2.EXTRA_INPUT_TYPE,
                type
            )
            setOnBindEditTextListener {
                it.inputType = type
            }
        }
    }
}

class SettingFragment : PreferenceFragmentCompat() {
    private val viewModel by sharedViewModel<UiViewModel>()
    private lateinit var delegate: SettingFragmentDelegate

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        delegate = SettingFragmentDelegate(this, viewModel)
        delegate.onCreatePreferences()
        viewModel.title.value = getString(R.string.settings)
    }
}

class TvSettingFragment : LeanbackSettingsFragmentCompat2() {

    override fun onPreferenceStartInitialScreen() {
        startPreferenceFragment(InitFragment())
    }

    override fun onPreferenceStartFragment(
        caller: PreferenceFragmentCompat,
        pref: Preference,
    ): Boolean {
        val f = childFragmentManager.fragmentFactory.instantiate(
            requireActivity().classLoader, requireNotNull(pref.fragment)
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
        caller: PreferenceFragmentCompat,
        pref: PreferenceScreen,
    ): Boolean {
        val f = InitFragment()
        f.arguments = bundleOf(
            PreferenceFragmentCompat.ARG_PREFERENCE_ROOT to pref.key
        )
        startPreferenceFragment(f)
        return true
    }

    class InitFragment : LeanbackPreferenceFragmentCompat() {
        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            SettingFragmentDelegate(this, getSharedViewModel<TvViewModel>()).onCreatePreferences()
        }
    }


    companion object {
        fun start(fm: FragmentManager) {
            val f = TvSettingFragment()
            fm.beginTransaction()
                .replace(android.R.id.content, f)
                .addToBackStack(null)
                .commit()
        }
    }
}

