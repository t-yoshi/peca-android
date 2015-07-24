package org.peercast.core.pref;

import android.content.Context;
import android.preference.Preference;
import android.support.annotation.AttrRes;
import android.support.annotation.StringRes;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.util.TypedValue;

import org.peercast.core.R;

/**
 * Htmlのinputタグ等を、Preferenceに変換する基底クラス
 *
 * @author (c) 2015, T Yoshizawa
 *         Dual licensed under the MIT or GPL licenses.
 */
public abstract class PreferenceFactory implements Preference.OnPreferenceChangeListener {
    @StringRes
    protected final int mTitle;

    protected PreferenceFactory(@StringRes int title) {
        mTitle = title;
    }

    /**
     * 設定を変更した場合、サマリの色を変えて見やすくする。
     * <p>NOTE: ListPreferenceは色を変更できない。</p>
     */
    static protected void updateSummary(Preference p, Object oldVal, Object newVal) {
        Context c = p.getContext();
        if (oldVal == null || oldVal.equals(newVal)) {
            p.setSummary(summaryString(c, newVal));
        } else {
            ForegroundColorSpan cs = new ForegroundColorSpan(
                    resolveThemeColor(c,
                            R.attr.textPreferenceChangedColor));
            SpannableString ss = new SpannableString(summaryString(c, newVal));
            ss.setSpan(cs, 0, ss.length(), 0);
            p.setSummary(ss);
        }
    }

    private static int resolveThemeColor(Context c, @AttrRes int attr) {
        TypedValue tv = new TypedValue();
        c.getTheme()
                .resolveAttribute(attr, tv, false);
        return tv.data;
    }

    private static String summaryString(Context c, Object val) {
        if (val instanceof Boolean)
            return c.getString(Boolean.class.cast(val) ?
                    android.R.string.yes : android.R.string.no);
        return "" + val;
    }


    public abstract Preference createPreference(Context c);
}
