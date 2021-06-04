package org.peercast.core.tv

/**
 * (c) 2019, T Yoshizawa
 * Dual licensed under the MIT or GPLv3 licenses.
 */
import android.app.Application
import org.peercast.core.lib.app.BasePeerCastViewModel


class PeerCastTvViewModel(
    private val a: Application,
    private val appPrefs: TvPreferences,
) : BasePeerCastViewModel(a) {

    init {
        bindService()
    }

}
