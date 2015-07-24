package org.peercast.core.pref;

import android.content.Context;
import android.preference.Preference;
import android.preference.PreferenceCategory;

/**
 * CategoryPreferenceを作成する
 * @author (c) 2015, T Yoshizawa
 *         Dual licensed under the MIT or GPL licenses.
 */
public class CategoryPreferenceFactory extends PreferenceFactory {
    public CategoryPreferenceFactory(int title) {
        super(title);
    }

    @Override
    public Preference createPreference(Context c) {
        PreferenceCategory p = new PreferenceCategory(c);
        p.setTitle(mTitle);
        return p;
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        return false;
    }
}
