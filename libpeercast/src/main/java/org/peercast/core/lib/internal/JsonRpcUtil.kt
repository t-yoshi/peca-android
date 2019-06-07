package org.peercast.core.lib.internal

import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import org.peercast.core.lib.NullSafeAdapter
import org.peercast.core.lib.rpc.ChannelInfo
import org.peercast.core.lib.rpc.EndPointAdapter
import org.peercast.core.lib.rpc.JsonRpcRequest
import org.peercast.core.lib.rpc.JsonRpcResponse
import java.util.concurrent.atomic.AtomicInteger

/**
 * 内部用のUtilクラス
 * (c) 2019, T Yoshizawa
 * Dual licensed under the MIT or GPL licenses.
 */
object JsonRpcUtil {
    internal val MOSHI = Moshi.Builder()
            .add(NullSafeAdapter)
            .add(EndPointAdapter)
            .build()

    /**[Exception]をJson-RPCのエラーに変換*/
    fun toRpcError(e: Exception, code: Int?): String {
        val res = JsonRpcResponse<String>("2.0",
                JsonRpcResponse.Error(e.message, code, null),
                null)
        val type = Types.newParameterizedType(JsonRpcResponse::class.java, String::class.java)
        return JsonRpcUtil.MOSHI.adapter<JsonRpcResponse<String>>(type).toJson(res)
    }

    /**
     * PeerCastService.notifyChannelで使用
     * */
    fun parseChannelInfo(json: String): ChannelInfo? {
        return MOSHI.adapter(ChannelInfo::class.java).fromJson(json)
    }

    private var id_ = AtomicInteger()

    fun createRequest(method: String, vararg params: String?): String {
        val req = JsonRpcRequest.Builder(method).setParams(*params).setId(id_.getAndIncrement()).build()
        return MOSHI.adapter(JsonRpcRequest::class.java).toJson(req)
    }

}