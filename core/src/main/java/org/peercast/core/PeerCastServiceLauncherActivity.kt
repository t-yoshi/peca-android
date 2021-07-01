package org.peercast.core

import android.app.Activity
import android.content.Intent
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import androidx.core.content.ContextCompat

class PeerCastServiceLauncherActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        //window.setBackgroundDrawable(ColorDrawable(0))
        val i = Intent(this, PeerCastService::class.java)
        ContextCompat.startForegroundService(this, i)
        finish()
    }
}