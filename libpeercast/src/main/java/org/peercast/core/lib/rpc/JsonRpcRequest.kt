package org.peercast.core.lib.rpc

import com.squareup.moshi.JsonClass
import java.util.concurrent.atomic.AtomicInteger
/**
 * (c) 2019, T Yoshizawa
 * Dual licensed under the MIT or GPL licenses.
 */
@JsonClass(generateAdapter = true)
class JsonRpcRequest internal constructor(
        val jsonrpc: String,
        val method: String,
        val params: Any, //Array<String?> or List<String> or Map<String,Any?>
        val id: Int?
) {

    class Builder(private val method: String) {
        private var params : Any? = null
        private var id: Int? = id_.getAndIncrement()

        fun setParam(param: Any) : Builder {
            if (this.params != null)
                throw IllegalStateException("already params set")
            this.params = param
            return this
        }
        fun setParams(vararg params: Any?) : Builder {
            if (this.params != null)
                throw IllegalStateException("already params set")
            this.params = params
            return this
        }
        fun setParams(params: List<String?>) : Builder {
            if (this.params != null)
                throw IllegalStateException("already params set")
            this.params = params
            return this
        }
        fun setParams(params: Map<String, Any?>) : Builder {
            if (this.params != null)
                throw IllegalStateException("already params set")
            this.params = params
            return this
        }
        fun setId(id: Int?) : Builder {
            this.id = id
            return this
        }
        fun build() = JsonRpcRequest("2.0", method, params ?: emptyList<String>(), id)
    }

    init {
        if (jsonrpc != "2.0")
            throw IllegalArgumentException("jsonrpc != 2.0")
    }

    companion object {
        private val id_ = AtomicInteger(10000)
    }
}