package com.automattic.android.tracks.crashlogging

import android.app.Application
import com.automattic.android.tracks.crashlogging.internal.SentryCrashLogging
import com.automattic.android.tracks.crashlogging.internal.SentryErrorTrackerWrapper
import com.automattic.android.tracks.fakes.FakeDataProvider
import com.automattic.android.tracks.fakes.testUser1
import com.automattic.android.tracks.fakes.testUser2
import io.sentry.Breadcrumb
import io.sentry.Hint
import io.sentry.SentryEvent
import io.sentry.SentryLevel
import io.sentry.SentryOptions
import io.sentry.protocol.SentryException
import io.sentry.protocol.User
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.SoftAssertions
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.atLeast
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.nullableArgumentCaptor
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import java.util.Locale

@OptIn(ExperimentalCoroutinesApi::class)
class SentryCrashLoggingTest {

    private val mockedWrapper: SentryErrorTrackerWrapper = mock()
    private val mockedContext = Application()

    private var dataProvider = FakeDataProvider()

    private lateinit var crashLogging: SentryCrashLogging

    private val testScope = TestScope(UnconfinedTestDispatcher())

    private fun initialize(
        locale: Locale? = dataProvider.locale,
        enableCrashLoggingLogs: Boolean = dataProvider.enableCrashLoggingLogs,
        crashLoggingEnabled: Boolean = dataProvider.crashLoggingEnabled,
        shouldDropException: (String, String, String) -> Boolean = dataProvider.shouldDropException,
        extraKeys: List<String> = dataProvider.extraKeys,
        provideExtrasForEvent: (Map<ExtraKnownKey, String>) -> Map<ExtraKnownKey, String> = dataProvider.provideExtrasForEvent,
        applicationContext: Map<String, String> = dataProvider.fakeApplicationContextEmitter.value
    ) {
        dataProvider = FakeDataProvider(
            locale = locale,
            enableCrashLoggingLogs = enableCrashLoggingLogs,
            crashLoggingEnabled = crashLoggingEnabled,
            shouldDropException = shouldDropException,
            extraKeys = extraKeys,
            provideExtrasForEvent = provideExtrasForEvent,
            initialApplicationContext = applicationContext
        )

        crashLogging = SentryCrashLogging(
            application = mockedContext,
            dataProvider = dataProvider,
            sentryWrapper = mockedWrapper,
            applicationScope = testScope.backgroundScope
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
    fun `should not send an event if crash logging is disabled`() {
        initialize(crashLoggingEnabled = false)

        val beforeSendResult = capturedOptions.beforeSend?.execute(SentryEvent(), Hint())

        assertThat(beforeSendResult).isEqualTo(null)
    }

    @Test
    fun `should send an event if crash logging is enabled`() {
        val testEvent = SentryEvent()
        initialize(crashLoggingEnabled = true)

        val beforeSendResult = beforeSendModifiedEvent(capturedOptions, testEvent)

        assertThat(beforeSendResult).isEqualTo(testEvent)
    }

    @Test
    fun `should apply user tracking if user is not null`() = testScope.runTest {
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
    fun `should not apply user tracking after initialization if user is null`() = runBlocking {
        initialize()
        dataProvider.fakeUserEmitter.emit(null)

        assertThat(capturedUser).isNull()
    }

    @Test
    fun `should apply application context to event tags`(): Unit = runBlocking {
        val testApplicationContext = mapOf("app" to "context")
        dataProvider.fakeApplicationContextEmitter.emit(testApplicationContext)
        initialize()

        assertThat(capturedApplicationContext).isEqualTo(testApplicationContext)
    }

    @Test
    fun `should update application context before sending new event if context has been changed`() =
        testScope.runTest {
            val testApplicationContext = mapOf("app" to "context", "another" to "value")
            val updatedApplicationContext = mapOf("app" to "updated context", "another" to "value")
            initialize()

            dataProvider.fakeApplicationContextEmitter.emit(testApplicationContext)
            dataProvider.fakeApplicationContextEmitter.emit(updatedApplicationContext)

            assertThat(capturedApplicationContext).isEqualTo(updatedApplicationContext)
        }

    @Test
    fun `should sent a report with exception`() {
        initialize()

        crashLogging.sendReport(TEST_THROWABLE)

        assertThat(capturedEvent.throwable).isEqualTo(TEST_THROWABLE)
    }

    @Test
    fun `should send a report with exception, tags and message`() {
        val additionalData = mapOf("additional" to "data", "another" to "extra")
        val testMessage = "test message"
        initialize()

        crashLogging.sendReport(TEST_THROWABLE, tags = additionalData, message = testMessage)

        capturedEvent.let { event ->
            SoftAssertions().apply {
                assertThat(event.message?.message).isEqualTo(testMessage)
                assertThat(event.level).isEqualTo(SentryLevel.ERROR)
                additionalData.forEach { additionalDataEntry ->
                    assertThat(event.getTag(additionalDataEntry.key))
                        .isEqualTo(additionalDataEntry.value)
                }
            }.assertAll()
        }
    }

    @Test
    fun `should send a report with message`() {
        val testMessage = "test message"
        initialize()

        crashLogging.sendReport(message = testMessage)

        capturedEvent.let { event ->
            SoftAssertions().apply {
                assertThat(event.message?.message).isEqualTo(testMessage)
                assertThat(event.level).isEqualTo(SentryLevel.INFO)
            }.assertAll()
        }
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
    fun `should send event with updated user on user update`(): Unit = runBlocking {
        initialize()
        dataProvider.fakeUserEmitter.emit(testUser1)

        assertThat(capturedUser?.username).isEqualTo(testUser1.username)

        dataProvider.fakeUserEmitter.emit(testUser2)
        assertThat(capturedUser?.username).isEqualTo(testUser2.username)
    }

    @Test
    fun `should stop sending events if client has decided to disable crash logging`() {
        initialize(crashLoggingEnabled = true)
        val options = capturedOptions

        assertThat(beforeSendModifiedEvent(options)).isNotNull

        dataProvider.crashLoggingEnabled = false

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

        initialize(
            extraKeys = listOf(extraKey),
            provideExtrasForEvent = { mapOf(extraKey to extraValue) }
        )

        val updatedEvent = beforeSendModifiedEvent(capturedOptions)

        assertThat(updatedEvent?.getExtra(extraKey)).isEqualTo(extraValue)
    }

    @Test
    fun `should return a null for extra key value if value is not applied`() {
        val extraKey = "key"
        initialize(extraKeys = listOf(extraKey))

        val updatedEvent = beforeSendModifiedEvent(capturedOptions)

        assertThat(requireNotNull(updatedEvent).getExtra(extraKey)).isNull()
    }

    @Test
    fun `should not modify events if the client disabled crash logging`() {
        initialize(crashLoggingEnabled = false)
        val testEvent: SentryEvent = mock()

        capturedOptions.beforeSend?.execute(testEvent, Hint())

        verifyNoInteractions(testEvent)
    }

    @Test
    fun `should map empty values of last exception bundled with an event`() {
        val mockedShouldDropException = mock<(String, String, String) -> Boolean>()
        whenever(mockedShouldDropException.invoke(any(), any(), any())).thenReturn(true)
        dataProvider.shouldDropException = mockedShouldDropException
        initialize()

        val event = SentryEvent().apply {
            exceptions = mutableListOf(
                SentryException().apply {
                    module = null
                    type = null
                    value = null
                }
            )
        }

        capturedOptions.beforeSend?.execute(event, Hint())

        verify(mockedShouldDropException, times(1)).invoke("", "", "")
    }

    @Test
    fun `should record exception to breadcrumbs`() {
        initialize()

        crashLogging.recordException(TEST_THROWABLE)

        capturedBreadcrumb.let { breadcrumb ->
            SoftAssertions().apply {
                assertThat(breadcrumb.message).isEqualTo(TEST_THROWABLE.toString())
                assertThat(breadcrumb.type).isEqualTo("error")
                assertThat(breadcrumb.category).isEqualTo(null)
                assertThat(breadcrumb.level).isEqualTo(SentryLevel.ERROR)
            }
        }
    }

    @Test
    fun `should record event with message and category to breadcrumbs`() {
        val testMessage = "test message"
        val testCategory = "test category"
        initialize()

        crashLogging.recordEvent(message = testMessage, category = testCategory)

        capturedBreadcrumb.let { breadcrumb ->
            SoftAssertions().apply {
                assertThat(breadcrumb.message).isEqualTo(testMessage)
                assertThat(breadcrumb.type).isEqualTo("default")
                assertThat(breadcrumb.category).isEqualTo(testCategory)
                assertThat(breadcrumb.level).isEqualTo(SentryLevel.INFO)
            }
        }
    }

    @Test
    fun `should apply single event tags`() = runBlocking {
        initialize()
        val eventTags = mapOf("event" to "tags")

        crashLogging.sendReport(tags = eventTags)
        val updatedEvent = beforeSendModifiedEvent(capturedOptions, event = capturedEvent)

        SoftAssertions().apply {
            (eventTags).forEach { (key, value) ->
                assertThat(updatedEvent?.getTag(key)).isEqualTo(value)
            }
        }.assertAll()
    }

    private val capturedOptions: SentryOptions
        get() = argumentCaptor<(SentryOptions) -> Unit>().let { captor ->
            verify(mockedWrapper).initialize(any(), captor.capture())
            SentryOptions().apply(captor.lastValue)
        }

    private val capturedUser: User?
        get() = nullableArgumentCaptor<User>().let { captor ->
            verify(mockedWrapper, atLeast(0)).setUser(captor.capture())
            captor.lastValue
        }

    private val capturedApplicationContext: Map<String, String>
        get() = argumentCaptor<Map<String, String>>().let { captor ->
            verify(mockedWrapper, atLeast(0)).setTags(captor.capture())
            captor.lastValue
        }

    private val capturedEvent: SentryEvent
        get() = argumentCaptor<SentryEvent>().let { captor ->
            verify(mockedWrapper).captureEvent(captor.capture())
            captor.lastValue
        }

    private val capturedBreadcrumb
        get() = argumentCaptor<Breadcrumb>().let { captor ->
            verify(mockedWrapper).addBreadcrumb(captor.capture())
            captor.lastValue
        }

    private fun beforeSendModifiedEvent(
        options: SentryOptions,
        event: SentryEvent = SentryEvent()
    ): SentryEvent? {
        return options.beforeSend?.execute(event, Hint())
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
