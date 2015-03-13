package org.peercast.core;

/**
 * (c) 2014, T Yoshizawa
 *
 * Dual licensed under the MIT or GPL licenses.
 */

import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.AsyncTask;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GuiListAdapter extends BaseExpandableListAdapter {

    private static final String TAG = "GuiListAdapter";

    private final Context mContext;
    private final HostNameCache mHostNameCache = new HostNameCache();
    private List<Channel> mChannels = Collections.emptyList();
    private final boolean mIsTablet;

    GuiListAdapter(Context c) {
        mContext = c;
        Configuration config = c.getResources().getConfiguration();
        mIsTablet = (config.screenLayout & Configuration.SCREENLAYOUT_SIZE_MASK) //
                 >= Configuration.SCREENLAYOUT_SIZE_LARGE;
    }


    public void setChannels(List<Channel> channels) {
        if (channels == null)
            throw new IllegalArgumentException();
        mChannels = channels;
        notifyDataSetChanged();
    }


    public List<Channel> getChannels() {
        return mChannels;
    }

    private class GroupViewHolder {
        final ImageView vChIcon;
        final TextView vChName;
        final TextView vChRelays;
        final TextView vChBitrate;
        final ImageView vChServents;

        GroupViewHolder(View v) {
            vChIcon = (ImageView) v.findViewById(R.id.vChIcon);
            vChName = (TextView) v.findViewById(R.id.vChName);
            vChRelays = (TextView) v.findViewById(R.id.vChRelays);
            vChBitrate = (TextView) v.findViewById(R.id.vChBitrate);
            vChServents = (ImageView) v.findViewById(R.id.vChServents);
            if (!mIsTablet)
                vChServents.setVisibility(View.GONE);
        }
    }

    @Override
    public View getGroupView(int pos, boolean isExpanded, View convertView, ViewGroup parent) {
        if (convertView == null) {
            LayoutInflater inflater = LayoutInflater.from(mContext);
            convertView = inflater.inflate(R.layout.ch_item_g, parent, false);
            convertView.setTag(new GroupViewHolder(convertView));
        }
        bindGroupView((GroupViewHolder) convertView.getTag(), mChannels.get(pos));
        return convertView;
    }

    private void bindGroupView(GroupViewHolder vh, Channel ch) {
        ChannelInfo info = ch.getInfo();

        vh.vChIcon.setImageResource(getChannelStatusIcon(ch));
        vh.vChName.setText(info.getName());

        String s = String.format("%d/%d  -  [%d/%d]", ch.getTotalListeners(), ch.getTotalRelays(), ch.getLocalListeners(), ch.getLocalRelays());
        vh.vChRelays.setText(s);

        s = String.format("% 5d kbps", info.getBitrate());
        vh.vChBitrate.setText(s);

        Bitmap bm = createServentMonitor(ch.getServents());
        vh.vChServents.setImageBitmap(bm);
    }

    private Bitmap createServentMonitor(List<Servent> servents) {
        // とりあえず10コ分の"■ ■ ■ ■ ■ ■ ■ ■ ■ ■"
        Bitmap bmp = Bitmap.createBitmap(12 * 10, 12, Bitmap.Config.ARGB_8888);

        Canvas cv = new Canvas(bmp);

        Paint pWhite = new Paint();
        pWhite.setColor(0x40ffffff);

        for (int i = 0; i < servents.size() && i < 10; i++) {
            Servent svt = servents.get(i);

            Paint pRect = new Paint();
            if (svt.isSetInfoFlg()) {
                if (svt.isFirewalled()) {
                    pRect.setColor(Color.RED);
                } else if (svt.isRelay()) {
                    pRect.setColor(Color.GREEN);
                } else {
                    if (svt.getNumRelays() > 0) {
                        pRect.setColor(Color.BLUE);
                    } else {
                        pRect.setColor(0xff800080); // Purple
                    }
                }
            } else {
                pRect.setColor(Color.BLACK);
            }
            cv.drawRect(1 + 12 * i, 1, 11 + 12 * i, 11, pRect);
        }
        return bmp;
    }

    private int getChannelStatusIcon(Channel ch) {
        switch (ch.getStatus()) {
            case Channel.S_IDLE:
                return R.drawable.ic_st_idle;

            case Channel.S_SEARCHING:
            case Channel.S_CONNECTING:
                return R.drawable.ic_st_connect;

            case Channel.S_RECEIVING:
                int nowTimeSec = (int) (System.currentTimeMillis() / 1000);

                if (ch.getSkipCount() > 2 && ch.getLastSkipTime() + 120 > nowTimeSec) {
                    return R.drawable.ic_st_conn_ok_skip;
                }
                return R.drawable.ic_st_conn_ok;

            case Channel.S_BROADCASTING:
                return R.drawable.ic_st_broad_ok;

            case Channel.S_ERROR:
                // if (ch && ch->bumped)
                // img = img_connect;
                return R.drawable.ic_st_error;
        }
        return R.drawable.ic_st_idle;

    }

    private int getServentStatusIcon(Channel ch, Servent svt) {
        if (ch.getStatus() != Channel.S_RECEIVING)
            return R.drawable.ic_empty;

        int nowTimeSec = (int) (System.currentTimeMillis() / 1000);
        if (ch.getSkipCount() > 2 && ch.getLastSkipTime() + 120 > nowTimeSec) {
            if (svt.isRelay())
                return R.drawable.ic_st_conn_ok_skip;
            if (svt.getNumRelays() > 0)
                return R.drawable.ic_st_conn_full_skip;
            return R.drawable.ic_st_conn_over_skip;
        } else {
            if (svt.isRelay())
                return R.drawable.ic_st_conn_ok;
            if (svt.getNumRelays() > 0)
                return R.drawable.ic_st_conn_full;
            return R.drawable.ic_st_conn_over;
        }
    }

    @Override
    public Object getChild(int gPos, int cPos) {
        List<Servent> servents = mChannels.get(gPos).getServents();
        return servents.get(cPos);
    }

    @Override
    public long getChildId(int gPos, int cPos) {
        return 0;
    }

    private static class ChildViewHolder {
        final ImageView vSvtIcon;
        final TextView vSvtVersion;
        final TextView vSvtRelays;
        final TextView vSvtHost;

        ChildViewHolder(View v) {
            vSvtIcon = (ImageView) v.findViewById(R.id.vSvtIcon);
            vSvtVersion = (TextView) v.findViewById(R.id.vSvtVersion);
            vSvtRelays = (TextView) v.findViewById(R.id.vSvtRelays);
            vSvtHost = (TextView) v.findViewById(R.id.vSvtHost);
        }
    }

    @Override
    public View getChildView(int gPos, int cPos, boolean isLastChild, View convertView, ViewGroup parent) {
        if (convertView == null) {
            LayoutInflater inflater = LayoutInflater.from(mContext);
            convertView = inflater.inflate(R.layout.ch_item_c, parent, false);
            convertView.setTag(new ChildViewHolder(convertView));
        }
        bindChildView((ChildViewHolder) convertView.getTag(), (Channel) getGroup(gPos), (Servent) getChild(gPos, cPos));
        return convertView;
    }

    private void bindChildView(ChildViewHolder vh, Channel ch, Servent svt) {
        vh.vSvtIcon.setImageResource(getServentStatusIcon(ch, svt));

        String hostName = mHostNameCache.getHostName(svt.getHost());
        // (Version) 0/0 123.0.0.0(hostname)
        vh.vSvtVersion.setText("(" + svt.getVersion() + ")");

        vh.vSvtRelays.setText(String.format("%d/%d", svt.getTotalListeners(), svt.getTotalRelays()));

        vh.vSvtHost.setText(String.format("%s (%s)", svt.getHost(), hostName));

        // tv.setText(String.format("(%s) %d/%d %s(%s)",
        // svt.getTotalListeners(),
        // svt.getTotalRelays(), svt.getHost(), hostName));
    }

    @Override
    public int getChildrenCount(int gPos) {
        return mChannels.get(gPos).getServents().size();
    }

    @Override
    public Object getGroup(int pos) {
        return mChannels.get(pos);
    }

    @Override
    public int getGroupCount() {
        return mChannels.size();
    }

    @Override
    public long getGroupId(int pos) {
        return pos;
    }

    @Override
    public boolean hasStableIds() {
        return false;
    }

    @Override
    public boolean isChildSelectable(int gPos, int cPos) {
        return true;
    }

    /**
     * ipから逆引きしてホスト名を得る。
     */
    private static class HostNameCache {
        // "192.0.43.10" -> "example.com"
        private Map<String, String> mHostNames = new HashMap<>();

        public synchronized String getHostName(final String ip) {
            if (!mHostNames.containsKey(ip)) {
                mHostNames.put(ip, "lookup..");
                new LookupTask().execute(ip);
            }
            return mHostNames.get(ip);
        }

        private class LookupTask extends AsyncTask<String, Void, Void> {
            @Override
            protected Void doInBackground(String... params) {
                String ip = params[0];
                String hostName;
                try {
                    hostName = InetAddress.getByName(ip).getHostName();
                } catch (UnknownHostException e) {
                    hostName = "unknown host";
                }
                mHostNames.put(ip, hostName);
                return null;
            }
        }

    }

}