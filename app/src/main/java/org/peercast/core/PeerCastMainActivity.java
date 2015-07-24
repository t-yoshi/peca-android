package org.peercast.core;

/**
 * (c) 2014, T Yoshizawa
 * <p/>
 * Dual licensed under the MIT or GPL licenses.
 */

import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ExpandableListView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Timer;
import java.util.TimerTask;

import static org.peercast.core.PeerCastServiceController.MSG_GET_APPLICATION_PROPERTIES;
import static org.peercast.core.PeerCastServiceController.MSG_GET_CHANNELS;
import static org.peercast.core.PeerCastServiceController.MSG_GET_STATS;

public class PeerCastMainActivity extends AppCompatActivity
        implements PeerCastServiceController.OnPeerCastEventListener, PeerCastServiceController.OnServiceResultListener {

    private static final String TAG = "PeerCastMainActivity";

    private GuiListAdapter mListAdapter;
    private PeerCastServiceController mPecaController;
    private Timer mRefreshTimer;
    private int mRunningPort;


    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);

        setContentView(R.layout.main_activity);

        ExpandableListView lv = (ExpandableListView) findViewById(R.id.vListChannel);
        mListAdapter = new GuiListAdapter(this);
        lv.setAdapter(mListAdapter);

        registerForContextMenu(lv);

        mPecaController = new PeerCastServiceController(getApplicationContext());
        mPecaController.setOnPeerCastEventListener(this);
        mPecaController.bindService();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        //設定画面から戻ってきた
        if (resultCode == SettingActivity.RESULT_PORT_CHANGED) {
            showAlertDialog(R.string.t_info, getString(R.string.msg_please_restart), true);
        }
    }

    @Override
    public void onResume() {
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
    public void onPause() {
        super.onPause();
        mRefreshTimer.cancel();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mPecaController.unbindService();
    }

    // タイマーによってUIスレッドから呼ばれる。
    private void onListRefresh() {
        if (!mPecaController.isConnected()) {
            TextView vBandwidth = (TextView) findViewById(R.id.vBandwidth);
            vBandwidth.setText("Stopped.");
            return;
        }
        mPecaController.sendCommand(MSG_GET_APPLICATION_PROPERTIES, this);
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

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }


    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        boolean started = mRunningPort > 0;
        menu.findItem(R.id.menu_html_settings).setEnabled(started);
        menu.findItem(R.id.menu_settings).setEnabled(started);
        return true;
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        ExpandableListView.ExpandableListContextMenuInfo info = (ExpandableListView.ExpandableListContextMenuInfo) menuInfo;

        int type = ExpandableListView.getPackedPositionType(info.packedPosition);
        int gPos = ExpandableListView.getPackedPositionGroup(info.packedPosition);
        int cPos = ExpandableListView.getPackedPositionChild(info.packedPosition);

        if (type == ExpandableListView.PACKED_POSITION_TYPE_GROUP) {
            // チャンネル
            getMenuInflater()
                    .inflate(R.menu.channel_menu, menu);
            Channel ch = (Channel) mListAdapter.getGroup(gPos);
            menu.setHeaderTitle(ch.getInfo()
                    .getName());
            MenuItem mKeep = menu.findItem(R.id.menu_ch_keep);
            mKeep.setChecked(ch.isStayConnected());
        } else if (type == ExpandableListView.PACKED_POSITION_TYPE_CHILD) {
            // サーヴァント
            getMenuInflater()
                    .inflate(R.menu.servent_menu, menu);
            Servent svt = (Servent) mListAdapter.getChild(gPos, cPos);
            menu.setHeaderTitle(svt.getHost());
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Intent settingIntent = new Intent(this, SettingActivity.class);

        switch (item.getItemId()) {
            case R.id.menu_remove_all_channel:
                for (Channel ch : mListAdapter.getChannels()) {
                    if (ch.getLocalRelays() + ch.getLocalListeners() == 0)
                        mPecaController.disconnectChannel(ch.getChannel_ID());
                }
                return true;

            case R.id.menu_html_settings:
                settingIntent.putExtra(SettingActivity.LAUNCH_HTML, true);
            case R.id.menu_settings: {
                settingIntent.putExtra(SettingActivity.PORT, mRunningPort);
                startActivityForResult(settingIntent, 0);
                overridePendingTransition(0, 0);
                return true;
            }

            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        ExpandableListView.ExpandableListContextMenuInfo info = (ExpandableListView.ExpandableListContextMenuInfo) item.getMenuInfo();
        int type = ExpandableListView.getPackedPositionType(info.packedPosition);
        int gPos = ExpandableListView.getPackedPositionGroup(info.packedPosition);
        int cPos = ExpandableListView.getPackedPositionChild(info.packedPosition);

        Channel ch = (Channel) mListAdapter.getGroup(gPos);

        switch (item.getItemId()) {

            case R.id.menu_ch_disconnect:
                Log.i(TAG, "Disconnect channel: " + ch);
                mPecaController.disconnectChannel(ch.getChannel_ID());
                return true;

            case R.id.menu_ch_keep:
                Log.i(TAG, "Keep channel: " + ch);
                mPecaController.setChannelKeep(ch.getChannel_ID(), !item.isChecked());
                return true;

            case R.id.menu_ch_play:
                Uri u = getStreamUri(ch);
                Intent intent = new Intent(Intent.ACTION_VIEW, u);
                try {
                    showToast(u.toString());
                    startActivity(intent);
                } catch (ActivityNotFoundException e) {
                    showAlertDialog(R.string.t_error, e.getLocalizedMessage(), false);
                }
                return true;

            case R.id.menu_ch_bump:
                Log.i(TAG, "Bump channel: " + ch);
                mPecaController.bumpChannel(ch.getChannel_ID());
                return true;

            case R.id.menu_svt_disconnect:
                //直下切断
                Servent svt = (Servent) mListAdapter.getChild(gPos, cPos);
                Log.i(TAG, "Disconnect servent: " + svt);
                mPecaController.disconnectServent(svt.getServent_ID());
                return true;

            default:
                return super.onContextItemSelected(item);
        }
    }


    private void showAlertDialog(int title, String msg, final boolean isOkActFinish) {
        new AlertDialog.Builder(this)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setTitle(title)
                .setMessage(msg)
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (isOkActFinish)
                            finish();
                        dialog.dismiss();
                    }
                })
                .show();
    }


    private void showToast(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_LONG)
                .show();
    }

    /**
     * チャンネル再生用のURL
     */
    public Uri getStreamUri(Channel ch) {
        if (ChannelInfo.T_WMV == ch.getInfo()
                .getType()) {
            return Uri.parse(String.format("mmsh://localhost:%d/stream/%s.wmv", mRunningPort, ch.getID()));
        } else {
            // とりあえずflvの可能性が高い
            return Uri.parse(String.format("http://localhost:%d/stream/%s.flv", mRunningPort, ch.getID()));
        }
    }


}
