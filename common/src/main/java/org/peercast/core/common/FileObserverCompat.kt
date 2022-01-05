package org.peercast.core.common

import android.os.FileObserver
import java.io.File

@Suppress("DEPRECATION")
abstract class FileObserverCompat(private val file: File, mask: Int) :
    FileObserver(file.absolutePath, mask) {
    final override fun onEvent(event: Int, path: String?) {
        onEvent(event, path?.let { File(file, it) })
    }

    abstract fun onEvent(event: Int, f: File?)
}