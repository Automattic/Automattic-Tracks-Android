@file:OptIn(ExperimentalCoroutinesApi::class)

package com.automattic.android.experimentation.remote

import com.automattic.android.experimentation.domain.Assignments
import com.automattic.android.experimentation.domain.Variation.Treatment
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

internal class ExperimentRestClientTest {

    private val server: MockWebServer = MockWebServer()
    private lateinit var sut: ExperimentRestClient

    @Before
    fun setUp() {
        sut = ExperimentRestClient(
            urlBuilder = { _, _, _ -> server.url("/").newBuilder().build() },
            clock = { TEST_TIMESTAMP },
        )
    }

    @Test
    fun `fetching assignments from api is successful`() = runTest {
        server.enqueue(SUCCESSFUL_RESPONSE)
        val expectedResponse = Result.success(
            Assignments(
                variations = mapOf(
                    "experiment1" to Treatment("variation1"),
                    "experiment2" to Treatment("variation2"),
                ),
                timeToLive = 3600,
                fetchedAt = TEST_TIMESTAMP,
            ),
        )

        val result = sut.fetchAssignments("", emptyList())

        assertEquals(expectedResponse, result)
    }

    @Test
    fun `fetching assignments from an unavailable api is a failure`() = runTest {
        val errorCode = 503
        server.enqueue(MockResponse().setResponseCode(errorCode))

        val result = sut.fetchAssignments("", emptyList())

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()!!.message!!.contains("$errorCode"))
    }

    @Test
    fun `fetching assignments from an api with unexpected response is a failure`() = runTest {
        server.enqueue(MockResponse().setResponseCode(200).setBody("unexpected response"))

        val result = sut.fetchAssignments("", emptyList())

        assertTrue(result.isFailure)
    }

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
    }
}
