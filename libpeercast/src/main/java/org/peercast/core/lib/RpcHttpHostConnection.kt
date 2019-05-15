package org.peercast.core.lib

import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

/**
 * 内部のPeerCastまたは、Lan内のPeerCastStationへの接続する
 * */
class RpcHttpHostConnection(host: String, port: Int) : RpcHostConnection {
    val url = URL("http://$host:$port/api/1")

    override suspend fun executeRpc(request: String): String {
        return try {
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
                    r.readText()
                }
            }
        } catch (e: IOException) {
            //IOExceptionをJson形式のエラーに変換
            val msg = JSONObject.quote(e.message)
            """
                {"jsonrpc": "2.0", "error": {"code": -10000, "message": "$msg"}, "id": null}
            """.trimIndent()
        }
    }

}