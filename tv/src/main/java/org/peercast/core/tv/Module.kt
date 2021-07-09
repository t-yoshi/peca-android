package org.peercast.core.tv

/**
 * @author (c) 2014-2021, T Yoshizawa
 * @licenses Dual licensed under the MIT or GPL licenses.
 */
import android.app.Application
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module
import org.peercast.core.tv.yp.Bookmark
import org.peercast.core.tv.yp.YpChannelsFlow

val tvModule = module {
    single { Bookmark(get<Application>()) }
    single { YpChannelsFlow() }
    viewModel { TvViewModel(get(), get(), get(), get()) }
}