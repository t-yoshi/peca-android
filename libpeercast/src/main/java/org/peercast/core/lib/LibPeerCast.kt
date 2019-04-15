package org.peercast.core.lib

import android.content.Intent
import android.net.Uri
import com.squareup.moshi.Moshi
import org.peercast.core.lib.rpc.ChannelInfo
import org.peercast.core.lib.rpc.EndPointAdapter

/**
 * (c) 2019, T Yoshizawa
 * Dual licensed under the MIT or GPL licenses.
 */
object LibPeerCast {
    internal val MOSHI = Moshi.Builder()
            .add(NullSafeAdapter)
            .add(EndPointAdapter)
            .build()

    /**
     * ストリーム再生用のURL。
     * */
    fun getStreamUrl(channelId: String, port: Int = 7144, channelInfo: ChannelInfo? = null) : Uri {
        val ext = when(channelInfo?.contentType){
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
    fun createStreamIntent(channelId: String, port: Int = 7144, channelInfo: ChannelInfo? = null) : Intent {
        val u = getStreamUrl(channelId, port, channelInfo)
        return Intent(Intent.ACTION_VIEW, u).apply {
            channelInfo?.also { info->
                putExtra(EXTRA_NAME, info.name)
                putExtra(EXTRA_COMMENT, info.comment)
                putExtra(EXTRA_DESCRIPTION,info.desc)
                putExtra(EXTRA_CONTACT_URL, info.url)
            }
        }
    }

    /**
     * hide
     * PeerCastService.notifyChannelで使用
     * */
    fun parseChannelInfo(json: String): ChannelInfo? {
        return MOSHI.adapter(ChannelInfo::class.java).fromJson(json)
    }

}
