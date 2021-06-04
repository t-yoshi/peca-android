package org.peercast.core.tv

import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val tvModule = module {
    viewModel { PeerCastTvViewModel(get(), get()) }
}