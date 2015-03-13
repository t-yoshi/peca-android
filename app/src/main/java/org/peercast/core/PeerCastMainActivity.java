package org.peercast.core;

/**
 * (c) 2014, T Yoshizawa
 *
 * Dual licensed under the MIT or GPL licenses.
 */

import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ExpandableListView;
import android.widget.ExpandableListView.ExpandableListContextMenuInfo;
import android.widget.TextView;
import android.widget.Toast;

import org.peercast.core.PeerCastServiceController.OnPeerCastEventListener;
import org.peercast.core.PeerCastServiceController.OnServiceResultListener;

import java.util.Timer;
import java.util.TimerTask;

import static org.peercast.core.PeerCastServiceController.MSG_GET_APPLICATION_PROPERTIES;
import static org.peercast.core.PeerCastServiceController.MSG_GET_CHANNELS;
import static org.peercast.core.PeerCastServiceController.MSG_GET_STATS;

public class PeerCastMainActivity extends ActionBarActivity implements OnPeerCastEventListener, OnServiceResultListener {

    private static final String TAG = "PeerCastMainActivity";

    private GuiListAdapter mListAdapter;
    private PeerCastServiceController mPecaController;
    private Timer mRefreshTimer;
    private int mRunningPort;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.gui);

        ExpandableListView lv = (ExpandableListView) findViewById(R.id.vListChannel);

        mListAdapter = new GuiListAdapter(this);
        lv.setAdapter(mListAdapter);

        mPecaController = new PeerCastServiceController(this);
        mPecaController.setOnPeerCastEventListener(this);

        registerForContextMenu(lv);

        mPecaController.bindService();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu, menu);

        MenuItem removeAllCh = menu.findItem(R.id.menu_remove_all_channel);
        //removeAllCh.setEnabled(!mListAdapter.getChannels().isEmpty());
        removeAllCh.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem menuItem) {
                for (Channel ch : mListAdapter.getChannels()) {
                    if (ch.getLocalRelays() + ch.getLocalListeners() == 0)
                        mPecaController.disconnectChannel(ch.getChannel_ID());
                }
                return true;
            }
        });
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        MenuItem settings = menu.findItem(R.id.menu_settings);
        settings.setEnabled(mRunningPort > 0);
        String settingUrl = "http://localhost:" + mRunningPort + "/";
        settings.setIntent(new Intent(Intent.ACTION_VIEW, Uri.parse(settingUrl)));
        return true;
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
        ExpandableListContextMenuInfo info = (ExpandableListContextMenuInfo) menuInfo;

        int type = ExpandableListView.getPackedPositionType(info.packedPosition);
        int gPos = ExpandableListView.getPackedPositionGroup(info.packedPosition);
        int cPos = ExpandableListView.getPackedPositionChild(info.packedPosition);

        if (type == ExpandableListView.PACKED_POSITION_TYPE_GROUP) {
            // チャンネル
            getMenuInflater().inflate(R.menu.cmenu_channel, menu);
            Channel ch = (Channel) mListAdapter.getGroup(gPos);
            menu.setHeaderTitle(ch.getInfo().getName());
            MenuItem mKeep = menu.findItem(R.id.cmenu_ch_keep);
            mKeep.setChecked(ch.isStayConnected());
        } else if (type == ExpandableListView.PACKED_POSITION_TYPE_CHILD) {
            // サーヴァント
            getMenuInflater().inflate(R.menu.cmenu_servent, menu);
            Servent svt = (Servent) mListAdapter.getChild(gPos, cPos);
            menu.setHeaderTitle(svt.getHost());
        }
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        ExpandableListContextMenuInfo info = (ExpandableListContextMenuInfo) item.getMenuInfo();
        int type = ExpandableListView.getPackedPositionType(info.packedPosition);
        int gPos = ExpandableListView.getPackedPositionGroup(info.packedPosition);
        int cPos = ExpandableListView.getPackedPositionChild(info.packedPosition);

        Channel ch = (Channel) mListAdapter.getGroup(gPos);

        switch (item.getItemId()) {

            case R.id.cmenu_ch_disconnect:
                Log.i(TAG, "Disconnect channel: " + ch);
                mPecaController.disconnectChannel(ch.getChannel_ID());
                return true;

            case R.id.cmenu_ch_keep:
                Log.i(TAG, "Keep channel: " + ch);
                mPecaController.setChannelKeep(ch.getChannel_ID(), !item.isChecked());
                return true;

            case R.id.cmenu_ch_play:
                Uri u = getStreamUri(ch);
                Intent intent = new Intent(Intent.ACTION_VIEW, u);
                try {
                    showToast(u.toString());
                    startActivity(intent);
                } catch (ActivityNotFoundException e) {
                    showErrorDialog(e.getLocalizedMessage());
                }
                return true;

            case R.id.cmenu_ch_bump:
                Log.i(TAG, "Bump channel: " + ch);
                mPecaController.bumpChannel(ch.getChannel_ID());
                return true;

            case R.id.cmenu_svt_disconnect:
                //直下切断
                Servent svt = (Servent) mListAdapter.getChild(gPos, cPos);
                Log.i(TAG, "Disconnect servent: " + svt);
                mPecaController.disconnetServent(svt.getServent_ID());
                return true;

            default:
                return super.onContextItemSelected(item);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        mRefreshTimer = new Timer(true);
        mRefreshTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        onListRefresh();
                    }
                });
            }
        }, 0, 5000);
    }

    @Override
    protected void onPause() {
        super.onPause();
        mRefreshTimer.cancel();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mPecaController.unbindService();
    }


    // タイマーによってUIスレッドから呼ばれる。
    private void onListRefresh() {
        if (!mPecaController.isConnected()) {
            return;
        }
        mPecaController.sendCommand(MSG_GET_CHANNELS, this);
        mPecaController.sendCommand(MSG_GET_STATS, this);
    }

    @Override
    public void onConnectPeerCastService() {
        mPecaController.sendCommand(MSG_GET_APPLICATION_PROPERTIES, this);
    }

    @Override
    public void onDisconnectPeerCastService() {
        mRunningPort = 0;
    }

    private void showErrorDialog(String msg) {
        new AlertDialog.Builder(this).setIcon(android.R.drawable.ic_dialog_alert).setMessage(msg).setTitle("Error").show();
    }

    @Override
    public void onServiceResult(int what, Bundle data) {
        switch (what) {

            case MSG_GET_CHANNELS:
                mListAdapter.setChannels(Channel.fromNativeResult(data));
                break;
            case MSG_GET_STATS:
                TextView vBandwidth = (TextView) findViewById(R.id.vBandwidth);
                Stats stats = Stats.fromNativeResult(data);
                String s = String.format("R: %.1fkbps S: %.1fkbps   Port: %d ", stats.getInBytes() / 1000f * 8, stats.getOutBytes() / 1000f * 8, mRunningPort);
                vBandwidth.setText(s);
                break;
            case MSG_GET_APPLICATION_PROPERTIES:
                mRunningPort = data.getInt("port");
                break;
            default:
                throw new IllegalArgumentException("what=" + what);
        }

    }

    private void showToast(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
    }

    /**
     * チャンネル再生用のURL
     *
     */
    public Uri getStreamUri(Channel ch) {
        if (ChannelInfo.T_WMV == ch.getInfo().getType()) {
            return Uri.parse(String.format("mmsh://localhost:%d/stream/%s.wmv", mRunningPort, ch.getID()));
        } else {
            // とりあえずflvの可能性が高い
            return Uri.parse(String.format("http://localhost:%d/stream/%s.flv", mRunningPort, ch.getID()));
        }
    }
}
