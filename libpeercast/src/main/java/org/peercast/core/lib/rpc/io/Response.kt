package org.peercast.core.lib.rpc.io

import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

/**
 * JsonRpcのレスポンス
 * @author (c) 2019, T Yoshizawa
 * @licenses Dual licensed under the MIT or GPL licenses.
 */

private val format = Json {
    isLenient = true
    ignoreUnknownKeys = true
    coerceInputValues = true
}

internal inline fun <reified T> decodeRpcResponse(s: String): T {
    return try {
        val res = format.decodeFromString<JsonRpcResponse<T>>(s)
        res.getResultOrThrow()
    } catch (e: SerializationException) {
        throw JsonRpcException("decodeFromString failed", -10000, 0, e)
    }
}

internal fun decodeRpcResponseOnlyErrorCheck(s: String) {
    try {
        val res = format.decodeFromString<JsonRpcResponse<String>>(s)
        res.throwJsonRpcExceptionIfError()
    } catch (e: SerializationException) {
        throw JsonRpcException("decodeFromString failed", -10000, 0, e)
    }
}

@Serializable
internal class JsonRpcResponse<T>(
    val jsonrpc: String,
    private val error: Error? = null,
    private val result: T? = null,
) {

    fun throwJsonRpcExceptionIfError() {
        error?.run {
            throw JsonRpcException(message, code, id)
        }
    }

    /**resultがnullの場合はJsonRpcExceptionをスローする*/
    fun getResultOrThrow(): T {
        throwJsonRpcExceptionIfError()
        return result ?: throw JsonRpcException("result is null")
    }

    @Serializable
    data class Error(val message: String = "rpc error", val code: Int = -1, val id: Int = 0)

}