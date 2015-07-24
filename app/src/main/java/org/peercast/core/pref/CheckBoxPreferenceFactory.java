package org.peercast.core.pref;

import android.content.Context;
import android.preference.CheckBoxPreference;
import android.preference.Preference;

import org.jsoup.nodes.Element;
import org.jsoup.nodes.FormElement;

/**
 * Inputタグ -> CheckBoxPreference
 * @author (c) 2015, T Yoshizawa
 *         Dual licensed under the MIT or GPL licenses.
 */
public class CheckBoxPreferenceFactory extends PreferenceFactory {
    private final Element mInput;
    private final Boolean mOldChecked;

    public CheckBoxPreferenceFactory(int title, FormElement form, String selector) {
        super(title);
        mInput = form.parent()
                .select(selector)
                .first();
        //System.out.println(mInput);
        if (mInput == null || !"input".equalsIgnoreCase(mInput.tagName()))
            throw new IllegalArgumentException("Invalid Selector");
        mOldChecked = mInput.hasAttr("checked");
    }

    public Preference createPreference(Context c) {
        CheckBoxPreference p = new CheckBoxPreference(c);
        p.setTitle(mTitle);
        p.setChecked(mOldChecked);
        updateSummary(p, null, mOldChecked);
        p.setOnPreferenceChangeListener(this);
        return p;
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if ((Boolean) newValue)
            mInput.attr("checked", "checked");
        else
            mInput.removeAttr("checked");
        updateSummary(preference, mOldChecked, newValue);
        return true;
    }


}
