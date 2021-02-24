package com.automattic.android.tracks.CrashLogging

import io.sentry.SentryEvent
import org.junit.Assert.*
import org.junit.Test

class BeforeSendCallbackTest {

    lateinit var sut: BeforeSendCallback

    private val testEvent = SentryEvent()

    @Test
    fun `should not send an event in debug`() {
        sut = BeforeSendCallback(
                debug = true,
                dataProvider = FakeDataProvider()
        )

        assertNull(sut.execute(testEvent, null))
    }

    @Test
    fun `should not send event if user has opted out and not in debug`() {
        sut = BeforeSendCallback(
                debug = false,
                dataProvider = FakeDataProvider(userOptedOut = true)
        )

        assertNull(sut.execute(testEvent, null))
    }

    @Test
    fun `should send event if user did not opt out and not in debug`() {
        sut = BeforeSendCallback(
                debug = false,
                dataProvider = FakeDataProvider(userOptedOut = false)
        )

        assertEquals(testEvent, sut.execute(testEvent, null))
    }
}