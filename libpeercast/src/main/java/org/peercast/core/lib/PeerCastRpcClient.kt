package org.peercast.core.lib

import android.net.Uri
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.add
import kotlinx.serialization.json.encodeToJsonElement
import kotlinx.serialization.json.put
import org.peercast.core.lib.internal.BaseJsonRpcConnection
import org.peercast.core.lib.rpc.Channel
import org.peercast.core.lib.rpc.ChannelConnection
import org.peercast.core.lib.rpc.ChannelInfoResult
import org.peercast.core.lib.rpc.ChannelRelayTree
import org.peercast.core.lib.rpc.ChannelStatus
import org.peercast.core.lib.rpc.Log
import org.peercast.core.lib.rpc.LogSettings
import org.peercast.core.lib.rpc.Settings
import org.peercast.core.lib.rpc.Status
import org.peercast.core.lib.rpc.VersionInfo
import org.peercast.core.lib.rpc.YellowPage
import org.peercast.core.lib.rpc.YpChannel
import org.peercast.core.lib.rpc.io.JsonRpcConnection
import org.peercast.core.lib.rpc.io.buildRpcRequest
import org.peercast.core.lib.rpc.io.buildRpcRequestArrayParams
import org.peercast.core.lib.rpc.io.buildRpcRequestObjectParams
import org.peercast.core.lib.rpc.io.decodeRpcResponse
import org.peercast.core.lib.rpc.io.decodeRpcResponseOnlyErrorCheck
import java.io.IOException

/**
 * PeerCastController経由でRPCコマンドを実行する。
 *
 * @licenses Dual licensed under the MIT or GPL licenses.
 * @author (c) 2019-2020, T Yoshizawa
 * @see <a href=https://github.com/kumaryu/peercaststation/wiki/JSON-RPC-API-%E3%83%A1%E3%83%A2>JSON RPC API メモ</a>
 * @version 4.0.0
 */
class PeerCastRpcClient(private val conn: BaseJsonRpcConnection) {

    /**@param endPoint RPC接続へのURL*/
    constructor(endPoint: String) : this(JsonRpcConnection(endPoint))

    constructor(controller: PeerCastController) : this(controller.rpcEndPoint)

    /**RPC接続へのURL*/
    val rpcEndPoint: Uri get() = Uri.parse(conn.endPoint)

    private suspend inline fun <reified T> JsonObject.sendCommand(): T {
        return conn.post(this.toString()) {
            decodeRpcResponse(it)
        }
    }

    //result=nullしか帰ってこない場合
    private suspend fun JsonObject.sendVoidCommand() {
        conn.post(this.toString()) {
            decodeRpcResponseOnlyErrorCheck(it)
        }
    }

    /**
     * 稼働時間、ポート開放状態、IPアドレスなどの情報の取得。
     * @throws IOException
     *  **/
    suspend fun getStatus(): Status {
        return buildRpcRequest("getStatus").sendCommand()
    }

    /**
     * チャンネルに再接続。
     * @throws IOException
     * @return なし
     * */
    suspend fun bumpChannel(channelId: String) {
        return buildRpcRequest("bumpChannel", channelId).sendVoidCommand()
    }

    /**
     * チャンネルを停止する。
     * @throws IOException
     * @return 成功か
     * */
    suspend fun stopChannel(channelId: String) {
        buildRpcRequest("stopChannel", channelId)
            .sendVoidCommand()
    }

    /**
     * チャンネルに関して特定の接続を停止する。成功すれば true、失敗すれば false を返す。
     * @throws IOException
     * @return 成功か
     * */
    suspend fun stopChannelConnection(channelId: String, connectionId: Int): Boolean {
        return buildRpcRequestArrayParams("stopChannelConnection") {
            add(channelId)
            add(connectionId)
        }.sendCommand()
    }

    /**
     * チャンネルの接続情報。
     * @throws IOException
     * */
    suspend fun getChannelConnections(channelId: String): List<ChannelConnection> {
        return buildRpcRequest("getChannelConnections", channelId).sendCommand()
    }

    /**
     * リレーツリー情報。ルートは自分自身。
     * @throws IOException
     * */
    suspend fun getChannelRelayTree(channelId: String): List<ChannelRelayTree> {
        return buildRpcRequest("getChannelRelayTree", channelId)
            .sendCommand()
    }

    suspend fun getChannelInfo(channelId: String): ChannelInfoResult {
        return buildRpcRequest("getChannelInfo", channelId).sendCommand()
    }

    /**
     * バージョン情報の取得。
     * @throws IOException
     * */
    suspend fun getVersionInfo(): VersionInfo {
        return buildRpcRequest("getVersionInfo").sendCommand()
    }

    /**
     * 特定のチャンネルの情報。
     * @throws IOException
     * */
    suspend fun getChannelStatus(channelId: String): ChannelStatus {
        return buildRpcRequest("getChannelStatus", channelId).sendCommand()
    }

    /**
     * すべてのチャンネルの情報。
     * @throws IOException
     * */
    suspend fun getChannels(): List<Channel> {
        return buildRpcRequest("getChannels").sendCommand()
    }

    /**
     * リレーに関する設定の取得。
     * @throws IOException
     * */
    suspend fun getSettings(): Settings {
        return buildRpcRequest("getSettings").sendCommand()
    }

    /**
     * リレーに関する設定を変更。
     * @throws IOException
     * */
    suspend fun setSettings(settings: Settings) {
        buildRpcRequestObjectParams("setSettings") {
            put("settings", Json.encodeToJsonElement(settings))
        }.sendVoidCommand()
    }

    /**
     * ログをクリア。
     * @throws IOException
     * */
    suspend fun clearLog() {
        buildRpcRequest("clearLog").sendVoidCommand()
    }

    /**
     * ログバッファーの内容の取得
     * @throws IOException
     * @since YT22
     * */
    suspend fun getLog(from: Int? = null, maxLines: Int? = null): List<Log> {
        return buildRpcRequestObjectParams("getLog") {
            put("from", from)
            put("maxLines", maxLines)
        }.sendCommand()
    }

    /**
     * ログレベルの取得。
     * @throws IOException
     * @since YT22
     * */
    suspend fun getLogSettings(): LogSettings {
        return buildRpcRequest("getLogSettings").sendCommand()
    }

    /**
     * ログレベルの設定。
     * @throws IOException
     * @since YT22
     * */
    suspend fun setLogSettings(settings: LogSettings) {
        buildRpcRequest(
            "setLogSettings",
            Json.encodeToJsonElement(settings)
        ).sendVoidCommand()
    }

    /**
     * 外部サイトの index.txtから取得されたチャンネル一覧。YPブラウザでの表示用。
     * @throws IOException
     * */
    suspend fun getYPChannels(): List<YpChannel> {
        return buildRpcRequest("getYPChannels").sendCommand()
    }

    /**
     * 登録されているイエローページの取得。
     * @throws IOException
     * */
    suspend fun getYellowPages(): List<YellowPage> {
        return buildRpcRequest("getYellowPages").sendCommand()
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
        buildRpcRequest("removeYellowPage", yellowPageId).sendVoidCommand()
    }

    /**
     * YP から index.txt を取得する。
     * @throws IOException
     * */
    suspend fun updateYPChannels() {
        throw NotImplementedError("Not implemented yet in jrpc.cpp")
    }

    override fun hashCode(): Int {
        return javaClass.hashCode() * 31 + conn.hashCode()
    }

    override fun equals(other: Any?): Boolean {
        return other is PeerCastRpcClient &&
                other.javaClass == javaClass &&
                other.conn == conn
    }


}