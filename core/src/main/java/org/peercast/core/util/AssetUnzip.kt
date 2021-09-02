package org.peercast.core.util

import android.content.res.AssetManager
import timber.log.Timber
import java.io.File
import java.util.zip.ZipInputStream

/**
 * @author (c) 2014-2019, T Yoshizawa
 * @licenses Dual licensed under the MIT or GPL licenses.
 */

private fun upzipCopyTo(zis: ZipInputStream, destDir: File) {
    while (true) {
        val ze = zis.nextEntry ?: break
        if (!ze.isDirectory) {
            val out = File(destDir, ze.name)
            //Fixing a Zip Path Traversal Vulnerability
            //https://support.google.com/faqs/answer/9294009?hl=ja
            if (!out.canonicalPath.startsWith(destDir.canonicalPath))
                throw SecurityException("Zip Path Traversal Vulnerability: [$out, $destDir]")
            Timber.i("Unzip: $out")
            val parent = out.parentFile ?: continue
            if (!parent.exists())
                parent.mkdirs()
            out.outputStream().use { os ->
                zis.copyTo(os)
            }
        }
        zis.closeEntry()
    }
}

internal fun AssetManager.unzipFile(fileName: String, destDir: File) {
    ZipInputStream(open(fileName)).use {
        upzipCopyTo(it, destDir)
    }
}
