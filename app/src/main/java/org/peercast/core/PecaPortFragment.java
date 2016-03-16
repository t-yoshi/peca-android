package org.peercast.core;

import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.CompoundButton;
import android.widget.Switch;

import org.peercast.pecaport.PecaPortFragmentBase;

import static org.peercast.core.PeerCastServiceController.MSG_GET_APPLICATION_PROPERTIES;

/**
 * * (c) 2015, T Yoshizawa
 * Dual licensed under the MIT or GPL licenses.
 */
public class PecaPortFragment extends PecaPortFragmentBase implements
        PeerCastServiceController.OnServiceResultListener {



    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        setHasOptionsMenu(true);

        AppCompatActivity act = (AppCompatActivity) getActivity();
        ActionBar actionBar = act.getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setTitle(R.string.t_pecaport);

        boolean enabled = Preferences.from(getContext()).isUPnPEnabled();
        if (enabled)
            startDiscoverer();
        getView().setEnabled(enabled);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_reload:
                research();
                return true;

            case R.id.menu_debug:
                setDebugMode(!item.isChecked());
                return true;

            case R.id.menu_close_on_exit:
                Preferences.from(getContext()).putUPnPCloseOnExit(!item.isChecked());
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);

        Preferences prefs = Preferences.from(getContext());
        Switch vEnabled = (Switch) menu.findItem(R.id.menu_enabled).getActionView();
        boolean enabled = prefs.isUPnPEnabled();

        vEnabled.setOnCheckedChangeListener(null);
        vEnabled.setChecked(enabled);
        vEnabled.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                getView().setEnabled(isChecked);
                Preferences.from(getContext()).putUPnPEnabled(isChecked);
                if (isChecked)
                    startDiscoverer();
            }
        });

        menu.findItem(R.id.menu_debug).setChecked(isDebugMode());

        boolean isCoe = prefs.isUPnPCloseOnExit();
        menu.findItem(R.id.menu_close_on_exit).setChecked(isCoe);

        for (int i = 0, count = menu.size(); i < count; i++) {
            MenuItem item = menu.getItem(i);
            if (item.getItemId() == R.id.menu_enabled)
                continue;
            item.setEnabled(enabled);
        }
    }


    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.pecaport_menu, menu);
    }

    @Override
    public void onServiceResult(int msgId, Bundle data) {
        if (getView() == null)
            return;

        switch (msgId) {
            case MSG_GET_APPLICATION_PROPERTIES:
                setRunningPort(data.getInt("port"));
                break;
        }
    }




}
