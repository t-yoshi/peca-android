package org.peercast.core;
/**
 * (c) 2013, T Yoshizawa
 *
 * Dual licensed under the MIT or GPL licenses.
 */

import android.os.Bundle;
import android.support.annotation.NonNull;

import java.util.ArrayList;
import java.util.List;

public class Channel {

    public static final int S_NONE = 0;
    public static final int S_WAIT = 1;
    public static final int S_CONNECTING = 2;
    public static final int S_REQUESTING = 3;
    public static final int S_CLOSING = 4;
    public static final int S_RECEIVING = 5;
    public static final int S_BROADCASTING = 6;
    public static final int S_ABORT = 7;
    public static final int S_SEARCHING = 8;
    public static final int S_NOHOSTS = 9;
    public static final int S_IDLE = 10;
    public static final int S_ERROR = 11;
    public static final int S_NOTFOUND = 12;

    public static final int T_NONE = 0;
    public static final int T_ALLOCATED = 1;
    public static final int T_BROADCAST = 2;
    public static final int T_RELAY = 3;

    private final Bundle mBundle;

    private Channel(@NonNull Bundle b) {
        mBundle = b;
    }

    public String getID() {
        return mBundle.getString("id");
    }

    public int getChannel_ID() {
        return mBundle.getInt("channel_id");
    }

    public int getTotalListeners() {
        return mBundle.getInt("totalListeners");
    }

    public int getTotalRelays() {
        return mBundle.getInt("totalRelays");
    }

    public int getLocalListeners() {
        return mBundle.getInt("localListeners");
    }

    public int getLocalRelays() {
        return mBundle.getInt("localRelays");
    }

    public int getStatus() {
        return mBundle.getInt("status");
    }

    public boolean isStayConnected() {
        return mBundle.getBoolean("stayConnected");
    }

    public boolean isTracker() {
        return mBundle.getBoolean("tracker");
    }

    public int getLastSkipTime() {
        return mBundle.getInt("lastSkipTime");
    }

    public int getSkipCount() {
        return mBundle.getInt("skipCount");
    }

    public ChannelInfo getInfo() {
        return ChannelInfo.fromNativeResult(mBundle.getBundle("info"));
    }

    private List<Servent> servents;

    public List<Servent> getServents() {
        if (servents == null) {
            servents = new ArrayList<Servent>();
            Bundle bServent = mBundle.getBundle("servent");
            while (bServent != null && !bServent.isEmpty()) {
                servents.add(new Servent(bServent));
                bServent = bServent.getBundle("next");
            }
        }
        return servents;
    }


    /**
     * nativeGetChannelsからの戻り値をwrapします。
     *
     * @see PeerCastService#nativeGetChannels()
     */
    public static List<Channel> fromNativeResult(Bundle bChannels) {
        List<Channel> channels = new ArrayList<>();

        while (bChannels != null && !bChannels.isEmpty()) {
            //Log.d("Channel.java", ""+bChannel);
            channels.add(new Channel(bChannels));
            bChannels = bChannels.getBundle("next");
        }
        return channels;
    }

    @Override
    public String toString() {
        String s = getClass().getSimpleName() + ": [";
        for (String k : mBundle.keySet()) {
            s += k + "=" + mBundle.get(k) + ", ";
        }
        return s + "]";
    }


}
