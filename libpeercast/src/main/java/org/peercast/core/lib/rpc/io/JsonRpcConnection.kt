package org.peercast.core.lib.rpc.io

import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import org.peercast.core.lib.BuildConfig
import org.peercast.core.lib.internal.BaseJsonRpcConnection
import org.peercast.core.lib.internal.ktorHttpClient

/**
 * PeerCast-YT または PeerCastStation へのRPC接続
 * @author (c) 2019, T Yoshizawa
 * @licenses Dual licensed under the MIT or GPL licenses.
 */
class JsonRpcConnection : BaseJsonRpcConnection {
    constructor(endPoint: String) : super(endPoint)
    constructor(host: String = "127.0.0.1", port: Int) : super(host, port)

    override suspend fun <T> post(postBody: String, decodeJson: (String) -> T): T {
        val res = ktorHttpClient.post<HttpResponse> {
            url(endPoint)
            contentType(ContentType.Application.Json)
            header("User-Agent", "LibPeerCast-${BuildConfig.LIB_VERSION}")
            header("X-Requested-With", "XMLHttpRequest")
            body = postBody
        }
        return decodeJson(res.readText())
    }
}
