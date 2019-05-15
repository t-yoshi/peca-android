package org.peercast.core.lib
/**
 * Rpcコマンドを実行し、返答を得る。
 * @author (c) 2019, T Yoshizawa
 * @licenses Dual licensed under the MIT or GPL licenses.
 */
interface RpcHostConnection {
    /**
     * Json-RPCコマンドを実行する。
     * @param request Json形式のリクエスト
     * @return Json形式のレスポンス
     * @throws android.os.RemoteException サービスへのsendでエラー
     * @see <a href=https://www.jsonrpc.org/specification>JSON-RPC 2.0 Specification</a>
     * */
    suspend fun executeRpc(request: String): String
}