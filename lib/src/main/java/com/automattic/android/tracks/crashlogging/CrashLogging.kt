package com.automattic.android.tracks.crashlogging

import android.content.Context
import androidx.annotation.VisibleForTesting
import com.automattic.android.tracks.crashlogging.internal.SentryErrorTrackerWrapper
import io.sentry.SentryEvent
import io.sentry.SentryLevel
import io.sentry.SentryOptions
import io.sentry.protocol.Message
import io.sentry.protocol.User

object CrashLogging {

    private lateinit var dataProvider: CrashLoggingDataProvider
    private lateinit var sentryWrapper: SentryErrorTrackerWrapper

    @JvmStatic
    fun start(
        context: Context,
        dataProvider: CrashLoggingDataProvider,
    ) {
        start(context, dataProvider, SentryErrorTrackerWrapper())
    }

    @VisibleForTesting
    internal fun start(
        context: Context,
        dataProvider: CrashLoggingDataProvider,
        sentryWrapper: SentryErrorTrackerWrapper
    ) {
        this.sentryWrapper = sentryWrapper
        this.dataProvider = dataProvider

        sentryWrapper.initialize(context) { options ->
            options.apply {
                dsn = dataProvider.sentryDSN
                environment = dataProvider.buildType
                release = dataProvider.releaseName
                setDebug(dataProvider.enableCrashLoggingLogs)
                setTag("locale", dataProvider.locale?.language ?: "unknown")
                beforeSend = SentryOptions.BeforeSendCallback { event, _ ->
                    return@BeforeSendCallback if (dataProvider.userHasOptedOut) {
                        null
                    } else {
                        event
                    }
                }
            }
        }
        setNeedsDataRefresh()
    }

    @JvmStatic
    fun setNeedsDataRefresh() {
        applyUserTracking()
        applyApplicationContext()
    }

    private fun applyUserTracking() {
        sentryWrapper.clearBreadcrumbs()

        sentryWrapper.setUser(
            dataProvider.currentUser?.let { tracksUser ->
                User().apply {
                    email = tracksUser.email
                    username = tracksUser.username
                    others = dataProvider.userContext
                        .plus("userID" to tracksUser.userID)
                }
            }
        )
    }

    private fun applyApplicationContext() {
        dataProvider.applicationContext.forEach { entry ->
            sentryWrapper.applyExtra(entry.key, entry.value.orEmpty())
        }
    }

    @JvmStatic
    fun log(throwable: Throwable) {
        sentryWrapper.captureException(throwable)
    }

    @JvmStatic
    fun log(throwable: Throwable, data: Map<String, String?>) {
        val event = SentryEvent().apply {
            message = Message().apply {
                message = throwable.message
            }
            level = SentryLevel.ERROR
            setExtras(data.toMutableMap() as Map<String, String?>)
        }
        sentryWrapper.captureEvent(event)
    }

    @JvmStatic
    fun log(message: String) {
        sentryWrapper.captureMessage(message)
    }
}
