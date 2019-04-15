package org.peercast.core.lib
/**
 * (c) 2019, T Yoshizawa
 * Dual licensed under the MIT or GPL licenses.
 */
internal interface PeerCastServiceRpcBridge {
    /**
     * Json-RPCコマンドを実行する。
     * @param request Json形式のリクエスト
     * @return Json形式のレスポンス
     * @see <a href=https://www.jsonrpc.org/specification>JSON-RPC 2.0 Specification</a>
     * */
    suspend fun executeRpc(request: String): String
}