package com.automattic.android.tracks.CrashLogging;

import com.automattic.android.tracks.TracksUser;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;
import java.util.Map;

public interface CrashLoggingDataProvider {
    /**
     * Provides the Crash Logging Service with the Sentry DSN for this application.
     * @return The Sentry DSN for this application.
     */
    String sentryDSN();

    /**
     * Provides the Crash Logging Service with information on whether the user has opted out
     * of data collection.
     *
     * The implementation of this method should fetch this value from persistent storage, and should
     * never call `CrashLogging.getUserHasOptedOut()`.
     *
     * @return A value indicating whether or not the user has opted out of data collection.
     */
    boolean getUserHasOptedOut();

    /**
     * Allows persisting whether or not the user has opted out of data collection.
     *
     * The implementation of this method should write this value to persistent storage, and should
     * never call `CrashLogging.setUserHasOptedOut()`.
     *
     * @param userHasOptedOut A value indicating whether or not the user has opted out
     *                        of data collection
     */
    void setUserHasOptedOut(boolean userHasOptedOut);

    /**
     * Provides the Crash Logging Service with information on what type of build this is.
     * @return The build type
     */
    @NotNull String buildType();

    /**
     * Provides the Crash Logging Service with the name of this release.
     * @return The release name
     */
    @NotNull String releaseName();

    /**
     * Provides the Crash Logging Service with information about the current user.
     * @return A `TracksUser` object containing data about the current user.
     *
     * @see TracksUser
     */
    @Nullable TracksUser currentUser();

    /**
     * Provides the Crash Logging Service with information about the current system state.
     *
     * @see TracksUser
     */
    @NotNull  Map<String, Object> context();

    /**
     * Provides the Crash Logging Service with information about the user's current locale
     *
     */
    @Nullable Locale locale();
}
