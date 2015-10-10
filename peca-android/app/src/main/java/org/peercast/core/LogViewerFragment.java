package org.peercast.core;

import android.content.Context;
import android.content.res.Configuration;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.ColorRes;
import android.support.v4.app.ListFragment;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.text.SpannableStringBuilder;
import android.text.method.LinkMovementMethod;
import android.text.style.URLSpan;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author (c) 2015, T Yoshizawa
 * Dual licensed under the MIT or GPL licenses.
 */
public class LogViewerFragment extends ListFragment {
    private static final String TAG = "LogViewerFragment";
    private LogAdapter mAdapter;
    private boolean mNarrowWidth;
    private AsyncTask mParserTask;


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.logviewer_fragment, container, false);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        setHasOptionsMenu(true);

        AppCompatActivity act = (AppCompatActivity) getActivity();
        ActionBar actionBar = act.getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setTitle(R.string.t_view_log);

        mNarrowWidth = getResources().getConfiguration().screenWidthDp < 800;

        mAdapter = new LogAdapter();
        setListAdapter(mAdapter);

        doLogParse();
    }

    private void doLogParse() {
        if (mParserTask != null)
            return;
        PeerCastApplication app = (PeerCastApplication)getActivity().getApplication();
        Collection<File> logFiles = app.getLogFiles();
        mParserTask = new LogParseTask().execute(logFiles.toArray(new File[0]));
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        mNarrowWidth = newConfig.screenWidthDp < 800;
        doLogParse();
    }

    private class LogParseTask extends AsyncTask<File, Void, List<CSVRecord>> {
        @Override
        protected void onPreExecute() {
            mAdapter.mRecords.clear();
        }

        @Override
        protected List<CSVRecord> doInBackground(File... files) {
            List<CSVRecord> records = new ArrayList<>(512);
            Charset utf8 = Charset.forName("utf8");
            for (File f : files) {
                try {
                    CSVParser parser = CSVParser.parse(f, utf8, CSVFormat.DEFAULT);
                    records.addAll(parser.getRecords());
                } catch (IOException e) {
                    Log.e(TAG, "LogParseTask", e);
                }
            }
            return records;
        }

        @Override
        protected void onPostExecute(List<CSVRecord> records) {
            //if (!mAdapter.isEmpty())
            //    mListView.setSelection(mAdapter.getCount() - 1);]
            mAdapter.mRecords = records;
            mAdapter.notifyDataSetChanged();
            mParserTask = null;
        }
    }

    private class LogAdapter extends BaseAdapter {
        private List<CSVRecord> mRecords = Collections.emptyList();

        @Override
        public int getCount() {
            return mRecords.size();
        }

        @Override
        public CSVRecord getItem(int position) {
            //return mRecords.get(position);
            return mRecords.get(getCount() - position - 1);
        }

        @Override
        public long getItemId(int position) {
            return 0;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            Context c = parent.getContext();
            if (convertView == null) {
                LayoutInflater inflater = LayoutInflater.from(c);
                convertView = inflater.inflate(R.layout.logviewer_line, parent, false);
                convertView.setTag(new Holder(convertView));
            }
            bindView(c, (Holder) convertView.getTag(), getItem(position));
            return convertView;
        }



        private void bindView(Context c, Holder holder, CSVRecord record) {
            holder.vTime.setText(record.get(0));
            String className = record.get(1);
            boolean isAppClasses = className.startsWith("org.peercast.");

            if (mNarrowWidth || className.length() > 64)
                className = StringUtils.substringAfterLast(className, ".");

            holder.vSource.setText(className + "#" + record.get(2) + "()");

            int level = NumberUtils.toInt(record.get(3), 0);
            @ColorRes int color = R.color.md_grey_800;
            if (isAppClasses)
                color = R.color.md_green_800;
            if (level >= 1000)//ERROR
                color = R.color.md_red_800;
            else if (level >= 900)//WARN
                color = R.color.md_orange_800;

            //holder.vMessage.setText(record.get(4));
            holder.setMessage(record.get(4));
            holder.vMessage.setTextColor(c.getResources().getColor(color));
        }


    }

    private static class Holder {
        final TextView vTime;
        final TextView vMessage;
        final TextView vSource;

        Holder(View v) {
            vTime = (TextView) v.findViewById(R.id.vTime);
            vMessage = (TextView) v.findViewById(R.id.vMessage);
            vMessage.setMovementMethod(LinkMovementMethod.getInstance());
            vSource = (TextView) v.findViewById(R.id.vSource);
        }

        private static final Pattern URL_PATTERN = Pattern.compile("http://[^,\\s]+");

        void setMessage(String s){
            SpannableStringBuilder ssb = new SpannableStringBuilder(s);
            Matcher ma = URL_PATTERN.matcher(s);
            int start = 0;
            while (ma.find(start)) {
                start = ma.start();
                int end = ma.end();
                ssb.setSpan(new URLSpan(s.substring(start, end)), start, end, 0);
                start = ma.end();
            }
            vMessage.setText(ssb);
        }
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.logviewer_menu, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_reload:
                doLogParse();
                return true;

            default:
                return super.onOptionsItemSelected(item);

        }
    }
}
