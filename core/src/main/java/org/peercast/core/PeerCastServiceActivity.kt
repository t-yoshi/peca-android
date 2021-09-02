package org.peercast.core

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import org.peercast.core.ui.PeerCastActivity

/**
 * 一部機種で独自実装されたバッテリーセイバーの制約をかいくぐるために、
 * Activity経由でフォアグラウンドでのサービス起動を試みる。
 * @see PeerCastController#tryBindService
 * @licensed (c) 2021, T Yoshizawa
 * Dual licensed under the MIT or GPL licenses.
 */
class PeerCastServiceActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        startActivity(
            Intent(Intent.ACTION_MAIN)
                .setClass(this, PeerCastActivity::class.java)
                .addCategory(Intent.CATEGORY_LAUNCHER)
                .putExtra(PeerCastActivity.EX_IS_INVISIBLE, true)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
        )
        overridePendingTransition(0, 0)
    }
}