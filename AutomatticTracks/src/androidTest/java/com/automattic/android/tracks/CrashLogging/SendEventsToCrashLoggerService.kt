package com.automattic.android.tracks.crashlogging


import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.automattic.android.tracks.TracksUser
import com.automattic.android.tracks.fakes.FakeDataProvider
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SendEventsToCrashLoggerService {

    val testUser = TracksUser(
            "testUserId",
            "testEmail",
            "testUsername"
    )

    var dataProvider: CrashLoggingDataProvider = FakeDataProvider(
            currentUser = testUser,
            sentryDSN = "DSN of Sentry test project"
    )

    @Before
    fun setUp() {
        CrashLogging.start(
                context = InstrumentationRegistry.getInstrumentation().context,
                dataProvider = dataProvider
        )
    }

    @Test
    fun logWithData() {
        CrashLogging.log(LogWithDataException, data = mapOf("test key" to "test value"))

        waitForEventToBeSent()
    }

    @Test
    fun logWithException() {
        CrashLogging.log(Log)

        waitForEventToBeSent()
    }

    @Test
    fun logMessage() {
        CrashLogging.log("This is test message")

        waitForEventToBeSent()
    }

    private fun waitForEventToBeSent() {
        Thread.sleep(5000)
    }

    object LogWithDataException : Exception()
    object Log : Exception()
}