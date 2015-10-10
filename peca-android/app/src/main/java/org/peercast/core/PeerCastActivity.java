package org.peercast.core;


import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.UiThread;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.view.MenuItem;
import android.widget.TextView;

import org.apache.commons.collections4.CollectionUtils;

import static org.peercast.core.PeerCastServiceController.MSG_GET_APPLICATION_PROPERTIES;
import static org.peercast.core.PeerCastServiceController.MSG_GET_CHANNELS;
import static org.peercast.core.PeerCastServiceController.MSG_GET_STATS;

/**
 * (c) 2015, T Yoshizawa
 * Dual licensed under the MIT or GPL licenses.
 */
public class PeerCastActivity extends AppCompatActivity implements
        PeerCastServiceController.OnPeerCastEventListener,
        PeerCastServiceController.OnServiceResultListener {

    private int mRunningPort;
    private PeerCastServiceController mPecaController;
    private static final String TAG = "PeerCastActivity";

    private Util.Timer mRefresh;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.peercast_activity);

        getSupportFragmentManager().beginTransaction()
                .replace(R.id.vFragContainer, new PeerCastFragment(), PeerCastFragment.TAG)
                .commit();

        mRefresh = new Util.Timer(new Runnable() {
            @Override
            @UiThread
            public void run() {
                if (!mPecaController.isConnected()) {
                    TextView vBandwidth = (TextView) findViewById(R.id.vBandwidth);
                    if (vBandwidth != null)
                        vBandwidth.setText("Stopped.");
                    return;
                }
                mPecaController.sendCommand(MSG_GET_APPLICATION_PROPERTIES, PeerCastActivity.this);
                mPecaController.sendCommand(MSG_GET_CHANNELS, PeerCastActivity.this);
                mPecaController.sendCommand(MSG_GET_STATS, PeerCastActivity.this);
            }
        }, 5000);

        mPecaController = new PeerCastServiceController(getApplicationContext());
        mPecaController.setOnPeerCastEventListener(this);
        mPecaController.bindService();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        //設定画面から戻ってきた
        if (resultCode == SettingActivity.RESULT_PORT_CHANGED) {
            showAlertDialog(R.string.t_info, getString(R.string.msg_please_restart), true);
        }
    }

    PeerCastServiceController getPeerCastServiceController() {
        return mPecaController;
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
    public void onResume() {
        super.onResume();
        mRefresh.start(2000);
    }

    @Override
    public void onPause() {
        super.onPause();
        mRefresh.cancel();
    }

    @Override
    public void onServiceResult(int what, Bundle data) {
        switch (what) {
            case MSG_GET_CHANNELS:
            case MSG_GET_STATS:
                break;

            case MSG_GET_APPLICATION_PROPERTIES:
                mRunningPort = data.getInt("port");
                break;

            default:
                throw new IllegalArgumentException("what=" + what);
        }

        for (Fragment frag : CollectionUtils.emptyIfNull(getSupportFragmentManager().getFragments())) {
            //Log.d(TAG, ""+frag);
            if (frag instanceof PeerCastServiceController.OnServiceResultListener)
                ((PeerCastServiceController.OnServiceResultListener) frag).onServiceResult(what, data);
        }
    }


    @Override
    public void onDestroy() {
        super.onDestroy();
        mPecaController.unbindService();
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch (item.getItemId()) {
            case android.R.id.home: {
                FragmentManager man = getSupportFragmentManager();
                if (man.getBackStackEntryCount() > 0)
                    man.popBackStack();
                return true;
            }
            case R.id.menu_upnp_fragment:
                startFragment(new PecaPortFragment());
                return true;

            case R.id.menu_view_log:
                startFragment(new LogViewerFragment());
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }

    void startFragment(Fragment frag) {
        getSupportFragmentManager().beginTransaction()
                .addToBackStack(null)
                .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
                .replace(R.id.vFragContainer, frag)
                .commit();
    }


    public void showAlertDialog(int title, String msg, final boolean isOkActivityFinish) {
        new AlertDialog.Builder(this)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setTitle(title)
                .setMessage(msg)
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (isOkActivityFinish)
                            finish();
                        dialog.dismiss();
                    }
                })
                .show();
    }
}
