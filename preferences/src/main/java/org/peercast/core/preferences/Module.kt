package org.peercast.core.preferences

import org.koin.dsl.module

val settingsModule = module {
    single<AppPreferences> { DefaultAppPreferences(get()) }
}
