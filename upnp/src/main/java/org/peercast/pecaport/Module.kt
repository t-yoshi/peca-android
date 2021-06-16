package org.peercast.pecaport

import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module
import org.peercast.pecaport.view.PecaPortViewModel

/**
 * (c) 2019, T Yoshizawa
 * Dual licensed under the MIT or GPL licenses.
 */

val upnpModule = module {
    single<PecaPortPreferences> { DefaultPecaPortPreferences(get()) }
    single { NetworkInterfaceManager(get()) }
    viewModel { PecaPortViewModel(get(), get()) }
}
