package org.peercast.core

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.*
import android.widget.Toast
import androidx.lifecycle.Observer
import androidx.preference.PreferenceCategory
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.PreferenceGroup
import kotlinx.android.synthetic.main.setting_fragment_footer.*
import kotlinx.coroutines.*
import org.jsoup.Jsoup
import org.jsoup.nodes.FormElement
import org.koin.androidx.viewmodel.ext.sharedViewModel
import timber.log.Timber
import java.io.IOException
import java.net.URL
import kotlin.coroutines.CoroutineContext


/**
 * 設定画面のHtmlを解析し、AndroidのPreference化する。
 * @author (c) 2019, T Yoshizawa
 * Dual licensed under the MIT or GPL licenses.
 */
class SettingFragment : PreferenceFragmentCompat(), CoroutineScope {
    private val viewModel by sharedViewModel<PeerCastViewModel>()
    private val job = Job()
    private val activity: PeerCastActivity
        get() = super.getActivity() as PeerCastActivity
    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Main + job
    private var runningPort = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
        viewModel.serviceResultLiveData.observe(this, Observer {
            if (runningPort != it.props.port && it.props.port > 0) {
                runningPort = it.props.port
                scrapingHtmlSettingPage()
            }
        })
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        preferenceScreen = preferenceManager.createPreferenceScreen(context)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val footer = layoutInflater.inflate(R.layout.setting_fragment_footer, view as ViewGroup, false)
        view.addView(footer)

        vOkButton.setOnClickListener { v ->
            v.isEnabled = false //二度押し防止

            val form = v.tag as FormElement?
            if (form != null) {
                submitExit(form)
            } else {
                //外部ブラウザでの設定が終了した
                val u = URL("http://localhost:$runningPort/")
                launch {
                    val isPortChanged = runBlocking(Dispatchers.Default) {
                        try {
                            Jsoup.parse(u, 2 * 1000)
                            false
                        } catch (e: IOException) {
                            true
                        }
                    }
                    exit(isPortChanged)
                }

            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        job.cancel()
    }

    /**
     * Htmlの設定画面を解析する
     */
    private fun scrapingHtmlSettingPage() {
        preferenceScreen?.removeAll()
        addPreferencesFromResource(R.xml.prefs)
        vOkButton.tag = null

        //http://localhost:{port}/html/{en|ja}/settings.html
        val u = URL(getString(R.string.html_setting_page_url, runningPort))

        launch {
            val r = withContext(Dispatchers.Default) {
                kotlin.runCatching {
                    Jsoup.parse(u, 3000)
                            .select("form").first() as? FormElement
                            ?: throw IOException("FORM element not found")
                }
            }
            try {
                htmlFormToPreference(r.getOrThrow())
            } catch (e: IOException) {
                Timber.e(e, "Jsoup.parse(): $u")
                showToast(R.string.t_failed, "Jsoup.parse() ${e.message} $u")
            }
        }
    }

    private fun showToast(titleRes: Int, msg: String) {
        Toast.makeText(context,
                "%s: $msg".format(getText(titleRes)),
                Toast.LENGTH_LONG).show()
    }

    private fun htmlFormToPreference(form: FormElement) {
        vOkButton.tag = form

        val prefs = with(HtmlPreferenceFactory(activity, form)) {
            arrayOf(
                    //==基本設定==
                    createPreferenceCategory(R.string.t_category_basic),
                    createEditTextPreference("input[name=port]") {
                        it.toIntOrNull() in 1025..65535 //無効なポートを設定すると動作が変になるので
                    },
                    createEditTextPreference("input[name=maxrelays]") {
                        it.toIntOrNull() in 0..10
                    },
                    //最大帯域幅 (Kbits/s)
                    createEditTextPreference("input[name=maxup]") {
                        it.toIntOrNull() in 1..10 * 1024
                    },

                    //==拡張設定==
                    createPreferenceCategory(R.string.t_category_detail),
                    //オートキープ
                    createListPreference("select[name=autoRelayKeep]"),
                    //壁蹴り開始リレー数(0:無効)
                    createEditTextPreference("input[name=kickPushStartRelays]")

            )
        }

        var group: PreferenceGroup = preferenceScreen
        prefs.forEach { p ->
            if (p is PreferenceCategory) {
                group = p
                preferenceScreen.addPreference(group)
            } else {
                p.setIcon(android.R.drawable.ic_menu_preferences)
                group.addPreference(p)
            }
        }
    }


    private fun exit(isPortChanged: Boolean) {
        if (isPortChanged) {
            //ポートが変更されたので再起動を促す
            activity.showAlertDialog(R.string.t_info, getString(R.string.msg_please_restart)) {
                activity.finish()
            }
        } else {
            fragmentManager?.popBackStack()
        }
    }

    private fun submitExit(form: FormElement) {
        val newPort = form.parent().select("input[name=port]").attr("value")
        launch {
            val r = withContext(Dispatchers.Default) {
                kotlin.runCatching {
                    form.submit()
                            .timeout(3 * 1000)
                            .followRedirects(false)
                            .execute()
                }
            }
            try {
                r.getOrThrow()
                showToast(R.string.t_success, "submit()")
            } catch (e: IOException) {
                Timber.e(e, "FormElement.submit()")
                showToast(R.string.t_failed, "submit() ${e.message}")
            }
            exit(newPort != "$runningPort")
        }
    }


    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.menu_html_settings -> {
                preferenceScreen?.removeAll()
                vOkButton.tag = null
                val u = Uri.parse("http://localhost:$runningPort/")
                startActivity(Intent(Intent.ACTION_VIEW, u))
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.setting_menu, menu)
    }

}
