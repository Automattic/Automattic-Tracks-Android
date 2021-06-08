package com.automattic.android.tracks.crashlogging.internal

import android.content.Context
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import io.sentry.Breadcrumb
import io.sentry.Hub
import io.sentry.SentryLevel.INFO
import io.sentry.android.core.SentryAndroidOptions
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.mockito.kotlin.check
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify

class FragmentLifecycleIntegrationTest {

    private class Fixture {
        val fragmentManager = mock<FragmentManager>()
        val hub = mock<Hub>()
        val options = SentryAndroidOptions()
        val fragment = mock<Fragment>()
        val context = mock<Context>()

        fun getSut(): FragmentLifecycleIntegration {
            return FragmentLifecycleIntegration(fragmentManager)
        }
    }

    private val fixture = Fixture()

    @Test
    fun `When FragmentBreadcrumbsIntegration is enabled, it registers callback`() {
        val sut = fixture.getSut()

        sut.register(fixture.hub, fixture.options)

        verify(fixture.fragmentManager).registerFragmentLifecycleCallbacks(sut, true)
    }

    @Test
    fun `When FragmentBreadcrumbsIntegration is closed, it should unregister the callback`() {
        val sut = fixture.getSut()
        sut.register(fixture.hub, fixture.options)

        sut.close()

        verify(fixture.fragmentManager).unregisterFragmentLifecycleCallbacks(sut)
    }

    @Test
    fun `When fragment is attached, it should add breadcrumb`() {
        val sut = fixture.getSut()
        sut.register(fixture.hub, fixture.options)

        sut.onFragmentAttached(fixture.fragmentManager, fixture.fragment, fixture.context)

        verifyBreadcrumbAdded("attached")
    }

    @Test
    fun `When fragment is created, it should add breadcrumb`() {
        val sut = fixture.getSut()
        sut.register(fixture.hub, fixture.options)

        sut.onFragmentCreated(fixture.fragmentManager, fixture.fragment, savedInstanceState = null)

        verifyBreadcrumbAdded("created")
    }

    @Test
    fun `When fragments view is created, it should add breadcrumb`() {
        val sut = fixture.getSut()
        sut.register(fixture.hub, fixture.options)

        sut.onFragmentViewCreated(
            fixture.fragmentManager,
            fixture.fragment,
            v = mock(),
            savedInstanceState = null
        )

        verifyBreadcrumbAdded("view created")
    }

    @Test
    fun `When fragment started, it should add breadcrumb`() {
        val sut = fixture.getSut()
        sut.register(fixture.hub, fixture.options)

        sut.onFragmentStarted(fixture.fragmentManager, fixture.fragment)

        verifyBreadcrumbAdded("started")
    }

    @Test
    fun `When fragment resumed, it should add breadcrumb`() {
        val sut = fixture.getSut()
        sut.register(fixture.hub, fixture.options)

        sut.onFragmentResumed(fixture.fragmentManager, fixture.fragment)

        verifyBreadcrumbAdded("resumed")
    }

    @Test
    fun `When fragment paused, it should add breadcrumb`() {
        val sut = fixture.getSut()
        sut.register(fixture.hub, fixture.options)

        sut.onFragmentPaused(fixture.fragmentManager, fixture.fragment)

        verifyBreadcrumbAdded("paused")
    }

    @Test
    fun `When fragment stopped, it should add breadcrumb`() {
        val sut = fixture.getSut()
        sut.register(fixture.hub, fixture.options)

        sut.onFragmentStopped(fixture.fragmentManager, fixture.fragment)

        verifyBreadcrumbAdded("stopped")
    }

    @Test
    fun `When fragments view is destroyed, it should add breadcrumb`() {
        val sut = fixture.getSut()
        sut.register(fixture.hub, fixture.options)

        sut.onFragmentViewDestroyed(fixture.fragmentManager, fixture.fragment)

        verifyBreadcrumbAdded("view destroyed")
    }

    @Test
    fun `When fragment destroyed, it should add breadcrumb`() {
        val sut = fixture.getSut()
        sut.register(fixture.hub, fixture.options)

        sut.onFragmentDestroyed(fixture.fragmentManager, fixture.fragment)

        verifyBreadcrumbAdded("destroyed")
    }

    @Test
    fun `When fragment detached, it should add breadcrumb`() {
        val sut = fixture.getSut()
        sut.register(fixture.hub, fixture.options)

        sut.onFragmentDetached(fixture.fragmentManager, fixture.fragment)

        verifyBreadcrumbAdded("detached")
    }

    private fun verifyBreadcrumbAdded(expectedState: String) {
        verify(fixture.hub).addBreadcrumb(
            check { breadcrumb: Breadcrumb ->
                assertThat("ui.lifecycle").isEqualTo(breadcrumb.category)
                assertThat("navigation").isEqualTo(breadcrumb.type)
                assertThat(INFO).isEqualTo(breadcrumb.level)
                assertThat(expectedState).isEqualTo(breadcrumb.getData("state"))
                assertThat(fixture.fragment.javaClass.simpleName).isEqualTo(breadcrumb.getData("screen"))
            }
        )
    }
}
