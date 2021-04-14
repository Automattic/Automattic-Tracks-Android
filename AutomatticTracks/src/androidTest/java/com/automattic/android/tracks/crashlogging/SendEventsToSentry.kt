package com.automattic.android.tracks.crashlogging

import android.database.sqlite.SQLiteOutOfMemoryException
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.automattic.android.tracks.fakes.FakeDataProvider
import com.automattic.android.tracks.fakes.testUser1
import com.automattic.android.tracks.fakes.testUser2
import org.junit.AfterClass
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.NotActiveException
import java.lang.NullPointerException
import java.lang.invoke.WrongMethodTypeException

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
 *
 * <b> Why every event has different exception? </b>
 * Sentry dashboard merges similar events. For making navigating there easier, we apply different
 * exceptions to prevent merging.
 */
@RunWith(AndroidJUnit4::class)
class SendEventsToSentry {

    private val dataProvider = FakeDataProvider(user = testUser1)
    private lateinit var crashLogging: CrashLogging

    @Before
    fun setUp() {
        crashLogging = CrashLoggingProvider.createInstance(
            context = InstrumentationRegistry.getInstrumentation().context,
            dataProvider = dataProvider,
        )
    }

    @Test
    fun sendExceptionReportWithTags() {
        dataProvider.applicationContext = mapOf("application" to "context")
        crashLogging.sendReport(
            exception = IllegalThreadStateException(),
            tags = mapOf("test key" to "test value"),
            message = "This should report IllegalThreadStateException and add `test key: test value` and `application: context` to tags"
        )
    }

    @Test
    fun sendReportWithMessage() {
        crashLogging.sendReport(message = "This should send event with just a message")
    }

    @Test
    fun sendTwoReportsAndChangeUserBetween() {
        dataProvider.user = testUser1
        crashLogging.sendReport(
            exception = OutOfMemoryError(),
            message = "This should apply ${testUser1.userID} user"
        )

        dataProvider.user = testUser2
        crashLogging.sendReport(
            exception = NullPointerException(),
            message = "This should apply ${testUser2.userID} user"
        )
    }

    @Test
    fun sendThreeReportsAndChangeApplicationContextBetween() {
        dataProvider.applicationContext = mapOf("1 application" to "context")
        crashLogging.sendReport(
            exception = NotActiveException(),
            message = "This should show `1 application: context` tag"
        )

        dataProvider.applicationContext = mapOf("2 application" to "context")
        crashLogging.sendReport(
            exception = ExceptionInInitializerError(),
            message = "This should show `2 application: context` tag"
        )

        dataProvider.applicationContext = mapOf("1 application" to "updated context")
        crashLogging.sendReport(
            exception = WrongMethodTypeException(),
            message = "This should show `1 application: updated context` tag"
        )
    }

    @Test
    fun sendReportWithExtraDataApplied() {
        val extraKey = "key"
        dataProvider.extraKeys = listOf(extraKey)
        dataProvider.provideExtrasForEvent = { _ ->
            mapOf(extraKey to "value")
        }

        crashLogging.sendReport(
            exception = ArrayIndexOutOfBoundsException(),
            message = "This should contain `key: value` in event's extra data"
        )
    }

    @Test
    fun recordEvent() {
        crashLogging.recordEvent("custom event", "custom category")

        crashLogging.sendReport(
            exception = SQLiteOutOfMemoryException(),
            message = "This should show `custom event` with `custom category` in event's breadcrumbs"
        )
    }

    @Test
    fun recordException() {
        crashLogging.recordException(NotImplementedError())

        crashLogging.sendReport(
            exception = NegativeArraySizeException(),
            message = "This should show `NotImplementedError` in event's breadcrumbs"
        )
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
}
