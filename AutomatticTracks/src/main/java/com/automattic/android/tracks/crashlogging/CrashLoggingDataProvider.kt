package com.automattic.android.tracks.crashlogging

import com.automattic.android.tracks.TracksUser
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
     * Provides [CrashLogging] with information about the current user.
     *
     * @see TracksUser
     */
    fun userProvider(): TracksUser?

    /**
     * Provides [CrashLogging] with information on whether the user has requested to opt out
     * of crash logging data collection
     */
    fun userHasOptOutProvider(): Boolean
}
