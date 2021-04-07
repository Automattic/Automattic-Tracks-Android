package com.automattic.android.tracks.crashlogging

import android.app.Activity
import com.automattic.android.tracks.crashlogging.internal.SentryErrorTrackerWrapper
import com.automattic.android.tracks.fakes.FakeDataProvider
import com.automattic.android.tracks.fakes.testUser1
import com.automattic.android.tracks.fakes.testUser2
import io.sentry.SentryEvent
import io.sentry.SentryLevel
import io.sentry.SentryOptions
import io.sentry.protocol.SentryException
import io.sentry.protocol.User
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.SoftAssertions
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import java.util.Locale

class CrashLoggingTest {

    private val mockedWrapper: SentryErrorTrackerWrapper = mock()
    private val mockedContext = Activity()

    private var dataProvider = FakeDataProvider()

    private fun initialize(
        locale: Locale? = dataProvider.locale,
        enableCrashLoggingLogs: Boolean = dataProvider.enableCrashLoggingLogs,
        userHasOptOut: Boolean = dataProvider.userHasOptOut,
        shouldDropException: (String, String, String) -> Boolean = dataProvider.shouldDropException,
        extraKeys: List<String> = dataProvider.extraKeys,
        appendBeforeSendAction: (String) -> String = dataProvider.appendBeforeSendAction,
    ) {
        dataProvider = FakeDataProvider(
            locale = locale,
            enableCrashLoggingLogs = enableCrashLoggingLogs,
            userHasOptOut = userHasOptOut,
            shouldDropException = shouldDropException,
            extraKeys = extraKeys,
            appendBeforeSendAction = appendBeforeSendAction,
        )

        CrashLogging.start(
            context = mockedContext,
            dataProvider = dataProvider,
            sentryWrapper = mockedWrapper,
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
        initialize(userHasOptOut = true)

        val beforeSendResult = capturedOptions.beforeSend?.execute(SentryEvent(), null)

        assertThat(beforeSendResult).isEqualTo(null)
    }

    @Test
    fun `should send an event if user has not opted out`() {
        val testEvent = SentryEvent()
        initialize(userHasOptOut = false)

        val beforeSendResult = beforeSendModifiedEvent(capturedOptions, testEvent)

        assertThat(beforeSendResult).isEqualTo(testEvent)
    }

    @Test
    fun `should apply user tracking if user is not null`() {
        initialize()

        capturedUser.let { user ->
            SoftAssertions().apply {
                assertThat(user?.email).isEqualTo(testUser1.email)
                assertThat(user?.username).isEqualTo(testUser1.username)
                assertThat(user?.id).isEqualTo(testUser1.userID)
            }.assertAll()
        }
    }

    @Test
    fun `should not apply user tracking after initialization if user is null`() {
        initialize()
        dataProvider.user = null

        assertThat(capturedUser).isNull()
    }

    @Test
    fun `should apply application context after initialization`() {
        val testApplicationContext = mapOf("app" to "context")
        initialize()
        CrashLogging.appendApplicationContext(testApplicationContext)

        testApplicationContext.forEach { (key, value) ->
            verify(mockedWrapper, times(1)).applyExtra(key, value)
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

    @Test
    fun `should enable logging if requested`() {
        initialize(enableCrashLoggingLogs = true)

        assertThat(capturedOptions.isDebug).isTrue
    }

    @Test
    fun `should disable logging if requested`() {
        initialize(enableCrashLoggingLogs = false)

        assertThat(capturedOptions.isDebug).isFalse
    }

    @Test
    fun `should send event with updated user on user update`() {
        initialize()
        dataProvider.user = testUser1

        assertThat(capturedUser?.username).isEqualTo(testUser1.username)

        dataProvider.user = testUser2
        assertThat(capturedUser?.username).isEqualTo(testUser2.username)
    }

    @Test
    fun `should stop sending events if user has decided to opt out`() {
        initialize(userHasOptOut = false)
        val options = capturedOptions

        assertThat(beforeSendModifiedEvent(options)).isNotNull

        dataProvider.userHasOptOut = true

        assertThat(beforeSendModifiedEvent(options)).isNull()
    }

    @Test
    fun `should drop exception from stacktrace if its defined and stacktrace contains it`() {
        val testExceptions = mutableListOf(
            DO_NOT_DROP,
            TO_DROP
        )
        initialize(shouldDropException = shouldDrop(TO_DROP))

        val event = mock<SentryEvent> {
            on { exceptions } doReturn testExceptions
        }

        val updatedEvent = beforeSendModifiedEvent(capturedOptions, event)

        assertThat(updatedEvent?.exceptions).contains(DO_NOT_DROP).doesNotContain(TO_DROP)
    }

    @Test
    fun `should append extra to event before sending it`() {
        val extraKey = "key"
        val extraValue = "value"

        initialize(extraKeys = listOf(extraKey), appendBeforeSendAction = { extraValue })

        val updatedEvent = beforeSendModifiedEvent(capturedOptions)

        assertThat(updatedEvent?.getExtra(extraKey)).isEqualTo(extraValue)
    }

    @Test
    fun `should not append extra to event if its already there`() {
        val extraKey = "key"
        val extraValue = "value"
        val updatedExtraValue = "updatedValue"
        val testEventWithExtraApplied = SentryEvent().apply { setExtra(extraKey, extraValue) }
        initialize(
            extraKeys = listOf(extraKey),
            appendBeforeSendAction = { updatedExtraValue }
        )

        val updatedEvent = beforeSendModifiedEvent(capturedOptions, testEventWithExtraApplied)

        assertThat(updatedEvent?.getExtra(extraKey)).isEqualTo(extraValue)
    }

    private val capturedOptions: SentryOptions
        get() = argumentCaptor<(SentryOptions) -> Unit>().let { captor ->
            verify(mockedWrapper).initialize(any(), captor.capture())
            SentryOptions().apply(captor.lastValue)
        }

    private val capturedUser: User?
        get() = beforeSendModifiedEvent(capturedOptions)?.user

    private val capturedEvent: SentryEvent
        get() = argumentCaptor<SentryEvent>().let { captor ->
            verify(mockedWrapper).captureEvent(captor.capture())
            captor.lastValue
        }

    private fun beforeSendModifiedEvent(
        options: SentryOptions,
        event: SentryEvent = SentryEvent(),
    ): SentryEvent? {
        return options.beforeSend?.execute(event, null)
    }

    private fun shouldDrop(exception: SentryException): (String, String, String) -> Boolean =
        { module: String, type: String, value: String ->
            exception.module == module && exception.type == type && exception.value == value
        }

    companion object {
        val TEST_THROWABLE = Throwable("test exception")

        val DO_NOT_DROP = SentryException().apply {
            module = "do"
            type = "not"
            value = "drop me"
        }

        val TO_DROP = SentryException().apply {
            module = "please"
            type = "drop"
            value = "me"
        }
    }
}
