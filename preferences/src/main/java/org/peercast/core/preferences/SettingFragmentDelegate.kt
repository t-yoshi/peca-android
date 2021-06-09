package org.peercast.core.preferences

import android.app.AlertDialog
import android.app.UiModeManager
import android.content.Context
import android.content.res.Configuration
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
import org.peercast.core.preferences.leanback.LeanbackEditTextPreferenceDialogFragmentCompat2
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
    private val prefs: AppPreferences,
) {

    fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        fragment.setPreferencesFromResource(R.xml.prefs, rootKey)
        initPortEditTextPreference()

        executeRpcCommand {
            loadSettings(it)
        }
    }

    private fun initPortEditTextPreference(){
        (fragment.findPreference<EditTextPreference>("_key_Port")!!).let { p ->
            p.text = prefs.port.toString()
            p.summary = p.text
            p.setEditInputType(INPUT_TYPE_NUMBER_SIGNED)

            p.setOnPreferenceChangeListener { _, newValue ->
                //Timber.d("-->$newValue")
                if (newValue is CharSequence && newValue.isDigitsOnly()) {
                    val n = newValue.toString().toInt()
                    if (prefs.port != n && n in 1025..65532) {
                        prefs.port = n
                        p.summary = newValue
                        confirmRestart()
                        return@setOnPreferenceChangeListener true
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

    private fun confirmRestart() {
        val c = fragment.requireContext()
        val uiModeManager = c.getSystemService(Context.UI_MODE_SERVICE) as UiModeManager

        if (uiModeManager.currentModeType == Configuration.UI_MODE_TYPE_NORMAL) {
            confirmKillApp(c)
        } else {
            confirmKillAppLeanback(c)
        }
    }

    private fun confirmKillApp(c: Context) {
        AlertDialog.Builder(c)
            .setCancelable(false)
            .setPositiveButton(R.string.msg_port_changed) { _, _ ->
                Process.killProcess(Process.myPid())
            }
            .show()
    }

    private fun confirmKillAppLeanback(c: Context) {
        Toast.makeText(c, R.string.msg_port_changed, Toast.LENGTH_LONG).show()
        Handler().postDelayed({
            Process.killProcess(Process.myPid())
        }, 3000)
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
