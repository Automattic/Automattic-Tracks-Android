package com.automattic.android.tracks.crashlogging

import android.app.Activity
import com.automattic.android.tracks.TracksUser
import com.automattic.android.tracks.fakes.FakeDataProvider
import com.automattic.android.tracks.fakes.testUser
import io.sentry.SentryEvent
import io.sentry.SentryLevel
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.SoftAssertions
import org.junit.Test

class CrashLoggingTest {

    val fakeProxy = FakeSentryErrorTrackerProxy()
    val mockedContext = Activity()

    lateinit var dataProvider: CrashLoggingDataProvider

    private fun initialize(
        currentUser: TracksUser? = null,
        userHasOptedOut: Boolean = false
    ) {
        dataProvider = FakeDataProvider(currentUser = currentUser, userHasOptedOut = userHasOptedOut)

        CrashLogging.start(
            context = mockedContext,
            dataProvider = dataProvider,
            sentryProxy = fakeProxy
        )
    }

    @Test
    fun `should assign required arguments to options`() {
        initialize()

        fakeProxy.sentryOptions.let { options ->
            SoftAssertions().apply {
                assertThat(options.dsn).isEqualTo(dataProvider.sentryDSN)
                assertThat(options.environment).isEqualTo(dataProvider.buildType)
                assertThat(options.release).isEqualTo(dataProvider.releaseName)
                assertThat(options.tags["locale"]).isEqualTo(dataProvider.locale?.language)
            }.assertAll()
        }
    }

    @Test
    fun `should not send an event if user opted out`() {
        initialize(userHasOptedOut = true)

        val beforeSendResult = fakeProxy.sentryOptions.beforeSend?.execute(SentryEvent(), null)

        assertThat(beforeSendResult).isEqualTo(null)
    }

    @Test
    fun `should send an event if user has not opted out`() {
        val testEvent = SentryEvent()
        initialize(userHasOptedOut = false)

        val beforeSendResult = fakeProxy.sentryOptions.beforeSend?.execute(testEvent, null)

        assertThat(beforeSendResult).isEqualTo(testEvent)
    }

    @Test
    fun `should apply user tracking after initialization if user is not null`() {
        initialize(currentUser = testUser)

        fakeProxy.currentUser.let { user ->
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

        assertThat(fakeProxy.currentUser).isNull()
    }

    @Test
    fun `should clear breadcrumbs on initialisation`() {
        initialize()

        assertThat(fakeProxy.clearBreadcrumbsCounter).isEqualTo(1)
    }

    @Test
    fun `should apply application context after initialization`() {
        initialize()

        assertThat(fakeProxy.extras).isEqualTo(dataProvider.applicationContext)
    }

    @Test
    fun `should capture exception`() {
        initialize()

        CrashLogging.log(TEST_THROWABLE)

        assertThat(fakeProxy.capturedException).isEqualTo(TEST_THROWABLE)
    }

    @Test
    fun `should capture exception with additional data`() {
        val additionalData = mapOf<String, String?>("additional" to "data", "another" to "extra")
        initialize()

        CrashLogging.log(TEST_THROWABLE, additionalData)

        fakeProxy.capturedEvent.let { event ->
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
    fun `should capture message`() {
        val testMessage = "test message"
        initialize()

        CrashLogging.log(testMessage)

        assertThat(fakeProxy.capturedMessage).isEqualTo(testMessage)
    }

    companion object {
        val TEST_THROWABLE = Throwable("test exception")
    }
}
