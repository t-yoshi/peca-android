package org.peercast.core

import android.app.Application
import android.util.Log
import com.google.firebase.crashlytics.FirebaseCrashlytics
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.core.context.startKoin
import org.koin.dsl.module
import org.peercast.pecaport.pecaPortModule
import timber.log.Timber

private val appModule = module {
    single<AppPreferences> { DefaultAppPreferences(get()) }
    viewModel { PeerCastViewModel(get(), get()) }
}

/**
 * @author (c) 2014-2019, T Yoshizawa
 * @licenses Dual licensed under the MIT or GPL licenses.
 */
@Suppress("unused")
class PeerCastApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        Timber.plant(ReleaseTree())

        startKoin {
            androidContext(this@PeerCastApplication)
            modules(listOf(appModule, pecaPortModule))
        }
    }
}

private class ReleaseTree : Timber.DebugTree() {
    override fun isLoggable(tag: String?, priority: Int): Boolean {
        return priority >= Log.INFO || BuildConfig.DEBUG
    }

    override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
        super.log(priority, tag, message, t)
        if (t != null)
            FirebaseCrashlytics.getInstance().recordException(t)
    }
}