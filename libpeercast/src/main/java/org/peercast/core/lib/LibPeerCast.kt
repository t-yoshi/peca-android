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
    fun getStreamUrl(channelId: String, port: Int, channelInfo: ChannelInfo? = null): Uri {
        return getStreamUrl(channelId, port, channelInfo?.contentType ?: "")
    }

    private fun getStreamMimeType(contentType: String?): String {
        return when (contentType?.uppercase()) {
            "WMV" -> "video/wmv"
            "FLV" -> "video/flv"
            "MKV" -> "video/mkv"
            "WEBM" -> "video/webm"
            else -> "video/x-unknown"
        }
    }

    private fun getStreamSuffix(contentType: String?): String {
        return when (contentType?.uppercase()) {
            "WMV" -> ".wmv"
            "FLV" -> ".flv"
            "MKV" -> ".mkv"
            "WEBM" -> ".webm"
            else -> ""
        }
    }

    private fun getPlayListMimeType(contentType: String?): String {
        return when (contentType?.uppercase()) {
            "WMV" -> "video/x-ms-asf"
            else -> "video/x-mpegurl"
        }
    }

    private fun getPlayListSuffix(contentType: String?): String {
        return when (contentType?.uppercase()) {
            "WMV" -> ".asx"
            else -> ".m3u"
        }
    }

    fun getStreamUrl(channelId: String, port: Int, contentType: String?): Uri {
        val ext = getPlayListSuffix(contentType)
        //キャッシュを防ぐために?v=(time)をつける
        val now = System.currentTimeMillis() / 1000
        return Uri.parse("http://127.0.0.1:$port/stream/$channelId$ext?v=$now")
    }

    fun getPlayListUrl(channelId: String, port: Int, contentType: String?): Uri {
        val ext = getPlayListSuffix(contentType)
        //キャッシュを防ぐために?v=(time)をつける
        val now = System.currentTimeMillis() / 1000
        return Uri.parse("http://127.0.0.1:$port/pls/$channelId$ext?v=$now")
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
    fun createStreamIntent(channelId: String, port: Int, channelInfo: ChannelInfo? = null): Intent {
        val u = getStreamUrl(channelId, port, channelInfo)
        return Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(u, getStreamMimeType(channelInfo?.contentType))
            channelInfo?.also { info ->
                putExtra(EXTRA_NAME, info.name)
                putExtra(EXTRA_COMMENT, info.comment)
                putExtra(EXTRA_DESCRIPTION, info.desc)
                putExtra(EXTRA_CONTACT_URL, info.url)
            }
        }
    }

    /**
     * ストリーム再生用のインテントを作成する。extraにチャンネル情報を含む。
     * @param ypChannel チャンネルの情報
     * @param port 稼働中のピアキャスのポート
     * */
    fun YpChannel.toStreamIntent(port: Int): Intent {
        val u = getStreamUrl(channelId, port, contentType)
        return Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(u, getStreamMimeType(contentType))
            putExtra(EXTRA_NAME, name)
            putExtra(EXTRA_COMMENT, comment)
            putExtra(EXTRA_DESCRIPTION, description)
            putExtra(EXTRA_CONTACT_URL, contactUrl)
        }
    }

    /**
     * ストリーム再生用のプレイリストのインテントを作成する。extraにチャンネル情報を含む。
     * @param ypChannel チャンネルの情報
     * @param port 稼働中のピアキャスのポート
     * */
    fun YpChannel.toPlayListIntent(port: Int): Intent {
        val u = getPlayListUrl(channelId, port, contentType)
        return Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(u, getPlayListMimeType(contentType))
            putExtra(EXTRA_NAME, name)
            putExtra(EXTRA_COMMENT, comment)
            putExtra(EXTRA_DESCRIPTION, description)
            putExtra(EXTRA_CONTACT_URL, contactUrl)
        }
    }

}

