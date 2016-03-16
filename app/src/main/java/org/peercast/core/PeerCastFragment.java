package org.peercast.core;


import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ExpandableListView;
import android.widget.TextView;
import android.widget.Toast;

import static org.peercast.core.PeerCastServiceController.MSG_GET_APPLICATION_PROPERTIES;
import static org.peercast.core.PeerCastServiceController.MSG_GET_CHANNELS;
import static org.peercast.core.PeerCastServiceController.MSG_GET_STATS;

/**
 * (c) 2015, T Yoshizawa
 * Dual licensed under the MIT or GPL licenses.
 */
public class PeerCastFragment extends Fragment implements
        PeerCastServiceController.OnServiceResultListener {

    static final String TAG = "PeerCastFragment";
    private int mRunningPort;

    private GuiListAdapter mListAdapter;


    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.peercast_fragment, container, false);
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        setHasOptionsMenu(true);
        AppCompatActivity act = (AppCompatActivity) getActivity();
        ActionBar actionBar = act.getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(false);
        actionBar.setTitle(R.string.app_name);

        ExpandableListView lv = (ExpandableListView) getView().findViewById(R.id.vListChannel);
        mListAdapter = new GuiListAdapter(getContext());
        lv.setAdapter(mListAdapter);

        registerForContextMenu(lv);
    }


    public PeerCastActivity getActivity2() {
        return (PeerCastActivity) getActivity();
    }

    /**
     * @see PeerCastActivity#onServiceResult(int, Bundle)
     */
    @Override
    public void onServiceResult(int what, Bundle data) {
        if (getView() == null)
            return;

        switch (what) {
            case MSG_GET_CHANNELS:
                mListAdapter.setChannels(Channel.fromNativeResult(data));
                break;

            case MSG_GET_STATS:
                TextView vBandwidth = (TextView) getView().findViewById(R.id.vBandwidth);
                Stats stats = Stats.fromNativeResult(data);
                String s = String.format("R: %.1fkbps S: %.1fkbps   Port: %d ", stats.getInBytes() / 1000f * 8, stats.getOutBytes() / 1000f * 8, mRunningPort);
                vBandwidth.setText(s);
                break;

            case MSG_GET_APPLICATION_PROPERTIES:
                mRunningPort = data.getInt("port");
                break;
        }
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.peercast_menu, menu);
    }


    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        boolean started = mRunningPort > 0;
        //menu.findItem(R.id.menu_html_settings).setEnabled(started);
        menu.findItem(R.id.menu_settings).setEnabled(started);
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        ExpandableListView.ExpandableListContextMenuInfo info = (ExpandableListView.ExpandableListContextMenuInfo) menuInfo;

        int type = ExpandableListView.getPackedPositionType(info.packedPosition);
        int gPos = ExpandableListView.getPackedPositionGroup(info.packedPosition);
        int cPos = ExpandableListView.getPackedPositionChild(info.packedPosition);

        if (type == ExpandableListView.PACKED_POSITION_TYPE_GROUP) {
            // チャンネル
            getActivity().getMenuInflater()
                    .inflate(R.menu.channel_context_menu, menu);
            Channel ch = mListAdapter.getGroup(gPos);
            menu.setHeaderTitle(ch.getInfo()
                    .getName());
            MenuItem mKeep = menu.findItem(R.id.menu_ch_keep);
            mKeep.setChecked(ch.isStayConnected());
        } else if (type == ExpandableListView.PACKED_POSITION_TYPE_CHILD) {
            // サーヴァント
            getActivity().getMenuInflater()
                    .inflate(R.menu.servent_context_menu, menu);
            Servent svt = mListAdapter.getChild(gPos, cPos);
            menu.setHeaderTitle(svt.getHost());
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Intent settingIntent = new Intent(getContext(), SettingActivity.class);

        switch (item.getItemId()) {
            case R.id.menu_remove_all_channel:
                for (Channel ch : mListAdapter.getChannels()) {
                    if (ch.getLocalRelays() + ch.getLocalListeners() == 0)
                        getActivity2().getPeerCastServiceController().disconnectChannel(ch.getChannel_ID());
                }
                return true;

            case R.id.menu_html_settings:
                settingIntent.putExtra(SettingActivity.LAUNCH_HTML, true);
            case R.id.menu_settings: {
                settingIntent.putExtra(SettingActivity.PORT, mRunningPort);
                startActivityForResult(settingIntent, 0);
                //overridePendingTransition(0, 0);
                return true;
            }

            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        PeerCastServiceController controller = getActivity2().getPeerCastServiceController();

        ExpandableListView.ExpandableListContextMenuInfo info = (ExpandableListView.ExpandableListContextMenuInfo) item.getMenuInfo();
        int type = ExpandableListView.getPackedPositionType(info.packedPosition);
        int gPos = ExpandableListView.getPackedPositionGroup(info.packedPosition);
        int cPos = ExpandableListView.getPackedPositionChild(info.packedPosition);

        Channel ch = (Channel) mListAdapter.getGroup(gPos);

        switch (item.getItemId()) {

            case R.id.menu_ch_disconnect:
                Log.i(TAG, "Disconnect channel: " + ch);
                controller.disconnectChannel(ch.getChannel_ID());
                return true;

            case R.id.menu_ch_keep:
                Log.i(TAG, "Keep channel: " + ch);
                controller.setChannelKeep(ch.getChannel_ID(), !item.isChecked());
                return true;

            case R.id.menu_ch_play:
                Uri u = getStreamUri(ch);
                Intent intent = new Intent(Intent.ACTION_VIEW, u);
                try {
                    showToast(u.toString());
                    startActivity(intent);
                } catch (ActivityNotFoundException e) {
                    getActivity2().showAlertDialog(R.string.t_error, e.getLocalizedMessage(), false);
                }
                return true;

            case R.id.menu_ch_bump:
                Log.i(TAG, "Bump channel: " + ch);
                controller.bumpChannel(ch.getChannel_ID());
                return true;

            case R.id.menu_svt_disconnect:
                //直下切断
                Servent svt = (Servent) mListAdapter.getChild(gPos, cPos);
                Log.i(TAG, "Disconnect servent: " + svt);
                controller.disconnectServent(svt.getServent_ID());
                return true;

            default:
                return super.onContextItemSelected(item);
        }
    }

    private void showToast(String msg) {
        Toast.makeText(getContext(), msg, Toast.LENGTH_LONG)
                .show();
    }

    /**
     * チャンネル再生用のURL
     */
    public Uri getStreamUri(Channel ch) {
        return Util.getStreamUrl(ch, mRunningPort);
    }


}
