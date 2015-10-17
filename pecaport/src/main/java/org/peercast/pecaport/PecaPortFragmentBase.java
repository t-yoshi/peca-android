package org.peercast.pecaport;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Configuration;
import android.database.DataSetObserver;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.ColorRes;
import android.support.annotation.DrawableRes;
import android.support.annotation.IdRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.UiThread;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

import org.apache.commons.lang3.time.DurationFormatUtils;
import org.fourthline.cling.model.meta.RemoteService;
import org.fourthline.cling.support.model.PortMapping;
import org.peercast.pecaport.widget.DefaultTableAdapter;
import org.peercast.pecaport.widget.PeerCastButton;
import org.peercast.pecaport.widget.TableView;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

/**
 * @author (c) 2015, T Yoshizawa
 *         Dual licensed under the MIT or GPL licenses.
 */
public abstract class PecaPortFragmentBase extends Fragment implements
        PortManipulator.OnResultListener {
    private static final String TAG = "PecaPortFragmentBase";

    @Nullable
    private RouterDiscoverer mRouterDiscoverer;

    private int mRunningPort;

    @Nullable
    private NetworkInterfaceInfo mActiveNicInfo;
    private final ViewBinder mViewBinder = new ViewBinder();

    private ContainerFrame mContentView;
    private PeerCastButton vPeerCast;
    private WanConnectionsAdapter mWanConnectionAdapter;
    private MappingEntryAdapter mMappingAdapter;
    private PecaPortPreferences mPreferences;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mPreferences = PecaPortPreferences.from(getContext());
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        //Disable時に半透明になり、イベントを子に渡さないFrameLayout
        ContainerFrame exFrame = new ContainerFrame(container.getContext());
        exFrame.addView(inflater.inflate(R.layout.pecaport, null));

        boolean isTablet = getContext().getResources().getConfiguration()
                .isLayoutSizeAtLeast(Configuration.SCREENLAYOUT_SIZE_LARGE);

        //タブレット時Gone: ボタンの左側にある空白のビュー。
        exFrame.findViewById(R.id.vLayoutPadding1).setVisibility(isTablet ? View.GONE : View.VISIBLE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            //6.0よりWifiアダプタのMacAddressは"02:00:00:00:00:00:00"を返すようになった
            exFrame.findViewById(R.id.vRowHardwareAddress).setVisibility(View.GONE);
        }
        return exFrame;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mContentView = (ContainerFrame) view;

        final Spinner vWanConnectionSelector = (Spinner) mContentView.findViewById(R.id.vWanConnectionSelector);
        final TableView vMappingEntries = (TableView) mContentView.findViewById(R.id.vMappingEntries);
        vPeerCast = (PeerCastButton) mContentView.findViewById(R.id.vPeerCast);
        final ImageButton vAdd = (ImageButton) mContentView.findViewById(R.id.vAdd);

        if (!mPreferences.isDebug())
            vAdd.setVisibility(View.GONE);

        mWanConnectionAdapter = new WanConnectionsAdapter();
        vWanConnectionSelector.setAdapter(mWanConnectionAdapter);

        final AdapterView.OnItemSelectedListener wanItemListener = new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                WanConnection conn = (WanConnection) parent.getItemAtPosition(position);
                String externalIp = conn.getExternalIp();
                if (externalIp == null)
                    externalIp = getContext().getString(R.string.t_empty);
                mViewBinder.updateTextView(R.id.vWanExternalIp, externalIp);

                onPreparePeerCastButton(vPeerCast, conn);
                mMappingAdapter.setPortMappings(conn.getMappingEntries());
                updateAddButton(vAdd);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        };
        vWanConnectionSelector.setOnItemSelectedListener(wanItemListener);

        mWanConnectionAdapter.registerDataSetObserver(new DataSetObserver() {
            @Override
            public void onChanged() {
                int enabledPos = mWanConnectionAdapter.getConnectedPosition();
                if (enabledPos != -1) {
                    wanItemListener.onItemSelected(vWanConnectionSelector, null, enabledPos, 0);
                }
                updateAddButton(vAdd);
            }
        });

        mMappingAdapter = new MappingEntryAdapter(getContext(), new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onDeleteMapping(
                        (WanConnection) vWanConnectionSelector.getSelectedItem(),
                        (PortMapping) v.getTag());
            }
        });
        vMappingEntries.setAdapter(mMappingAdapter);


        vPeerCast.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(TAG, "" + v);
                onClickPeerCastButton(vPeerCast, (WanConnection) vWanConnectionSelector.getSelectedItem());
            }
        });

        vAdd.setEnabled(mActiveNicInfo != null);
        vAdd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onRequestAddPort(mWanConnectionAdapter.getConnected());
            }
        });
    }

    private void updateAddButton(View vAdd) {
        if (!mWanConnectionAdapter.getConnected().isEmpty()) {
            vAdd.setEnabled(true);
            vAdd.setAlpha(1.0f);
        } else {
            vAdd.setEnabled(false);
            vAdd.setAlpha(0.6f);
        }
    }

    protected void startDiscoverer() {
        if (mRouterDiscoverer != null)
            return;

        NetworkDeviceManager nicManager = NetworkDeviceManager.from(getContext());
        mActiveNicInfo = nicManager.getActiveInterface();
        if (mActiveNicInfo == null) {
            Log.i(TAG, "mActiveNicInfo == null");
            mViewBinder.setNetworkInterfaceInfo(null);
        }

        RouterEventHandler routerEventHandler = new RouterEventHandler(mViewBinder);
        mRouterDiscoverer = new RouterDiscoverer(
                getActivity().getApplicationContext(), routerEventHandler
        );

        getActivity().getApplicationContext().bindService(
                new Intent(getContext(), UpnpInternalService.class),
                mRouterDiscoverer,
                Context.BIND_AUTO_CREATE
        );
    }


    protected void setRunningPort(int port) {
        if (mRunningPort != port) {
            mRunningPort = port;
            //Log.d(TAG, "port="+mRunningPort);
            mMappingAdapter.notifyDataSetChanged();
        }
    }

    protected void setDebugMode(boolean b){
        mPreferences.putDebug(b);
    }

    protected boolean isDebugMode(){
        return mPreferences.isDebug();
    }

    public void onPreparePeerCastButton(PeerCastButton button, WanConnection selected) {
        boolean enabled = mActiveNicInfo != null && selected != null;
        button.setEnabled(enabled);
        if (enabled) {
            NetworkIdentity identity = new NetworkIdentity(getActivity(), mActiveNicInfo, selected.getService());
            //Log.d(TAG, identity+"##");
            //Log.d(TAG, mPreferences.getAllDisabledNetworks()+"");
            button.setDeny(mPreferences.getAllDisabledNetworks().contains(identity));
        }
    }


    private void onClickPeerCastButton(PeerCastButton button, WanConnection selected) {

        NetworkIdentity identity = new NetworkIdentity(getActivity(), mActiveNicInfo, selected.getService());
        if (mPreferences.getAllDisabledNetworks().contains(identity)) {
            mPreferences.removeDisabledNetwork(identity);
            showToast(getString(R.string.t_upnp_used_in_this_connection_yes));
        } else {
            mPreferences.addDisabledNetwork(identity);
            showToast(getString(R.string.t_upnp_used_in_this_connection_no));
        }
        onPreparePeerCastButton(button, selected);
    }


    private void onRequestAddPort(Collection<WanConnection> connected) {
        new AddMappingDialog(getContext(), mRunningPort, connected,
                mActiveNicInfo.getPrivateAddress(),
                new AddMappingDialog.Listener() {
                    @Override
                    public void onOkClick(final WanConnection conn, final PortMapping mapping) {
                        Future f = conn.getManipulator().addPort(mapping, PecaPortFragmentBase.this);
                        getFuture(f);
                    }
                }
        ).show();
    }


    private void onDeleteMapping(final WanConnection conn, final PortMapping mapping) {
        new AlertDialog.Builder(getContext())
                .setIcon(R.drawable.ic_delete)
                .setTitle(R.string.t_delete)
                .setMessage(getString(R.string.t_do_you_want_to_delete,
                        mapping.getDescription(), mapping.getInternalClient()
                ))
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        PortManipulator manipulator = conn.getManipulator();
                        Future f = manipulator.deletePort(mapping, PecaPortFragmentBase.this);
                        getFuture(f);
                    }
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    @Override
    public void onSuccess(Method m, RemoteService service, PortMapping mapping) {
        research();
    }

    @Override
    public void onFailure(Method m, RemoteService service, PortMapping mapping, String errMsg) {

    }

    protected void research() {
        if (mRouterDiscoverer != null)
            mRouterDiscoverer.research();
    }


    private void getFuture(Future<?> future) {
        try {
            future.get();
        } catch (ExecutionException e) {
            throw new RuntimeException(e.getCause());
        } catch (InterruptedException | CancellationException e) {
            throw new RuntimeException(e);
        }
    }


    private void showToast(CharSequence text) {
        Toast toast = Toast.makeText(getContext(), text, Toast.LENGTH_SHORT);
        toast.setGravity(Gravity.CENTER, 0, 0);
        toast.show();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        if (mRouterDiscoverer != null) {
            mRouterDiscoverer.onServiceDisconnected(null);
            getActivity().getApplicationContext().unbindService(mRouterDiscoverer);
        }
    }

    /**
     * Disable時に半透明になり、イベントを子に渡さないFrameLayout
     */
    public static class ContainerFrame extends FrameLayout {
        public ContainerFrame(Context context) {
            super(context);
        }

        @Override
        public void setEnabled(boolean enabled) {
            super.setEnabled(enabled);
            if (enabled) {
                setAlpha(1);
            } else {
                setAlpha(0.5f);
            }
        }

        @Override
        public boolean onInterceptTouchEvent(MotionEvent ev) {
            return !isEnabled();
        }
    }


    /**
     * UIスレッド以外からビューの内容を更新できる。
     */
    public class ViewBinder {
        public Context getContext() {
            return PecaPortFragmentBase.this.getContext();
        }

        public void updateTextView(final @IdRes int id, final @Nullable CharSequence text) {
            mContentView.post(new Runnable() {
                @Override
                public void run() {
                    TextView textView = (TextView) mContentView.findViewById(id);
                    textView.setText(text);
                }
            });
        }


        public void updateImageViewResource(final @IdRes int id,
                                            final @DrawableRes int drawableRes,
                                            final @ColorRes int colorRes) {
            mContentView.post(new Runnable() {
                @Override
                public void run() {
                    ImageView imageView = (ImageView) mContentView.findViewById(id);
                    imageView.setImageResource(drawableRes);
                    imageView.setColorFilter(mContentView.getResources().getColor(colorRes));
                }
            });
        }

        public void setNetworkInterfaceInfo(@Nullable NetworkInterfaceInfo nicInfo) {
            if (nicInfo == null) {
                updateImageViewResource(R.id.vNicIcon, R.drawable.ic_close, R.color.md_red_800);
                updateTextView(R.id.vNicName, getContext().getString(R.string.t_network_not_found));
                updateTextView(R.id.vPrivateIp, getContext().getString(R.string.t_empty));
                updateTextView(R.id.vHardwareAddress, getContext().getString(R.string.t_empty));
                updateTextView(R.id.vRouterName, "");
                updateTextView(R.id.vRouterManufacturer, "");
                updateTextView(R.id.vWanExternalIp, "");
                return;
            }

            if (nicInfo instanceof NetworkInterfaceInfo.Ethernet) {
                updateImageViewResource(R.id.vNicIcon, R.drawable.ic_server_network, R.color.md_orange_500);
            } else if (nicInfo instanceof NetworkInterfaceInfo.Wifi) {
                updateImageViewResource(R.id.vNicIcon, R.drawable.ic_wifi, R.color.md_orange_500);
            }
            updateTextView(R.id.vNicName, nicInfo.getDisplayName(getContext()));
            updateTextView(R.id.vPrivateIp, nicInfo.getPrivateAddress().getHostAddress());
            updateTextView(R.id.vHardwareAddress, nicInfo.getHardwareAddress());
        }

        public void setWanConnections(@NonNull Collection<WanConnection> connections) {
            final Collection<WanConnection> connections1 = new ArrayList<>(connections);

            mContentView.post(new Runnable() {
                @Override
                public void run() {
                    mWanConnectionAdapter.setConnections(connections1);
                }
            });
        }

    }

    private class MappingEntryAdapter extends DefaultTableAdapter<Object> {
        private final LayoutInflater mInflater;
        private final View.OnClickListener mOnRemoveButtonClick;

        //列のインデックスとgetItem()のデータ形式
        private static final int C_ICON = 0; //PortMapping -> icon.visible
        private static final int C_CLIENT = 1; //String
        private static final int C_PORT = 2;  //String
        private static final int C_PROTO = 3; //String
        private static final int C_DESC = 4; //String
        private static final int C_DURATION = 5; //Long
        private static final int C_ENABLED = 6; //Boolean
        private static final int C_REMOVE = 7; //PortMapping -> removeButton.setTag

        MappingEntryAdapter(Context c, View.OnClickListener onRemove) {
            super(8);
            mInflater = LayoutInflater.from(c);
            mOnRemoveButtonClick = onRemove;
        }

        @Nullable
        @Override
        protected TableRow createHeaderRow(ViewGroup parent) {
            TableRow header = (TableRow) mInflater.inflate(R.layout.mapping_entry_header_row, parent, false);
            int[] resHeaders = {
                    0,
                    R.string.t_wan_internal_client,
                    R.string.t_wan_external_internal_port,
                    R.string.t_protocol,
                    R.string.t_wan_description,
                    R.string.t_wan_duration,
                    R.string.t_enabled,
                    0
            };

            for (int i = 0, count = header.getChildCount(); i < count; i++) {
                if (resHeaders[i] != 0)
                    ((TextView) header.getChildAt(i)).setText(resHeaders[i]);
            }
            return header;
        }

        @UiThread
        public void setPortMappings(List<PortMapping> mappings) {
            removeAll();

            for (PortMapping m : mappings) {
                String port;
                if (m.getExternalPort().equals(m.getInternalPort()))
                    port = m.getExternalPort().toString();
                else
                    port = m.getExternalPort() + "/" + m.getInternalPort();

                addRow(
                        m,
                        m.getInternalClient(),
                        port,
                        m.getProtocol().name(),
                        m.getDescription(),
                        m.getLeaseDurationSeconds().getValue(),
                        m.isEnabled(),
                        m
                );
            }
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        protected TableRow createRow(ViewGroup parent) {
            return (TableRow) mInflater.inflate(R.layout.mapping_entry_data_row, parent, false);
        }

        @Override
        protected void bindView(View view, int row, int column) {
            Object item = getItem(row, column);
            if (item == null)
                throw new NullPointerException();

            switch (column) {
                case C_ICON:
                    view.findViewById(R.id.vIcon).setVisibility(
                            isPeerCastPort((PortMapping) item) ? View.VISIBLE : View.INVISIBLE
                    );
                    break;

                case C_CLIENT:
                case C_PORT:
                case C_PROTO:
                    super.bindView(view, row, column);
                    ((TextView) view).setGravity(Gravity.CENTER);
                    break;

                case C_DURATION: {
                    TextView textView = (TextView) view;
                    textView.setGravity(Gravity.CENTER);
                    long sec = (Long) item;
                    if (sec == 0) {
                        textView.setText(R.string.t_wan_duration_is_0);
                    } else {
                        textView.setText(DurationFormatUtils.formatDuration(sec * 1000, "dd'd' HH'h'", false));
                    }
                    break;
                }
                case C_DESC:
                    TextView textView = (TextView) view;
                    textView.setGravity(Gravity.LEFT);
                    super.bindView(view, row, column);
                    break;

                case C_ENABLED: {
                    break;
                }

                case C_REMOVE:
                    ImageButton vRemove = (ImageButton) view.findViewById(R.id.vRemove);
                    vRemove.setOnClickListener(mOnRemoveButtonClick);
                    //tag is PortMappingAction object
                    //Log.d(TAG, view + ",, " + row);
                    vRemove.setTag(getItem(row, column));
                    break;

                default:
                    throw new RuntimeException();
            }
        }

        private boolean isPeerCastPort(PortMapping m) {
            return mRunningPort == m.getExternalPort().getValue().intValue() &&
                    mRunningPort == m.getInternalPort().getValue().intValue() &&
                    m.getProtocol() == PortMapping.Protocol.TCP;
        }
    }


}
