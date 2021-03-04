package com.automattic.android.tracks.CrashLogging

import android.content.Context
import com.automattic.android.tracks.BuildConfig
import io.sentry.Sentry
import io.sentry.SentryEvent
import io.sentry.SentryLevel
import io.sentry.SentryOptions
import io.sentry.android.core.SentryAndroid
import io.sentry.protocol.Message
import io.sentry.protocol.User
import java.util.*

object CrashLogging {

    private lateinit var dataProvider: CrashLoggingDataProvider

    @JvmStatic
    fun start(
            context: Context,
            dataProvider: CrashLoggingDataProvider,
    ) {
        SentryAndroid.init(context) { options ->
            options.apply {
                dsn = dataProvider.sentryDSN
                environment = dataProvider.buildType
                release = dataProvider.releaseName
                setTag("locale", getCurrentLanguage(dataProvider.locale))
                beforeSend = SentryOptions.BeforeSendCallback { event, _ ->
                    return@BeforeSendCallback if (BuildConfig.DEBUG || dataProvider.userHasOptedOut) {
                        null
                    } else {
                        event
                    }
                }
            }
        }
        this.dataProvider = dataProvider
        setNeedsDataRefresh()
    }

    @JvmStatic
    fun crash() {
        throw UnsupportedOperationException("This is a sample crash")
    }

    @JvmStatic
    fun setNeedsDataRefresh() {
        applyUserTracking()
        applySentryContext()
    }

    private fun applyUserTracking() {
        Sentry.clearBreadcrumbs()
        Sentry.setUser(null)
        val tracksUser = dataProvider.currentUser ?: return

        val user = User().apply {
            email = tracksUser.email
            username = tracksUser.username
            others = dataProvider.userContext
                    .plus("userID" to tracksUser.userID)
        }

        Sentry.setUser(user)
    }

    private fun applySentryContext() {
        Sentry.setExtra("context", dataProvider.applicationContext.toString())
    }

    // Locale Helpers
    private fun getCurrentLanguage(locale: Locale?): String {
        return if (locale == null) {
            "unknown"
        } else locale.language
    }

    // Logging Helpers
    @JvmStatic
    fun log(e: Throwable) {
        Sentry.captureException(e)
    }

    @JvmStatic
    fun log(throwable: Throwable, data: Map<String?, String?>?) {
        val event = SentryEvent().apply {
            message = Message().apply {
                message = throwable.message
            }
            level = SentryLevel.ERROR
            setExtras(data)
        }
        Sentry.captureEvent(event)
    }

    @JvmStatic
    fun log(message: String) {
        Sentry.captureMessage(message)
    }
}
