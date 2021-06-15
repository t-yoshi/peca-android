package org.peercast.core.tv
/**
 * @author (c) 2014-2021, T Yoshizawa
 * @licenses Dual licensed under the MIT or GPL licenses.
 */
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val tvModule = module {
    viewModel { TvViewModel(get(), get()) }
}