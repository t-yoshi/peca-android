package org.peercast.core.common

import android.app.Application
import android.os.FileObserver
import androidx.core.content.edit
import androidx.preference.PreferenceManager
import timber.log.Timber
import java.io.File
import java.io.IOException
import java.util.*

internal class DefaultAppPreferences(a: Application) : AppPreferences {

    private val prefs = PreferenceManager.getDefaultSharedPreferences(a)
    private var props = emptyMap<String, Properties>()
    private val iniFile = File(a.filesDir, "peercast.ini")


    private val filesDirObserver = createFileObserver(a.filesDir,
        FileObserver.MOVED_TO or FileObserver.CLOSE_WRITE) { event, f ->
        if (f == iniFile) {
            Timber.d("modified: peercast.ini")
            parsePeerCastIni()
        }
    }

    init {
        parsePeerCastIni()
        filesDirObserver.startWatching()
    }

    override val port: Int
        get() = props["Server"]?.getProperty("serverPort")?.toIntOrNull() ?: 7144

    private fun parsePeerCastIni() {
        try {
            if (iniFile.isFile) {
                props = iniFile.reader().use(::parseIni)
                Timber.d("-> $props")
            }
        } catch (e: IOException) {
            Timber.e(e)
        }
    }

    override val htmlTheme: String
        get() = props["Client"]?.getProperty("preferredTheme") ?: "system"


    private fun createFileObserver(
        f: File, mask: Int,
        onEvent: (event: Int, f: File?) -> Unit,
    ): FileObserver {
        @Suppress("DEPRECATION")
        return object : FileObserver(f.absolutePath, mask) {
            override fun onEvent(event: Int, path: String?) {
                onEvent(event, path?.let { File(f, it) })
            }
        }
    }


    override var isUPnPEnabled: Boolean
        get() = prefs.getBoolean(KEY_UPNP_ENABLED, false)
        set(value) {
            prefs.edit {
                putBoolean(KEY_UPNP_ENABLED, value)
            }
        }

    companion object {
        private const val KEY_UPNP_ENABLED = "key_upnp_enabled"
    }

}
