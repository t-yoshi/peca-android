package org.peercast.core

import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.FragmentActivity
import org.koin.android.ext.android.inject
import org.peercast.core.common.AppPreferences

/**
 * Activity経由でフォアグラウンドでのサービス起動を試みる。
 * @see PeerCastController#tryBindService
 * @licensed (c) 2021, T Yoshizawa
 * Dual licensed under the MIT or GPL licenses.
 */
class PeerCastServiceActivity : FragmentActivity() {
    private val appPref by inject<AppPreferences>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setTheme(R.style.Theme_AppCompat_DayNight)
        overridePendingTransition(0, 0)

        val i = Intent(this, PeerCastService::class.java)
        startService(i)

        setResult(RESULT_OK, Intent().also {
            it.putExtra("port", appPref.port)
        })

        finish()
    }
}