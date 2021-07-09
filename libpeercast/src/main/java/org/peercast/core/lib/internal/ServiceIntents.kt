package org.peercast.core.lib.internal

import android.content.ComponentName
import android.content.Intent

object ServiceIntents {
    const val PKG_PEERCAST = "org.peercast.core"
    const val CLASS_NAME_PEERCAST_SERVICE = "$PKG_PEERCAST.PeerCastService"
    const val CLASS_NAME_PEERCAST_SERVICE_ACTIVITY =
        "$PKG_PEERCAST.PeerCastServiceActivity"

    /**"org.peercast.core.PeerCastService" 旧バージョンからの接続*/
    const val ACT_PEERCAST_SERVICE = "org.peercast.core.PeerCastService"

    /**"org.peercast.core.PeerCastService4"*/
    const val ACT_PEERCAST_SERVICE4 = "org.peercast.core.PeerCastService4"

    /**バージョン4*/
    val SERVICE4_INTENT = Intent(ACT_PEERCAST_SERVICE4).also {
        it.component = ComponentName(
            PKG_PEERCAST, CLASS_NAME_PEERCAST_SERVICE
        )
        it.`package` = PKG_PEERCAST
    }

    /**start PeerCastServiceActivity*/
    val SERVICE_LAUNCHER_INTENT = Intent(Intent.ACTION_MAIN).also {
        it.component = ComponentName(
            PKG_PEERCAST, CLASS_NAME_PEERCAST_SERVICE_ACTIVITY
        )
        it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
}