package org.peercast.core.lib.rpc.io

import kotlinx.serialization.json.JsonArrayBuilder
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonObjectBuilder
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject
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
    buildRpcRequestArrayParams(method) {
        add(param)
    }

internal fun buildRpcRequest(method: String, param: Number) =
    buildRpcRequestArrayParams(method) {
        add(param)
    }


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


