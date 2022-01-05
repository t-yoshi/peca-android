package org.peercast.core.common

import kotlinx.coroutines.flow.SharedFlow

abstract class PeerCastConfig {

    data class IniKey(
        val section: String,
        val key: String,
        val sectionIndex: Int = 0,
    )

    data class OnChangeEvent(
        val key: IniKey,
        val value: String
    )

    protected abstract val iniMap : Map<IniKey, String>

    abstract val changeEvent: SharedFlow<OnChangeEvent>

    inline operator fun <reified T> get(key: IniKey): T? {
        val v = getString(key)
        return when (T::class) {
            String::class -> v as T?
            Int::class -> v?.toIntOrNull() as T?
            Long::class -> v?.toLongOrNull() as T?
            Boolean::class -> v?.equals("Yes", true) as T?
            else -> throw NotImplementedError(T::class.java.name)
        }
    }

    inline operator fun <reified T> get(section: String, sectionIndex: Int, key: String): T? = get(
        IniKey(section, key, sectionIndex)
    )

    inline operator fun <reified T> get(section: String, key: String): T? = get(
        IniKey(section, key)
    )

    fun getString(key: IniKey) = iniMap[key]

    fun getSection(section: String) = iniMap.filter {
        it.key.section == section
    }.map {
        it.key
    }

    val sections get() = iniMap.entries.map { it.key.section }.distinct()

    operator fun contains(key: IniKey) = key in iniMap

    val port get() = get(KEY_PORT) ?: 7144
    val preferredTheme get() = get(KEY_THEME) ?: "system"

    companion object {
        val KEY_PORT = IniKey("Server", "serverPort")
        val KEY_THEME = IniKey("Client", "preferredTheme")
    }
}



