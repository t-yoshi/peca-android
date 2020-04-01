package org.peercast.core.lib

/**
 * JsonRpcのレスポンス
 * @author (c) 2019, T Yoshizawa
 * @licenses Dual licensed under the MIT or GPL licenses.
 */
class JsonRpcResponse<T> internal constructor(
        val jsonrpc: String,
        private val error: Error?,
        private val result: T?
) {
    /**resultがnullの場合はJsonRpcExceptionをスローする*/
    fun getResultOrThrow(): T {
        result?.let {
            return it
        }
        error?.throwException()

        throw RuntimeException("result==null && error==null")
    }

    /**resultがnullの場合はそのままnullを返す*/
    fun getResultOrNull() = result

    internal class Error(val message: String?, val code: Int?, val id:Int?){
        fun throwException() {
           throw JsonRpcException(message
                   ?: "rpc error", code ?: -1, id
                   ?: 0)
        }
    }

}