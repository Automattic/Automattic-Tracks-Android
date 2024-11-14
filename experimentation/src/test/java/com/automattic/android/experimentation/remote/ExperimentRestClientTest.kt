@file:OptIn(ExperimentalCoroutinesApi::class)

package com.automattic.android.experimentation.remote

import com.automattic.android.experimentation.domain.Assignments
import com.automattic.android.experimentation.domain.Variation.Treatment
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import org.assertj.core.api.Assertions.assertThat
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

internal class ExperimentRestClientTest {

    private val server: MockWebServer = MockWebServer()

    @Test
    fun `fetching assignments from api is successful`() = runTest {
        val sut = buildSut(this)
        server.enqueue(SUCCESSFUL_RESPONSE)
        val expectedResponse = Result.success(
            Assignments(
                variations = mapOf(
                    "experiment1" to Treatment("variation1"),
                    "experiment2" to Treatment("variation2"),
                ),
                timeToLive = 3600,
                fetchedAt = TEST_TIMESTAMP,
                anonymousId = "id",
            ),
        )

        val result = sut.fetchAssignments("", emptyList(), anonymousId = "id", oAuthToken = null)

        assertEquals(expectedResponse, result)
    }

    @Test
    fun `fetching assignments from an unavailable api is a failure`() = runTest {
        val sut = buildSut(this)
        val errorCode = 503
        server.enqueue(MockResponse().setResponseCode(errorCode))

        val result = sut.fetchAssignments("", emptyList(), "", oAuthToken = null)

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()!!.message!!.contains("$errorCode"))
    }

    @Test
    fun `fetching assignments from an api with unexpected response is a failure`() = runTest {
        val sut = buildSut(this)
        server.enqueue(MockResponse().setResponseCode(200).setBody("unexpected response"))

        val result = sut.fetchAssignments("", emptyList(), "", oAuthToken = null)

        assertTrue(result.isFailure)
    }

    @Test
    fun `fetching assignments from an api that requires anon id is successful`() = runTest {
        val sut = buildSut(this)
        val respondOnlyOnAnonIdDispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse {
                return if (!request.requestUrl?.queryParameter("anon_id").isNullOrEmpty()) {
                    SUCCESSFUL_RESPONSE
                } else {
                    EMPTY_RESPONSE
                }
            }
        }
        server.dispatcher = respondOnlyOnAnonIdDispatcher

        val result = sut.fetchAssignments("", emptyList(), "random_id", oAuthToken = null)

        assertThat(result.getOrNull()!!.variations).isNotEmpty
    }

    @Test
    fun `fetching assignments from an unknown host is a failure without crash`() = runTest {
        val sut = buildSut(
            this,
            UnknownHostMockWebServerUrlBuilder(ExPlatUrlBuilder(), server)
        )

        val result = sut.fetchAssignments("", emptyList(), "random_id", oAuthToken = null)

        assertThat(result.isFailure).isTrue()
    }

    private fun buildSut(
        scope: TestScope,
        urlBuilder: MockWebServerUrlBuilder = MockWebServerUrlBuilder(ExPlatUrlBuilder(), server)
    ) = ExperimentRestClient(
        urlBuilder = urlBuilder,
        clock = { TEST_TIMESTAMP },
        dispatcher = StandardTestDispatcher(scope.testScheduler),
        okHttpClient = OkHttpClient(),
    )

    companion object {
        private const val TEST_TIMESTAMP = 123456789L
        private val SUCCESSFUL_RESPONSE = MockResponse().setResponseCode(200).setBody(
            """
                    {
                        "variations": {
                            "experiment1": "variation1",
                            "experiment2": "variation2"
                        },
                        "ttl": 3600
                    }
            """.trimIndent(),
        )
        private val EMPTY_RESPONSE = MockResponse().setResponseCode(200).setBody(
            """
                    {
                        "variations": {},
                        "ttl": 3600
                    }
            """.trimIndent(),
        )
    }
}
