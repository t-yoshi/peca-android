package org.peercast.core

import android.app.Application
import android.util.Log
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.core.context.startKoin
import org.koin.dsl.module
import org.peercast.pecaport.pecaPortModule
import timber.log.Timber

/**
 * (c) 2015, T Yoshizawa
 *
 * Dual licensed under the MIT or GPL licenses.
 */
private val appModule = module {
    single { DefaultAppPreferences.from(get()) }
    single { PeerCastController.from(get()) }
    viewModel { PeerCastViewModel(get(), get()) }
}

class PeerCastApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        Timber.plant(ReleaseTree())

        startKoin {
            androidContext(this@PeerCastApplication)
            modules(appModule, pecaPortModule)
        }
    }
}

private class ReleaseTree : Timber.DebugTree() {
    override fun isLoggable(tag: String?, priority: Int): Boolean {
        return priority >= Log.INFO || BuildConfig.DEBUG
    }
}