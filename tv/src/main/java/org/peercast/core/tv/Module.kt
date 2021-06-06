package org.peercast.core.tv

import org.koin.dsl.module

val tvModule = module {
    single { TvViewModel(get(), get()) }
}