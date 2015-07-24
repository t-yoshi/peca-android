package org.peercast.core;
/**
 * (c) 2014, T Yoshizawa
 *
 * Dual licensed under the MIT or GPL licenses.
 */

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.support.v4.app.NotificationCompat;

import java.util.Timer;
import java.util.TimerTask;

/**
 * 通知バーのボタン処理用<br>
 * <p/>
 * JB以降では通知バーに [コンタクト、再接続、切断]ボタンを表示する。
 *
 * @see PeerCastService#onStartCommand(Intent, int, int)
 */

class NotificationHelper {
    private static final String TAG = "NotificationHelper";

    // 通知ボタンAction
    static final String ACTION_BUMP = "org.peercast.core.ACTION_CHANNEL_BUMP";
    static final String ACTION_DISCONNECT = "org.peercast.core.ACTION_CHANNEL_DISCONNECT";

    private static final int NOTIFY_ID = 0x7144; // 適当
    private static final int T_PERIOD = 8000; // タイマー更新間隔

    private final PeerCastService mService;
    private final NotificationManager mNotificationManager;
    private final Timer mTimer = new Timer();
    private boolean mIsForeground; // 視聴中か

    public NotificationHelper(PeerCastService s) {
        mService = s;
        mNotificationManager = (NotificationManager) s
                .getSystemService(Context.NOTIFICATION_SERVICE);

        mTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                update();
            }
        }, 0, T_PERIOD);

    }

    public void finish() {
        mTimer.cancel();
        if (mIsForeground) {
            mNotificationManager.cancel(NOTIFY_ID);
            mService.stopForeground(true);
        }
    }

    // 視聴中のチャンネル。なければnull
    private Channel getReceivingChannel() {
        for (Channel ch : Channel.fromNativeResult(mService
                .nativeGetChannels())) {
            if (ch.getStatus() == Channel.S_RECEIVING) {
                return ch;
            }
        }
        return null;
    }

    /**
     * <pre>
     * [アイコン] PeerCast is running...
     * クリックするとPeerCastMainActivityへ
     */
    public NotificationCompat.Builder createDefaultNotification() {
        Bitmap icon = BitmapFactory.decodeResource(mService.getResources(),
                R.drawable.ic_notify_icon);
        Intent it = new Intent(mService, PeerCastMainActivity.class);
        it.setFlags(Intent.FLAG_ACTIVITY_BROUGHT_TO_FRONT);
        PendingIntent pi = PendingIntent.getActivity(mService, 0, it, 0);

        return new NotificationCompat.Builder(mService) //
                .setSmallIcon(R.drawable.ic_notify_icon) //
                //.setLargeIcon(icon) //
                .setContentIntent(pi)//
                .setContentTitle("PeerCast running...");
    }

    private PendingIntent createContactPendingIntent(Channel ch) {
        // コンタクトURLを開く
        ChannelInfo info = ch.getInfo();
        Intent iContact = new Intent();
        if (info.getUrl() != null) {
            iContact = new Intent(Intent.ACTION_VIEW, Uri.parse(info.getUrl()));
            iContact.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        }
        return PendingIntent.getActivity(mService, 0, iContact,
                PendingIntent.FLAG_UPDATE_CURRENT);
    }

    private void addContactAction(NotificationCompat.Builder nb, Channel ch) {
        // 通知バー [コンタクト] ボタン
        nb.addAction(R.drawable.ic_notification_contact_url,
                mService.getText(R.string.t_contact),
                createContactPendingIntent(ch));
    }

    private void addBumpAction(NotificationCompat.Builder nb, Channel ch) {
        // 通知バー [再接続] ボタン
        Intent iBump = new Intent(ACTION_BUMP, null, mService,
                PeerCastService.class);
        iBump.putExtra("channel_id", ch.getChannel_ID());
        PendingIntent piBump = PendingIntent.getService(mService, 0, iBump,
                PendingIntent.FLAG_UPDATE_CURRENT);
        nb.addAction(R.drawable.ic_notification_bump,
                mService.getText(R.string.t_bump), piBump);
    }

    private void addDisconnectAction(NotificationCompat.Builder nb, Channel ch) {
        // 通知バー [切断] ボタン
        Intent iDisconnect = new Intent(ACTION_DISCONNECT, null, mService,
                PeerCastService.class);
        iDisconnect.putExtra("channel_id", ch.getChannel_ID());
        PendingIntent piDisconnect = PendingIntent.getService(mService, 0,
                iDisconnect, PendingIntent.FLAG_UPDATE_CURRENT);
        nb.addAction(R.drawable.ic_notification_disconnect,
                mService.getText(R.string.t_disconnect), piDisconnect);
    }

    private void update() {
        //Log.d(TAG, "update");

        Channel ch = getReceivingChannel();
        if (ch != null) {
            // 視聴中のとき
            NotificationCompat.Builder nb = createDefaultNotification();

            ChannelInfo info = ch.getInfo();
            String title = "Playing: " + info.getName();
            String text = info.getDesc() + " " + info.getComment();

            // 通信量表示用
            Stats stats = Stats.fromNativeResult(mService
                    .nativeGetStats());
            String sStats = String.format("R % 2d / S % 2d kbps",
                    stats.getInBytes() / 1024 * 8,
                    stats.getOutBytes() / 1024 * 8);

            // 通知バー タイトル部分
            nb.setContentTitle(title).setContentText(text)
                    .setContentInfo(sStats);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                // JB以降では通知バーに [コンタクト、再接続、切断]ボタンを表示する。
                addContactAction(nb, ch);
                addBumpAction(nb, ch);
                addDisconnectAction(nb, ch);
            } else {
                // ICSは各ボタン無いのでクリックしたらコンタクトURLへ飛ばす
                nb.setContentIntent(createContactPendingIntent(ch));
            }
            nb.setPriority(NotificationCompat.PRIORITY_HIGH);

            if (mIsForeground) {
                mNotificationManager.notify(NOTIFY_ID, nb.build());
            } else {
                mService.startForeground(NOTIFY_ID, nb.build());
                mIsForeground = true;
            }
        } else {
            if (!mIsForeground)
                return;
            mIsForeground = false;
            // 視聴してないならForeground解除
            mNotificationManager.cancel(NOTIFY_ID);
            mService.stopForeground(true);
        }
    }

    // バイト単位フォーマット (通信量表示用)
    private String fmtBytes(long v) {
        if (v > 1024 * 1024 * 1024)
            return String.format("%.1fGB", v / 1024f / 1024 / 1024);
        if (v > 1024 * 1024)
            return String.format("%.1fMB", v / 1024f / 1024);
        return String.format("%.1fKB", v / 1024f);
    }

}
