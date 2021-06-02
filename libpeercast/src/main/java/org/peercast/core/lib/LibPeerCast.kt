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
    fun getStreamUrl(channelId: String, port: Int = 7144, channelInfo: ChannelInfo? = null) : Uri {
        return getStreamUrl(channelId, port, channelInfo?.contentType ?: "")
    }

    fun getStreamUrl(channelId: String, port: Int = 7144, contentType: String) : Uri {
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
     * ストリーム再生用のインテントを作成する。extraにチャンネル情報を含む。
     * @param ypChannel チャンネルの情報
     * @param port 稼働中のピアキャスのポート
     * */
    fun createStreamIntent(ypChannel: YpChannel, port: Int = 7144) : Intent {
        val u = getStreamUrl(ypChannel.channelId, port, ypChannel.contentType)
        return Intent(Intent.ACTION_VIEW, u).apply {
            ypChannel.also { info->
                putExtra(EXTRA_NAME, info.name)
                putExtra(EXTRA_COMMENT, info.comment)
                putExtra(EXTRA_DESCRIPTION,info.description)
                putExtra(EXTRA_CONTACT_URL, info.contactUrl)
            }
        }
    }
}
