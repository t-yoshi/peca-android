package org.peercast.core.common

/**
 * @author (c) 2014-2019, T Yoshizawa
 * @licenses Dual licensed under the MIT or GPL licenses.
 */
interface AppPreferences {
    /**UPnPを有効にし、サービス開始時にポートを開ける。*/
    @Deprecated("Obsoleted v4.0")
    var isUPnPEnabled: Boolean

    /**サービス終了時にポートを閉じる。*/
    @Deprecated("Obsoleted v4.0")
    var isUPnPCloseOnExit: Boolean

    /**動作ポート。peercast.iniの設定より優先する。*/
    var port: Int

    /**シンプルなリスト表示で起動するか、WebViewでYTのHTMLを表示するか*/
    @Deprecated("Obsoleted v4.0")
    var isSimpleMode: Boolean
}

