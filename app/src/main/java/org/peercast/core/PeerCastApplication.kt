package org.peercast.core

import android.app.Application
import android.util.Log
import com.crashlytics.android.Crashlytics
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.core.context.startKoin
import org.koin.dsl.module
import org.peercast.core.lib.PeerCastController
import org.peercast.pecaport.pecaPortModule
import timber.log.Timber

private val appModule = module {
    single<AppPreferences> { DefaultAppPreferences(get()) }
    single { PeerCastController.from(get()) }
    viewModel { PeerCastViewModel(get(), get(), get()) }
}

/**
 * @author (c) 2014-2019, T Yoshizawa
 * @licenses Dual licensed under the MIT or GPL licenses.
 */
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

    override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
        super.log(priority, tag, message, t)
        if (priority <= Log.WARN && t != null)
            Crashlytics.logException(t)
    }
}