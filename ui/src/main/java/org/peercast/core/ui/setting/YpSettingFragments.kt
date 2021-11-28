package org.peercast.core.ui.setting

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.leanback.preference.LeanbackPreferenceFragmentCompat
import androidx.preference.CheckBoxPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import org.jsoup.Jsoup
import org.jsoup.nodes.FormElement
import org.peercast.core.ui.R
import timber.log.Timber
import java.io.IOException
import java.net.URL

private class YpSettingFragmentDelegate(fragment: PreferenceFragmentCompat) :
    BaseLoadableDelegate(fragment) {

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        super.onCreatePreferences(savedInstanceState, rootKey)
        fragment.preferenceScreen.setTitle(R.string.yt_yellow_page)
        scrapingHtmlSettingPage()
    }

    /**
     * Htmlの設定画面を解析する
     */
    private fun scrapingHtmlSettingPage() {
        fragment.preferenceScreen?.removeAll()

        //http://localhost:{port}/html/{en|ja}/settings.html
        val u = URL("http://localhost:${appPrefs.port}/html/ja/settings.html")

        asyncExecute({
            Jsoup.parse(u, 8_000)
                .selectFirst("form") as? FormElement
                ?: throw IOException("FORM element not found")
        }, ::htmlFormToPreference)
    }

    private fun addYpPreference(
        ypName: String,
        ypUrl: String,
        isChecked: Boolean,
        onChange: (Boolean) -> Unit
    ) {
        val p = CheckBoxPreference(fragment.requireContext())
        p.title = ypName
        p.summary = ypUrl
        p.isChecked = isChecked
        p.onPreferenceChangeListener = Preference.OnPreferenceChangeListener { _, v ->
            onChange(v as Boolean)
            true
        }
        fragment.preferenceScreen.addPreference(p)
    }

    private fun htmlFormToPreference(form: FormElement) {
        val sel = form.selectFirst("select[data-index]") ?: return
        sel.select("option[data-url$=index.txt]").forEach { opt ->
            val ypName = opt.attr("value")
            val ypUrl = opt.attr("data-url")
            val qChannelFeedUrl = "input[name=channel_feed_url][value=$ypUrl]"
            val isInitChecked = form.select(qChannelFeedUrl).isNotEmpty()
            if (!isInitChecked) {
                form.append("<input name=channel_feed_url value=$ypUrl disabled/>")
            }

            Timber.d(" $ypName: $ypUrl")
            addYpPreference(ypName, ypUrl, isInitChecked) { isChecked ->
                //disabledをトグルする
                if (isChecked) {
                    form.select(qChannelFeedUrl).removeAttr("disabled")
                } else {
                    form.select(qChannelFeedUrl).attr("disabled", "")
                }
                //Timber.d("-->$isChecked ${form.formData()}")

                asyncExecute({
                    Timber.i("submit()")
                    form.submit().timeout(12_000)
                        .followRedirects(false)
                        .execute()
                })
            }
        }
    }

}


class YpSettingFragment : PreferenceFragmentCompat() {
    private val delegate = YpSettingFragmentDelegate(this)

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

class TvYpSettingFragment : LeanbackPreferenceFragmentCompat() {
    private val delegate = YpSettingFragmentDelegate(this)

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
