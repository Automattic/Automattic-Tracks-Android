package com.automattic.android.experimentation

import com.automattic.android.experimentation.domain.Assignments
import com.automattic.android.experimentation.domain.AssignmentsValidator
import com.automattic.android.experimentation.domain.Clock
import com.automattic.android.experimentation.domain.Variation
import com.automattic.android.experimentation.domain.Variation.Control
import com.automattic.android.experimentation.domain.Variation.Treatment
import com.automattic.android.experimentation.local.FileBasedCache
import com.automattic.android.experimentation.remote.ExPlatUrlBuilder
import com.automattic.android.experimentation.remote.ExperimentRestClient
import com.automattic.android.experimentation.remote.MockWebServerUrlBuilder
import com.automattic.android.experimentation.repository.AssignmentsRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.Test
import kotlin.io.path.createTempDirectory

@ExperimentalCoroutinesApi
internal class ExPlatTest {
    private val server: MockWebServer = MockWebServer()
    private val platform = "wpandroid"
    private lateinit var tempCache: FileBasedCache

    @Test
    fun `initializing in case of empty cache is fetches assignments`() = runTest {
        enqueueSuccessfulNetworkResponse()
        val exPlat = createExPlat(init = {})

        exPlat.initialize(anonymousId)
        runCurrent()

        assertThat(tempCache.latest).isEqualTo(testAssignment)
    }

    @Test
    fun `initializing in case of stale cache updates it`() = runTest {
        enqueueSuccessfulNetworkResponse()
        val exPlat = createExPlat(clock = { 3601 }, init = { })
        tempCache.saveAssignments(testAssignment.copy(timeToLive = 3600, fetchedAt = 0))

        exPlat.initialize(anonymousId)
        runCurrent()

        assertThat(tempCache.latest).isEqualTo(
            testAssignment.copy(fetchedAt = 3601),
        )
    }

    @Test
    fun `initializing in case of fresh cache doesn't fetch new assignments`() = runTest {
        enqueueSuccessfulNetworkResponse()
        val exPlat = createExPlat(clock = { 3599 }, init = { })
        tempCache.saveAssignments(testAssignment.copy(timeToLive = 3600))

        exPlat.initialize(anonymousId)
        runCurrent()

        assertThat(tempCache.latest).isEqualTo(
            testAssignment,
        )
    }

    @Test
    fun `clearing removes cached data`() = runTest {
        val exPlat = createExPlat()
        tempCache.saveAssignments(testAssignment)

        exPlat.clear()
        runCurrent()

        assertThat(tempCache.latest).isNull()
    }

    /*
    This scenario is about returning the same variation during a single session.
    We don't want SDK consumers to mix variations during the same session,
    as it could lead to unexpected behavior.
     */
    @Test
    fun `getting variation for the second time returns the same value, even if cache was updated`() =
        runTest {
            val exPlat = createExPlat(clock = { 3601 }, init = { })
            enqueueSuccessfulNetworkResponse(variation = Treatment("variation2"))
            tempCache.saveAssignments(
                testAssignment.copy(mapOf(testExperimentName to Control), fetchedAt = 0, timeToLive = 3600),
            )
            exPlat.initialize(anonymousId)
            val firstGet = exPlat.getVariation(testExperiment)
            runCurrent() // performs fetch from initialization

            val secondGet = exPlat.getVariation(testExperiment)
            runCurrent()

            // Even though the cache was updated...
            assertThat(tempCache.latest!!.variations[testExperimentName]).isEqualTo(Treatment("variation2"))
            // ...the second `get` should return the same value as the first one
            assertThat(secondGet).isEqualTo(firstGet).isEqualTo(Control)
        }

    @Test
    fun `in debug, getting variation throws an exception experiment was not found`() = runTest {
        val exPlat = createExPlat(experiments = emptySet())

        assertThatThrownBy {
            exPlat.getVariation(testExperiment)
        }.isInstanceOf(IllegalArgumentException::class.java)
            .hasMessageContaining("experiment not found")
    }

    @Test
    fun `initializing fetches assignments`() = runTest {
        enqueueSuccessfulNetworkResponse()

        createExPlat()

        assertThat(tempCache.latest).isEqualTo(testAssignment)
    }

    /**
     * In this scenario we are testing that initializing with a different anonymous id than the one
     * currently in the cache, will fetch new assignments.
     */
    @Test
    fun `initializing with an anonymous id different than current cache, fetches assignments`() =
        runTest {
            enqueueSuccessfulNetworkResponse(variation = Treatment("variation2"))
            val exPlat = createExPlat(init = { })
            tempCache.saveAssignments(
                testAssignment.copy(mapOf(testExperimentName to Control), fetchedAt = 0),
            )

            exPlat.initialize("newId")
            runCurrent()

            assertThat(tempCache.latest).isEqualTo(
                Assignments(
                    mapOf(testExperimentName to Treatment("variation2")),
                    3600,
                    0,
                    "newId",
                ),
            )
        }

    @Test
    fun `getting variations without initializing throws an exception`() = runTest {
        val exPlat = createExPlat(init = { })

        assertThatThrownBy {
            exPlat.getVariation(testExperiment)
        }.isInstanceOf(IllegalStateException::class.java)
            .hasMessageContaining("anonymousId is null")
    }

    private val testExperimentName = "testExperiment"
    private val testVariationName = "testVariation"
    private val testExperiment = Experiment(identifier = testExperimentName)
    private val testVariation = Treatment(testVariationName)
    private val anonymousId = "id"
    private val testAssignment = Assignments(
        variations = mapOf(testExperimentName to testVariation),
        timeToLive = 3600,
        fetchedAt = 0L,
        anonymousId = anonymousId,
    )

    private fun TestScope.createExPlat(
        clock: Clock = Clock { 0 },
        experiments: Set<Experiment> = setOf(testExperiment),
        init: ExPlat.() -> Unit = {
            enqueueSuccessfulNetworkResponse()
            initialize(anonymousId)
            runCurrent()
        },
    ): ExPlat {
        val logger = object : ExperimentLogger {
            override fun d(message: String) = Unit
            override fun e(message: String, throwable: Throwable?) = Unit
        }
        val coroutineScope = this
        val dispatcher = StandardTestDispatcher(coroutineScope.testScheduler)
        val restClient = ExperimentRestClient(
            urlBuilder = MockWebServerUrlBuilder(ExPlatUrlBuilder(), server),
            dispatcher = dispatcher,
            clock = clock,
            callFactory = OkHttpClient(),
        )
        tempCache = FileBasedCache(
            createTempDirectory().toFile(),
            dispatcher = dispatcher,
            scope = coroutineScope,
            logger = logger,
            failFast = true,
        )

        return ExPlat(
            platform = platform,
            experiments = experiments,
            logger = logger,
            coroutineScope = coroutineScope,
            failFast = true,
            assignmentsValidator = AssignmentsValidator(clock = clock),
            repository = AssignmentsRepository(restClient, tempCache),
        ).apply(init)
    }

    private fun enqueueSuccessfulNetworkResponse(variation: Variation = testVariation) {
        val variationName = when (variation) {
            is Control -> "control"
            is Treatment -> variation.name
        }
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody(
                    """
                    {
                        "variations": {
                            "$testExperimentName": "$variationName"
                        },
                        "ttl": 3600
                    }
                    """.trimIndent(),
                ),
        )
    }
}
