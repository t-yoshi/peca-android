package org.peercast.core.lib.rpc.io

import kotlinx.serialization.json.*
import java.util.concurrent.atomic.AtomicInteger

/**
 * JsonRpcのリクエスト。
 * @author (c) 2019, T Yoshizawa
 * @licenses Dual licensed under the MIT or GPL licenses.
 */

private val id = AtomicInteger(1)

private fun baseRpcBuildRequest(method: String, action: JsonObjectBuilder.() -> Unit): JsonObject {
    return buildJsonObject {
        put("jsonrpc", "2.0")
        put("method", method)
        action() //set "params"
        put("id", id.incrementAndGet())
    }
}

internal fun buildRpcRequest(method: String) =
    baseRpcBuildRequest(method) {}

internal fun buildRpcRequest(method: String, param: String) =
    buildRpcRequest(method, JsonPrimitive(param))

internal fun buildRpcRequest(method: String, param: Number) =
    buildRpcRequest(method, JsonPrimitive(param))


internal fun buildRpcRequest(method: String, param: JsonElement) =
    baseRpcBuildRequest(method) {
        put("params", param)
    }


internal fun buildRpcRequestObjectParams(
    method: String,
    action: JsonObjectBuilder.() -> Unit,
): JsonObject {
    return baseRpcBuildRequest(method) {
        putJsonObject("params", action)
    }
}

internal fun buildRpcRequestArrayParams(
    method: String,
    action: JsonArrayBuilder.() -> Unit,
): JsonObject {
    return baseRpcBuildRequest(method) {
        putJsonArray("params", action)
    }
}


