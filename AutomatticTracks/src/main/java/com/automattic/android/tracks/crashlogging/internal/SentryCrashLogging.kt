package com.automattic.android.tracks.crashlogging.internal

import android.app.Application
import com.automattic.android.tracks.crashlogging.CrashLogging
import com.automattic.android.tracks.crashlogging.CrashLoggingDataProvider
import com.automattic.android.tracks.crashlogging.CrashLoggingUser
import com.automattic.android.tracks.crashlogging.ExtraKnownKey
import com.automattic.android.tracks.crashlogging.PerformanceMonitoringConfig.Disabled
import com.automattic.android.tracks.crashlogging.PerformanceMonitoringConfig.Enabled
import com.automattic.android.tracks.crashlogging.eventLevel
import io.sentry.Breadcrumb
import io.sentry.SentryEvent
import io.sentry.SentryLevel
import io.sentry.SentryOptions
import io.sentry.android.fragment.FragmentLifecycleIntegration
import io.sentry.protocol.Message
import io.sentry.protocol.User
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

internal class SentryCrashLogging constructor(
    application: Application,
    private val dataProvider: CrashLoggingDataProvider,
    private val sentryWrapper: SentryErrorTrackerWrapper,
    applicationScope: CoroutineScope
) : CrashLogging {

    init {
        sentryWrapper.initialize(application) { options ->

            val (tracesSampleRate, profilesSampleRate) = dataProvider.performanceMonitoringConfig.let {
                when (it) {
                    Disabled -> null to null
                    is Enabled -> it.sampleRate to it.profilesSampleRate
                }
            }

            options.apply {
                dsn = dataProvider.sentryDSN
                environment = dataProvider.buildType
                release = dataProvider.releaseName
                this.tracesSampleRate = tracesSampleRate
                this.profilesSampleRate = profilesSampleRate
                isDebug = dataProvider.enableCrashLoggingLogs
                setTag("locale", dataProvider.locale?.language ?: "unknown")
                setBeforeBreadcrumb { breadcrumb, _ ->
                    if (breadcrumb.type == "http") null else breadcrumb
                }
                addIntegration(
                    FragmentLifecycleIntegration(
                        application,
                        enableFragmentLifecycleBreadcrumbs = false,
                        enableAutoFragmentLifecycleTracing = true
                    )
                )

                isEnableAutoSessionTracking = true
                beforeSend = SentryOptions.BeforeSendCallback { event, _ ->
                    if (!dataProvider.crashLoggingEnabled()) return@BeforeSendCallback null

                    dropExceptionIfRequired(event)
                    appendExtra(event)
                    event
                }
            }
        }

        applicationScope.launch {
            dataProvider.user.collect {
                sentryWrapper.setUser(it?.toSentryUser())
            }
        }
        applicationScope.launch {
            dataProvider.applicationContextProvider.collect {
                sentryWrapper.setTags(it)
            }
        }
    }

    private fun appendExtra(event: SentryEvent) {
        event.setExtras(
            dataProvider.provideExtrasForEvent(
                currentExtras = mergeKnownKeysWithValues(event),
                eventLevel = event.eventLevel
            )
        )
    }

    private fun mergeKnownKeysWithValues(event: SentryEvent): Map<ExtraKnownKey, String> =
        dataProvider.extraKnownKeys()
            .associateWith { knownKey -> event.getExtra(knownKey) }
            .filterValues { it != null }
            .mapValues(Any::toString)

    private fun dropExceptionIfRequired(event: SentryEvent) {
        event.exceptions?.lastOrNull()?.let { lastException ->
            if (dataProvider.shouldDropWrappingException(
                    lastException.module.orEmpty(),
                    lastException.type.orEmpty(),
                    lastException.value.orEmpty()
                )
            ) {
                event.exceptions?.remove(lastException)
            }
        }
    }

    override fun recordEvent(message: String, category: String?) {
        val breadcrumb = Breadcrumb().apply {
            this.category = category
            this.type = "default"
            this.message = message
            this.level = SentryLevel.INFO
        }
        sentryWrapper.addBreadcrumb(breadcrumb)
    }

    override fun recordException(exception: Throwable, category: String?) {
        val breadcrumb = Breadcrumb().apply {
            this.category = category
            this.type = "error"
            this.message = exception.toString()
            this.level = SentryLevel.ERROR
        }
        sentryWrapper.addBreadcrumb(breadcrumb)
    }

    override fun sendReport(exception: Throwable?, tags: Map<String, String>, message: String?) {
        val event = SentryEvent(exception).apply {
            this.message = Message().apply { this.message = message }
            this.level = if (exception != null) SentryLevel.ERROR else SentryLevel.INFO
            this.appendTags(tags)
        }
        sentryWrapper.captureEvent(event)
    }

    private fun SentryEvent.appendTags(tags: Map<String, String>) {
        for ((key, value) in tags) {
            this.setTag(key, value)
        }
    }

    private fun CrashLoggingUser.toSentryUser(): User = User().let { sentryUser ->
        sentryUser.email = email
        sentryUser.username = username
        sentryUser.id = userID
        sentryUser
    }
}
