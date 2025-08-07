package com.example.sampletracksapp

import android.app.Application
import com.automattic.android.tracks.crashlogging.CrashLogging
import com.automattic.android.tracks.crashlogging.CrashLoggingDataProvider
import com.automattic.android.tracks.crashlogging.CrashLoggingProvider
import com.automattic.android.tracks.crashlogging.CrashLoggingUser
import com.automattic.android.tracks.crashlogging.EventLevel
import com.automattic.android.tracks.crashlogging.ExtraKnownKey
import com.automattic.android.tracks.crashlogging.PerformanceMonitoringConfig
import com.automattic.android.tracks.crashlogging.ReleaseName
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.flowOf
import java.util.Locale

class SampleApp : Application() {

    lateinit var crashLogging: CrashLogging

    override fun onCreate() {
        super.onCreate()
        crashLogging = CrashLoggingProvider.createInstance(
            this,
            object : CrashLoggingDataProvider {
                override val sentryDSN = BuildConfig.SENTRY_TEST_PROJECT_DSN
                override val buildType = BuildConfig.BUILD_TYPE
                override val releaseName = ReleaseName.SetByApplication("test")
                override val locale = Locale.US
                override val enableCrashLoggingLogs = true
                override val performanceMonitoringConfig =
                    PerformanceMonitoringConfig.Enabled(sampleRate = 1.0, profilesSampleRate = 1.0)
                override val user = flowOf(
                    CrashLoggingUser(
                        userID = "test user id",
                        email = "test@user.com",
                        username = "test username",
                    ),
                )
                override val applicationContextProvider =
                    flowOf(mapOf("extra" to "application context"))

                override fun shouldDropWrappingException(
                    module: String,
                    type: String,
                    value: String,
                ): Boolean {
                    return false
                }

                override fun crashLoggingEnabled(): Boolean {
                    return true
                }

                override fun extraKnownKeys(): List<String> {
                    return emptyList()
                }

                override fun provideExtrasForEvent(
                    currentExtras: Map<ExtraKnownKey, String>,
                    eventLevel: EventLevel,
                ): Map<ExtraKnownKey, String> {
                    return mapOf("extra" to "event value")
                }
            },
            appScope = GlobalScope,
        )
        crashLogging.initialize()
    }
}
