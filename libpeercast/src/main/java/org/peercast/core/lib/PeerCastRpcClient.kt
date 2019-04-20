package org.peercast.core.lib

import com.squareup.moshi.Types
import org.peercast.core.lib.rpc.*
import java.lang.reflect.Type

/**
 * PeerCastController経由でRPCコマンドを実行する。
 *
 * @licenses Dual licensed under the MIT or GPL licenses.
 * @author (c) 2019, T Yoshizawa
 * @see <a href=https://github.com/kumaryu/peercaststation/wiki/JSON-RPC-API-%E3%83%A1%E3%83%A2>JSON RPC API メモ</a>
 * @version 3.0.0
 */
class PeerCastRpcClient internal constructor(private val rpcBridge: PeerCastServiceRpcBridge) {

    constructor(controller: PeerCastController) : this(controller as PeerCastServiceRpcBridge)

    /**
     * 稼働時間、ポート開放状態、IPアドレスなどの情報の取得。
     * @throws JsonRpcException
     *  **/
    suspend fun getStatus(): Status {
        return sendCommand(Status::class.java, JsonRpcRequest.Builder("getStatus").build())
    }

    /**
     * チャンネルに再接続。
     * @throws JsonRpcException
     * @return なし
     * */
    suspend fun bumpChannel(channelId: String) {
        return sendVoidCommand(
                JsonRpcRequest.Builder("bumpChannel").setParams(channelId).build()
        )
    }

    /**
     * チャンネルを停止する。
     * @throws JsonRpcException
     * @return 成功か
     * */
    suspend fun stopChannel(channelId: String): Boolean {
        return sendCommand(Boolean::class.javaObjectType,
                JsonRpcRequest.Builder("stopChannel").setParams(channelId).build()
        )
    }

    /**
     * チャンネルに関して特定の接続を停止する。成功すれば true、失敗すれば false を返す。
     * @throws JsonRpcException
     * @return 成功か
     * */
    suspend fun stopChannelConnection(channelId: String, connectionId: Int): Boolean {
        return sendCommand(Boolean::class.javaObjectType,
                JsonRpcRequest.Builder("stopChannelConnection").setParams(channelId, connectionId).build()
        )
    }

    /**
     * チャンネルの接続情報。
     * @throws JsonRpcException
     * */
    suspend fun getChannelConnections(channelId: String): List<ChannelConnection> {
        val t = Types.newParameterizedType(List::class.java, ChannelConnection::class.java)
        return sendCommand(t,
                JsonRpcRequest.Builder("getChannelConnections").setParams(channelId).build()
        )
    }

    /**
     * リレーツリー情報。ルートは自分自身。
     * @throws JsonRpcException
     * */
    suspend fun getChannelRelayTree(channelId: String): List<ChannelRelayTree> {
        return sendCommand(
                Types.newParameterizedType(List::class.java, ChannelRelayTree::class.java),
                JsonRpcRequest.Builder("getChannelRelayTree").setParams(channelId).build()
        )
    }

    suspend fun getChannelInfo(channelId: String): ChannelInfoResult {
        return sendCommand(
                ChannelInfoResult::class.java,
                JsonRpcRequest.Builder("getChannelInfo").setParams(channelId).build()
        )
    }


    /**
     * バージョン情報の取得。
     * @throws JsonRpcException
     * */
    suspend fun getVersionInfo(): VersionInfo {
        return sendCommand(
                VersionInfo::class.java,
                JsonRpcRequest.Builder("getVersionInfo").build()
        )
    }

    /**
     * 特定のチャンネルの情報。
     * @throws JsonRpcException
     * */
    suspend fun getChannelStatus(channelId: String): ChannelStatus {
        return sendCommand(
                ChannelStatus::class.java,
                JsonRpcRequest.Builder("getChannelStatus").setParams(channelId).build()
        )
    }

    /**
     * すべてのチャンネルの情報。
     * @throws JsonRpcException
     * */
    suspend fun getChannels(): List<Channel> {
        val t = Types.newParameterizedType(List::class.java, Channel::class.java)
        return sendCommand(t, JsonRpcRequest.Builder("getChannels").build())
    }

    /**
     * リレーに関する設定の取得。
     * @throws JsonRpcException
     * */
    suspend fun getSettings(): Settings {
        return sendCommand(Settings::class.java, JsonRpcRequest.Builder("getSettings").build())
    }

    /**
     * リレーに関する設定を変更。
     * @throws JsonRpcException
     * */
    suspend fun setSettings(settings: Settings) {
        return sendVoidCommand(
                JsonRpcRequest.Builder("setSettings").setParams(
                        mapOf("settings" to settings)
                ).build())
    }

    /**
     * ログをクリア。
     * @throws JsonRpcException
     * */
    suspend fun clearLog() {
        return sendVoidCommand(JsonRpcRequest.Builder("clearLog").build())
    }

    /**
     * ログバッファーの内容の取得
     * @throws JsonRpcException
     * @since YT22
     * */
    suspend fun getLog(from: Int? = null, maxLines: Int? = null): List<Log> {
        val t = Types.newParameterizedType(List::class.java, Log::class.java)
        return sendCommand(t,
                JsonRpcRequest.Builder("getLog").setParams(
                        mapOf("from" to from, "maxLines" to maxLines)
                ).build())
    }

    /**
     * ログレベルの取得。
     * @throws JsonRpcException
     * @since YT22
     * */
    suspend fun getLogSettings(): LogSettings {
        return sendCommand(LogSettings::class.java,
                JsonRpcRequest.Builder("getLogSettings").build())
    }

    /**
     * ログレベルの設定。
     * @throws JsonRpcException
     * @since YT22
     * */
    suspend fun setLogSettings(settings: LogSettings) {
        return sendVoidCommand(JsonRpcRequest.Builder("setLogSettings").setParam(settings).build())
    }

    private suspend fun <R> sendCommand(resultType: Type, req: JsonRpcRequest): R {
        val type = Types.newParameterizedType(JsonRpcResponse::class.java, resultType)
        val adapter = LibPeerCast.MOSHI.adapter<JsonRpcResponse<R>>(type)
        val jsRequest = LibPeerCast.MOSHI.adapter(JsonRpcRequest::class.java).toJson(req)
        val jsResponse = rpcBridge.executeRpc(jsRequest)

        return adapter.fromJson(jsResponse)?.getResultOrThrow()
                ?: throw JsonRpcException("fromJson() returned null", -10000, 0)
    }

    //result=nullしか帰ってこない場合
    private suspend fun sendVoidCommand(req: JsonRpcRequest) {
        val type = Types.newParameterizedType(JsonRpcResponse::class.java, Any::class.java)
        val adapter = LibPeerCast.MOSHI.adapter<JsonRpcResponse<Any>>(type)
        val jsRequest = LibPeerCast.MOSHI.adapter(JsonRpcRequest::class.java).toJson(req)
        val jsResponse = rpcBridge.executeRpc(jsRequest)

        adapter.fromJson(jsResponse)?.let {
            it.getResultOrNull()
            return
        } ?: throw JsonRpcException("fromJson() returned null", -10000, 0)
    }
}