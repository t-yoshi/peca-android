package org.peercast.core.lib.rpc
/**
 * (c) 2019, T Yoshizawa
 * Dual licensed under the MIT or GPL licenses.
 */
enum class ConnectionStatus {
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