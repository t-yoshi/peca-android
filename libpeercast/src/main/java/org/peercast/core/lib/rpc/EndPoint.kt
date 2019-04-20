package org.peercast.core.lib.rpc

import com.squareup.moshi.FromJson
import com.squareup.moshi.JsonDataException
import com.squareup.moshi.JsonReader
/**
 * ホストとポート
 * @author (c) 2019, T Yoshizawa
 * @licenses Dual licensed under the MIT or GPL licenses.
 */
data class EndPoint internal constructor(val host: String, val port: Int) {
    override fun toString() = "$host:$port"
}

internal object EndPointAdapter {
    @FromJson
    fun fromJson(reader: JsonReader) : EndPoint? {
        return when(val t = reader.peek()){
            JsonReader.Token.NULL -> {
                reader.nextNull<Unit>()
                null
            }
            JsonReader.Token.BEGIN_ARRAY -> {
                reader.beginArray()
                val h = reader.nextString()
                val p = reader.nextInt()
                reader.endArray()
                EndPoint(h, p)
            }
            JsonReader.Token.STRING -> {
                val s = reader.nextString()
                """(.+):(\d+)""".toRegex().matchEntire(s)?. groupValues?.let {
                    return EndPoint(it[1], it[2].toInt())
                } ?: throw JsonDataException("not matched: $s")
            }
            else->throw JsonDataException("Expected: $t")
        }
    }

}