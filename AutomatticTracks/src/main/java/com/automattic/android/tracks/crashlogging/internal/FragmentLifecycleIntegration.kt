package com.automattic.android.tracks.crashlogging.internal

import android.content.Context
import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentManager.FragmentLifecycleCallbacks
import io.sentry.Breadcrumb
import io.sentry.IHub
import io.sentry.Integration
import io.sentry.SentryLevel.DEBUG
import io.sentry.SentryLevel.INFO
import io.sentry.SentryOptions
import io.sentry.android.core.SentryAndroidOptions
import java.io.Closeable

class FragmentLifecycleIntegration(
    private val fragmentManager: FragmentManager
) : FragmentLifecycleCallbacks(), Integration, Closeable {
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
            fragmentManager.registerFragmentLifecycleCallbacks(this, true)
            this.options.logger.log(DEBUG, "FragmentLifecycleIntegration installed.")
        }
    }

    override fun close() {
        fragmentManager.unregisterFragmentLifecycleCallbacks(this)
        options.logger.log(DEBUG, "ActivityLifecycleIntegration removed.")
    }

    private fun addBreadcrumb(fragment: Fragment, state: String) {
        if (options.isEnableActivityLifecycleBreadcrumbs) {
            val breadcrumb = Breadcrumb().apply {
                type = "navigation"
                setData("state", state)
                setData("screen", getFragmentName(fragment))
                category = "ui.lifecycle"
                level = INFO
            }
            hub.addBreadcrumb(breadcrumb)
        }
    }

    private fun getFragmentName(fragment: Fragment): String {
        return fragment.javaClass.simpleName
    }

    override fun onFragmentAttached(fm: FragmentManager, f: Fragment, context: Context) {
        addBreadcrumb(f, "attached")
    }

    override fun onFragmentCreated(fm: FragmentManager, f: Fragment, savedInstanceState: Bundle?) {
        addBreadcrumb(f, "created")
    }

    override fun onFragmentViewCreated(
        fm: FragmentManager,
        f: Fragment,
        v: View,
        savedInstanceState: Bundle?
    ) {
        addBreadcrumb(f, "view created")
    }

    override fun onFragmentStarted(fm: FragmentManager, f: Fragment) {
        addBreadcrumb(f, "started")
    }

    override fun onFragmentResumed(fm: FragmentManager, f: Fragment) {
        addBreadcrumb(f, "resumed")
    }

    override fun onFragmentPaused(fm: FragmentManager, f: Fragment) {
        addBreadcrumb(f, "paused")
    }

    override fun onFragmentStopped(fm: FragmentManager, f: Fragment) {
        addBreadcrumb(f, "stopped")
    }

    override fun onFragmentViewDestroyed(fm: FragmentManager, f: Fragment) {
        addBreadcrumb(f, "view destroyed")
    }

    override fun onFragmentDestroyed(fm: FragmentManager, f: Fragment) {
        addBreadcrumb(f, "destroyed")
    }

    override fun onFragmentDetached(fm: FragmentManager, f: Fragment) {
        addBreadcrumb(f, "detached")
    }
}
