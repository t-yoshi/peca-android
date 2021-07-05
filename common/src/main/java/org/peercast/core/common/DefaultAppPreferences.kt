package org.peercast.core.common

import android.app.Application
import androidx.core.content.edit
import androidx.preference.PreferenceManager
import timber.log.Timber
import java.io.File
import java.io.IOException
import java.util.*

internal class DefaultAppPreferences(private val a: Application) : AppPreferences {

    private val prefs = PreferenceManager.getDefaultSharedPreferences(a)
    private val props = Properties()

    override val port: Int
        get() {
            parsePeerCastIni()
            return props.getProperty("serverPort")?.toIntOrNull() ?: 7144
        }

    private fun parsePeerCastIni() {
        val f = File(a.filesDir, "peercast.ini")
        try {
            if (f.isFile)
                props.load(f.reader())
        } catch (e: IOException) {
            Timber.e(e)
        }
    }

    override var startupPort: Int
        get() = prefs.getInt(KEY_STARTUP_PORT, 0)
        set(value) {
            prefs.edit {
                putInt(KEY_STARTUP_PORT, value)
            }
        }

    companion object {
        private const val KEY_STARTUP_PORT = "key_startup_port"
    }

}
