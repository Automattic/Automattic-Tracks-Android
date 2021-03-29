package com.automattic.android.tracks.crashlogging

import android.content.Context
import com.automattic.android.tracks.BuildConfig
import com.automattic.android.tracks.crashlogging.internal.SentryErrorTrackerProxy
import com.automattic.android.tracks.crashlogging.internal.SentryErrorTrackerProxyImpl
import io.sentry.SentryEvent
import io.sentry.SentryLevel
import io.sentry.SentryOptions
import io.sentry.protocol.Message
import io.sentry.protocol.User

object CrashLogging {

    private lateinit var dataProvider: CrashLoggingDataProvider
    private lateinit var sentryProxy: SentryErrorTrackerProxy

    @JvmStatic
    fun start(
        context: Context,
        dataProvider: CrashLoggingDataProvider,
    ) {
        start(context, dataProvider, SentryErrorTrackerProxyImpl())
    }

    internal fun start(
        context: Context,
        dataProvider: CrashLoggingDataProvider,
        sentryProxy: SentryErrorTrackerProxy
    ) {
        this.sentryProxy = sentryProxy
        this.dataProvider = dataProvider

        sentryProxy.initialize(context) { options ->
            options.apply {
                dsn = dataProvider.sentryDSN
                environment = dataProvider.buildType
                release = dataProvider.releaseName
                setDebug(BuildConfig.DEBUG)
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
        sentryProxy.clearBreadcrumbs()

        sentryProxy.setUser(
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
            sentryProxy.applyExtra(entry.key, entry.value.orEmpty())
        }
    }

    @JvmStatic
    fun log(throwable: Throwable) {
        sentryProxy.captureException(throwable)
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
        sentryProxy.captureEvent(event)
    }

    @JvmStatic
    fun log(message: String) {
        sentryProxy.captureMessage(message)
    }
}
