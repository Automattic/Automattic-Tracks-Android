package com.automattic.android.tracks.crashlogging

import android.app.Activity
import com.automattic.android.tracks.TracksUser
import com.automattic.android.tracks.crashlogging.internal.SentryErrorTrackerWrapper
import com.automattic.android.tracks.fakes.FakeDataProvider
import com.automattic.android.tracks.fakes.testUser
import io.sentry.SentryEvent
import io.sentry.SentryLevel
import io.sentry.SentryOptions
import io.sentry.protocol.User
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.SoftAssertions
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.mock
import org.mockito.kotlin.nullableArgumentCaptor
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import java.util.Locale

class CrashLoggingTest {

    private val mockedWrapper: SentryErrorTrackerWrapper = mock()
    private val mockedContext = Activity()

    lateinit var dataProvider: CrashLoggingDataProvider

    private fun initialize(
        currentUser: TracksUser? = null,
        userHasOptedOut: Boolean = false,
        locale: Locale? = Locale.US,
    ) {
        dataProvider = FakeDataProvider(
            currentUser = currentUser,
            userHasOptedOut = userHasOptedOut,
            locale = locale
        )

        CrashLogging.start(
            context = mockedContext,
            dataProvider = dataProvider,
            sentryWrapper = mockedWrapper
        )
    }

    @Test
    fun `should assign required arguments to options`() {
        initialize()

        capturedOptions.let { options ->
            SoftAssertions().apply {
                assertThat(options.dsn).isEqualTo(dataProvider.sentryDSN)
                assertThat(options.environment).isEqualTo(dataProvider.buildType)
                assertThat(options.release).isEqualTo(dataProvider.releaseName)
            }.assertAll()
        }
    }

    @Test
    fun `should assign language code if locale is known`() {
        initialize(locale = Locale.US)

        assertThat(capturedOptions.tags["locale"]).isEqualTo(dataProvider.locale?.language)
    }

    @Test
    fun `should assign 'unknown' if locale is not known`() {
        initialize(locale = null)

        assertThat(capturedOptions.tags["locale"]).isEqualTo("unknown")
    }

    @Test
    fun `should not send an event if user opted out`() {
        initialize(userHasOptedOut = true)

        val beforeSendResult = capturedOptions.beforeSend?.execute(SentryEvent(), null)

        assertThat(beforeSendResult).isEqualTo(null)
    }

    @Test
    fun `should send an event if user has not opted out`() {
        val testEvent = SentryEvent()
        initialize(userHasOptedOut = false)

        val beforeSendResult = capturedOptions.beforeSend?.execute(testEvent, null)

        assertThat(beforeSendResult).isEqualTo(testEvent)
    }

    @Test
    fun `should apply user tracking after initialization if user is not null`() {
        initialize(currentUser = testUser)

        capturedUser.let { user ->
            SoftAssertions().apply {
                assertThat(user?.email).isEqualTo(testUser.email)
                assertThat(user?.username).isEqualTo(testUser.username)
                assertThat(user?.others?.get("userID")).isEqualTo(testUser.userID)
                assertThat(user?.others).containsAllEntriesOf(dataProvider.userContext)
            }.assertAll()
        }
    }

    @Test
    fun `should not apply user tracking after initialization if user is null`() {
        initialize(currentUser = null)

        assertThat(capturedUser).isNull()
    }

    @Test
    fun `should clear breadcrumbs on initialization`() {
        initialize()

        verify(mockedWrapper, times(1)).clearBreadcrumbs()
    }

    @Test
    fun `should apply application context after initialization`() {
        initialize()

        dataProvider.applicationContext.forEach { (key, value) ->
            verify(mockedWrapper, times(1)).applyExtra(key, value.orEmpty())
        }
    }

    @Test
    fun `should log exception`() {
        initialize()

        CrashLogging.log(TEST_THROWABLE)

        verify(mockedWrapper, times(1)).captureException(TEST_THROWABLE)
    }

    @Test
    fun `should log exception with additional data`() {
        val additionalData = mapOf<String, String?>("additional" to "data", "another" to "extra")
        initialize()

        CrashLogging.log(TEST_THROWABLE, additionalData)

        capturedEvent.let { event ->
            SoftAssertions().apply {
                assertThat(event.message?.message).isEqualTo(TEST_THROWABLE.message)
                assertThat(event.level).isEqualTo(SentryLevel.ERROR)
                additionalData.forEach { additionalDataEntry ->
                    assertThat(event.getExtra(additionalDataEntry.key))
                        .isEqualTo(additionalDataEntry.value)
                }
            }.assertAll()
        }
    }

    @Test
    fun `should log message`() {
        val testMessage = "test message"
        initialize()

        CrashLogging.log(testMessage)

        verify(mockedWrapper, times(1)).captureMessage(testMessage)
    }

    private val capturedOptions: SentryOptions
        get() = argumentCaptor<(SentryOptions) -> Unit>().let { captor ->
            verify(mockedWrapper).initialize(any(), captor.capture())
            SentryOptions().apply(captor.lastValue)
        }

    private val capturedUser: User?
        get() = nullableArgumentCaptor<User>().let { captor ->
            verify(mockedWrapper).setUser(captor.capture())
            captor.lastValue
        }

    private val capturedEvent: SentryEvent
        get() = argumentCaptor<SentryEvent>().let { captor ->
            verify(mockedWrapper).captureEvent(captor.capture())
            captor.lastValue
        }

    companion object {
        val TEST_THROWABLE = Throwable("test exception")
    }
}
