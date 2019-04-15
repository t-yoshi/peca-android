package org.peercast.core.util

import android.content.res.AssetManager
import timber.log.Timber
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
/**
 * @author (c) 2014-2019, T Yoshizawa
 * @licenses Dual licensed under the MIT or GPL licenses.
 */
class AssetUnzip(private val assets: AssetManager) {

    private fun zipAssetCopy(fileName: String, outFile: (ZipEntry) -> File) {
        return ZipInputStream(assets.open(fileName)).use { zis ->
            while (true) {
                val ze = zis.nextEntry ?: break
                if (!ze.isDirectory) {
                    val out = outFile(ze)
                    Timber.i("Unzip: $out")
                    if (!out.parentFile.exists())
                        out.parentFile.mkdirs()
                    out.outputStream().use { os ->
                        zis.copyTo(os)
                    }
                }
                zis.closeEntry()
            }
        }
    }

    fun doExtract(fileName: String, destDir: File) {
        zipAssetCopy(fileName) {
            File(destDir, it.name)
        }
    }
}