package org.peercast.core;

/**
 * (c) 2014, T Yoshizawa
 *
 * Dual licensed under the MIT or GPL licenses.
 */

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import android.app.Service;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.util.Log;

import static org.peercast.core.NotificationHelper.ACTION_BUMP;
import static org.peercast.core.NotificationHelper.ACTION_DISCONNECT;


public class PeerCastService extends Service {
    private static final String TAG = "PeerCastService";

    public static final int MSG_GET_APPLICATION_PROPERTIES = 0x00;
    public static final int MSG_GET_CHANNELS = 0x01;
    public static final int MSG_GET_STATS = 0x02;
    public static final int MSG_CMD_CHANNEL_BUMP = 0x10;
    public static final int MSG_CMD_CHANNEL_DISCONNECT = 0x11;
    public static final int MSG_CMD_CHANNEL_KEEP_YES = 0x12;
    public static final int MSG_CMD_CHANNEL_KEEP_NO = 0x13;
    public static final int MSG_CMD_SERVENT_DISCONNECT = 0x20;

    private Messenger mServiceMessenger;
    private ServiceHandler mServiceHandler;
    private Looper mServiceLooper;
    private int mRunningPort;
    private NotificationHelper mNotificationHelper;

    /**
     * PeerCastServiceController からの .sendCommand() .sendChannelCommand()
     * を処理するHandler。
     */
    private static class ServiceHandler extends Handler {
        private final WeakReference<PeerCastService> mOuter;

        public ServiceHandler(PeerCastService outer) {
            super(outer.mServiceLooper);
            mOuter = new WeakReference<PeerCastService>(outer);
        }

        @Override
        public void handleMessage(Message msg) {
            Message reply = obtainMessage(msg.what);
            Boolean result = null;
            Bundle data = new Bundle();
            switch (msg.what) {
                case MSG_GET_APPLICATION_PROPERTIES:
                    data = mOuter.get().nativeGetApplicationProperties();
                    break;

                case MSG_GET_CHANNELS:
                    data = mOuter.get().nativeGetChannels();
                    break;

                case MSG_GET_STATS:
                    data = mOuter.get().nativeGetStats();
                    break;

                case MSG_CMD_CHANNEL_BUMP:
                case MSG_CMD_CHANNEL_DISCONNECT:
                case MSG_CMD_CHANNEL_KEEP_YES:
                case MSG_CMD_CHANNEL_KEEP_NO:
                    result = mOuter.get().nativeChannelCommand(msg.what, msg.arg1);
                    break;

                case MSG_CMD_SERVENT_DISCONNECT:
                    result = mOuter.get().nativeDisconnectServent(msg.arg1);
                    break;

                default:
                    Log.e(TAG, "Illegal value: msg.what=" + msg.what);
                    return;
            }

            if (msg.replyTo == null) {
                //Log.d(TAG, "msg.replyTo == null");
                return;
            }

            if (result != null)
                data.putBoolean("result", result);

            reply.setData(data);
            try {
                msg.replyTo.send(reply);
            } catch (RemoteException e) {
                Log.e(TAG, "msg.replyTo.send(reply)", e);
            }
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();

        HandlerThread thread = new HandlerThread("PeerCastService");
        thread.start();

        mServiceLooper = thread.getLooper();
        mServiceHandler = new ServiceHandler(this);
        mServiceMessenger = new Messenger(mServiceHandler);

    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // Log.d(TAG, "onStartCommand: " + intent);
        String act = intent.getAction();
        if (act != null) {
            // 通知バーのボタンが押された時
            int channel_id = intent.getIntExtra("channel_id", -1);
            if (ACTION_BUMP.equals(act)) {
                nativeChannelCommand(MSG_CMD_CHANNEL_BUMP, channel_id);
            } else if (ACTION_DISCONNECT.equals(act)) {
                nativeChannelCommand(MSG_CMD_CHANNEL_DISCONNECT, channel_id);
            }
            return START_NOT_STICKY;
        }
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public IBinder onBind(Intent i) {
        String iniPath = getFileStreamPath("peercast.ini").getAbsolutePath();
        String resDirPath = getFilesDir().getAbsolutePath();

        synchronized (this) {
            if (mRunningPort == 0) {
                try {
                    HtmlResource res = new HtmlResource();
                    if (!res.isInstalled())
                        res.doExtract();
                } catch (IOException e) {
                    Log.e(TAG, "html-dir install failed.", e);
                    return null;
                }

                mRunningPort = nativeStart(iniPath, resDirPath);
                mNotificationHelper = new NotificationHelper(this);
            }
        }
        return mServiceMessenger.getBinder();
    }

    @Override
    public boolean onUnbind(Intent intent) {
        return false;
    }

    @Override
    public void onDestroy() {
        stopForeground(true);
        mServiceLooper.quit();
        if (mNotificationHelper != null)
            mNotificationHelper.finish();

        nativeQuit();
    }

    /**
     * ネイティブ側から呼ばれる。<br>
     * <p/>
     * AndroidPeercastApp::notifyMessage( ServMgr::NOTIFY_TYPE tNotify, const
     * char *message)
     *
     * @see #NOTIFY_CHANNEL_START
     * @see #NOTIFY_CHANNEL_UPDATE
     * @see #NOTIFY_CHANNEL_STOP
     */
    private void notifyMessage(int notifyType, String message) {
        if (BuildConfig.DEBUG) {
            Log.i(TAG, "notifyMessage: " + notifyType + ", " + message);
        }
    }

    private static final int NOTIFY_CHANNEL_START = 0;
    private static final int NOTIFY_CHANNEL_UPDATE = 1;
    private static final int NOTIFY_CHANNEL_STOP = 2;

    /**
     * ネイティブ側から呼ばれる。<br>
     * <p/>
     * <pre>
     * AndroidPeercastApp::
     *   channelStart(ChanInfo *info)
     *   channelUpdate(ChanInfo *info)
     *   channelStop(ChanInfo *info)
     * </pre>
     *
     * @see #NOTIFY_CHANNEL_START
     * @see #NOTIFY_CHANNEL_UPDATE
     * @see #NOTIFY_CHANNEL_STOP
     * *
     */
    private void notifyChannel(int notifyType, Bundle bInfo) {
        if (BuildConfig.DEBUG) {
            String s = bInfo.getString("id");
            for (String key : bInfo.keySet())
                s += ", " + key + "=" + bInfo.get(key);
            Log.i(TAG, "notifyType_" + notifyType + ": " + s);
        }
    }

    /**
     * PeerCastを開始します。
     *
     * @param iniPath     peercast.iniのパス(要・書き込み可能)
     * @param resourceDir htmlディレクトリのある場所
     * @return 動作ポート
     */
    private native int nativeStart(String iniPath, String resourceDir);

    /**
     * PeerCastを終了します。
     */
    private native void nativeQuit();

    /**
     * 現在アクティブなChannel情報を取得します。
     *
     * @return Bundle。1つも無い場合はnull。2つ目以降はnextキーにリンクリスト形式で収納。
     * @see Channel#fromNativeResult(Bundle)
     */
    native Bundle nativeGetChannels();

    /**
     * 通信量の状態を取得します。
     *
     * @return Bundle
     * @see Stats#fromNativeResult(Bundle)
     */
    native Bundle nativeGetStats();

    /**
     * 動作中のポート番号などを取得します。
     *
     * @return Bundle
     */
    native Bundle nativeGetApplicationProperties();

    /**
     * チャンネルの再接続、キープの有無を操作する。
     *
     * @return 成功した場合 true
     */
    private native boolean nativeChannelCommand(int cmdType,
                                                int channel_id);

    /**
     * 指定したサーヴァントを切断する。
     *
     * @return 成功した場合 true
     */
    private native boolean nativeDisconnectServent(int servent_id);

    /**
     * クラス初期化に呼ぶ。
     */
    private static native void nativeClassInit();

    static {
        System.loadLibrary("peercast");
        nativeClassInit();
    }

    private class HtmlResource {
        private File mDataDir = getFilesDir();
        private File mInstalled = new File(mDataDir, ".IM45");

        /**
         * peca.zipからhtmlフォルダを解凍して /data/data/org.peercast.core/ にインストール。<br>
         * <p/>
         * 成功した場合は解凍済みを示す .IM45ファルダを作成する。
         *
         * @throws IOException
         */
        public void doExtract() throws IOException {
            ZipInputStream zipIs = new ZipInputStream(getAssets().open(
                    "peca.zip"));
            try {
                ZipEntry ze;
                while ((ze = zipIs.getNextEntry()) != null) {
                    File file = new File(mDataDir, ze.getName());

                    if (ze.isDirectory()) {
                        file.mkdirs();
                        continue;
                    }
                    file.getParentFile().mkdirs();
                    Log.i(TAG, "Extract resource -> " + file.getAbsolutePath());
                    FileOutputStream fout = new FileOutputStream(file);
                    byte[] buffer = new byte[4096];
                    int length = 0;
                    while ((length = zipIs.read(buffer)) > 0) {
                        fout.write(buffer, 0, length);
                    }
                    zipIs.closeEntry();
                    fout.close();
                }
            } finally {
                zipIs.close();
            }
            mInstalled.mkdir();
        }

        public boolean isInstalled() {
            return mInstalled.exists();
        }

    }

}
