package org.peercast.core.lib.rpc

/**
 * (c) 2019, T Yoshizawa
 * Dual licensed under the MIT or GPL licenses.
 */
class JsonRpcException internal constructor(message: String,
                       val code: Int=-10000,
                       val id: Int?=null,
                       cause: Throwable? = null) : Exception(message, cause) {

    override fun toString() = "${javaClass.name}: $message ($code, id=$id)"
}
