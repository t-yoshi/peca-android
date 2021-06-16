package org.peercast.core.ui.util

import android.content.Context
import timber.log.Timber
import java.io.File
import java.util.zip.ZipInputStream

/**
 * @author (c) 2014-2019, T Yoshizawa
 * @licenses Dual licensed under the MIT or GPL licenses.
 */
object AssetUnzip {

    private fun zipAssetCopy(zis: ZipInputStream, destDir: File) {
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

    fun doExtract(c: Context, fileName: String, destDir: File) {
        ZipInputStream(c.assets.open(fileName)).use { zis ->
            zipAssetCopy(zis, destDir)
        }
    }
}