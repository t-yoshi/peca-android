package org.peercast.core.lib

import com.squareup.moshi.Types
import okhttp3.RequestBody.Companion.toRequestBody
import org.peercast.core.lib.internal.SquareUtils
import org.peercast.core.lib.rpc.*
import java.io.IOException
import java.lang.reflect.Type

/**
 * PeerCastController経由でRPCコマンドを実行する。
 *
 * @licenses Dual licensed under the MIT or GPL licenses.
 * @author (c) 2019-2020, T Yoshizawa
 * @see <a href=https://github.com/kumaryu/peercaststation/wiki/JSON-RPC-API-%E3%83%A1%E3%83%A2>JSON RPC API メモ</a>
 * @version 3.1.0
 */
class PeerCastRpcClient(private val conn: IJsonRpcConnection) {

    constructor(controller: PeerCastController) : this(JsonRpcConnection(controller))

    private suspend fun <T> sendCommand(request: JsonRpcRequest, resultType: Type): T {
        val type = Types.newParameterizedType(JsonRpcResponse::class.java, resultType)
        val adapter = SquareUtils.moshi.adapter<JsonRpcResponse<T>>(type)
        val jsRequest = SquareUtils.moshi.adapter(JsonRpcRequest::class.java).toJson(request)
        return conn.post(jsRequest.toRequestBody()) {
            adapter.fromJson(it.source())
                ?: throw JsonRpcException("fromJson() returned null", -10000, 0)
        }.getResultOrThrow()
    }

    //result=nullしか帰ってこない場合
    private suspend fun sendVoidCommand(request: JsonRpcRequest) {
        val type = Types.newParameterizedType(JsonRpcResponse::class.java, Any::class.java)
        val adapter = SquareUtils.moshi.adapter<JsonRpcResponse<Any>>(type)
        val jsRequest = SquareUtils.moshi.adapter(JsonRpcRequest::class.java).toJson(request)
        conn.post(jsRequest.toRequestBody()) {
            adapter.fromJson(it.source())
                ?: throw JsonRpcException("fromJson() returned null", -10000, 0)
        }.getResultOrNull()
    }

    /**
     * 稼働時間、ポート開放状態、IPアドレスなどの情報の取得。
     * @throws IOException
     *  **/
    suspend fun getStatus(): Status {
        return sendCommand(
            JsonRpcRequest.Builder("getStatus").build(),
            Status::class.java
        )
    }

    /**
     * チャンネルに再接続。
     * @throws IOException
     * @return なし
     * */
    suspend fun bumpChannel(channelId: String) {
        return sendVoidCommand(
            JsonRpcRequest.Builder("bumpChannel").setParams(channelId).build()
        )
    }

    /**
     * チャンネルを停止する。
     * @throws IOException
     * @return 成功か
     * */
    suspend fun stopChannel(channelId: String) {
        return sendVoidCommand(
            JsonRpcRequest.Builder("stopChannel").setParams(channelId).build()
        )
    }

    /**
     * チャンネルに関して特定の接続を停止する。成功すれば true、失敗すれば false を返す。
     * @throws IOException
     * @return 成功か
     * */
    suspend fun stopChannelConnection(channelId: String, connectionId: Int): Boolean {
        return sendCommand(
            JsonRpcRequest.Builder("stopChannelConnection").setParams(channelId, connectionId)
                .build(),
            Boolean::class.javaObjectType
        )
    }

    /**
     * チャンネルの接続情報。
     * @throws IOException
     * */
    suspend fun getChannelConnections(channelId: String): List<ChannelConnection> {
        return sendCommand(
            JsonRpcRequest.Builder("getChannelConnections").setParams(channelId).build(),
            Types.newParameterizedType(List::class.java, ChannelConnection::class.java)
        )
    }

    /**
     * リレーツリー情報。ルートは自分自身。
     * @throws IOException
     * */
    suspend fun getChannelRelayTree(channelId: String): List<ChannelRelayTree> {
        return sendCommand(
            JsonRpcRequest.Builder("getChannelRelayTree").setParams(channelId).build(),
            Types.newParameterizedType(List::class.java, ChannelRelayTree::class.java)
        )
    }

    suspend fun getChannelInfo(channelId: String): ChannelInfoResult {
        return sendCommand(
            JsonRpcRequest.Builder("getChannelInfo").setParams(channelId).build(),
            ChannelInfoResult::class.java
        )
    }

    /**
     * バージョン情報の取得。
     * @throws IOException
     * */
    suspend fun getVersionInfo(): VersionInfo {
        return sendCommand(
            JsonRpcRequest.Builder("getVersionInfo").build(),
            VersionInfo::class.java
        )
    }

    /**
     * 特定のチャンネルの情報。
     * @throws IOException
     * */
    suspend fun getChannelStatus(channelId: String): ChannelStatus {
        return sendCommand(
            JsonRpcRequest.Builder("getChannelStatus").setParams(channelId).build(),
            ChannelStatus::class.java
        )
    }

    /**
     * すべてのチャンネルの情報。
     * @throws IOException
     * */
    suspend fun getChannels(): List<Channel> {
        return sendCommand(
            JsonRpcRequest.Builder("getChannels").build(),
            Types.newParameterizedType(List::class.java, Channel::class.java)
        )
    }

    /**
     * リレーに関する設定の取得。
     * @throws IOException
     * */
    suspend fun getSettings(): Settings {
        return sendCommand(JsonRpcRequest.Builder("getSettings").build(), Settings::class.java)
    }

    /**
     * リレーに関する設定を変更。
     * @throws IOException
     * */
    suspend fun setSettings(settings: Settings) {
        return sendVoidCommand(
            JsonRpcRequest.Builder("setSettings").setParams(
                mapOf("settings" to settings)
            ).build())
    }

    /**
     * ログをクリア。
     * @throws IOException
     * */
    suspend fun clearLog() {
        return sendVoidCommand(JsonRpcRequest.Builder("clearLog").build())
    }

    /**
     * ログバッファーの内容の取得
     * @throws IOException
     * @since YT22
     * */
    suspend fun getLog(from: Int? = null, maxLines: Int? = null): List<Log> {
        val t = Types.newParameterizedType(List::class.java, Log::class.java)
        return sendCommand(
            JsonRpcRequest.Builder("getLog").setParams(
                mapOf("from" to from, "maxLines" to maxLines)
            ).build(), t)
    }

    /**
     * ログレベルの取得。
     * @throws IOException
     * @since YT22
     * */
    suspend fun getLogSettings(): LogSettings {
        return sendCommand(
            JsonRpcRequest.Builder("getLogSettings").build(),
            LogSettings::class.java)
    }

    /**
     * ログレベルの設定。
     * @throws IOException
     * @since YT22
     * */
    suspend fun setLogSettings(settings: LogSettings) {
        return sendVoidCommand(JsonRpcRequest.Builder("setLogSettings").setParam(settings).build())
    }

    /**
     * 外部サイトの index.txtから取得されたチャンネル一覧。YPブラウザでの表示用。
     * @throws IOException
     * */
    suspend fun getYPChannels(): List<YpChannel> {
        return sendCommand(
            JsonRpcRequest.Builder("getYPChannels").build(),
            Types.newParameterizedType(List::class.java, YpChannel::class.java)
        )
    }

    /**
     * 登録されているイエローページの取得。
     * @throws IOException
     * */
    suspend fun getYellowPages(): List<YellowPage> {
        return sendCommand(
            JsonRpcRequest.Builder("getYellowPages").build(),
            Types.newParameterizedType(List::class.java, YellowPage::class.java)
        )
    }

    /**
     * イエローページを追加。
     * @throws IOException
     * */
    suspend fun addYellowPage(
        protocol: String, name: String,
        uri: String = "", announceUri: String = "", channelsUri: String = "",
    ) {
        throw NotImplementedError("Not implemented yet in jrpc.cpp")
    }

    /**
     * イエローページを削除。
     * @throws IOException
     * */
    suspend fun removeYellowPage(yellowPageId: Int) {
        sendVoidCommand(
            JsonRpcRequest.Builder("removeYellowPage")
                .setParams(yellowPageId)
                .build()
        )
    }

    /**
     * YP から index.txt を取得する。
     * @throws IOException
     * */
    suspend fun updateYPChannels() {
        throw NotImplementedError("Not implemented yet in jrpc.cpp")
    }
}