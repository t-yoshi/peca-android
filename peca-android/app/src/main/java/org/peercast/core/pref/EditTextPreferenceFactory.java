package org.peercast.core.pref;

import android.content.Context;
import android.support.annotation.StringRes;
import android .preference.EditTextPreference;
import android .preference.Preference;
import android.view.View;
import android.widget.EditText;

import org.jsoup.nodes.Element;
import org.jsoup.nodes.FormElement;

/**
 * Inputタグ -> EditTextPreference
 * @author (c) 2015, T Yoshizawa
 *         Dual licensed under the MIT or GPL licenses.
 */
public class EditTextPreferenceFactory extends PreferenceFactory {
    private final Element mInput;
    private final int mEditTextInputType;
    private final String mOldValue;

    public EditTextPreferenceFactory(@StringRes int title, FormElement form, String selector, int editTextInputType) {
        super(title);

        mInput = form.parent()
                .select(selector)
                .first();
        //System.out.println(mInput);
        if (mInput == null || !"input".equalsIgnoreCase(mInput.tagName()))
            throw new IllegalArgumentException("Invalid Selector");
        mEditTextInputType = editTextInputType;
        mOldValue = mInput.attr("value");
    }

    public Preference createPreference(Context c) {
        EditTextPreference p = new EditTextPreference(c) {
            @Override
            protected void onAddEditTextToDialogView(View dialogView, EditText editText) {
                super.onAddEditTextToDialogView(dialogView, editText);
                editText.setInputType(mEditTextInputType);
            }
        };
        p.setTitle(mTitle);
        p.setText(mOldValue);
        p.setSummary(mOldValue);
        p.setTitle(mTitle);
        //p.setDialogTitle(mTitle);
        p.setOnPreferenceChangeListener(this);
        return p;
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if (!isValidValue(newValue.toString()))
            return false;
        EditTextPreference p = (EditTextPreference) preference;
        mInput.attr("value", newValue.toString());
        updateSummary(p, mOldValue, newValue);
        return true;
    }

    /**設定に入力した値が有効か*/
    protected  boolean isValidValue(String val){
        return true;
    }
}
