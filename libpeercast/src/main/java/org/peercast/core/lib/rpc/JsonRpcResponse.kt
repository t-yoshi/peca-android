package org.peercast.core.lib.rpc
/**
 * (c) 2019, T Yoshizawa
 * Dual licensed under the MIT or GPL licenses.
 */
class JsonRpcResponse<T> (
        val jsonrpc: String,
        private val error: Error?,
        private val result: T?
) {
    fun getResultOrThrow(): T {
        result?.let {
            return it
        }
        error?.throwException()

        throw RuntimeException("result==null && error==null")
    }

    fun getResultOrNull() = result

    class Error(val message: String?, val code: Int?, val id:Int?){
        fun throwException() {
           throw JsonRpcException(message ?: "rpc error", code ?: -1, id
                   ?: 0)
        }
    }

}