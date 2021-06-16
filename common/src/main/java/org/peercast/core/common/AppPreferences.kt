package org.peercast.core.common

/**
 * @author (c) 2014-2019, T Yoshizawa
 * @licenses Dual licensed under the MIT or GPL licenses.
 */
interface AppPreferences {
    /**UPnPを有効にし、サービス開始時にポートを開ける。*/
    var isUPnPEnabled: Boolean

    /**サービス終了時にポートを閉じる。*/
    var isUPnPCloseOnExit: Boolean

    /**動作ポート。peercast.iniのserverPortを上書きする。*/
    var port: Int

    /**シンプルなリスト表示で起動するか、WebViewでYTのHTMLを表示するか*/
    var isSimpleMode: Boolean
}

