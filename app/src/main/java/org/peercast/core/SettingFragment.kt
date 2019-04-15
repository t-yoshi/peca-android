package org.peercast.core

import android.net.Uri
import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import androidx.browser.customtabs.CustomTabsIntent
import androidx.preference.EditTextPreference
import androidx.preference.PreferenceFragmentCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.sharedViewModel
import org.koin.ext.isInt
import org.peercast.core.lib.PeerCastRpcClient
import org.peercast.core.lib.rpc.JsonRpcException
import org.peercast.core.lib.rpc.Settings
import timber.log.Timber
import kotlin.coroutines.CoroutineContext
import kotlin.reflect.KMutableProperty0


/**
 * Json-RPCのgetSettingsを実行し、Preference化する。
 * @author (c) 2019, T Yoshizawa
 * @licenses Dual licensed under the MIT or GPL licenses.
 */
class SettingFragment : PreferenceFragmentCompat(), CoroutineScope {
    private val appPrefs by inject<AppPreferences>()
    private val activity: PeerCastActivity?
        get() = super.getActivity() as PeerCastActivity?
    private val viewModel by sharedViewModel<PeerCastViewModel>()
    private val job = Job()
    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Main + job


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.prefs, rootKey)

        (findPreference("_key_Port") as EditTextPreference).let { p ->
            p.text = appPrefs.port.toString()
            p.summary = p.text
            p.setOnPreferenceChangeListener { _, newValue ->
                if (newValue is String && newValue.isInt()) {
                    val n = newValue.toInt()
                    if (n in 1025..65532) {
                        appPrefs.port = n
                        showRequireRestartDialog()
                        return@setOnPreferenceChangeListener true
                    }
                }
                false
            }
        }
        viewModel.rpcClient?.let(::loadSettings)
    }

    private fun showRequireRestartDialog() {
        activity?.showAlertDialog(R.string.t_info,
                getString(R.string.msg_please_restart)) {
            activity?.finishAffinity()
        }
    }

    private fun loadSettings(client: PeerCastRpcClient) = launch {
        val settings = try {
            client.getSettings()
        } catch (e: JsonRpcException) {
            Timber.e(e)
            return@launch
        }

        Timber.d("--> $settings")
        settings.let {
            setPrefsValue(it, it::maxDirects)
            setPrefsValue(it, it::maxDirectsPerChannel)
            setPrefsValue(it, it::maxRelays)
            setPrefsValue(it, it::maxRelaysPerChannel)
            setPrefsValue(it, it::maxUpstreamRate)
        }
    }

    private fun setPrefsValue(settings: Settings, prop: KMutableProperty0<Int>) {
        val p = editTextPreferences.firstOrNull {
            it.key == "_key_${prop.name}"
        } ?: return
        val value = prop.get()

        p.text = value.toString()
        p.summary = p.text
        p.setOnPreferenceChangeListener { _, newValue ->
            newValue as String
            if (newValue == "" || "$value" == newValue)
                return@setOnPreferenceChangeListener false

            launch {
                try {
                    prop.set(newValue.toInt())
                    viewModel.rpcClient?.setSettings(settings)
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


    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.setting_menu, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.menu_html_settings -> {
                val u = Uri.parse(getString(R.string.yt_settings_url, appPrefs.port))
                //startActivityForResult(Intent(Intent.ACTION_VIEW, u), REQ_HTML_SETTING)
                CustomTabsIntent.Builder()
                        .build()
                        .launchUrl(context, u)
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }


    companion object {
        private const val REQ_HTML_SETTING = 1
    }
}
