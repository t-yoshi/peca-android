package org.peercast.core.lib

import android.content.Intent
import android.net.Uri
import org.peercast.core.lib.rpc.ChannelInfo
import org.peercast.core.lib.rpc.YpChannel

/**
 * (c) 2019, T Yoshizawa
 * Dual licensed under the MIT or GPL licenses.
 */
object LibPeerCast {
    /**
     * ストリーム再生用のURL。
     * */
    fun getStreamUrl(channelId: String, port: Int, channelInfo: ChannelInfo? = null) : Uri {
        return getStreamUrl(channelId, port, channelInfo?.contentType ?: "")
    }

    internal fun getMimeType (contentType: String?) : String {
        return when(contentType?.uppercase()){
            "WMV" -> "video/x-wmv"
            "FLV" -> "video/x-flv"
            "MKV" -> "video/x-mkv"
            "WEBM" -> "video/x-webm"
            else -> "video/x-unknown"
        }
    }

    fun getStreamUrl(channelId: String, port: Int, contentType: String) : Uri {
        val ext = when(contentType.uppercase()){
            "WMV" -> ".wmv"
            "FLV" -> ".flv"
            "MKV" -> ".mkv"
            "WEBM" -> ".webm"
            else -> ""
        }
        return Uri.parse("http://localhost:$port/stream/$channelId$ext")
    }

    /**チャンネル名 (String)*/
    const val EXTRA_NAME = "name"

    /**チャンネル詳細 (String)*/
    const val EXTRA_DESCRIPTION = "description"

    /**チャンネルコメント (String)*/
    const val EXTRA_COMMENT = "comment"

    /**チャンネルコンタクトURL (String)*/
    const val EXTRA_CONTACT_URL = "contact"

    /**
     * ストリーム再生用のインテントを作成する。extraにチャンネル情報を含む。
     * @param channelId チャンネルId
     * @param port 稼働中のピアキャスのポート
     * @param channelInfo チャンネルの情報
     * */
    fun createStreamIntent(channelId: String, port: Int, channelInfo: ChannelInfo? = null) : Intent {
        val u = getStreamUrl(channelId, port, channelInfo)
        return Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(u, getMimeType(channelInfo?.contentType))
            channelInfo?.also { info->
                putExtra(EXTRA_NAME, info.name)
                putExtra(EXTRA_COMMENT, info.comment)
                putExtra(EXTRA_DESCRIPTION,info.desc)
                putExtra(EXTRA_CONTACT_URL, info.url)
            }
        }
    }


}

/**
 * ストリーム再生用のインテントを作成する。extraにチャンネル情報を含む。
 * @param ypChannel チャンネルの情報
 * @param port 稼働中のピアキャスのポート
 * */
fun YpChannel.toStreamIntent(port: Int) : Intent {
    val u = LibPeerCast.getStreamUrl(channelId, port, contentType)
    return Intent(Intent.ACTION_VIEW).apply {
        setDataAndType(u, LibPeerCast.getMimeType(contentType))
        data = u
        putExtra(LibPeerCast.EXTRA_NAME, name)
        putExtra(LibPeerCast.EXTRA_COMMENT, comment)
        putExtra(LibPeerCast.EXTRA_DESCRIPTION, description)
        putExtra(LibPeerCast.EXTRA_CONTACT_URL, contactUrl)
    }
}

val YpChannel.isNilId: Boolean
    get() = channelId == NIL_ID

val YpChannel.isNotNilId: Boolean
    get() = channelId != NIL_ID

/**00000000000000000000000000000000*/
const val NIL_ID = "00000000000000000000000000000000"
