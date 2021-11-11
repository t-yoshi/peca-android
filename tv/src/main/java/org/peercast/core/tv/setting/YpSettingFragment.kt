package org.peercast.core.tv.setting

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.core.view.get
import androidx.core.view.isVisible
import androidx.leanback.preference.LeanbackPreferenceFragmentCompat
import androidx.lifecycle.lifecycleScope
import androidx.preference.CheckBoxPreference
import androidx.preference.Preference
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup
import org.jsoup.nodes.FormElement
import org.koin.android.ext.android.inject
import org.peercast.core.common.AppPreferences
import org.peercast.core.tv.R
import timber.log.Timber
import java.io.IOException
import java.net.URL

class YpSettingFragment : LeanbackPreferenceFragmentCompat() {
    private val appPrefs by inject<AppPreferences>()
    private val isBusy = MutableStateFlow(true)

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        preferenceScreen = preferenceManager.createPreferenceScreen(requireContext())
        preferenceScreen.setTitle(R.string.yt_yellow_page)
        isBusy.onEach {
            preferenceScreen.isEnabled = !it
        }.launchIn(lifecycleScope)

        scrapingHtmlSettingPage()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val v = FrameLayout(inflater.context)
        v.addView(super.onCreateView(inflater, container, savedInstanceState))

        val vProgress = inflater.inflate(R.layout.progress, container, false)
        v.addView(vProgress)
        isBusy.onEach {
            vProgress.isVisible = it
        }.launchIn(viewLifecycleOwner.lifecycleScope)

        return v
    }

    private fun <T> asyncExecute(
        doInBackground: () -> T,
        onPostExecute: (T) -> Unit = {}
    ) {
        isBusy.value = true

        lifecycleScope.launch {
            withContext(Dispatchers.IO) {
                kotlin.runCatching { doInBackground() }
            }
                .onSuccess(onPostExecute)
                .onFailure { e ->
                    when (e) {
                        is IOException -> Timber.e(e)
                        else -> throw e
                    }
                }
            isBusy.value = false
        }
    }

    override fun onResume() {
        super.onResume()
        //戻るボタンのイベントを拾うため
        (view as ViewGroup)[0].requestFocus()
    }

    /**
     * Htmlの設定画面を解析する
     */
    private fun scrapingHtmlSettingPage() {
        preferenceScreen?.removeAll()

        //http://localhost:{port}/html/{en|ja}/settings.html
        val u = URL("http://localhost:${appPrefs.port}/html/ja/settings.html")

        asyncExecute({
            Jsoup.parse(u, 8_000)
                .select("form").first() as? FormElement
                ?: throw IOException("FORM element not found")
        }, ::htmlFormToPreference)
    }

    private fun addYpPreference(
        ypName: String,
        ypUrl: String,
        enabled: Boolean,
        onChange: (Boolean) -> Unit
    ) {
        val p = CheckBoxPreference(requireContext())
        p.title = ypName
        p.summary = ypUrl
        p.isChecked = enabled
        p.onPreferenceChangeListener = Preference.OnPreferenceChangeListener { _, v ->
            onChange(v as Boolean)
            true
        }
        preferenceScreen.addPreference(p)
    }

    private fun htmlFormToPreference(form: FormElement) {
        val sel = form.parent()?.selectFirst("select[data-index=1]") ?: return
        sel.select("option[data-url$=index.txt]").forEach { opt ->
            val ypName = opt.attr("value")
            val ypUrl = opt.attr("data-url")
            val inpFeedUrl =
                form.parent()?.selectFirst("input[class=channel-feed-url][value=$ypUrl]")
            Timber.d(" $ypName: $ypUrl")
            addYpPreference(ypName, ypUrl, inpFeedUrl != null) { b ->
                val conn = if (b) {
                    form.submit().data("channel_feed_url", ypUrl)
                } else {
                    inpFeedUrl?.removeAttr("value")
                    form.submit()
                }

                asyncExecute({
                    Timber.i("submit()")
                    conn.timeout(12_000)
                        .followRedirects(false)
                        .execute()
                })
            }
        }
    }

}