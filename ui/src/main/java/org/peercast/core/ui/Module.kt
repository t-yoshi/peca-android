package org.peercast.core.ui

import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module
import org.peercast.core.ui.tv.TvViewModel
import org.peercast.core.ui.tv.yp.YpChannelsFlow

val uiModule = module {
    viewModel { UiViewModel(get()) }

    //tv
    single { YpChannelsFlow() }
    viewModel { TvViewModel(get(), get(), get()) }
}