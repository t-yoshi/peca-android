package org.peercast.core.lib.test

import org.peercast.core.lib.internal.BaseJsonRpcConnection

class MockJsonRpcConnection(private val s: String) : BaseJsonRpcConnection("data:$s") {
    override suspend fun <T> post(postBody: String, decodeJson: (String) -> T): T {
        return decodeJson(s)
    }
}