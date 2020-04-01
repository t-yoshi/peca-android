package org.peercast.core.lib

import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.ResponseBody
import org.peercast.core.lib.internal.IPeerCastEndPoint
import org.peercast.core.lib.internal.SquareUtils
import org.peercast.core.lib.internal.SquareUtils.runAwait
import java.io.IOException

/**
 * PeerCast-YT または PeerCastStation へのRPC接続
 * @author (c) 2019, T Yoshizawa
 * @licenses Dual licensed under the MIT or GPL licenses.
 */
class JsonRpcConnection internal constructor(private val endPoint: IPeerCastEndPoint) : IJsonRpcConnection {

    constructor(host: String = "127.0.0.1", port: Int) : this(object : IPeerCastEndPoint {
        override suspend fun getRpcEndPoint(): String {
            return "http://$host:$port/api/1"
        }
    }) {
        if (host.isEmpty() || port !in IntRange(1025, 65535))
            throw IllegalArgumentException("Invalid host or port. [$host:$port]")
    }

    override suspend fun <T> post(requestBody: RequestBody, convertBody: (ResponseBody) -> T): T {
        val req = Request.Builder()
                .url(endPoint.getRpcEndPoint())
                .header("User-Agent", "LibPeerCast-${BuildConfig.VERSION_NAME}")
                .header("X-Requested-With", "XMLHttpRequest")
                .post(requestBody)
                .build()
        return SquareUtils.okHttpClient.newCall(req)
                .runAwait {
                    convertBody(it.body ?: throw IOException("body is null"))
                }
    }
}