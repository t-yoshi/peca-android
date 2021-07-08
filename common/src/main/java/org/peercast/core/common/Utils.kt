package org.peercast.core.common

/**
 * @author (c) 2014-2021, T Yoshizawa
 * @licenses Dual licensed under the MIT or GPL licenses.
 */
import android.app.UiModeManager
import android.content.Context
import android.content.res.Configuration
import java.io.Reader
import java.util.*
import kotlin.collections.HashMap

/**@see https://qiita.com/hirano/items/10da4d1b9c86218dd50a*/
val Context.isFireTv: Boolean
    get() = packageManager.hasSystemFeature("amazon.hardware.fire_tv")

val Context.isTvMode: Boolean
    get() {
        val uiModeManager = getSystemService(Context.UI_MODE_SERVICE) as UiModeManager
        return uiModeManager.currentModeType == Configuration.UI_MODE_TYPE_TELEVISION
    }


private val RE_SECTION = """^\[(\w+)]""".toRegex()

internal fun parseIni(r: Reader): Map<String, Properties> {
    val res = HashMap<String, Properties>()
    var section = res.getOrPut("", ::Properties)

    object : Properties() {
        override fun put(key: Any, value: Any): Any? {
            //Timber.d("->$key")
            val m = RE_SECTION.find("$key")
            return if (m != null) {
                section = res.getOrPut(m.groupValues[1], ::Properties)
            } else {
                section.put(key, value)
            }
        }
    }.load(r)
    res.remove("[End]")
    //Timber.d(":->$res")
    return res
}