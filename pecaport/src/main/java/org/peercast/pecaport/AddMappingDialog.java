package org.peercast.pecaport;

import android.content.Context;
import android.content.DialogInterface;
import android.os.Build;
import android.support.v7.app.AlertDialog;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.Spinner;
import android.widget.TextView;

import org.apache.commons.lang3.StringUtils;
import org.fourthline.cling.model.types.UnsignedIntegerTwoBytes;
import org.fourthline.cling.support.model.PortMapping;


import java.net.Inet4Address;
import java.util.Collection;

/**
 * (c) 2015, T Yoshizawa
 * Dual licensed under the MIT or GPL licenses.
 */
public class AddMappingDialog implements DialogInterface.OnShowListener {

    private final AlertDialog.Builder mBuilder;
    private final TextView vPrivateIp;
    private final Spinner vConnections;
    private final EditText vInternalPort;
    private final EditText vExternalPort;
    private final EditText vDescription;
    private final CheckBox vSomePort;
    private final RadioButton vTcp;


    private Button mOk;
    private AlertDialog mShowingDialog;

    public interface Listener {
        void onOkClick(WanConnection conn, PortMapping mapping);
    }


    public AddMappingDialog(Context c, int pecaRunningPort,
                            Collection<WanConnection> connected,
                            final Inet4Address clientIp,
                            final Listener listener) {

        String port, description;
        if (pecaRunningPort > 0) {
            port = pecaRunningPort + "";
            description = PecaPortService.DESCRIPTION;
        } else {
            port = "";
            String modelName = StringUtils.substring(Build.MODEL, 0, 16);
            description = String.format("PecaPort(%s)", modelName);
        }


        View view = LayoutInflater.from(c).inflate(R.layout.add_port_dialog, null);
        vPrivateIp = (TextView) view.findViewById(R.id.vPrivateIp);
        vPrivateIp.setText(clientIp.getHostAddress());

        vConnections = (Spinner) view.findViewById(R.id.vConnections);

        vExternalPort = (EditText) view.findViewById(R.id.vExternalPort);
        vExternalPort.addTextChangedListener(mValidPortWatcher);
        vExternalPort.setText(port);

        vInternalPort = (EditText) view.findViewById(R.id.vInternalPort);
        vInternalPort.setText(vExternalPort.getText());
        vInternalPort.addTextChangedListener(mValidPortWatcher);

        vSomePort = (CheckBox) view.findViewById(R.id.vSamePort);
        vSomePort.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    vInternalPort.setText(vExternalPort.getText());
                    vInternalPort.setEnabled(false);
                } else {
                    vInternalPort.setEnabled(true);
                }
            }
        });
        vExternalPort.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                if (vSomePort.isChecked()) {
                    vInternalPort.setText(vExternalPort.getText());
                }
            }
        });

        vTcp = (RadioButton) view.findViewById(R.id.vTcp);
        vDescription = (EditText) view.findViewById(R.id.vDescription);

        vDescription.setText(description);

        WanConnectionsAdapter wanAdapter = new WanConnectionsAdapter();

        wanAdapter.setConnections(connected);
        vConnections.setAdapter(wanAdapter);


        mBuilder = new AlertDialog.Builder(c)
                .setTitle(R.string.t_add_port)
                .setView(view)
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        PortMapping mapping = new PortMapping();
                        mapping.setExternalPort(toUI2Bytes(vExternalPort));
                        mapping.setInternalPort(toUI2Bytes(vInternalPort));
                        mapping.setDescription(vDescription.getText().toString());
                        mapping.setInternalClient(vPrivateIp.getText().toString());
                        mapping.setEnabled(true);
                        mapping.setProtocol(vTcp.isChecked() ?
                                PortMapping.Protocol.TCP : PortMapping.Protocol.UDP);
                        listener.onOkClick(
                                (WanConnection) vConnections.getSelectedItem(),
                                mapping);
                    }
                })
                .setIcon(R.drawable.ic_plus_box)
                .setNegativeButton(android.R.string.cancel, null)
                .setCancelable(false);
    }

    private final TextWatcher mValidPortWatcher = new TextWatcher() {
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
        }

        @Override
        public void afterTextChanged(Editable s) {
            updateViews();
        }
    };


    @Override
    public void onShow(DialogInterface d) {
        mShowingDialog = (AlertDialog) d;
        mOk = mShowingDialog.getButton(DialogInterface.BUTTON_POSITIVE);
        updateViews();
    }

    private void updateViews() {
        if (mShowingDialog == null)
            return;

        mOk.setEnabled(isValidPort(vExternalPort.getText().toString()) &&
                        isValidPort(vInternalPort.getText().toString()) &&
                        vDescription.getText().length() > 1 &&
                        vConnections.getSelectedItem() != null
        );
    }

    static private UnsignedIntegerTwoBytes toUI2Bytes(EditText e) {
        return new UnsignedIntegerTwoBytes(
                Integer.parseInt(e.getText().toString())
        );
    }

    static private boolean isValidPort(String p) {
        //Log.d("", p+"");
        try {
            int i = Integer.parseInt(p);
            return i > 1024 && i < 65536;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    public void show() {
        AlertDialog dialog = mBuilder.create();
        dialog.setOnShowListener(this);
        dialog.show();
    }

}
