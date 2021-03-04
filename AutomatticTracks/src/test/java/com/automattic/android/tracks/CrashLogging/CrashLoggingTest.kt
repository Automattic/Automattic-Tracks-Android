package com.automattic.android.tracks.CrashLogging

import com.automattic.android.tracks.TracksUser
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.*
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class CrashLoggingTest {

    lateinit var dataProvider: CrashLoggingDataProvider
    var user: TracksUser? = null

    val testUser = TracksUser(
            "testUserId",
            "testEmail",
            "testUsername"
    )

    private fun initialize(currentUser: TracksUser? = null) {

        user = if (currentUser != null) spy(currentUser) else null

        dataProvider = spy(FakeDataProvider(currentUser = user))

        CrashLogging.start(
                context = RuntimeEnvironment.systemContext,
                dataProvider = dataProvider
        )
    }

    @Test(expected = UnsupportedOperationException::class)
    fun shouldThrowExceptionOnRequest() {
        CrashLogging.crash()
    }

    @Test
    fun `should call for required arguments during initialisation`() {
        initialize()

        verify(dataProvider, times(1)).sentryDSN
        verify(dataProvider, times(1)).buildType
        verify(dataProvider, times(1)).releaseName
        verify(dataProvider, times(1)).locale
    }

    @Test
    fun `should not apply user tracking after initialization if user is null`() {
        initialize()

        verify(dataProvider, never()).userContext
    }

    @Test
    fun `should apply user tracking after initialization if user is not null`() {
        initialize(currentUser = testUser)

        verify(user, times(1))?.email
        verify(user, times(1))?.username
        verify(dataProvider, times(1)).userContext
    }

    @Test
    fun `should apply sentry context after initialization`() {
        initialize()

        verify(dataProvider, times(1)).applicationContext
    }
}
