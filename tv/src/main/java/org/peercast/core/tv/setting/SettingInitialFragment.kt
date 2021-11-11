package org.peercast.core.tv.setting

import android.content.ComponentName
import android.content.Context
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import android.text.InputType
import android.view.*
import androidx.leanback.preference.LeanbackPreferenceFragmentCompat
import androidx.lifecycle.lifecycleScope
import androidx.preference.EditTextPreference
import kotlinx.coroutines.launch
import org.koin.androidx.viewmodel.ext.android.sharedViewModel
import org.peercast.core.IPeerCastService
import org.peercast.core.lib.PeerCastRpcClient
import org.peercast.core.lib.internal.ServiceIntents
import org.peercast.core.lib.rpc.Settings
import org.peercast.core.lib.rpc.io.JsonRpcException
import org.peercast.core.tv.R
import org.peercast.core.tv.TvViewModel
import org.peercast.core.tv.setting.leanback.LeanbackEditTextPreferenceDialogFragmentCompat2
import timber.log.Timber
import kotlin.reflect.KProperty0


/**
 * Json-RPCのgetSettingsを実行し、Preference化する。
 * @author (c) 2021, T Yoshizawa
 * @licenses Dual licensed under the MIT or GPL licenses.
 */
class SettingInitialFragment : LeanbackPreferenceFragmentCompat() {
    private val viewModel by sharedViewModel<TvViewModel>()

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

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.prefs, rootKey)

        requireContext().bindService(
            ServiceIntents.SERVICE4_INTENT, serviceConnection, Context.BIND_AUTO_CREATE
        )

        executeRpcCommand {
            loadSettings(it)
        }
    }

    private fun initPortEditTextPreference(service: IPeerCastService) {
        val oldPort = kotlin.runCatching { service.port }.getOrNull() ?: return
        (findPreference<EditTextPreference>("_key_Port")!!).let { p ->
            p.text = oldPort.toString()
            p.summary = oldPort.toString()
            p.setEditInputType(INPUT_TYPE_NUMBER_SIGNED)

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

    override fun onDestroy() {
        super.onDestroy()
        if (service != null)
            requireContext().unbindService(serviceConnection)
        service = null
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
        val p = editTextPreferences.firstOrNull {
            it.key == "_key_${prop.name}"
        } ?: return

        p.text = prop.get().toString()
        p.summary = p.text
        p.setEditInputType(INPUT_TYPE_NUMBER_SIGNED)

        p.setOnPreferenceChangeListener { _, newValue ->
            newValue as String
            if (newValue == "" || p.text == newValue)
                return@setOnPreferenceChangeListener false

            lifecycleScope.launch {
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

    private val editTextPreferences: List<EditTextPreference>
        get() = (0 until preferenceScreen.preferenceCount).mapNotNull {
            preferenceScreen.getPreference(it) as? EditTextPreference
        }

    private fun executeRpcCommand(f: suspend (PeerCastRpcClient) -> Unit) {
        lifecycleScope.launchWhenResumed {
            val client = viewModel.rpcClient.value
            if (client == null) {
                Timber.w("client is null")
                return@launchWhenResumed
            }
            kotlin.runCatching {
                f(client)
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
