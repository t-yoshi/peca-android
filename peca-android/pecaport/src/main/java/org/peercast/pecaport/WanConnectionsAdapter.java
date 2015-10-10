package org.peercast.pecaport;

import android.support.annotation.StringRes;
import android.support.annotation.UiThread;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.ListUtils;
import org.apache.commons.collections4.Predicate;
import org.fourthline.cling.support.model.Connection;


import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * 複数の接続先があるルーターに対応する、Spinner用のアダプタ
 *
 * @author (c) 2015, T Yoshizawa
 * Dual licensed under the MIT or GPL licenses.
 * */
public class WanConnectionsAdapter extends BaseAdapter {
    private final List<WanConnection> mConnections = new ArrayList<>();

    @StringRes
    private static int getStatusString(Connection.Status s) {
        switch (s) {
            case Connected:
                return R.string.t_status_connected;
            case Connecting:
                return R.string.t_status_connecting;
            case Disconnected:
                return R.string.t_status_disconnected;
            case Disconnecting:
                return R.string.t_status_disconnecting;
            case PendingDisconnect:
                return R.string.t_status_pending_disconnect;
            case Unconfigured:
                return R.string.t_status_unconfigured;
            default:
                return R.string.t_unknown;
        }
    }

    @Override
    public int getCount() {
        return mConnections.size();
    }

    @Override
    public WanConnection getItem(int position) {
        return mConnections.get(position);
    }

    @Override
    public long getItemId(int position) {
        return 0;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        return _getView(position, convertView, parent,
                R.layout.wan_connection_item);
    }

    @Override
    public View getDropDownView(int position, View convertView, ViewGroup parent) {
        return _getView(position, convertView, parent,
                R.layout.wan_connection_dropdown_item);
    }

    private View _getView(int position, View convertView, ViewGroup parent, int layoutRes) {
        if (convertView == null) {
            LayoutInflater inflater = LayoutInflater.from(parent.getContext());
            convertView = inflater.inflate(layoutRes, parent, false);
        }
        bindView(position, convertView);
        return convertView;
    }

    private void bindView(int position, View view) {
        WanConnection conn = getItem(position);
        TextView vServiceId = (TextView) view.findViewById(R.id.vServiceId);
        TextView vStatus = (TextView) view.findViewById(R.id.vStatus);

        vServiceId.setText(conn.getService().getServiceId().getId());

        if (vStatus != null) {
            vStatus.setText(getStatusString(conn.getStatus()));
            vStatus.setEnabled(conn.getStatus() == Connection.Status.Connected);
        }
    }

    /**接続状態にあるWanConnectionが見つかればそのインデックス。なければ-1。*/
    public int getConnectedPosition() {
        return ListUtils.indexOf(mConnections, new Predicate<WanConnection>() {
            @Override
            public boolean evaluate(WanConnection conn) {
                return conn.getStatus() == Connection.Status.Connected;
            }
        });
    }

    /**接続状態にあるWanConnectionを返す。*/
    public List<WanConnection> getConnected() {
        return (List<WanConnection>) CollectionUtils.select(mConnections, new Predicate<WanConnection>() {
            @Override
            public boolean evaluate(WanConnection conn) {
                return conn.getStatus() == Connection.Status.Connected;
            }
        });
    }

    @UiThread
    public void setConnections(Collection<WanConnection> connections) {
        mConnections.clear();
        mConnections.addAll(connections);
        notifyDataSetChanged();
    }

}
