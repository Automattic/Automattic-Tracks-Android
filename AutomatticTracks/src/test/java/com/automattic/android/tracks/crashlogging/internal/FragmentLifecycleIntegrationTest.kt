package com.automattic.android.tracks.crashlogging.internal

import android.app.Application
import io.sentry.Hub
import io.sentry.android.core.SentryAndroidOptions
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify

class FragmentLifecycleIntegrationTest {

    private class Fixture {
        val application = mock<Application>()
        val hub = mock<Hub>()
        val options = SentryAndroidOptions()

        fun getSut(): FragmentLifecycleIntegration {
            return FragmentLifecycleIntegration(application = application)
        }
    }

    private val fixture = Fixture()

    @Test
    fun `When FragmentBreadcrumbsIntegration is enabled, it registers callback`() {
        val sut = fixture.getSut()

        sut.register(fixture.hub, fixture.options)

        verify(fixture.application).registerActivityLifecycleCallbacks(sut)
    }

    @Test
    fun `When FragmentBreadcrumbsIntegration is closed, it should unregister the callback`() {
        val sut = fixture.getSut()
        sut.register(fixture.hub, fixture.options)

        sut.close()

        verify(fixture.application).unregisterActivityLifecycleCallbacks(sut)
    }
}
