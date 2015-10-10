package org.peercast.core;

/**
 * (c) 2013, T Yoshizawa
 *
 * Dual licensed under the MIT or GPL licenses.
 */

import android.os.Bundle;
import android.support.annotation.NonNull;

public class ChannelInfo {

    // enum ChanInfo::TYPE
    public static final int T_UNKNOWN = 0;

    public static final int T_RAW = 1;
    public static final int T_MP3 = 2;
    public static final int T_OGG = 3;
    public static final int T_OGM = 4;
    public static final int T_MOV = 5;
    public static final int T_MPG = 6;
    public static final int T_NSV = 7;
    public static final int T_WMA = 8;
    public static final int T_WMV = 9;
    public static final int T_PLS = 10;
    public static final int T_ASX = 11;

    // b->putInt("contentType", info->contentType);
    // b->putString("track.artist", info->track.artist);
    // b->putString("track.title", info->track.title);
    // b->putString("name", info->name);
    // b->putString("desc", info->desc);
    // b->putString("genre", info->genre);
    // b->putString("comment", info->comment);
    // b->putString("url", info->url);
    // b->putInt("bitrate", info->bitrate);

    private final Bundle mBundle;


    private ChannelInfo(@NonNull Bundle b) {
        mBundle = b;
    }

    public String getId() {
        return mBundle.getString("id");
    }

    public int getType() {
        return mBundle.getInt("contentType");
    }

    /**
     * See [Native] ChanInfo::getTypeStr(TYPE)
     */
    public String getTypeStr() {
        switch (getType()) {
            case T_RAW:
                return "RAW";
            case T_MP3:
                return "MP3";
            case T_OGG:
                return "OGG";
            case T_OGM:
                return "OGM";
            case T_WMA:
                return "WMA";
            case T_MOV:
                return "MOV";
            case T_MPG:
                return "MPG";
            case T_NSV:
                return "NSV";
            case T_WMV:
                return "WMV";
            case T_PLS:
                return "PLS";
            case T_ASX:
                return "ASX";
            default:
                return "UNKNOWN";
        }
    }

    public String getTrackArtist() {
        return mBundle.getString("track.artist");
    }

    public String getTrackTitle() {
        return mBundle.getString("track.title");
    }

    public String getName() {
        return mBundle.getString("name");
    }

    public String getDesc() {
        return mBundle.getString("desc");
    }

    public String getGenre() {
        return mBundle.getString("genre");
    }

    public String getComment() {
        return mBundle.getString("comment");
    }

    public String getUrl() {
        return mBundle.getString("url");
    }

    public int getBitrate() {
        return mBundle.getInt("bitrate");
    }

    /**
     * @see Channel#getInfo()
     * @see PeerCastService#notifyChannel(int, Bundle)
     */
    public static ChannelInfo fromNativeResult(Bundle b) {
        return new ChannelInfo(b);
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
