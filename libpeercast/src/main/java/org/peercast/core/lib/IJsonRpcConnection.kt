package org.peercast.core.lib
/**
 * @author (c) 2014-2021, T Yoshizawa
 * @licenses Dual licensed under the MIT or GPL licenses.
 */
import okhttp3.RequestBody
import okhttp3.ResponseBody

interface IJsonRpcConnection {
    /**
     * Json-RPCリクエストをpostする。
     * @param convertBody ResponseBodyをTに変換する。
     * @throws java.io.IOException
     * */
    suspend fun <T> post(requestBody: RequestBody, convertBody: (ResponseBody) -> T): T
}