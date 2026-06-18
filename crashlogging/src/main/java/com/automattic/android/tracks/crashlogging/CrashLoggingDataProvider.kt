package com.automattic.android.tracks.crashlogging

import kotlinx.coroutines.flow.Flow
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
    val releaseName: ReleaseName

    /**
     * Provides the [CrashLogging] with information about the user's current locale
     */
    val locale: Locale?

    /**
     * Provides [CrashLogging] with information on whether error tracker should log debug data
     */
    val enableCrashLoggingLogs: Boolean

    /**
     * Provides configuration for Sentry Performance Monitoring
     */
    val performanceMonitoringConfig: PerformanceMonitoringConfig

    /**
     * Provides [CrashLogging] with information about the current user.
     *
     * @see CrashLoggingUser
     */
    val user: Flow<CrashLoggingUser?>

    /**
     * Provides the [CrashLogging] with information about the current application state.
     */
    val applicationContextProvider: Flow<Map<String, String>>

    /**
     * Provides [CrashLogging] with information about the sampling for **error tracking**.
     * By default, sampling is disabled meaning all errors are reported.
     */
    val errorSampling: ErrorSampling
        get() = ErrorSampling.Disabled

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
        eventLevel: EventLevel,
    ): Map<ExtraKnownKey, String>
}

typealias ExtraKnownKey = String

sealed class ReleaseName {
    /**
     * Sets release name attached for every event sent to Sentry. It's indented to use in debug.
     */
    data class SetByApplication(val name: String) : ReleaseName()

    /**
     * Delegates setting the release name to the Tracks library. It's indented to use in release
     * builds. The crash logging framework will single-handledly set the release name based on the
     * build configuration.
     */
    object SetByTracksLibrary : ReleaseName()
}

sealed class PerformanceMonitoringConfig {
    object Disabled : PerformanceMonitoringConfig()

    data class Enabled(
        /**
         * Provides sample rate for performance monitoring. Indicates how often do we measure performance.
         * Has to be between 0 and 1.
         */
        val sampleRate: Double,
        /**
         * Provides sample rate for recording profiles.
         * Indicates how often do we record profile for a performance transaction.
         * Mind that this value is **relative** to [sampleRate] value.
         * Has to be between 0 and 1.
         */
        @Deprecated(
            "Use [profileSessionSampleRate] instead. This configuration option is no longer used.",
            level = DeprecationLevel.HIDDEN
        )
        val profilesSampleRate: Double = 0.0,
        /**
         * Provides sample rate for recording profiles.
         * Indicates how often a profiling session is recorded.
         * This value is evaluated once per session, not per transaction.
         * Has to be between 0 and 1.
         * For example, a value of 0.5 means 50% of sessions will have profiling enabled.
         * Replaces the deprecated [profilesSampleRate] option, which was transaction-scoped.
         */
        val profileSessionSampleRate: Double = 0.0,
    ) : PerformanceMonitoringConfig() {
        init {
            assert(sampleRate in 0.0..1.0)
            assert(profileSessionSampleRate in 0.0..1.0)
        }
    }
}

sealed class ErrorSampling {
    object Disabled : ErrorSampling()

    data class Enabled(
        /**
         * Provides sample rate for error tracking. Indicates how often do we report errors.
         * Has to be between 0 and 1.
         */
        val sampleRate: Double,
    ) : ErrorSampling() {
        init {
            assert(sampleRate in 0.0..1.0)
        }
    }
}
