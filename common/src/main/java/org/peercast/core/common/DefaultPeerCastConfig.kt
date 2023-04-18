package org.peercast.core.common

import android.app.Application
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import java.io.File
import java.io.IOException
import java.util.Properties
import kotlin.properties.Delegates

internal class DefaultPeerCastConfig(a: Application) : PeerCastConfig() {

    override var iniMap by Delegates.observable(emptyMap<IniKey, String>()) { _, old, new ->
        if (old != new)
            fireChangeEvent(old.entries, new.entries)
    }
    private val iniFile = File(a.filesDir, "peercast.ini")
    private val observer = object : FileObserverCompat(
        checkNotNull(iniFile.parentFile),
        MOVED_TO or CLOSE_WRITE
    ) {
        override fun onEvent(event: Int, f: File?) {
            if (f == iniFile)
                parseIniFile()
        }
    }

    /**プロパティーの変更を通知する*/
    override val changeEvent = MutableSharedFlow<OnChangeEvent>()

    init {
        if (BuildConfig.DEBUG) {
            SCOPE.launch {
                changeEvent.collect {
                    Timber.d("=> $it")
                }
            }
        }

        parseIniFile()
        observer.startWatching()
    }

    private fun parseIniFile() {
        val m = LinkedHashMap<IniKey, String>()
        var iniKey = DEFAULT_KEY

        val p = object : Properties() {
            override fun put(key: Any, value: Any): Any? {
                //Timber.d("->$key")
                val sec = RE_SECTION.find(key as String)?.groupValues?.get(1)
                when {
                    sec == null -> {
                        if (value != "")
                            m[iniKey.copy(key = key)] = value as String
                    }

                    sec.equals("[End]", true) -> {
                        iniKey = DEFAULT_KEY
                    }

                    else -> {
                        val i = m.entries.count { it.key.section == sec }
                        //Timber.d("-->$sec, $i")
                        iniKey = IniKey(sec, key, i)
                    }
                }
                return null
            }
        }

        try {
            iniFile.reader().use(p::load)
            iniMap = m
        } catch (e: IOException) {
            Timber.e(e)
        }
    }

    private fun fireChangeEvent(
        old: Set<Map.Entry<IniKey, String>>,
        new: Set<Map.Entry<IniKey, String>>
    ) {
        new.filter { it !in old }.forEach {
            SCOPE.launch {
                changeEvent.emit(OnChangeEvent(it.key, it.value))
            }
        }
    }

    companion object {
        private val RE_SECTION = """^\[(\w+)]""".toRegex()
        private val SCOPE = MainScope()

        private val DEFAULT_KEY = IniKey("", "")
    }
}

