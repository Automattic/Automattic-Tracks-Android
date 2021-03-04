package com.automattic.android.tracks.CrashLogging;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.automattic.android.tracks.TracksUser;

import java.util.Locale;
import java.util.Map;

public interface CrashLoggingDataProvider {
    /**
     * Provides {@link CrashLogging} with the Sentry DSN for this application.
     * @return The Sentry DSN for this application.
     */
    String sentryDSN();

    /**
     * Provides {@link CrashLogging} with information on whether the user has opted out
     * of data collection.
     *
     * The implementation of this method should fetch this value from persistent storage, and should
     * never call `CrashLogging.getUserHasOptedOut()`.
     *
     * @return A value indicating whether or not the user has opted out of data collection.
     */
    boolean getUserHasOptedOut();

    /**
     * Provides {@link CrashLogging} with information on what type of build this is.
     * @return The build type
     */
    @NonNull String buildType();

    /**
     * Provides {@link CrashLogging} with the name of this release.
     * @return The release name
     */
    @NonNull String releaseName();

    /**
     * Provides {@link CrashLogging} with information about the current user.
     * @return A `TracksUser` object containing data about the current user.
     *
     * @see TracksUser
     */
    @Nullable TracksUser currentUser();

    /**
     * Provides the {@link CrashLogging} with information about the current application state.
     */
    @NonNull Map<String, Object> applicationContext();

    /**
     * Provides the {@link CrashLogging} with information about the current application state.
     */
    @NonNull Map<String, Object> userContext();

    /**
     * Provides the {@link CrashLogging} with information about the user's current locale
     *
     */
    @Nullable Locale locale();
}
