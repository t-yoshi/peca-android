package org.peercast.core.preferences

import android.app.Application
import androidx.core.content.edit
import androidx.preference.PreferenceManager
import timber.log.Timber
import java.io.File
import java.io.IOException
import java.util.*

internal class DefaultAppPreferences(private val a: Application) : AppPreferences {

    private val prefs = PreferenceManager.getDefaultSharedPreferences(a)

    override var isUPnPEnabled: Boolean
        get() = prefs.getBoolean(KEY_UPNP_ENABLED, false)
        set(value) {
            prefs.edit {
                putBoolean(KEY_UPNP_ENABLED, value)
            }
        }

    override var isUPnPCloseOnExit: Boolean
        get() = prefs.getBoolean(KEY_UPNP_CLOSE_ON_EXIT, false)
        set(value) {
            prefs.edit {
                putBoolean(KEY_UPNP_CLOSE_ON_EXIT, value)
            }
        }


    override var port: Int
        get() = prefs.getInt(KEY_PORT, -1).let {
            var p = it
            if (p == -1)
                p = parsePeerCastIni().getProperty("serverPort")?.toIntOrNull() ?: 7144

            return when {
                p in 1025..65532 -> p
                else -> 7144
            }
        }
        set(value) = prefs.edit {
            putInt(KEY_PORT, value)
        }

    private fun parsePeerCastIni(): Properties {
        return Properties().also {
            try {
                val f = File(a.filesDir, "peercast.ini")
                if (f.isFile)
                    it.load(f.reader())
            } catch (e: IOException) {
                Timber.e(e)
            }
        }
    }

    override var isSimpleMode: Boolean
        get() = prefs.getBoolean(KEY_SIMPLE_MODE, false)
        set(value) {
            prefs.edit {
                putBoolean(KEY_SIMPLE_MODE, value)
            }
        }

    companion object {
        const val KEY_UPNP_ENABLED = "key_upnp_enabled"
        const val KEY_UPNP_CLOSE_ON_EXIT = "key_upnp_close_on_exit"
        const val KEY_PORT = "key_port"
        const val KEY_SIMPLE_MODE = "key_simple_mode"
    }

}