package org.peercast.pecaport

import android.content.Context
import androidx.work.*
import ch.qos.logback.classic.Level
import ch.qos.logback.classic.LoggerContext
import ch.qos.logback.classic.android.LogcatAppender
import ch.qos.logback.classic.encoder.PatternLayoutEncoder
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.FileAppender
import ch.qos.logback.core.util.StatusPrinter
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module
import org.peercast.pecaport.view.PecaPortViewModel
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.slf4j.bridge.SLF4JBridgeHandler
import java.io.File


/**
 * (c) 2019, T Yoshizawa
 * Dual licensed under the MIT or GPL licenses.
 */

object PecaPort {

    fun openPort(c: Context, port: Int){
        WorkManager.getInstance(c).enqueue(
                createOneTimeWorkRequest(port, false)
        )
    }

    fun closePort(c: Context, port: Int){
        WorkManager.getInstance(c).enqueue(
                createOneTimeWorkRequest(port, true)
        )
    }

    private fun createOneTimeWorkRequest(port: Int, isDelete: Boolean ): OneTimeWorkRequest {
        return OneTimeWorkRequest.Builder(PecaPortWorker::class.java)
                .setInputData(
                        Data.Builder()
                                .putInt(PecaPortWorker.PARAM_PORT, port)
                                .putBoolean(PecaPortWorker.PARAM_DELETE, isDelete)
                                .build())
                .setConstraints(Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .build())
                .build()
    }

    fun getLogFile(c: Context) : File{
        return File(c.filesDir, "log/pecaport.log")
    }

    fun installLogger(c: Context){
        //cling: jul
        //jetty: slf4j
        // jul -> slf -> logback-android
        SLF4JBridgeHandler.removeHandlersForRootLogger()
        SLF4JBridgeHandler.install()

        val logFile = getLogFile(c)

        if (logFile.length() > 32 * 1024)
            logFile.delete()

        configureLogback(logFile)
    }

    private fun configureLogback(logFile: File) {
        val context = LoggerFactory.getILoggerFactory() as LoggerContext
        context.stop()

        val encoder = PatternLayoutEncoder().also {
            it.context = context
            it.pattern = "%d{HH:mm:ss.SSS} [%thread] %-5level %logger - %msg%n"
            it.start()
        }

        val jetty = LoggerFactory.getLogger("org.eclipse.jetty") as ch.qos.logback.classic.Logger
        jetty.level = Level.INFO

        val root = LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME) as ch.qos.logback.classic.Logger
        root.level = Level.DEBUG

        val fileAppender = FileAppender<ILoggingEvent>().also {
            it.context = context
            it.file = logFile.absolutePath
            it.encoder = encoder
            it.start()
        }

        val logcatAppender = LogcatAppender().also {
            it.context = context
            it.encoder = encoder
            it.start()
        }

        root.addAppender(fileAppender)
        root.addAppender(logcatAppender)

        StatusPrinter.print(context)
    }
}