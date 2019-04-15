package org.peercast.core.lib

import com.squareup.moshi.FromJson
import com.squareup.moshi.JsonQualifier
import com.squareup.moshi.JsonReader
import com.squareup.moshi.ToJson
/**
 * (c) 2019, T Yoshizawa
 * Dual licensed under the MIT or GPL licenses.
 */
@Retention(AnnotationRetention.RUNTIME)
@JsonQualifier
internal annotation class NullSafe

/**Json内のnullをデフォルト値に置き換える*/
internal object NullSafeAdapter {
    @FromJson
    @NullSafe
    fun fromJsonB(reader: JsonReader) : Boolean {
        if (reader.peek() == JsonReader.Token.NULL){
            reader.nextNull<Boolean>()
            return false
        }
        return reader.nextBoolean()
    }


    @FromJson
    @NullSafe
    fun fromJsonI(reader: JsonReader) : Int {
        if (reader.peek() == JsonReader.Token.NULL){
            reader.nextNull<Int>()
            return 0
        }
        return reader.nextInt()
    }

    @FromJson
    @NullSafe
    fun fromJsonL(reader: JsonReader) : Long {
        if (reader.peek() == JsonReader.Token.NULL){
            reader.nextNull<Long>()
            return 0L
        }
        return reader.nextLong()
    }

    @FromJson
    @NullSafe
    fun fromJsonD(reader: JsonReader) : Double {
        if (reader.peek() == JsonReader.Token.NULL){
            reader.nextNull<Double>()
            return 0.0
        }
        return reader.nextDouble()
    }

    @FromJson
    @NullSafe
    fun fromJsonS(reader: JsonReader) : String {
        if (reader.peek() == JsonReader.Token.NULL){
            reader.nextNull<String>()
            return ""
        }
        return reader.nextString()
    }

    @ToJson
    fun toJson(@NullSafe value: Boolean) : Boolean = value

    @ToJson
    fun toJson(@NullSafe value: Int) : Int = value

    @ToJson
    fun toJson(@NullSafe value: Long) : Long = value

    @ToJson
    fun toJson(@NullSafe value: Double) : Double = value

    @ToJson
    fun toJson(@NullSafe value: String) : String = value
}

