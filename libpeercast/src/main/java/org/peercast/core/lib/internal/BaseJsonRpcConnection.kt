package org.peercast.core.lib.internal

/**
 * @author (c) 2019, T Yoshizawa
 * @licenses Dual licensed under the MIT or GPL licenses.
 */
abstract class BaseJsonRpcConnection internal constructor(val endPoint: String) {
    internal constructor(host: String, port: Int) : this("http://$host:$port/api/1") {
        if (host.isEmpty() || port !in IntRange(1025, 65535))
            throw IllegalArgumentException("Invalid host or port. [$host:$port]")
    }

    abstract suspend fun <T> post(postBody: String, decodeJson: (String) -> T): T

    override fun hashCode(): Int {
        return javaClass.hashCode() * 31 + endPoint.hashCode()
    }

    override fun equals(other: Any?): Boolean {
        return other is BaseJsonRpcConnection &&
                other.javaClass == javaClass &&
                other.endPoint == endPoint
    }
}