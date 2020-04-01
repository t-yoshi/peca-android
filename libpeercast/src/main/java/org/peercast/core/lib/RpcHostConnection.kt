package org.peercast.core.lib
/**
 * Rpcコマンドを実行し、返答を得る。
 * @author (c) 2019, T Yoshizawa
 * @licenses Dual licensed under the MIT or GPL licenses.
 */
interface RpcHostConnection {
    /**
     * Json-RPCコマンドを実行する。エラーが起きた場合はError objectを返す。
     * @param request Json形式のリクエスト
     * @return Json形式のレスポンス
     * @see <a href=https://www.jsonrpc.org/specification>JSON-RPC 2.0 Specification</a>
     * */
    suspend fun executeRpc(request: String): String

    companion object {
        /**[android.os.RemoteException]が発生した。*/
        const val E_REMOTE = -10001
        /**ネットワーク接続時にエラーが発生した。*/
        const val E_NETWORK = -10002
    }
}