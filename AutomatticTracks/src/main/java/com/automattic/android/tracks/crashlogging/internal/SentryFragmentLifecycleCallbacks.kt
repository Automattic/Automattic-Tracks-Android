package com.automattic.android.tracks.crashlogging.internal

import android.content.Context
import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentManager.FragmentLifecycleCallbacks
import io.sentry.Breadcrumb
import io.sentry.IHub
import io.sentry.SentryLevel.INFO
import io.sentry.android.core.SentryAndroidOptions

class SentryFragmentLifecycleCallbacks(
    private val hub: IHub,
    private val options: SentryAndroidOptions,
) : FragmentLifecycleCallbacks() {
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
}
