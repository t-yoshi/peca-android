package org.peercast.core.lib

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