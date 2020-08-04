package com.automattic.android.tracks.CrashLogging;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import com.automattic.android.tracks.BuildConfig;
import com.automattic.android.tracks.TracksUser;

import java.util.Locale;
import java.util.Map;

import io.sentry.Sentry;
import io.sentry.SentryClient;
import io.sentry.event.User;
import io.sentry.android.AndroidSentryClientFactory;
import io.sentry.connection.EventSendCallback;
import io.sentry.event.Event;
import io.sentry.event.EventBuilder;
import io.sentry.event.UserBuilder;
import io.sentry.event.helper.ShouldSendEventCallback;
import io.sentry.event.interfaces.ExceptionInterface;

public class CrashLogging {

    private static CrashLoggingDataProvider mDataProvider;
    private static SentryClient sentry;

    private static Boolean isStarted = false;

    public static void start(Context context, CrashLoggingDataProvider dataProvider) {

        synchronized (isStarted) {
            if (isStarted) {
                return;
            }

            isStarted = true;
        }

        sentry = Sentry.init(
                dataProvider.sentryDSN(),
                new AndroidSentryClientFactory(context)
        );

        // Apply Sentry Tags
        sentry.setEnvironment(dataProvider.buildType());
        sentry.setRelease(dataProvider.releaseName());
        sentry.addTag("locale", getCurrentLanguage(dataProvider.locale()));

        sentry.addShouldSendEventCallback(new ShouldSendEventCallback() {
            @Override
            public boolean shouldSend(Event event) {

                if (BuildConfig.DEBUG) {
                    return false;
                }

                return !mDataProvider.getUserHasOptedOut();
            }
        });

        sentry.addEventSendCallback(new EventSendCallback() {
            @Override
            public void onFailure(Event event, Exception exception) {
                Log.println(Log.WARN, "SENTRY", exception.getLocalizedMessage());
            }

            @Override
            public void onSuccess(Event event) {
                Log.println(Log.INFO, "SENTRY", event.getMessage());
            }
        });

        mDataProvider = dataProvider;
        setNeedsDataRefresh();
    }

    public static void crash() {
        throw new UnsupportedOperationException("This is a sample crash");
    }

    public static void setNeedsDataRefresh() {
        applyUserTracking();
        applySentryContext();
    }

    private static void applyUserTracking() {
        sentry.getContext().clearUser();

        TracksUser tracksUser = mDataProvider.currentUser();

        if (tracksUser == null) {
            return;
        }

        sentry.getContext().setUser(new UserBuilder()
                .setEmail(tracksUser.getEmail())
                .setUsername(tracksUser.getUsername())
                .withData("userID", tracksUser.getUserID())
                .setData(mDataProvider.userContext())
                .build());
    }

    private static void applySentryContext() {
        sentry.setExtra(mDataProvider.applicationContext());
    }

    // Locale Helpers
    private static String getCurrentLanguage(@Nullable Locale locale) {
        if (locale == null) {
            return "unknown";
        }

        return locale.getLanguage();
    }

    // Logging Helpers
    public static void log(Throwable e) {
        sentry.sendException(e);
    }

    public static void log(@NonNull Throwable e, @Nullable Map<String, String> data) {

        EventBuilder eventBuilder = new EventBuilder()
                .withMessage(e.getMessage())
                .withLevel(Event.Level.ERROR)
                .withSentryInterface(new ExceptionInterface(e));

        if (data != null) {
            for (Map.Entry<String,String> entry : data.entrySet()) {
                eventBuilder.withExtra(entry.getKey(), entry.getValue());
            }
        }

        sentry.sendEvent(eventBuilder.getEvent());
    }

    public static void log(String message) {
        sentry.sendMessage(message);
    }
}
