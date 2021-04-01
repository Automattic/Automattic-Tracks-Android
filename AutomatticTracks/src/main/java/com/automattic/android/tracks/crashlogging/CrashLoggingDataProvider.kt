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
     * Provides [CrashLogging] with information on whether the user has opted out
     * of data collection.
     */
    fun userHasOptedOut(): Boolean

    /**
     * Provides [CrashLogging] with information about the current user.
     *
     * @see TracksUser
     */
    fun currentUser(): TracksUser?

    /**
     * Provides the [CrashLogging] with information about the current application state.
     */
    fun applicationContext(): Map<String, String>

    /**
     * Provides the [CrashLogging] with information about the current user state.
     */
    fun userContext(): Map<String, String>
}
