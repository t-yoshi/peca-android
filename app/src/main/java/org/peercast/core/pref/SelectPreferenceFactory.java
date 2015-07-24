package org.peercast.core.pref;

import android.content.Context;
import android.preference.ListPreference;
import android.preference.Preference;
import android.support.annotation.ArrayRes;
import android.support.annotation.StringRes;

import org.jsoup.nodes.Element;
import org.jsoup.nodes.FormElement;

import java.util.Arrays;

/**
 * Selectタグ -> ListPreference
 * @author (c) 2015, T Yoshizawa
 *         Dual licensed under the MIT or GPL licenses.
 */
public class SelectPreferenceFactory extends PreferenceFactory {
    private final Element mSelect;
    private final int mEntry;
    private final int mEntryVal;
    private String mOldVal = "???";

    public SelectPreferenceFactory(@StringRes int title, FormElement form, String selector,
                                @ArrayRes int resEntry, @ArrayRes int resEntryVal) {
        super(title);
        mSelect = form.parent()
                .select(selector)
                .first();
        //System.err.println(mSelect);
        if (mSelect == null || !"select".equals(mSelect.tagName()))
            throw new IllegalArgumentException("Invalid Selector");
        mEntry = resEntry;
        mEntryVal = resEntryVal;

        int i = 0;
        for (Element opt : mSelect.select("option")) {
            if (opt.hasAttr("selected") || opt.hasAttr("checked")) {
                mOldVal = opt.attr("value");
                //p.setValue(value);
                //p.setSummary(p.getEntries()[i]);
            }
            i++;
        }
    }

    public Preference createPreference(Context c) {
        ListPreference p = new HackedListPreference(c);
        p.setTitle(mTitle);
        p.setEntries(mEntry);
        p.setEntryValues(mEntryVal);
        p.setOnPreferenceChangeListener(this);
        p.setDialogTitle(mTitle);

        int index = Arrays.asList(p.getEntryValues())
                .indexOf(mOldVal);
        p.setValue(mOldVal);
        p.setSummary(p.getEntries()[index]);
        return p;
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        ListPreference p = (ListPreference) preference;
        int oldIndex = Arrays.asList(p.getEntryValues())
                .indexOf(mOldVal);
        int newIndex = Arrays.asList(p.getEntryValues())
                .indexOf(newValue);
        mSelect.children()
                .remove();
        mSelect.appendElement("option")
                .attr("value", newValue.toString())
                .attr("selected", "selected");

        updateSummary(p, p.getEntries()[oldIndex], p.getEntries()[newIndex]);

        return true;
    }

    /**
     * ListPreferenceはgetSummary()の動作変更してるので、
     * そのままではSummaryの色を反映できない。
     */
    private static class HackedListPreference extends ListPreference {
        private CharSequence mSummary;

        HackedListPreference(Context c) {
            super(c);
        }

        @Override
        public CharSequence getSummary() {
            return mSummary;
        }

        @Override
        public void setSummary(CharSequence cs) {
            mSummary = cs;
        }
    }
}
