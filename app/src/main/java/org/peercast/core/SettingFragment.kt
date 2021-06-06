package org.peercast.core

import android.os.Bundle
import android.view.*
import androidx.core.text.isDigitsOnly
import androidx.preference.EditTextPreference
import androidx.preference.PreferenceFragmentCompat
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.sharedViewModel
import org.peercast.core.lib.PeerCastRpcClient
import org.peercast.core.lib.JsonRpcException
import org.peercast.core.lib.rpc.Settings
import timber.log.Timber
import kotlin.coroutines.CoroutineContext
import kotlin.reflect.KProperty0


/**
 * Json-RPCのgetSettingsを実行し、Preference化する。
 * @author (c) 2019, T Yoshizawa
 * @licenses Dual licensed under the MIT or GPL licenses.
 */
class SettingFragment : PreferenceFragmentCompat(), CoroutineScope {
    private val appPrefs by inject<AppPreferences>()
    private val activity: PeerCastActivity?
        get() = super.getActivity() as PeerCastActivity?
    private val viewModel by sharedViewModel<AppViewModel>()
    private lateinit var job: Job
    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Main + job


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        job = Job()
        setPreferencesFromResource(R.xml.prefs, rootKey)

        (findPreference<EditTextPreference>("_key_Port") as EditTextPreference).let { p ->
            p.text = appPrefs.port.toString()
            p.summary = p.text
            p.setOnPreferenceChangeListener { _, newValue ->
                if (newValue is String && newValue.isDigitsOnly()) {
                    val n = newValue.toInt()
                    if (n in 1025..65532) {
                        appPrefs.port = n
                        return@setOnPreferenceChangeListener true
                    }
                }
                false
            }
        }
        viewModel.executeRpcCommand(this) { loadSettings(it) }
    }

    override fun onCreateRecyclerView(inflater: LayoutInflater?, parent: ViewGroup?, savedInstanceState: Bundle?): RecyclerView {
        return super.onCreateRecyclerView(inflater, parent, savedInstanceState).also {
            it.isNestedScrollingEnabled = false
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
        p.setOnPreferenceChangeListener { _, newValue ->
            newValue as String
            if (newValue == "" || p.text == newValue)
                return@setOnPreferenceChangeListener false

            launch {
                try {
                    viewModel.executeRpcCommand {
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


    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
    }

    override fun onDestroy() {
        super.onDestroy()
        job.cancel()
    }

}
