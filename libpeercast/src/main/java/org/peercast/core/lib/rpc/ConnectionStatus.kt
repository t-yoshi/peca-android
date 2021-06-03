package org.peercast.core.lib.rpc

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * 接続状態。PeerCastStationの仕様と混在?
 * @author (c) 2019, T Yoshizawa
 * @licenses Dual licensed under the MIT or GPL licenses.
 */
@Parcelize
enum class ConnectionStatus : Parcelable {
    //type=source
//  JrpcApi::toSourceConnection

        /**PeerCastStation*/
        Receiving,
        /**PeerCastStation*/
        Searching,
        /**PeerCastStation*/
        Error,
        /**PeerCastStation*/
        Idle,

        //Channel::statusMsgs
        NONE,
        WAIT, CONNECT, REQUEST, CLOSE, //
        RECEIVE, BROADCAST, ABORT, SEARCH, NOHOSTS,
        IDLE, ERROR, NOTFOUND,


    // type=direct
//        NONE,
        CONNECTING,
        PROTOCOL, HANDSHAKE,
        CONNECTED, CLOSING, LISTENING,
        TIMEOUT, REFUSED, VERIFIED,
//        ERROR, WAIT,
        FREE;
}