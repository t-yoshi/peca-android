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
    private val props = Properties()
    private val iniFile = File(a.filesDir, "peercast.ini")

    private val filesDirObserver = createFileObserver(a.filesDir,
        FileObserver.MOVED_TO or FileObserver.CLOSE_WRITE) { event, f ->
        if (iniFile == f) {
            Timber.d("modified: peercast.ini")
            parsePeerCastIni()
        }
    }

    init {
        parsePeerCastIni()
        filesDirObserver.startWatching()
    }

    override val port: Int
        get() = props.getProperty("serverPort")?.toIntOrNull() ?: 7144

    private fun parsePeerCastIni() {
        try {
            if (iniFile.isFile)
                props.load(iniFile.reader())
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


    private fun createFileObserver(
        f: File,
        mask: Int = FileObserver.ALL_EVENTS,
        onEvent: (event: Int, f: File?) -> Unit,
    ): FileObserver {
        @Suppress("DEPRECATION")
        return object : FileObserver(f.absolutePath, mask) {
            override fun onEvent(event: Int, path: String?) {
                onEvent(event, path?.let { File(f, it) })
            }
        }
    }

    companion object {
        private const val KEY_STARTUP_PORT = "key_startup_port"
    }

}
