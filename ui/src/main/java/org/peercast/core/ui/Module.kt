package org.peercast.core.ui

import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val uiModule = module {
    viewModel { UiViewModel(get(), get()) }
}