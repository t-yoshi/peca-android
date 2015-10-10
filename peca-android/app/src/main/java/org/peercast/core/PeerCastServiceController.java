package org.peercast.core;

import android.app.ActivityManager;
import android.app.ActivityManager.RunningServiceInfo;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.util.Log;

import java.util.List;

/**
 * PeerCast for Androidをコントロールする。<br>
 * <br>
 * Dual licensed under the MIT or GPL licenses.
 *
 * @author (c) 2014, T Yoshizawa
 * @version 1.1 Android-5.0対応。disconnectServent()追加。
 */
public class PeerCastServiceController {

    public static final int MSG_GET_APPLICATION_PROPERTIES = 0x00;
    public static final int MSG_GET_CHANNELS = 0x01;
    public static final int MSG_GET_STATS = 0x02;
    public static final int MSG_CMD_CHANNEL_BUMP = 0x10;
    public static final int MSG_CMD_CHANNEL_DISCONNECT = 0x11;
    public static final int MSG_CMD_CHANNEL_KEEP_YES = 0x12;
    public static final int MSG_CMD_CHANNEL_KEEP_NO = 0x13;
    public static final int MSG_CMD_SERVENT_DISCONNECT = 0x20;

    private static final String TAG = "PeCaCtrl";
    private static final String PKG_PEERCAST = "org.peercast.core";
    private static final String CLASS_NAME_PEERCAST_SERVICE = PKG_PEERCAST
            + ".PeerCastService";

    private final Context mContext;
    private Messenger mServerMessenger;
    private OnPeerCastEventListener mPeCaEventListener;

    public PeerCastServiceController(Context c) {
        if (c == null)
            throw new NullPointerException("null context");
        mContext = c;
    }

    /**
     * sendCommand() の戻り値を得るコールバック関数
     */
    public interface OnServiceResultListener {
        void onServiceResult(int msgId, Bundle data);
    }

    public interface OnPeerCastEventListener {
        /**
         * bindService後にコネクションが確立されると呼ばれます。
         */
        void onConnectPeerCastService();

        /**
         * unbindServiceを呼んだ後、もしくはOSによってサービスがKillされたときに呼ばれます。
         */
        void onDisconnectPeerCastService();
    }

    /**
     * PeerCastServiceにコマンドを送り、戻り値を得る。
     * <p/>
     * <ul>
     * <li>
     * MSG_GET_APPLICATION_PROPERTIES<br>
     * ピアキャスの動作ポートを取得します。<br>
     * 戻り値: getInt("port") ピアキャス動作ポート。停止時は0。<br>
     * <br>
     * <li>
     * MSG_GET_CHANNELS;<br>
     * 現在アクティブなチャンネルの情報を取得します。<br>
     * 戻り値: nativeGetChannel()参照。<br>
     * ラッパー: Channel.java <br>
     * <br>
     * <li>
     * MSG_GET_STATS<br>
     * 通信量の状況を取得します。<br>
     * 戻り値: nativeGetStats()参照。<br>
     * ラッパー: Stats.java <br>
     * <br>
     *
     * @param msgId    MSG_ で始まる定数。
     * @param listener サービスからの戻り値をBundleで受け取るリスナー。
     */
    public void sendCommand(final int msgId,
                            final OnServiceResultListener listener) {
        if (mServerMessenger == null)
            throw new IllegalStateException("service not connected.");
        if (listener == null)
            throw new NullPointerException("listener == null");

        Message msg = Message.obtain(null, msgId);
        msg.replyTo = new Messenger(new Handler() {
            @Override
            public void handleMessage(Message msg) {
                listener.onServiceResult(msgId, msg.getData());
            }
        });
        try {
            mServerMessenger.send(msg);
        } catch (RemoteException e) {
            Log.e(TAG, "msgId=" + msgId, e);
        }
    }

    /**
     * チャンネルに関する操作を行う。
     * <p/>
     * <ul>
     * <li>
     * MSG_CMD_CHANNEL_BUMP;<br>
     * bumpして再接続する。 <br>
     * <br>
     * <li>
     * MSG_CMD_CHANNEL_DISCONNECT<br>
     * チャンネルを切断する。 <br>
     * <br>
     * <li>
     * MSG_CMD_CHANNEL_KEEP_YES<br>
     * チャンネルをキープする。<br>
     * <br>
     * <li>
     * MSG_CMD_CHANNEL_KEEP_NO<br>
     * チャンネルのキープを解除する。<br>
     * <br>
     *
     * @param msgCmdId   MSG_CMD_CHANNEL_ で始まる定数。
     * @param channel_id 対象のchannel_id
     */
    private void sendChannelCommand(int msgCmdId, int channel_id) {
        if (mServerMessenger == null)
            throw new IllegalStateException("service not connected.");

        Message msg = Message.obtain(null, msgCmdId, channel_id, 0);
        try {
            mServerMessenger.send(msg);
        } catch (RemoteException e) {
            Log.e(TAG, "msgCmdId=" + msgCmdId, e);
        }
    }

    /**
     * Bumpして再接続する。
     *
     * @param channel_id 対象のchannel_id
     */
    public void bumpChannel(int channel_id) {
        sendChannelCommand(MSG_CMD_CHANNEL_BUMP, channel_id);
    }

    /**
     * チャンネルを切断する。
     *
     * @param channel_id 対象のchannel_id
     */
    public void disconnectChannel(int channel_id) {
        sendChannelCommand(MSG_CMD_CHANNEL_DISCONNECT, channel_id);
    }

    /**
     * チャンネルのキープと解除を設定する。
     *
     * @param channel_id 対象のchannel_id
     * @param value
     */
    public void setChannelKeep(int channel_id, boolean value) {
        if (value)
            sendChannelCommand(MSG_CMD_CHANNEL_KEEP_YES, channel_id);
        else
            sendChannelCommand(MSG_CMD_CHANNEL_KEEP_NO, channel_id);
    }

    /**
     * 指定したServentを切断する。
     */
    public void disconnectServent(int servent_id) {
        if (mServerMessenger == null)
            throw new IllegalStateException("service not connected.");

        Message msg = Message.obtain(null, MSG_CMD_SERVENT_DISCONNECT,
                servent_id, 0);
        try {
            mServerMessenger.send(msg);
        } catch (RemoteException e) {
            Log.e(TAG, "MSG_CMD_SERVENT_DISCONNECT", e);
        }
    }

    /**
     * context.bindServiceを呼び、PeerCastのサービスを開始する。
     *
     * @return
     */
    public boolean bindService() {
        if (!isInstalled()) {
            Log.e(TAG, "PeerCast not installed.");
            return false;
        }
        Intent intent = new Intent(CLASS_NAME_PEERCAST_SERVICE);
        // NOTE: LOLLIPOPからsetPackage()必須
        intent.setPackage(PKG_PEERCAST);
        return mContext.bindService(intent, mServiceConnection,
                Context.BIND_AUTO_CREATE);
    }

    public boolean isConnected() {
        return mServerMessenger != null;
    }

    /**
     * context.unbindServiceを呼ぶ。 他からもbindされていなければPeerCastサービスは終了する。
     *
     * @return
     */
    public void unbindService() {
        mContext.unbindService(mServiceConnection);
        mServerMessenger = null;
        if (mPeCaEventListener != null)
            mPeCaEventListener.onDisconnectPeerCastService();
    }

    public void setOnPeerCastEventListener(OnPeerCastEventListener listener) {
        mPeCaEventListener = listener;
    }

    private ServiceConnection mServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName arg0, IBinder binder) {
            // Log.d(TAG, "onServiceConnected!");
            mServerMessenger = new Messenger(binder);
            if (mPeCaEventListener != null)
                mPeCaEventListener.onConnectPeerCastService();
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            // OSにKillされたとき。
            // Log.d(TAG, "onServiceDisconnected!");
            mServerMessenger = null;
            if (mPeCaEventListener != null)
                mPeCaEventListener.onDisconnectPeerCastService();
        }
    };

    /**
     * 現在、PeerCastのサービスがOS上で起動していればtrue。
     *
     * @return
     */
    public boolean isServiceRunning() {
        ActivityManager activityManager = (ActivityManager) mContext
                .getSystemService(Context.ACTIVITY_SERVICE);
        List<RunningServiceInfo> services = activityManager
                .getRunningServices(256);
        for (RunningServiceInfo info : services) {
            if (CLASS_NAME_PEERCAST_SERVICE.equals(info.service.getClassName())) {
                return true;
            }
        }
        return false;
    }

    /**
     * 「PeerCast for Android」がインストールされているか調べる。
     *
     * @return "org.peercast.core" がインストールされていればtrue。
     */
    public boolean isInstalled() {
        PackageManager pm = mContext.getPackageManager();
        try {
            pm.getApplicationInfo(PKG_PEERCAST, 0);
            return true;
        } catch (NameNotFoundException e) {
            return false;
        }
    }

}