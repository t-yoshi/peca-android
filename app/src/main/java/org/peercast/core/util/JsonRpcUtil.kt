package org.peercast.core.util

import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
/**
 * @author (c) 2014-2019, T Yoshizawa
 * @licenses Dual licensed under the MIT or GPL licenses.
 */
object JsonRpcUtil {
    /**nothrow*/
    fun exec(port: Int, jsonRequest: String): String {
        return try {
            val url = URL("http://localhost:$port/api/1")
            with(url.openConnection() as HttpURLConnection) {
                setRequestProperty("X-Requested-With", "XMLHttpRequest")
                requestMethod = "POST"
                doOutput = true
                connectTimeout = 3_000
                readTimeout = 3_000
                outputStream.writer().use {
                    it.write(jsonRequest)
                }
                inputStream.reader().use { r ->
                    r.readText()
                }
            }
        } catch (e: IOException) {
            //IOExceptionをJson形式のエラーに変換
            createErrorResponse("${e.message}", -10000)
        }
    }

    private fun jsonrpc() = JSONObject().also {
        it.put("jsonrpc", "2.0")
    }

    private fun createErrorResponse(message: String, code: Int): String {
        val error = JSONObject().also {
            it.put("code", code)
            it.put("message", message)
        }
        return jsonrpc().also {
            it.put("error", error)
        }.toString()
    }

    private var id_ = 2000

    fun createRequest(method: String, vararg params: String?): String {
        return jsonrpc().also {
            it.put("method", method)
            it.put("id", id_++)
            it.put("params", JSONArray(params))
        }.toString()
    }
}
