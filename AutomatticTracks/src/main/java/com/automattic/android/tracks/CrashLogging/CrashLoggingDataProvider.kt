package com.automattic.android.tracks.CrashLogging

import com.automattic.android.tracks.TracksUser
import java.util.*

interface CrashLoggingDataProvider {
    /**
     * Provides [CrashLogging] with the Sentry DSN for this application.
     * @return The Sentry DSN for this application.
     */
    val sentryDSN: String

    /**
     * Provides [CrashLogging] with information on whether the user has opted out
     * of data collection.
     *
     * The implementation of this method should fetch this value from persistent storage, and should
     * never call `CrashLogging.getUserHasOptedOut()`.
     *
     * @return A value indicating whether or not the user has opted out of data collection.
     */
    val userHasOptedOut: Boolean

    /**
     * Provides [CrashLogging] with information on what type of build this is.
     * @return The build type
     */
    val buildType: String

    /**
     * Provides [CrashLogging] with the name of this release.
     * @return The release name
     */
    val releaseName: String

    /**
     * Provides [CrashLogging] with information about the current user.
     * @return A `TracksUser` object containing data about the current user.
     *
     * @see TracksUser
     */
    val currentUser: TracksUser?

    /**
     * Provides the [CrashLogging] with information about the current application state.
     */
    val applicationContext: Map<String, String?>

    /**
     * Provides the [CrashLogging] with information about the current application state.
     */
    val userContext: Map<String, String?>

    /**
     * Provides the [CrashLogging] with information about the user's current locale
     *
     */
    val locale: Locale?
}