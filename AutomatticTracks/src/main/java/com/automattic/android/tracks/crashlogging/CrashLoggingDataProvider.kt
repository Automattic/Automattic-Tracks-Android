package com.automattic.android.tracks.crashlogging

import java.util.Locale

interface CrashLoggingDataProvider {
    /**
     * Provides [CrashLogging] with the Sentry DSN for this application.
     */
    val sentryDSN: String

    /**
     * Provides [CrashLogging] with information on what type of build this is.
     */
    val buildType: String

    /**
     * Provides [CrashLogging] with the name of this release.
     */
    val releaseName: String

    /**
     * Provides the [CrashLogging] with information about the user's current locale
     */
    val locale: Locale?

    /**
     * Provides [CrashLogging] with information on whether error tracker should log debug data
     */
    val enableCrashLoggingLogs: Boolean

    /**
     * Provides [CrashLogging] with information about exceptions that should be dropped if is the
     * last one on stack trace
     *
     * E.g. "Invoking subscriber failed" exception means that an exception occurred during
     * an EventBus event and it's not particularly useful for debugging.
     *
     */
    fun shouldDropWrappingException(module: String, type: String, value: String): Boolean

    /**
     * Provides [CrashLogging] with information about the current user.
     *
     * @see CrashLoggingUser
     */
    fun userProvider(): CrashLoggingUser?

    /**
     * Provides [CrashLogging] with information on whether the client has enable crash logging
     * data collection
     */
    fun crashLoggingEnabled(): Boolean

    /**
     * Provides [CrashLogging] with information about possible keys for events extra data applied
     * just-before sending it
     */
    fun extraKnownKeys(): List<ExtraKnownKey>

    /**
     * Provides [CrashLogging] with content of extra value to append to an event based on key
     * provided by [extraKnownKeys]
     */
    fun provideExtrasForEvent(
        currentExtras: Map<ExtraKnownKey, String>,
        eventLevel: EventLevel
    ): Map<ExtraKnownKey, String>
}

typealias ExtraKnownKey = String
