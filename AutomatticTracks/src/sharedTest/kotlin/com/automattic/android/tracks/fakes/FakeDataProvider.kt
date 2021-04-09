package com.automattic.android.tracks.fakes

import com.automattic.android.tracks.BuildConfig
import com.automattic.android.tracks.crashlogging.CrashLoggingDataProvider
import com.automattic.android.tracks.crashlogging.CrashLoggingUser
import java.util.Locale

class FakeDataProvider(
    override val sentryDSN: String = BuildConfig.SENTRY_TEST_PROJECT_DSN,
    override val buildType: String = "testBuildType",
    override val releaseName: String = "testReleaseName",
    override val locale: Locale? = Locale.US,
    override val enableCrashLoggingLogs: Boolean = true,
    var user: CrashLoggingUser? = testUser1,
    var crashLoggingEnabled: Boolean = true,
    var shouldDropException: (String, String, String) -> Boolean = { _: String, _: String, _: String -> false },
) : CrashLoggingDataProvider {

    override fun userProvider(): CrashLoggingUser? {
        return user
    }

    override fun crashLoggingEnabled(): Boolean {
        return crashLoggingEnabled
    }

    override fun shouldDropWrappingException(module: String, type: String, value: String): Boolean {
        return shouldDropException(module, type, value)
    }
}
