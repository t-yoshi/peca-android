package org.peercast.core

import android.app.Application
import android.util.Log
import com.google.firebase.crashlytics.FirebaseCrashlytics
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin
import org.koin.dsl.module
import org.peercast.core.common.commonModule
import org.peercast.core.common.upnp.UpnpManager
import org.peercast.core.ui.uiModule
import org.peercast.core.upnp.MiniUpnpManager
import timber.log.Timber


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
            modules(listOf(commonModule, uiModule, module {
                single<UpnpManager> { MiniUpnpManager(get()) }
            }))
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
}

