package org.peercast.core.lib.rpc

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonPrimitive

/**
 * ホストとポート
 * @author (c) 2019, T Yoshizawa
 * @licenses Dual licensed under the MIT or GPL licenses.
 */
@Parcelize
@Serializable(with = EndPointSerializer::class)
data class EndPoint internal constructor(val host: String, val port: Int) : Parcelable {
    init {
        check(host.isNotEmpty())
        check(port in 1..65532)
    }

    override fun toString() = "$host:$port"

    companion object {
        internal fun from(js: JsonPrimitive): EndPoint {
            val (h, p) = js.content.split(':')
            return EndPoint(h, p.toInt())
        }

        internal fun from(js: JsonArray): EndPoint {
            check(js.size == 2)
            val h = js[0].jsonPrimitive.content
            val p = js[1].jsonPrimitive.int
            return EndPoint(h, p)
        }
    }
}

private object EndPointSerializer : KSerializer<EndPoint> {
    override val descriptor = buildClassSerialDescriptor("EndPoint") {

    }

    override fun serialize(encoder: Encoder, value: EndPoint) {
        error("not implemented")
    }

    override fun deserialize(decoder: Decoder): EndPoint {
        check(decoder is JsonDecoder)
        return when (val e = decoder.decodeJsonElement()) {
            is JsonArray -> {
                EndPoint.from(e)
            }

            is JsonPrimitive -> {
                EndPoint.from(e)
            }

            else -> throw IllegalArgumentException("$e")
        }
    }
}
