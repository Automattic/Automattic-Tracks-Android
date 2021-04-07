package com.automattic.android.tracks.crashlogging

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.automattic.android.tracks.fakes.FakeDataProvider
import com.automattic.android.tracks.fakes.testUser1
import com.automattic.android.tracks.fakes.testUser2
import org.junit.AfterClass
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.lang.NullPointerException

/**
 * This class is *not* a test in a formal way. This is a helper tool for making it easier to send
 * events by Tracks. Correctness assertion is done by a developer by visiting Sentry dashboard.
 *
 * To start using this class, please change a `sentryTestProjectDSN` property in gradle.properties
 * to DSN of a test project. Then run this class. In a few seconds you should expect 3 new events
 * on Sentry dashboard.
 *
 * Troubleshooting: if running this "test" results with `Test framework quit unexpectedly` make
 * sure, that `Include Extra Params from Gradle build file` under `Instrumentation arguments`
 * in run configuration is unchecked.
 */
@RunWith(AndroidJUnit4::class)
class SendEventsToSentry {

    private val dataProvider = FakeDataProvider(user = testUser1)
    private lateinit var crashLogging: CrashLogging

    @Before
    fun setUp() {
        crashLogging = SentryCrashLogging(
            context = InstrumentationRegistry.getInstrumentation().context,
            dataProvider = dataProvider,
        )
    }

    @Test
    fun logWithData() {
        crashLogging.log(LogWithDataException, data = mapOf("test key" to "test value"))
    }

    @Test
    fun logWithException() {
        crashLogging.log(Log)
    }

    @Test
    fun logMessage() {
        crashLogging.log("This is test message")
    }

    @Test
    fun logMessageWithUpdatedUser() {
        dataProvider.user = testUser1
        crashLogging.log(NullPointerException())

        dataProvider.user = testUser2
        crashLogging.log(NullPointerException())
    }

    @Test
    fun logMessageWithAppendedApplicationContext() {
        crashLogging.appendApplicationContext(mapOf("1 application" to "context"))
        crashLogging.log(OutOfMemoryError())

        crashLogging.appendApplicationContext(mapOf("2 application" to "context"))
        crashLogging.log(OutOfMemoryError())

        crashLogging.appendApplicationContext(mapOf("1 application" to "updated context"))
        crashLogging.log(OutOfMemoryError())
    }

    companion object {
        @AfterClass
        @JvmStatic
        fun tearDown() {
            waitForEventsToBeSent()
        }

        private fun waitForEventsToBeSent() {
            Thread.sleep(5000)
        }
    }

    object LogWithDataException : Exception()
    object Log : Exception()
}
