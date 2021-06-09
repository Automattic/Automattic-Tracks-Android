package com.automattic.android.tracks.crashlogging.internal

import android.app.Activity
import android.app.Application
import android.app.Application.ActivityLifecycleCallbacks
import android.os.Bundle
import androidx.fragment.app.FragmentActivity
import io.sentry.IHub
import io.sentry.Integration
import io.sentry.SentryLevel.DEBUG
import io.sentry.SentryOptions
import io.sentry.android.core.SentryAndroidOptions
import java.io.Closeable

internal class FragmentLifecycleIntegration(
    private val application: Application
) : ActivityLifecycleCallbacks, Integration, Closeable {

    private lateinit var hub: IHub
    private lateinit var options: SentryAndroidOptions

    override fun register(hub: IHub, options: SentryOptions) {
        this.options = options as SentryAndroidOptions
        this.hub = hub

        this.options
            .logger
            .log(
                DEBUG,
                "FragmentLifecycleIntegration enabled: %s",
                this.options.isEnableActivityLifecycleBreadcrumbs
            )
        if (this.options.isEnableActivityLifecycleBreadcrumbs) {
            application.registerActivityLifecycleCallbacks(this)
            this.options.logger.log(DEBUG, "FragmentLifecycleIntegration installed.")
        }
    }

    override fun close() {
        application.unregisterActivityLifecycleCallbacks(this)
        options.logger.log(DEBUG, "ActivityLifecycleIntegration removed.")
    }

    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
        (activity as? FragmentActivity)?.let { fragmentActivity ->
            fragmentActivity.supportFragmentManager.registerFragmentLifecycleCallbacks(
                SentryFragmentLifecycleCallbacks(hub, options),
                true
            )
        }
    }

    override fun onActivityStarted(activity: Activity) {
    }

    override fun onActivityResumed(activity: Activity) {
    }

    override fun onActivityPaused(activity: Activity) {
    }

    override fun onActivityStopped(activity: Activity) {
    }

    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {
    }

    override fun onActivityDestroyed(activity: Activity) {
    }
}
