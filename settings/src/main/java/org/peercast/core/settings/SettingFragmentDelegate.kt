package org.peercast.core.settings

import android.os.Bundle
import android.os.Handler
import android.os.Process
import android.text.InputType
import android.view.*
import android.widget.Toast
import androidx.core.text.isDigitsOnly
import androidx.lifecycle.lifecycleScope
import androidx.preference.EditTextPreference
import androidx.preference.PreferenceFragmentCompat
import kotlinx.coroutines.launch
import org.peercast.core.lib.JsonRpcException
import org.peercast.core.lib.PeerCastRpcClient
import org.peercast.core.lib.app.BasePeerCastViewModel
import org.peercast.core.lib.rpc.Settings
import org.peercast.core.settings.preference.leanback.LeanbackEditTextPreferenceDialogFragmentCompat2
import timber.log.Timber
import kotlin.reflect.KProperty0


/**
 * Json-RPCのgetSettingsを実行し、Preference化する。
 * @author (c) 2021, T Yoshizawa
 * @licenses Dual licensed under the MIT or GPL licenses.
 */
class SettingFragmentDelegate(
    private val fragment: PreferenceFragmentCompat,
    private val viewModel: BasePeerCastViewModel,
    private val setting: AppSetting,
) {


    fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        fragment.setPreferencesFromResource(R.xml.prefs, rootKey)

        (fragment.findPreference<EditTextPreference>("_key_Port") as EditTextPreference).let { p ->
            p.text = setting.port.toString()
            p.summary = p.text

            p.extras.putInt(LeanbackEditTextPreferenceDialogFragmentCompat2.EXTRA_INPUT_TYPE,
                INPUT_TYPE_NUMBER_SIGNED)

            p.setOnPreferenceChangeListener { _, newValue ->
                //Timber.d("-->$newValue")
                if (newValue is String && newValue.isDigitsOnly()) {
                    val n = newValue.toString().toInt()
                    if (setting.port != n && n in 1025..65532) {
                        setting.port = n
                        p.summary = newValue
                        Toast.makeText(p.context, R.string.msg_port_changed, Toast.LENGTH_LONG).show()
                        //fragment.activity?.finishAffinity()
                        Handler().postDelayed({
                            Process.killProcess(Process.myPid())
                        }, 3000)

                        return@setOnPreferenceChangeListener true
                    }
                }
                false
            }
        }
        executeRpcCommand {
            loadSettings(it)
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
        val p = editTextPreferences.firstOrNull {
            it.key == "_key_${prop.name}"
        } ?: return

        p.text = prop.get().toString()
        p.summary = p.text
        p.extras.putInt(LeanbackEditTextPreferenceDialogFragmentCompat2.EXTRA_INPUT_TYPE,
            INPUT_TYPE_NUMBER_SIGNED)

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

    private val editTextPreferences: List<EditTextPreference>
        get() = (0 until fragment.preferenceScreen.preferenceCount).mapNotNull {
            fragment.preferenceScreen.getPreference(it) as? EditTextPreference
        }

    private fun executeRpcCommand(f: suspend (PeerCastRpcClient) -> Unit) {
        fragment.lifecycleScope.launchWhenResumed {
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
        private const val INPUT_TYPE_NUMBER_SIGNED = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_SIGNED
    }
}
