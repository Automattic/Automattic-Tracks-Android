package com.automattic.android.tracks.fakes

import com.automattic.android.tracks.BuildConfig
import com.automattic.android.tracks.crashlogging.CrashLoggingDataProvider
import com.automattic.android.tracks.crashlogging.CrashLoggingUser
import com.automattic.android.tracks.crashlogging.EventLevel
import com.automattic.android.tracks.crashlogging.ExtraKnownKey
import com.automattic.android.tracks.crashlogging.PerformanceMonitoringConfig
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import java.util.Locale

class FakeDataProvider(
    override val sentryDSN: String = BuildConfig.SENTRY_TEST_PROJECT_DSN,
    override val buildType: String = "testBuildType",
    override val releaseName: String = "testReleaseName",
    override val locale: Locale? = Locale.US,
    override val enableCrashLoggingLogs: Boolean = true,
    var crashLoggingEnabled: Boolean = true,
    var shouldDropException: (String, String, String) -> Boolean = { _: String, _: String, _: String -> false },
    var extraKeys: List<String> = emptyList(),
    var provideExtrasForEvent: (Map<ExtraKnownKey, String>) -> Map<ExtraKnownKey, String> = { currentExtras -> currentExtras },
    initialUser: CrashLoggingUser? = testUser1,
    initialApplicationContext: Map<String, String> = emptyMap()
) : CrashLoggingDataProvider {

    val fakeUserEmitter = MutableStateFlow(initialUser)

    val fakeApplicationContextEmitter = MutableStateFlow(initialApplicationContext)

    override val performanceMonitoringConfig = PerformanceMonitoringConfig.Enabled(1.0)

    override val user: Flow<CrashLoggingUser?> = fakeUserEmitter

    override val applicationContextProvider: Flow<Map<String, String>> = fakeApplicationContextEmitter

    override fun crashLoggingEnabled(): Boolean {
        return crashLoggingEnabled
    }

    override fun extraKnownKeys(): List<String> {
        return extraKeys
    }

    override fun provideExtrasForEvent(
        currentExtras: Map<ExtraKnownKey, String>,
        eventLevel: EventLevel
    ): Map<ExtraKnownKey, String> {
        return provideExtrasForEvent(currentExtras)
    }

    override fun shouldDropWrappingException(module: String, type: String, value: String): Boolean {
        return shouldDropException(module, type, value)
    }
}
