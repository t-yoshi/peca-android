package org.peercast.core

import android.content.Context
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.util.Log
import android.util.TypedValue
import android.view.ContextThemeWrapper
import androidx.annotation.StringRes
import androidx.preference.*
import org.jsoup.nodes.FormElement

/**
 * Htmlの設定画面をスクレイピングしてPreferenceに変換する。
 *
 * @author (c) 2019, T Yoshizawa
 * Dual licensed under the MIT or GPL licenses.
 */

class HtmlPreferenceFactory(c: Context, val form: FormElement) {
    private val themedContext: Context

    // <tr>
    //   <td>title
    //   <td><input name=...>...

    init {
        val tv = TypedValue()
        c.theme.resolveAttribute(R.attr.preferenceTheme, tv, true)
        themedContext = ContextThemeWrapper(c, tv.resourceId)
    }

    fun createPreferenceCategory(@StringRes titleRes: Int): PreferenceCategory {
        return PreferenceCategory(themedContext).also { p ->
            p.setTitle(titleRes)
        }
    }

    /**
     * Inputタグ -> EditTextPreference
     */
    fun createEditTextPreference(
            selector: String,
            validator: (String) -> Boolean = { true }): EditTextPreference {
        val input = form.parent().select(selector).first() //
        if (!"input".equals(input?.tagName(), true))
            throw IllegalArgumentException("Invalid Selector: $selector")
        val title = input.parent().previousElementSibling()?.text() ?: "?($selector)"
        val value = input.attr("value")

        return EditTextPreference(themedContext).also { p ->
            p.isPersistent = false
            p.title = title
            p.dialogTitle = title
            p.key = "$title#$selector"

            //数字のみ
            if (value.matches("""^\d+$""".toRegex()))
                p.dialogLayoutResource = R.layout.preference_dialog_edittext_number

            p.text = value
            p.summary = value
            p.setOnPreferenceChangeListener { _, newValue ->
                if (!validator(newValue.toString()))
                    return@setOnPreferenceChangeListener false

                input.attr("value", newValue.toString())
                p.updateSummary(newValue)
                true
            }
        }
    }

    /**
     * Selectタグ -> ListPreference
     */
    fun createListPreference(selector: String): ListPreference {
        val elSelect = form.parent().select(selector).first()
        val title = elSelect.parent().previousElementSibling()?.text() ?: "?($selector)"

        if (!"select".equals(elSelect?.tagName(), true))
            throw IllegalArgumentException("Invalid Selector: $selector")
        val elOptions = elSelect.select("option")
        val entries = elOptions.map { it.text() }
        val entryValues = elOptions.map { it.attr("value") }
        val value = elOptions.firstOrNull { e ->
            e.hasAttr("selected") || e.hasAttr("checked")
        }?.attr("value") ?: "????"
        Log.d(TAG, "selected=$value,entries=$entries, entryValues=$entryValues")

        return HackedListPreference(themedContext).also { p ->
            p.title = title
            p.dialogTitle = title
            p.key = "$title#$selector"
            p.isPersistent = false
            p.entries = entries.toTypedArray()
            if (value in entryValues)
                p.summary = entries[entryValues.indexOf(value)]
            p.entryValues = entryValues.toTypedArray()
            if (value in p.entryValues)
                p.value = value
            p.setOnPreferenceChangeListener { _, newValue ->
                p.updateSummary(entries[entryValues.indexOf(newValue)])
                true
            }
        }
    }

    /**
     * Input(type=checkbox)タグ -> CheckBoxPreference
     */
    fun createCheckBoxPreference(selector: String): CheckBoxPreference {
        val elInput = form.parent()
                .select(selector)
                .first()
        if (!"input".equals(elInput?.tagName(), true))
            throw IllegalArgumentException("Invalid Selector: $selector")
        val title = elInput.parent().previousElementSibling()?.text() ?: "?($selector)"
        val checked = elInput.hasAttr("checked")
        return CheckBoxPreference(themedContext).also { p ->
            p.title = title
            p.isChecked = checked
            p.summary = checked.toString()
            p.setOnPreferenceChangeListener { _, newValue ->
                if (newValue as Boolean)
                    elInput.attr("checked", "checked")
                else
                    elInput.removeAttr("checked")
                p.updateSummary(newValue)
                true
            }
        }
    }

    companion object {
        private const val TAG = "HtmlPreferenceFactory"

        /**
         * 設定を変更した場合、サマリの色を変えて見やすくする。
         * NOTE: 通常のListPreferenceは色を変更できない。
         */
        private fun Preference.updateSummary(newSummary: Any) {
            val oldSummary = summary
            if (oldSummary.isNullOrEmpty() || oldSummary == newSummary) {
                summary = newSummary.toString()
            } else {
                summary = SpannableString(newSummary.toString()).also { ss ->
                    val tv = TypedValue()
                    context.theme.resolveAttribute(R.attr.textPreferenceChangedColor, tv, false)
                    ss.setSpan(ForegroundColorSpan(tv.data), 0, ss.length, 0)
                }
            }
        }

    }

    /**
     * ListPreferenceはgetSummary()の動作変更してるので、
     * そのままではSummaryの色を反映できない。
     */
    private class HackedListPreference(c: Context) : ListPreference(c) {
        private var summary_: CharSequence? = null

        override fun getSummary(): CharSequence? = summary_

        override fun setSummary(cs: CharSequence?) {
            summary_ = cs
        }
    }
}

