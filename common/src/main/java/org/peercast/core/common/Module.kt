package org.peercast.core.common

import org.koin.dsl.module

val commonModule = module {
    single<AppPreferences> { DefaultAppPreferences(get()) }
    single<PeerCastConfig> { DefaultPeerCastConfig(get()) }
}
