package org.peercast.core;

import android.annotation.TargetApi;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceGroup;
import android.preference.PreferenceScreen;
import android.text.InputType;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.FormElement;
import org.peercast.core.pref.AppCompatPreferenceActivity;
import org.peercast.core.pref.CategoryPreferenceFactory;
import org.peercast.core.pref.EditTextPreferenceFactory;
import org.peercast.core.pref.PreferenceFactory;
import org.peercast.core.pref.SelectPreferenceFactory;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

/**
 * 設定画面のHtmlを解析し、AndroidのPreference化する。
 *
 * @author (c) 2015, T Yoshizawa
 *         Dual licensed under the MIT or GPL licenses.
 */
public class SettingActivity extends AppCompatPreferenceActivity
        implements View.OnClickListener {

    /**
     * PeerCast動作中のポート (int)
     */
    public static final String PORT = "port";
    /**
     * 従来のHtml設定をブラウザで起動するとき  (boolean)
     */
    public static final String LAUNCH_HTML = "html";

    private static final String TAG = "SettingActivity";

    /**
     * ポートの設定が変更された (=再起動が必要)
     */
    public static final int RESULT_PORT_CHANGED = RESULT_FIRST_USER;

    private static final int INPUT_SIGNED_NUMBER = InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_SIGNED;
    private static final int INPUT_TEXT = InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS;

    private int mRunningPort;

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.empty_preference_screen);

        View footer = getLayoutInflater().inflate(R.layout.setting_activity_button, null);

        getListView().addFooterView(footer);
        Button ok = (Button) footer.findViewById(R.id.vOkButton);
        ok.setOnClickListener(this);

        mRunningPort = getIntent().getIntExtra(PORT, 0);
        if (getIntent().getBooleanExtra(LAUNCH_HTML, false)) {
            launchHtmlSetting();
        } else {
            new HtmlLoadTask().execute();
        }
    }

    /**
     * Htmlの設定画面を解析する
     */
    private class HtmlLoadTask extends AsyncTask<Void, String, FormElement> {
        @Override
        protected void onPreExecute() {
            getPreferenceScreen().removeAll();
            findViewById(R.id.vOkButton).setTag(null);
        }

        @Override
        protected FormElement doInBackground(Void... params) {
            URL u = null;
            try {
                u = new URL("http://localhost:" + mRunningPort + "/html/ja/settings.html");
                Document doc = Jsoup.parse(u.openStream(), "utf8", u.toString());
                return (FormElement) doc.select("form")
                        .first();
            } catch (MalformedURLException e) {
                throw new RuntimeException(e);
            } catch (IOException e) {
                Log.e(TAG, "Jsoup.parse(): " + u, e);
                publishProgress(getString(R.string.t_failed) +
                        ": Jsoup.parse()\n " + e.getMessage() + "\n " + u);
                return null;
            }
        }

        @Override
        protected void onProgressUpdate(String... values) {
            showToast(TextUtils.join("", values));
        }

        @Override
        protected void onPostExecute(FormElement form) {
            if (form != null) {
                for (Element e : form.elements()) {
                    //System.out.println(e);
                }
                onLoadSuccess(form);
            }
        }
    }

    private void showToast(CharSequence msg) {
        Toast.makeText(this, msg, Toast.LENGTH_LONG)
                .show();
    }

    @TargetApi(11)
    private void onLoadSuccess(FormElement form) {
        findViewById(R.id.vOkButton).setTag(form);

        PreferenceFactory[] factories = new PreferenceFactory[]{
                //==基本設定==
                new CategoryPreferenceFactory(R.string.t_category_basic),
                //ポート
                new EditTextPreferenceFactory(R.string.t_port, form, "input[name=port]", INPUT_SIGNED_NUMBER) {
                    @Override
                    protected boolean isValidValue(String val) {
                        try { //無効なポートを設定すると動作が変になる
                            int port = Integer.parseInt(val);
                            return port > 1024 && port < 0xffff;
                        } catch (NumberFormatException e) {
                        }
                        return false;
                    }
                },
                //パスワード

                //最大リレー本数
                new EditTextPreferenceFactory(R.string.t_maxrelays, form, "input[name=maxrelays]", INPUT_SIGNED_NUMBER),

                //Max. Direct
                //new EditTextPreferenceFactory(R.string.t_maxdirect, form, "input[name=maxdirect]", INPUT_SIGNED_NUMBER),

                //==ネットワーク==
                new CategoryPreferenceFactory(R.string.t_category_network),
                //イエローページ1
                //new EditTextPreferenceFactory(R.string.t_yp, form, "input[name=yp]", INPUT_TEXT),
                //イエローページ2
                //new EditTextPreferenceFactory(R.string.t_yp2, form, "input[name=yp2]", INPUT_TEXT),
                //最大帯域幅 (Kbits/s)
                new EditTextPreferenceFactory(R.string.t_maxup, form, "input[name=maxup]", INPUT_SIGNED_NUMBER),

                //チャンネル毎の最大数
                ///Max. Controls In
                ///Max. Connections In


                //==拡張設定==
                new CategoryPreferenceFactory(R.string.t_category_detail),
                //オートキープ
                new SelectPreferenceFactory(R.string.t_autoRelayKeep, form, "select[name=autoRelayKeep]", R.array.t_ent_autoRelayKeep, R.array.t_entval_autoRelayKeep),
                //オート最大リレー数設定(0:無効，設定値:上限)
                new EditTextPreferenceFactory(R.string.t_autoMaxRelaySetting, form, "input[name=autoMaxRelaySetting]", INPUT_SIGNED_NUMBER),
                //オートBumpスキップカウント
                new EditTextPreferenceFactory(R.string.t_autoBumpSkipCount, form, "input[name=autoBumpSkipCount]", INPUT_SIGNED_NUMBER),
                //壁蹴り開始リレー数(0:無効)
                new EditTextPreferenceFactory(R.string.t_kickPushStartRelays, form, "input[name=kickPushStartRelays]", INPUT_SIGNED_NUMBER),
                //壁蹴り間隔(秒 >= 60)
                //ホスト逆引き
                //(配信時)直下リレー不可ホスト自動切断
                //(配信時)VP版のみ直下接続受け入れ
                //自動切断ホスト情報保持時間(秒、0:無効)
                //index.txt チャンネル毎の最大リレー本数

        };

        PreferenceScreen screen = getPreferenceScreen();
        PreferenceGroup group = screen;
        for (PreferenceFactory f : factories) {
            if (f instanceof CategoryPreferenceFactory) {
                group = (PreferenceGroup) f.createPreference(this);
                screen.addPreference(group);
                continue;
            }
            Preference p = f.createPreference(this);
            if (BuildConfig.VERSION_CODE >= 11) {
                p.setIcon(android.R.drawable.ic_menu_preferences);
            }
            group.addPreference(p);
        }
    }

    //OKボタン
    @Override
    public void onClick(View v) {
        v.setEnabled(false);//二度押し防止

        FormElement form = (FormElement) v.getTag();
        if (form != null) {
            //System.err.println(form.formData());
            String newPort = form.parent()
                    .select("input[name=port]")
                    .attr("value");
            if (newPort.equals("" + mRunningPort)) {
                setResult(RESULT_OK);
            } else {
                setResult(RESULT_PORT_CHANGED);
            }
            new SubmitAndFinishTask().execute(form);
        } else {
            //外部ブラウザでの設定が終了した
            new PortChangeCheckAndFinishTask().execute();
        }
    }

    /**
     * formをsubmitして終了
     */
    private class SubmitAndFinishTask extends AsyncTask<FormElement, String, Boolean> {
        @Override
        protected Boolean doInBackground(FormElement... params) {
            FormElement form = params[0];

            String newPort = form.parent().select("input[name=port]")
                    .attr("value");
            boolean portNoChange = newPort.equals("" + mRunningPort);
            try {
                Connection.Response r = form.submit()
                        .timeout(3 * 1000)
                        .followRedirects(false)
                        .execute();
                publishProgress(getString(R.string.t_success) + ": submit()");
            } catch (IOException e) {
                Log.e(TAG, "FormElement.submit()", e);
                publishProgress(getString(R.string.t_failed) + ": submit()\n" + e.getMessage());
            }
            return portNoChange;
        }

        @Override
        protected void onProgressUpdate(String... values) {
            showToast(TextUtils.join("", values));
        }

        @Override
        protected void onPostExecute(Boolean portNoChange) {
            if (portNoChange)
                setResult(RESULT_OK);
            else
                setResult(RESULT_PORT_CHANGED);
            finish();
        }
    }

    //外部ブラウザでの設定で、ポート変更されたか
    private class PortChangeCheckAndFinishTask extends AsyncTask<Void, Void, Boolean> {
        @Override
        protected Boolean doInBackground(Void... params) {
            try {
                URL u = new URL("http://localhost:" + mRunningPort + "/");
                Jsoup.parse(u, 2 * 1000);
                return true;
            } catch (MalformedURLException e) {
                throw new RuntimeException(e);
            } catch (IOException e) {
                return false;
            }
        }

        @Override
        protected void onPostExecute(Boolean isLive) {
            if (isLive)
                setResult(RESULT_OK);
            else
                setResult(RESULT_PORT_CHANGED);
            finish();
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_html_settings:
                launchHtmlSetting();
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void launchHtmlSetting() {
        getPreferenceScreen().removeAll();
        findViewById(R.id.vOkButton).setTag(null);

        Uri u = Uri.parse("http://localhost:" + mRunningPort + "/");
        startActivity(new Intent(Intent.ACTION_VIEW, u));
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.setting_menu, menu);
        return true;
    }

}
