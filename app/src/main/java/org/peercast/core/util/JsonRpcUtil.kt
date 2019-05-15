package org.peercast.core.util

import org.json.JSONArray
import org.json.JSONObject
/**
 * @author (c) 2014-2019, T Yoshizawa
 * @licenses Dual licensed under the MIT or GPL licenses.
 */
object JsonRpcUtil {

    private fun jsonrpc() = JSONObject().also {
        it.put("jsonrpc", "2.0")
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
