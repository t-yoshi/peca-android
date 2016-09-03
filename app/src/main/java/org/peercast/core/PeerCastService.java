package org.peercast.core;

/**
 * (c) 2014, T Yoshizawa
 * Dual licensed under the MIT or GPL licenses.
 */

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.res.AssetManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.util.Log;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.peercast.pecaport.PecaPortService;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

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

    private AppPreferences mPreferences;
    private int mRunningPort;
    private NotificationHelper mNotificationHelper;
    private ServiceReceiver mServiceReceiver;
    /**
     * arg2=startId
     */
    private static final Object MSG_OBJ_CALL_STOP_SELF = new Object();

    /**
     * PeerCastServiceController からの
     * .sendCommand() .sendChannelCommand()      を処理するHandler。
     */
    private final class ServiceHandler extends Handler {
        public ServiceHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            Message reply = obtainMessage(msg.what);
            Boolean result = null;
            Bundle data = new Bundle();
            switch (msg.what) {
                case MSG_GET_APPLICATION_PROPERTIES:
                    data = nativeGetApplicationProperties();
                    break;

                case MSG_GET_CHANNELS:
                    data = nativeGetChannels();
                    break;

                case MSG_GET_STATS:
                    data = nativeGetStats();
                    break;

                case MSG_CMD_CHANNEL_BUMP:
                case MSG_CMD_CHANNEL_DISCONNECT:
                case MSG_CMD_CHANNEL_KEEP_YES:
                case MSG_CMD_CHANNEL_KEEP_NO:
                    result = nativeChannelCommand(msg.what, msg.arg1);
                    break;

                case MSG_CMD_SERVENT_DISCONNECT:
                    result = nativeDisconnectServent(msg.arg1);
                    break;

                default:
                    Log.e(TAG, "Illegal value: msg.what=" + msg.what);
                    return;
            }

            if (msg.obj == MSG_OBJ_CALL_STOP_SELF) {
                stopSelf(msg.arg2);
                return;
            }

            if (msg.replyTo == null) {
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
        HandlerThread thread = new HandlerThread(TAG);
        thread.start();

        mServiceHandler = new ServiceHandler(thread.getLooper());
        mServiceMessenger = new Messenger(mServiceHandler);
        mServiceReceiver = new ServiceReceiver(this);
        mPreferences = AppPreferences.from(this);
    }

    /**
     * 通知バーのボタンのイベントを処理する。
     */
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // Log.d(TAG, "onStartCommand: " + intent);
        String act = intent.getAction();
        Message msg = mServiceHandler.obtainMessage(-1);

        if (act != null) {
            // 通知バーのボタンが押された時
            msg.arg1 = intent.getIntExtra("channel_id", -1);
            if (ACTION_BUMP.equals(act)) {
                msg.what = MSG_CMD_CHANNEL_BUMP;
            } else if (ACTION_DISCONNECT.equals(act)) {
                msg.what = MSG_CMD_CHANNEL_DISCONNECT;
            }
        }
        msg.obj = MSG_OBJ_CALL_STOP_SELF;
        msg.arg2 = startId;
        mServiceHandler.sendMessage(msg);
        return START_NOT_STICKY;
    }

    @Override
    public IBinder onBind(Intent i) {
        File filesDir = getFilesDir();

        synchronized (this) {
            if (mRunningPort == 0) {
                try {
                    HtmlResource res = new HtmlResource(this);
                    if (!res.isInstalled())
                        res.doExtract();
                } catch (IOException e) {
                    Log.e(TAG, "html-dir install failed.", e);
                    return null;
                }
                mRunningPort = nativeStart(
                        new File(filesDir, "peercast.ini").getAbsolutePath(),
                        filesDir.getAbsolutePath()
                );
                mNotificationHelper = new NotificationHelper(this);

                if (mPreferences.isUPnPEnabled())
                    mServiceReceiver.registerConnectivityReceiver();

                if (mPreferences.isPowerSaveMode())
                    mServiceReceiver.registerScreenOffReceiver();
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

        mServiceReceiver.unregisterAll();

        mServiceHandler.getLooper().quit();
        if (mNotificationHelper != null)
            mNotificationHelper.quit();

        if (mRunningPort > 0 &&
                mPreferences.isUPnPEnabled() &&
                mPreferences.isUPnPCloseOnExit()) {
            Intent intent = new Intent(this, PecaPortService.class);
            intent.putExtra("close", mRunningPort);
            startService(intent);
        }

        nativeQuit();
    }

    /**
     * ネイティブ側から呼ばれる。<br>
     * <p>
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
     * <p>
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
    native boolean nativeChannelCommand(int cmdType,
                                        int channel_id);

    /**
     * 指定したサーヴァントを切断する。
     *
     * @return 成功した場合 true
     */
    native boolean nativeDisconnectServent(int servent_id);

    /**
     * クラス初期化に呼ぶ。
     */
    private static native void nativeClassInit();

    static {
        System.loadLibrary("peercast");
        nativeClassInit();
    }

    int getRunningPort() {
        return mRunningPort;
    }

    private static class HtmlResource {
        private final File mDataDir;
        //解凍済みを示す.IM45フォルダ
        private final File mInstalled;
        private final AssetManager mAssetManager;

        HtmlResource(Context c) {
            mDataDir = c.getFilesDir();
            mInstalled = new File(mDataDir, ".IM45");
            mAssetManager = c.getAssets();
        }

        /**
         * peca.zipからhtmlフォルダを解凍して /data/data/org.peercast.core/ にインストール。<br>
         * 成功した場合は解凍済みを示す .IM45フォルダを作成する。
         *
         * @throws IOException
         */
        public void doExtract() throws IOException {
            ZipInputStream zipIs = new ZipInputStream(mAssetManager.open("peca.zip"));
            try {
                ZipEntry entry;
                while ((entry = zipIs.getNextEntry()) != null) {
                    File file = new File(mDataDir, entry.getName());
                    if (entry.isDirectory())
                        continue;

                    Log.i(TAG, "Extract resource -> " + file.getAbsolutePath());
                    OutputStream os = null;
                    try {
                        os = FileUtils.openOutputStream(file);
                        IOUtils.copy(zipIs, os);
                    } finally {
                        IOUtils.closeQuietly(os);
                        zipIs.closeEntry();
                    }
                }
            } finally {
                IOUtils.closeQuietly(zipIs);
            }

            mInstalled.mkdir();
        }

        public boolean isInstalled() {
            return mInstalled.exists();
        }
    }
}