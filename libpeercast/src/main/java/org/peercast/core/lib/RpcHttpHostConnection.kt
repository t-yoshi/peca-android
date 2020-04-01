package org.peercast.core.lib

import org.peercast.core.lib.internal.JsonRpcUtil
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

/**
 * PeerCast-YT or PeerCastStationに接続する
 * */
class RpcHttpHostConnection(host: String, port: Int) : RpcHostConnection {
    val url = URL("http://$host:$port/api/1")

    override suspend fun executeRpc(request: String): String {
        try {
            with(url.openConnection() as HttpURLConnection) {
                setRequestProperty("X-Requested-With", "XMLHttpRequest")
                requestMethod = "POST"
                doOutput = true
                connectTimeout = 3_000
                readTimeout = 3_000
                outputStream.writer().use {
                    it.write(request)
                }
                inputStream.reader().use { r ->
                    return r.readText()
                }
            }
        } catch (e: IOException) {
            return JsonRpcUtil.toRpcError(e, RpcHostConnection.E_NETWORK)
        }
    }

}