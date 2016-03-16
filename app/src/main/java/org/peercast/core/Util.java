package org.peercast.core;

import android.net.Uri;
import android.os.Handler;
import android.support.annotation.NonNull;

/**
 * (c) 2015, T Yoshizawa
 *
 * Dual licensed under the MIT or GPL licenses.
 */
class Util {
    private Util() {
    }

    /**Handlerを利用したタイマー*/
    public static class Timer {
        private final Handler mHandler;
        private final long mInterval;
        private final Runnable mTimerTask;
        private final Runnable mHandlerRunnable = new Runnable() {
            @Override
            public void run() {
                mTimerTask.run();
                mHandler.postDelayed(this, mInterval);
            }
        };

        /**指定のハンドラで実行するタイマーを作成する。*/
        public Timer(@NonNull Handler handler, @NonNull Runnable timerTask, long interval) {
            mHandler = handler;
            mTimerTask = timerTask;
            mInterval = interval;
        }

        /**現在のスレッドで実行するタイマー。*/
        public Timer(@NonNull Runnable timerTask, long interval) {
            mHandler = new Handler();
            mTimerTask = timerTask;
            mInterval = interval;
        }

        public void start(long delayed) {
            mHandler.postDelayed(mHandlerRunnable, delayed);
        }

        public void start() {
            mHandler.post(mHandlerRunnable);
        }

        public void cancel() {
            mHandler.removeCallbacks(mHandlerRunnable);
        }
    }

    static public Uri getStreamUrl(Channel ch, int port){
        String fmt;
        String type = ch.getInfo().getTypeStr();
        if ("WMV".equals(type)){
            fmt = "mmsh://localhost:%d/stream/%s.%s";
        } else {
            fmt = "http://localhost:%d/stream/%s.%s";
        }
        return Uri.parse(String.format(fmt, port, ch.getID(), type.toLowerCase()));
    }

}
