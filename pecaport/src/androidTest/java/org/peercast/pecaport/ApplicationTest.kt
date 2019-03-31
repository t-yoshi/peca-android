package org.peercast.pecaport

import android.util.Log
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.work.Configuration
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.impl.utils.SynchronousExecutor
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import timber.log.Timber

/**
 * [Testing Fundamentals](http://d.android.com/tools/testing/testing_android.html)
 */
@RunWith(AndroidJUnit4::class)
class ApplicationTest {

    @Before
    fun setup() {
        val context = InstrumentationRegistry.getInstrumentation().context
        val config = Configuration.Builder()
                // Set log level to Log.DEBUG to make it easier to debug
                .setMinimumLoggingLevel(Log.DEBUG)
                // Use a SynchronousExecutor here to make it easier to write tests
                .setExecutor(SynchronousExecutor())
                .build()

        // Initialize WorkManager for instrumentation tests.
        androidx.work.testing.WorkManagerTestInitHelper.initializeTestWorkManager(context, config)

        Timber.plant(Timber.DebugTree())
    }



    //https://developer.android.com/topic/libraries/architecture/workmanager/how-to/testing
    @Test
    @Throws(Exception::class)
    fun testEchoWorkerNoInput() {
        // Create request
        val request = OneTimeWorkRequestBuilder<PecaPortWorker>()
                .build()

        val workManager = WorkManager.getInstance()
        // Enqueue and wait for result. This also runs the Worker synchronously
        // because we are using a SynchronousExecutor.
        workManager.enqueue(request).result.get()

        Log.d("TAG","xxxxxxxxxxxxxxxxx")
//        // Get WorkInfo
//        val workInfo = workManager.getWorkInfoById(request.id).get()
//        // Assert
//       // Assert.assertThat(workInfo.state, `is`(WorkInfo.State.FAILED))
    }


    @Test
    fun test() {
        val c = InstrumentationRegistry.getInstrumentation().context
        println("context: $c")
        Assert.assertEquals(1 , 0)
    }
}